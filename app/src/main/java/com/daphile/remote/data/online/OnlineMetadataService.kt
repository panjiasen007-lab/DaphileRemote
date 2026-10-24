package com.daphile.remote.data.online

import android.content.Context
import com.daphile.remote.data.model.LyricLine
import com.daphile.remote.data.model.Lyrics
import com.daphile.remote.data.model.LyricsSource
import com.daphile.remote.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Online metadata helpers. Lyrics use LRCLIB (open, no key) and fall back to
 * NetEase's public search endpoint for Chinese catalog coverage. Artwork uses
 * MusicBrainz + Cover Art Archive (open data), then iTunes Search (free).
 */
class OnlineMetadataService(private val context: Context) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .build()

    private val lyricsDir = File(context.filesDir, "lyrics_cache").apply { mkdirs() }
    private val lyricsLibraryDir = File(context.filesDir, "lyrics").apply { mkdirs() }
    private val artworkPrefs = context.getSharedPreferences("online_artwork_cache", Context.MODE_PRIVATE)
    private val lyricsMemory = ConcurrentHashMap<String, Lyrics>()

    suspend fun lyricsFor(track: Track, force: Boolean = false): Lyrics? = withContext(Dispatchers.IO) {
        if (track.title.isBlank()) return@withContext null
        val key = hashKey("${track.artist}|${track.title}|${track.album}")
        if (!force) {
            lyricsMemory[key]?.let { return@withContext it }
            // 优先读取按歌曲目录保存的 .lrc
            readSavedLyrics(track)?.let {
                lyricsMemory[key] = it
                writeLyricsCache(key, it)
                return@withContext it
            }
            readLyricsCache(key)?.let {
                lyricsMemory[key] = it
                saveLyricsCopy(track, it)
                return@withContext it
            }
        }
        val online = fetchNetease(track) ?: fetchQqMusic(track) ?: fetchLrclib(track)
        if (online != null) {
            lyricsMemory[key] = online
            writeLyricsCache(key, online)
            saveLyricsCopy(track, online)
        }
        online
    }

    /** 按指定来源强制在线获取，成功后覆盖本地缓存与 .lrc 副本。 */
    suspend fun lyricsFrom(track: Track, source: LyricsSource): Lyrics? = withContext(Dispatchers.IO) {
        if (track.title.isBlank()) return@withContext null
        val fetched = when (source) {
            LyricsSource.AUTO -> fetchNetease(track) ?: fetchQqMusic(track) ?: fetchLrclib(track)
            LyricsSource.NETEASE -> fetchNetease(track)
            LyricsSource.QQ -> fetchQqMusic(track)
            LyricsSource.LRCLIB -> fetchLrclib(track)
        } ?: return@withContext null
        val key = hashKey("${track.artist}|${track.title}|${track.album}")
        lyricsMemory[key] = fetched
        writeLyricsCache(key, fetched)
        saveLyricsCopy(track, fetched)
        fetched
    }

    /** 本地歌词库中的 .lrc 路径，按 艺术家/专辑/曲名 归档，便于随音乐一起备份。 */
    fun lyricsFileFor(track: Track): File = File(
        File(File(lyricsLibraryDir, safeName(track.artist.ifBlank { "未知艺术家" })), safeName(track.album.ifBlank { "未知专辑" })),
        "${safeName(track.title)}.lrc"
    )

    /** 已保存歌词的文件路径，尚未保存时返回 null。 */
    fun savedLyricsPath(track: Track): String? =
        lyricsFileFor(track).takeIf { it.exists() }?.absolutePath

    suspend fun saveLyrics(track: Track, lyrics: Lyrics): File = withContext(Dispatchers.IO) {
        saveLyricsCopy(track, lyrics)
    }

    fun lyricsLibraryPath(): String = lyricsLibraryDir.absolutePath

    fun lyricsLibraryCount(): Int =
        lyricsLibraryDir.walkTopDown().count { it.isFile && it.extension.equals("lrc", true) }

    /** 清空本地歌词库与在线歌词缓存，返回删除的文件数。 */
    fun clearLyricsLibrary(): Int {
        var removed = 0
        runCatching {
            lyricsLibraryDir.deleteRecursively()
            lyricsDir.deleteRecursively()
        }
        lyricsLibraryDir.mkdirs()
        lyricsDir.mkdirs()
        lyricsMemory.clear()
        // 统计删除数量仅用于提示，目录已重建
        removed = lyricsLibraryCount()
        return removed
    }

    private fun readSavedLyrics(track: Track): Lyrics? {
        val file = lyricsFileFor(track)
        if (!file.exists()) return null
        return runCatching {
            buildLyrics(file.readText(), "本地歌词")
        }.getOrNull()
    }

    private fun saveLyricsCopy(track: Track, lyrics: Lyrics): File {
        val file = lyricsFileFor(track)
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(lyrics.plainText.ifBlank { lyrics.lines.joinToString("\n") { "[${formatTime(it.timeMs)}]${it.text}" } })
        }
        return file
    }

    private fun formatTime(timeMs: Long): String {
        val minutes = timeMs / 60_000
        val seconds = (timeMs % 60_000) / 1000
        val centis = (timeMs % 1000) / 10
        return String.format(Locale.US, "%02d:%02d.%02d", minutes, seconds, centis)
    }

    private fun safeName(value: String): String {
        val cleaned = value
            .replace(Regex("""[\\/:*?"<>|]"""), "_")
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd('.')
        return cleaned.ifBlank { "未命名" }.take(80)
    }

    fun parseLyricsText(raw: String, source: String): Lyrics? = buildLyrics(raw, source)

    suspend fun coverFor(artist: String, album: String, force: Boolean = false): String? =
        withContext(Dispatchers.IO) {
            if (album.isBlank()) return@withContext null
            val key = hashKey("${artist.trim()}|${album.trim()}")
            if (!force) {
                val cached = artworkPrefs.getString(key, null)
                if (cached != null) {
                    return@withContext cached.takeIf { it.isNotBlank() && it != NONE }
                }
            }
            val resolved = fetchMusicBrainzCover(artist, album)
                ?: fetchNeteaseCover(artist, album)
                ?: fetchQqCover(artist, album)
                ?: fetchItunesCover(artist, album)
            artworkPrefs.edit().putString(key, resolved ?: NONE).apply()
            resolved
        }

    private fun fetchLrclib(track: Track): Lyrics? {
        return try {
            val url = "https://lrclib.net/api/search".toHttpUrl().newBuilder()
                .addQueryParameter("track_name", cleanTitle(track.title, track.artist))
                .addQueryParameter("artist_name", track.artist)
                .apply {
                    if (track.album.isNotBlank()) addQueryParameter("album_name", track.album)
                }
                .build()
            val body = get(url.toString(), mapOf("User-Agent" to USER_AGENT)) ?: return null
            val array = JSONArray(body)
            if (array.length() == 0) return null
            val candidates = (0 until array.length()).mapNotNull { array.optJSONObject(it) }
            val best = candidates
                .sortedByDescending { item ->
                    var score = 0
                    if (item.optString("syncedLyrics").isNotBlank()) score += 100
                    if (item.optString("plainLyrics").isNotBlank()) score += 10
                    val duration = item.optDouble("duration", 0.0)
                    if (track.duration > 0 && duration > 0) {
                        val diff = abs(duration - track.duration)
                        if (diff < 3) score += 30 else if (diff < 15) score += 10
                    }
                    score
                }
                .firstOrNull()
                ?: return null
            buildLyrics(
                best.optString("syncedLyrics").ifBlank { best.optString("plainLyrics") },
                "LRCLIB"
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun fetchNetease(track: Track): Lyrics? {
        return try {
            val query = listOf(track.artist, cleanTitle(track.title, track.artist))
                .filter { it.isNotBlank() && it != "无艺人名" }
                .joinToString(" ")
            if (query.isBlank()) return null
            val searchUrl = "https://music.163.com/api/search/get/web".toHttpUrl().newBuilder()
                .addQueryParameter("csrf_token", "")
                .addQueryParameter("s", query)
                .addQueryParameter("type", "1")
                .addQueryParameter("offset", "0")
                .addQueryParameter("total", "true")
                .addQueryParameter("limit", "5")
                .build()
            val searchBody = get(
                searchUrl.toString(),
                mapOf(
                    "User-Agent" to DESKTOP_UA,
                    "Referer" to "https://music.163.com/"
                )
            ) ?: return null
            val songs = JSONObject(searchBody)
                .optJSONObject("result")
                ?.optJSONArray("songs")
                ?: return null
            if (songs.length() == 0) return null
            val song = songs.optJSONObject(0) ?: return null
            val songId = song.optLong("id")
            if (songId <= 0) return null
            val lyricUrl = "https://music.163.com/api/song/lyric".toHttpUrl().newBuilder()
                .addQueryParameter("id", songId.toString())
                .addQueryParameter("lv", "1")
                .addQueryParameter("kv", "1")
                .addQueryParameter("tv", "-1")
                .build()
            val lyricBody = get(
                lyricUrl.toString(),
                mapOf(
                    "User-Agent" to DESKTOP_UA,
                    "Referer" to "https://music.163.com/"
                )
            ) ?: return null
            val root = JSONObject(lyricBody)
            val text = root.optJSONObject("lrc")?.optString("lyric").orEmpty()
                .ifBlank { root.optJSONObject("tlyric")?.optString("lyric").orEmpty() }
            if (text.isBlank()) return null
            buildLyrics(text, "网易云音乐")
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * QQ 音乐：先用 smartbox 搜索取歌曲 mid，再请求歌词。
     * 该接口无需鉴权，但需要 y.qq.com 的 Referer。
     */
    private fun fetchQqMusic(track: Track): Lyrics? {
        return try {
            val songMid = qqSongMid(track) ?: return null
            val url = "https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg".toHttpUrl().newBuilder()
                .addQueryParameter("songmid", songMid)
                .addQueryParameter("format", "json")
                .addQueryParameter("nobase64", "1")
                .addQueryParameter("g_tk", "5381")
                .addQueryParameter("loginUin", "0")
                .addQueryParameter("hostUin", "0")
                .addQueryParameter("inCharset", "utf8")
                .addQueryParameter("outCharset", "utf-8")
                .addQueryParameter("notice", "0")
                .addQueryParameter("platform", "yqq.json")
                .addQueryParameter("needNewCode", "0")
                .build()
            val body = get(url.toString(), QQ_HEADERS) ?: return null
            val text = JSONObject(body).optString("lyric")
            if (text.isBlank()) return null
            buildLyrics(text, "QQ 音乐")
        } catch (_: Throwable) {
            null
        }
    }

    /** 通过 smartbox 搜索曲目，优先返回歌名与艺人同时匹配的条目。 */
    private fun qqSongMid(track: Track): String? {
        val title = cleanTitle(track.title, track.artist)
        val query = listOf(track.artist, title)
            .filter { it.isNotBlank() && it != "无艺人名" }
            .joinToString(" ")
        if (query.isBlank()) return null
        val url = "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg".toHttpUrl().newBuilder()
            .addQueryParameter("key", query)
            .addQueryParameter("format", "json")
            .addQueryParameter("g_tk", "5381")
            .addQueryParameter("loginUin", "0")
            .addQueryParameter("hostUin", "0")
            .addQueryParameter("inCharset", "utf8")
            .addQueryParameter("outCharset", "utf-8")
            .addQueryParameter("notice", "0")
            .addQueryParameter("platform", "yqq.json")
            .addQueryParameter("needNewCode", "0")
            .build()
        val body = get(url.toString(), QQ_HEADERS) ?: return null
        val songs = JSONObject(body)
            .optJSONObject("data")
            ?.optJSONObject("song")
            ?.optJSONArray("itemlist")
            ?: return null
        if (songs.length() == 0) return null
        val candidates = (0 until songs.length()).mapNotNull { songs.optJSONObject(it) }
        val best = candidates.firstOrNull { item ->
            val name = item.optString("name")
            val singer = item.optString("singer")
            (name.contains(title, true) || title.contains(name, true)) &&
                (track.artist.isBlank() || singer.contains(track.artist, true) ||
                    track.artist.contains(singer, true))
        } ?: candidates.firstOrNull()
        return best?.optString("mid")?.takeIf { it.isNotBlank() }
    }

    private fun buildLyrics(raw: String, source: String): Lyrics? {
        if (raw.isBlank()) return null
        val parsed = parseLrc(raw)
        if (parsed.isEmpty()) return null
        return Lyrics(
            lines = parsed,
            plainText = raw.trim(),
            synced = parsed.any { it.timeMs > 0 },
            source = source
        )
    }

    private fun parseLrc(text: String): List<LyricLine> {
        val timeTag = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]""")
        val metadataTag = Regex(
            """^(作词|作曲|编曲|制作人|制作|监制|混音|母带|母带处理|人声编辑|人声|录音|和声|吉他|贝斯|鼓|弦乐|出品|发行|策划|统筹|演唱|原唱|词|曲)\s*[:：]"""
        )
        val lines = mutableListOf<LyricLine>()
        text.lineSequence().forEach { raw ->
            val matches = timeTag.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val content = raw.replace(timeTag, "").trim()
            if (content.isBlank()) return@forEach
            // 过滤网易云 LRC 头部的作词/作曲等制作信息
            if (metadataTag.containsMatchIn(content)) return@forEach
            matches.forEach { match ->
                val minutes = match.groupValues[1].toLongOrNull() ?: 0
                val seconds = match.groupValues[2].toLongOrNull() ?: 0
                val fractionRaw = match.groupValues[3]
                val fraction = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLongOrNull()?.times(100) ?: 0
                    2 -> fractionRaw.toLongOrNull()?.times(10) ?: 0
                    else -> fractionRaw.take(3).toLongOrNull() ?: 0
                }
                lines += LyricLine(minutes * 60_000 + seconds * 1_000 + fraction, content)
            }
        }
        if (lines.isEmpty()) {
            return text.lineSequence()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("[") }
                .map { LyricLine(0, it) }
                .toList()
        }
        return lines.distinctBy { it.timeMs to it.text }.sortedBy { it.timeMs }
    }

    private fun fetchMusicBrainzCover(artist: String, album: String): String? {
        if (album.isBlank()) return null
        return try {
            val query = buildString {
                if (artist.isNotBlank()) append("artist:\"${artist.replace("\"", "")}\" AND ")
                append("release:\"${album.replace("\"", "")}\"")
            }
            val url = "https://musicbrainz.org/ws/2/release-group/".toHttpUrl().newBuilder()
                .addQueryParameter("query", query)
                .addQueryParameter("fmt", "json")
                .addQueryParameter("limit", "3")
                .build()
            val body = get(url.toString(), mapOf("User-Agent" to USER_AGENT)) ?: return null
            val groups = JSONObject(body).optJSONArray("release-groups") ?: return null
            for (index in 0 until groups.length()) {
                val id = groups.optJSONObject(index)?.optString("id").orEmpty()
                if (id.isBlank()) continue
                val coverUrl = "https://coverartarchive.org/release-group/$id/front-500"
                if (headOk(coverUrl)) return coverUrl
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * NetEase album search covers Chinese catalogs that MusicBrainz and the
     * iTunes US store do not index. Only albums with artwork are considered.
     */
    private fun fetchNeteaseCover(artist: String, album: String): String? {
        if (album.isBlank()) return null
        fun search(term: String): List<JSONObject> = try {
            val url = "https://music.163.com/api/search/get/web".toHttpUrl().newBuilder()
                .addQueryParameter("csrf_token", "")
                .addQueryParameter("s", term)
                .addQueryParameter("type", "10")
                .addQueryParameter("offset", "0")
                .addQueryParameter("total", "true")
                .addQueryParameter("limit", "8")
                .build()
            val body = get(
                url.toString(),
                mapOf(
                    "User-Agent" to DESKTOP_UA,
                    "Referer" to "https://music.163.com/"
                )
            ) ?: return emptyList()
            val albums = JSONObject(body)
                .optJSONObject("result")
                ?.optJSONArray("albums")
                ?: return emptyList()
            (0 until albums.length()).mapNotNull { albums.optJSONObject(it) }
        } catch (_: Throwable) {
            emptyList()
        }

        val albumName = album.trim()
        val artistName = artist.trim()
        val queries = buildList {
            if (artistName.isNotBlank() && artistName != "无艺人名") add("$artistName $albumName")
            add(albumName)
        }
        for (query in queries) {
            val results = search(query).filter { it.optString("picUrl").isNotBlank() }
            if (results.isEmpty()) continue
            val best = results.maxByOrNull { item ->
                var score = 0
                val name = item.optString("name")
                if (name == albumName) score += 6
                else if (name.contains(albumName, true) || albumName.contains(name, true)) score += 3
                val itemArtist = item.optJSONObject("artist")?.optString("name").orEmpty()
                if (artistName.isNotBlank() && artistName != "无艺人名" && itemArtist.isNotBlank()) {
                    if (itemArtist.equals(artistName, true)) score += 8
                    else if (itemArtist.contains(artistName, true) ||
                        artistName.contains(itemArtist, true)
                    ) score += 5
                }
                score
            } ?: continue
            val pic = best.optString("picUrl")
            if (pic.isNotBlank()) {
                return pic + if (pic.contains("?")) "&param=600y600" else "?param=600y600"
            }
        }
        return null
    }

    /**
     * QQ 音乐封面：用 smartbox 搜索专辑，取专辑 mid 拼出 500x500 封面地址。
     */
    private fun fetchQqCover(artist: String, album: String): String? {
        if (album.isBlank()) return null
        return try {
            val term = listOf(artist, album)
                .filter { it.isNotBlank() && it != "无艺人名" }
                .joinToString(" ")
            val url = QQ_SEARCH_URL.toHttpUrl().newBuilder()
                .addQueryParameter("key", term)
                .addQueryParameter("format", "json")
                .addQueryParameter("g_tk", "5381")
                .addQueryParameter("loginUin", "0")
                .addQueryParameter("hostUin", "0")
                .addQueryParameter("inCharset", "utf8")
                .addQueryParameter("outCharset", "utf-8")
                .addQueryParameter("notice", "0")
                .addQueryParameter("platform", "yqq.json")
                .addQueryParameter("needNewCode", "0")
                .build()
            val body = get(url.toString(), QQ_HEADERS) ?: return null
            val albums = JSONObject(body)
                .optJSONObject("data")
                ?.optJSONObject("album")
                ?.optJSONArray("itemlist")
                ?: return null
            if (albums.length() == 0) return null
            val candidates = (0 until albums.length()).mapNotNull { albums.optJSONObject(it) }
            val best = candidates.firstOrNull { item ->
                val name = item.optString("name")
                val singer = item.optString("singer")
                (name.contains(album, true) || album.contains(name, true)) &&
                    (artist.isBlank() || singer.contains(artist, true) ||
                        artist.contains(singer, true))
            } ?: candidates.firstOrNull()
            val mid = best?.optString("mid")?.takeIf { it.isNotBlank() } ?: return null
            val coverUrl = "https://y.gtimg.cn/music/photo_new/T002R500x500M000$mid.jpg"
            if (headOk(coverUrl)) coverUrl else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun fetchItunesCover(artist: String, album: String): String? {
        return try {
            val term = listOf(artist, album).filter { it.isNotBlank() }.joinToString(" ")
            val url = "https://itunes.apple.com/search".toHttpUrl().newBuilder()
                .addQueryParameter("term", term)
                .addQueryParameter("entity", "album")
                .addQueryParameter("limit", "5")
                .build()
            val body = get(url.toString(), mapOf("User-Agent" to USER_AGENT)) ?: return null
            val results = JSONObject(body).optJSONArray("results") ?: return null
            for (index in 0 until results.length()) {
                val item = results.optJSONObject(index) ?: continue
                val artwork = item.optString("artworkUrl100")
                if (artwork.isNotBlank()) {
                    return artwork
                        .replace("/100x100bb.", "/600x600bb.")
                        .replace("100x100bb.jpg", "600x600bb.jpg")
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun headOk(url: String): Boolean = try {
        val request = Request.Builder()
            .url(url)
            .head()
            .header("User-Agent", USER_AGENT)
            .build()
        http.newCall(request).execute().use { it.isSuccessful || it.isRedirect }
    } catch (_: Throwable) {
        false
    }

    private fun get(url: String, headers: Map<String, String> = emptyMap()): String? = try {
        val builder = Request.Builder().url(url).get()
        headers.forEach { (key, value) -> builder.header(key, value) }
        http.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }
    } catch (_: IOException) {
        null
    } catch (_: Throwable) {
        null
    }

    private fun cleanTitle(title: String, artist: String = ""): String {
        var value = title
            .replace(Regex("""^\s*\d{1,3}[\s.\-]+"""), "")
            .replace(Regex("""\s*-\s*cnhifi\.com\s*$""", RegexOption.IGNORE_CASE), "")
            .trim()
        if (artist.isNotBlank() && artist != "无艺人名") {
            value = value.removePrefix("$artist - ")
                .removePrefix("$artist-")
                .removePrefix("$artist ")
                .trim()
        }
        return value
    }

    private fun readLyricsCache(key: String): Lyrics? {
        val file = File(lyricsDir, "$key.json")
        if (!file.exists()) return null
        if (System.currentTimeMillis() - file.lastModified() > 30L * 24 * 60 * 60 * 1000) return null
        return try {
            val root = JSONObject(file.readText())
            val array = root.optJSONArray("lines") ?: return null
            val lines = (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                LyricLine(item.optLong("t"), item.optString("s"))
            }
            Lyrics(
                lines = lines,
                plainText = root.optString("plain"),
                synced = root.optBoolean("synced"),
                source = root.optString("source")
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun writeLyricsCache(key: String, lyrics: Lyrics) {
        try {
            val array = JSONArray()
            lyrics.lines.forEach { line ->
                array.put(JSONObject().put("t", line.timeMs).put("s", line.text))
            }
            val root = JSONObject()
                .put("plain", lyrics.plainText)
                .put("synced", lyrics.synced)
                .put("source", lyrics.source)
                .put("lines", array)
            File(lyricsDir, "$key.json").writeText(root.toString())
        } catch (_: Throwable) {
            // Best-effort cache.
        }
    }

    private fun hashKey(value: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val NONE = "NONE"
        private const val USER_AGENT = "DaphileRemote/1.1 (Android)"
        private const val DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
        private const val QQ_SEARCH_URL = "https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg"
        private val QQ_HEADERS = mapOf(
            "User-Agent" to DESKTOP_UA,
            "Referer" to "https://y.qq.com/"
        )
    }
}
