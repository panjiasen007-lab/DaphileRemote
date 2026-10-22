package com.daphile.remote.data.online

import android.content.Context
import com.daphile.remote.data.model.LyricLine
import com.daphile.remote.data.model.Lyrics
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
    private val artworkPrefs = context.getSharedPreferences("online_artwork_cache", Context.MODE_PRIVATE)
    private val lyricsMemory = ConcurrentHashMap<String, Lyrics>()

    suspend fun lyricsFor(track: Track): Lyrics? = withContext(Dispatchers.IO) {
        if (track.title.isBlank()) return@withContext null
        val key = hashKey("${track.artist}|${track.title}|${track.album}")
        lyricsMemory[key]?.let { return@withContext it }
        readLyricsCache(key)?.let {
            lyricsMemory[key] = it
            return@withContext it
        }
        val online = fetchLrclib(track) ?: fetchNetease(track)
        if (online != null) {
            lyricsMemory[key] = online
            writeLyricsCache(key, online)
        }
        online
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
        val lines = mutableListOf<LyricLine>()
        text.lineSequence().forEach { raw ->
            val matches = timeTag.findAll(raw).toList()
            if (matches.isEmpty()) return@forEach
            val content = raw.replace(timeTag, "").trim()
            if (content.isBlank()) return@forEach
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
    }
}
