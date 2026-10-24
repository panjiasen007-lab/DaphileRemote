package com.daphile.remote.data.remote

import com.daphile.remote.data.model.Album
import com.daphile.remote.data.model.Artist
import com.daphile.remote.data.model.BrowserItem
import com.daphile.remote.data.model.Genre
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.data.model.LmsError
import com.daphile.remote.data.model.LmsException
import com.daphile.remote.data.model.Paged
import com.daphile.remote.data.model.Player
import com.daphile.remote.data.model.PlaylistSummary
import com.daphile.remote.data.model.PluginEntry
import com.daphile.remote.data.model.ScanStatus
import com.daphile.remote.data.model.SearchResults
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.model.ServerInfo
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.YearItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class LmsClient {
    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .callTimeout(18, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun request(
        server: ServerConfig,
        playerId: String,
        command: List<String>
    ): JSONObject = withContext(Dispatchers.IO) {
        val payload = JSONObject()
            .put("id", 1)
            .put("method", "slim.request")
            .put("params", JSONArray().put(playerId).put(JSONArray(command)))
            .toString()
        val builder = Request.Builder()
            .url("${server.baseUrl}/jsonrpc.js")
            .post(payload.toRequestBody(jsonMedia))
        if (server.username.isNotBlank()) {
            builder.header("Authorization", Credentials.basic(server.username, server.password))
        }
        try {
            http.newCall(builder.build()).execute().use { response ->
                val body = response.body?.string().orEmpty()
                when {
                    response.code == 401 || response.code == 403 ->
                        throw LmsException(LmsError.Auth, "用户名或密码错误")
                    !response.isSuccessful ->
                        throw LmsException(LmsError.Network, "服务器返回 HTTP ${response.code}")
                    body.isBlank() ->
                        throw LmsException(LmsError.Network, "服务器未返回数据")
                    body.trimStart().startsWith("<") ->
                        throw LmsException(LmsError.Auth, "需要登录 Daphile")
                }
                val json = try {
                    JSONObject(body)
                } catch (error: Exception) {
                    throw LmsException(LmsError.Server, "服务器返回了无法解析的数据", error)
                }
                json.optJSONObject("error")?.let {
                    throw LmsException(
                        LmsError.Server,
                        it.optString("message").ifBlank { "服务器返回错误" }
                    )
                }
                val result = json.optJSONObject("result") ?: JSONObject()
                val resultError = result.optString("_error").ifBlank { result.optString("error") }
                if (resultError.isNotBlank()) {
                    val kind = if (resultError.contains("invalid player", true)) LmsError.Player else LmsError.Server
                    throw LmsException(kind, resultError)
                }
                result
            }
        } catch (error: LmsException) {
            throw error
        } catch (error: SocketTimeoutException) {
            throw LmsException(LmsError.Timeout, "连接超时，请重试", error)
        } catch (error: IOException) {
            throw LmsException(LmsError.Network, "无法连接服务器，请检查 IP 与端口", error)
        }
    }

    suspend fun serverStatus(server: ServerConfig): ServerInfo =
        LmsParser.parseServerInfo(request(server, "", listOf("serverstatus", "0", "999")))

    suspend fun players(server: ServerConfig): List<Player> =
        LmsParser.parsePlayers(request(server, "", listOf("players", "0", "200")))

    suspend fun quickStatus(server: ServerConfig, playerId: String): JSONObject =
        request(
            server,
            playerId,
            listOf("status", "-", "2", "tags:${LmsParser.TRACK_TAGS}")
        )

    suspend fun queueStatus(server: ServerConfig, playerId: String, limit: Int): JSONObject =
        request(
            server,
            playerId,
            listOf("status", "0", limit.coerceIn(1, 1000).toString(), "tags:${LmsParser.TRACK_TAGS}")
        )

    suspend fun playerBrief(server: ServerConfig, base: Player): Player {
        val status = quickStatus(server, base.id)
        if (status.length() == 0) throw LmsException(LmsError.Player, "播放器不可达")
        return LmsParser.parsePlayerStatus(base, status)
    }

    suspend fun albums(
        server: ServerConfig,
        start: Int,
        count: Int,
        sort: String,
        artistId: String? = null,
        genreId: String? = null,
        year: String? = null
    ): Paged<Album> {
        val command = mutableListOf(
            "albums", start.toString(), count.toString(), "tags:layjq", "sort:$sort"
        )
        artistId?.takeIf { it.isNotBlank() }?.let { command += "artist_id:$it" }
        genreId?.takeIf { it.isNotBlank() }?.let { command += "genre_id:$it" }
        year?.takeIf { it.isNotBlank() }?.let { command += "year:$it" }
        return LmsParser.parseAlbums(request(server, "", command))
    }

    suspend fun artists(server: ServerConfig, start: Int, count: Int): Paged<Artist> =
        LmsParser.parseArtists(
            request(server, "", listOf("artists", start.toString(), count.toString()))
        )

    suspend fun genres(server: ServerConfig, start: Int, count: Int): Paged<Genre> =
        LmsParser.parseGenres(
            request(server, "", listOf("genres", start.toString(), count.toString()))
        )

    suspend fun years(server: ServerConfig, start: Int, count: Int): Paged<YearItem> =
        LmsParser.parseYears(
            request(server, "", listOf("years", start.toString(), count.toString()))
        )

    suspend fun playlists(server: ServerConfig, start: Int, count: Int): Paged<PlaylistSummary> =
        LmsParser.parsePlaylists(
            request(server, "", listOf("playlists", start.toString(), count.toString()))
        )

    suspend fun albumInfo(server: ServerConfig, albumId: String): Album? =
        LmsParser.parseAlbums(
            request(server, "", listOf("albums", "0", "1", "album_id:$albumId", "tags:layjq"))
        ).items.firstOrNull()

    suspend fun albumTracks(server: ServerConfig, albumId: String): Paged<Track> =
        LmsParser.parseTracks(
            request(
                server,
                "",
                listOf(
                    "tracks", "0", "500",
                    "album_id:$albumId",
                    "tags:${LmsParser.TRACK_TAGS}",
                    "sort:tracknum"
                )
            )
        )

    suspend fun playlistTracks(server: ServerConfig, playlistId: String): Paged<Track> =
        LmsParser.parseTracks(
            request(
                server,
                "",
                listOf(
                    "playlists", "tracks", "0", "1000",
                    "playlist_id:$playlistId",
                    "tags:${LmsParser.TRACK_TAGS}"
                )
            )
        )

    suspend fun songs(server: ServerConfig, start: Int, count: Int): Paged<Track> =
        LmsParser.parseTracks(
            request(
                server,
                "",
                listOf("titles", start.toString(), count.toString(), "tags:${LmsParser.TRACK_TAGS}")
            )
        )

    suspend fun musicFolder(
        server: ServerConfig,
        parentId: String?,
        start: Int,
        count: Int
    ): Paged<BrowserItem> {
        val command = mutableListOf("musicfolder", start.toString(), count.toString(), "tags:ucd")
        parentId?.takeIf { it.isNotBlank() && it != "0" }?.let { command += "folder_id:$it" }
        val result = request(server, "", command)
        val items = LmsParser.parseBrowserItems(result)
        return Paged(result.optInt("count", items.size), items)
    }

    suspend fun songInfo(server: ServerConfig, trackId: String): JSONObject =
        LmsParser.parseSongInfo(
            request(server, "", listOf("songinfo", "0", "100", "track_id:$trackId", "tags:wa"))
        )

    suspend fun scanProgress(server: ServerConfig): ScanStatus =
        LmsParser.parseScanStatus(request(server, "", listOf("rescanprogress")))

    suspend fun startScan(server: ServerConfig, full: Boolean) {
        request(server, "", listOf(if (full) "wipecache" else "rescan"))
    }

    suspend fun search(server: ServerConfig, query: String): SearchResults {
        val meta = LmsParser.parseSearch(
            query,
            request(server, "", listOf("search", "0", "30", "term:$query"))
        )
        // search 接口不返回音质字段，再用 titles + search 过滤取带完整元数据的曲目
        val richSongs = runCatching {
            LmsParser.parseTracks(
                request(
                    server,
                    "",
                    listOf("titles", "0", "30", "search:$query", "tags:${LmsParser.TRACK_TAGS}")
                )
            ).items
        }.getOrNull()
        return if (!richSongs.isNullOrEmpty()) meta.copy(songs = richSongs) else meta
    }

    suspend fun favorites(server: ServerConfig, parentId: String? = null, playerId: String = ""): List<BrowserItem> {
        val command = mutableListOf("favorites", "items", "0", "200", "want_url:1")
        parentId?.takeIf { it.isNotBlank() && it != "0" }?.let { command += "item_id:$it" }
        return LmsParser.parseBrowserItems(request(server, playerId, command))
    }

    suspend fun radios(server: ServerConfig): List<PluginEntry> =
        LmsParser.parsePlugins(request(server, "", listOf("radios", "0", "100")))

    suspend fun apps(server: ServerConfig): List<PluginEntry> =
        LmsParser.parsePlugins(request(server, "", listOf("apps", "0", "100")))

    suspend fun pluginItems(
        server: ServerConfig,
        playerId: String,
        pluginCmd: String,
        parentId: String
    ): List<BrowserItem> = LmsParser.parseBrowserItems(
        request(
            server,
            playerId,
            listOf(pluginCmd, "items", "0", "100", "item_id:$parentId", "want_url:1")
        )
    )

    suspend fun command(server: ServerConfig, playerId: String, vararg command: String) {
        request(server, playerId, command.toList())
    }

    suspend fun hostAction(server: ServerConfig, action: HostPowerAction) {
        withContext(Dispatchers.IO) {
            val builder = Request.Builder()
                .url(server.daphileBaseUrl + action.path)
                .get()
            if (server.username.isNotBlank()) {
                builder.header("Authorization", Credentials.basic(server.username, server.password))
            }
            try {
                http.newCall(builder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw LmsException(LmsError.Server, "主机接口返回 HTTP ${response.code}")
                    }
                }
            } catch (error: LmsException) {
                throw error
            } catch (error: IOException) {
                throw LmsException(LmsError.Network, "主机电源接口不可达（${server.daphileBaseUrl}）", error)
            }
        }
    }
}
