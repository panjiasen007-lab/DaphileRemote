package com.daphile.remote.data.model

import java.net.URLEncoder

data class ServerConfig(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 9000,
    val daphilePort: Int = 80,
    val username: String = "",
    val password: String = "",
    val useHttps: Boolean = false
) {
    val baseUrl: String
        get() = "${if (useHttps) "https" else "http"}://$host:$port"

    val daphileBaseUrl: String
        get() = "http://$host:$daphilePort"

    val displayAddress: String
        get() = "$host:$port"

    fun coverUrl(coverId: String?, playerId: String? = null): String? {
        if (!coverId.isNullOrBlank()) {
            return "$baseUrl/music/${encodePath(coverId)}/cover.jpg"
        }
        if (!playerId.isNullOrBlank()) {
            return "$baseUrl/music/current/cover.jpg?player=${encodeQuery(playerId)}"
        }
        return null
    }

    private fun encodePath(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private fun encodeQuery(value: String): String = URLEncoder.encode(value, "UTF-8")
}

data class Player(
    val id: String,
    val name: String,
    val model: String = "",
    val connected: Boolean = true,
    val power: Boolean = true,
    val volume: Int = 0,
    val muted: Boolean = false,
    val mode: String = "stop",
    val currentTitle: String = "",
    val currentArtist: String = "",
    val syncMaster: String? = null,
    val syncSlaves: List<String> = emptyList()
) {
    val isPlaying: Boolean get() = mode == "play"
}

data class Track(
    val index: Int = 0,
    val id: String = "",
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumId: String = "",
    val duration: Double = 0.0,
    val coverId: String = "",
    val type: String = "",
    val sampleRate: String = "",
    val sampleSize: String = "",
    val bitRate: String = "",
    val trackNum: Int = 0,
    val disc: Int = 0,
    val url: String = "",
    val lossless: Boolean = false,
    val channels: Int = 0
)

data class NowPlaying(
    val playerId: String,
    val power: Boolean = true,
    val mode: String = "stop",
    val time: Double = 0.0,
    val duration: Double = 0.0,
    val volume: Int = 0,
    val muted: Boolean = false,
    val repeat: Int = 0,
    val shuffle: Int = 0,
    val currentIndex: Int = 0,
    val trackCount: Int = 0,
    val track: Track? = null,
    val queue: List<Track> = emptyList(),
    val canSeek: Boolean = true,
    val canSetVolume: Boolean = true,
    val playlistTimestamp: String = ""
) {
    val isPlaying: Boolean get() = mode == "play"
    val isPaused: Boolean get() = mode == "pause"
}

data class Album(
    val id: String,
    val title: String,
    val artist: String = "",
    val year: String = "",
    val artworkId: String = "",
    val trackCount: Int = 0
)

data class Artist(val id: String, val name: String)

data class Genre(val id: String, val name: String)

data class YearItem(val year: String)

data class PlaylistSummary(val id: String, val name: String)

data class SearchResults(
    val query: String,
    val songs: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList()
)

data class Paged<T>(val total: Int, val items: List<T>)

data class ServerInfo(
    val version: String = "",
    val ip: String = "",
    val httpPort: Int = 9000,
    val playerCount: Int = 0,
    val albumCount: Int = 0,
    val artistCount: Int = 0,
    val genreCount: Int = 0,
    val songCount: Int = 0,
    val scanning: Boolean = false,
    val lastScan: String = ""
)

data class BrowserItem(
    val id: String,
    val name: String,
    val hasItems: Boolean = false,
    val type: String = "",
    val url: String = "",
    val isAudio: Boolean = false,
    val icon: String = "",
    val description: String = "",
    val filename: String = "",
    val durationText: String = "",
    val coverId: String = ""
)

data class PluginEntry(
    val cmd: String,
    val name: String,
    val type: String = "",
    val icon: String = ""
)

data class DiscoveredServer(
    val host: String,
    val port: Int,
    val version: String = "",
    val name: String = ""
)

data class AppSettings(
    val theme: String = "dark",
    val pollSeconds: Int = 3,
    val notificationsEnabled: Boolean = true,
    val hostPowerEnabled: Boolean = true,
    val coverCacheMb: Int = 200,
    val onlineMetadataEnabled: Boolean = true,
    val lockScreenControls: Boolean = true
)

enum class HostPowerAction(val key: String, val title: String, val path: String) {
    Restart("restart", "重启主机", "/cgi-bin/Settings?ACTION=restart"),
    Shutdown("shutdown", "关闭主机", "/cgi-bin/Info?ACTION=shutdown"),
    Suspend("suspend", "挂起主机", "/cgi-bin/Info?ACTION=pmsuspend")
}

data class LyricLine(val timeMs: Long, val text: String)

data class Lyrics(
    val lines: List<LyricLine>,
    val plainText: String = "",
    val synced: Boolean = false,
    val source: String = ""
)

data class ScanStatus(
    val scanning: Boolean,
    val message: String = "",
    val percent: Int? = null
)

enum class ConnectionState { Idle, Connecting, Connected, Reconnecting, Lost }

enum class SleepAction { Pause, Stop, PowerOff }

enum class LmsError { Network, Timeout, Auth, Server, Player, Unsupported, Empty }

class LmsException(
    val kind: LmsError,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

fun friendlyErrorMessage(error: Throwable): String = when (error) {
    is LmsException -> when (error.kind) {
        LmsError.Auth -> "用户名或密码错误"
        LmsError.Timeout -> "连接超时，请重试"
        LmsError.Player -> "播放器不可达"
        LmsError.Empty -> "队列为空，请先选择音乐"
        LmsError.Unsupported -> "当前 Daphile 版本不支持该功能"
        else -> error.message ?: "网络连接失败"
    }
    else -> error.message ?: "操作失败，请重试"
}
