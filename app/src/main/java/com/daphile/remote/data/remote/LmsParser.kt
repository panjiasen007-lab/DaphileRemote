package com.daphile.remote.data.remote

import com.daphile.remote.data.model.Album
import com.daphile.remote.data.model.Artist
import com.daphile.remote.data.model.BrowserItem
import com.daphile.remote.data.model.Genre
import com.daphile.remote.data.model.NowPlaying
import com.daphile.remote.data.model.Paged
import com.daphile.remote.data.model.Player
import com.daphile.remote.data.model.PlaylistSummary
import com.daphile.remote.data.model.PluginEntry
import com.daphile.remote.data.model.SearchResults
import com.daphile.remote.data.model.ServerInfo
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.YearItem
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.abs

object LmsParser {
    const val TRACK_TAGS = "cdelgoaIirstTyquQ"

    fun parseServerInfo(result: JSONObject): ServerInfo = ServerInfo(
        version = stringAny(result, "version"),
        ip = stringAny(result, "ip"),
        httpPort = intAny(result, "httpport", default = 9000),
        playerCount = intAny(result, "player count", "player_count"),
        albumCount = intAny(result, "info total albums", "info_total_albums"),
        artistCount = intAny(result, "info total artists", "info_total_artists"),
        genreCount = intAny(result, "info total genres", "info_total_genres"),
        songCount = intAny(result, "info total songs", "info_total_songs"),
        scanning = truthy(result, "rescan"),
        lastScan = stringAny(result, "lastscan")
    )

    fun parsePlayers(result: JSONObject): List<Player> {
        val loop = objectArray(result, "players_loop") ?: objectLoop(result)
        return loop.mapNotNull { item ->
            val id = stringAny(item, "playerid", "id")
            if (id.isBlank()) return@mapNotNull null
            Player(
                id = id,
                name = stringAny(item, "name").ifBlank { id },
                model = stringAny(item, "model"),
                connected = truthy(item, "connected", default = true),
                power = truthy(item, "power", default = true)
            )
        }
    }

    fun parsePlayerStatus(base: Player, status: JSONObject): Player {
        val now = parseNowPlaying(base.id, status)
        val master = stringAny(status, "sync_master").takeIf { it.isNotBlank() && it != "-" }
        val slaves = stringAny(status, "sync_slaves")
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() && it != "-" }
        return base.copy(
            connected = truthy(status, "player_connected", default = base.connected),
            power = now.power,
            volume = now.volume,
            muted = now.muted,
            mode = now.mode,
            currentTitle = now.track?.title ?: stringAny(status, "current_title"),
            currentArtist = now.track?.artist.orEmpty(),
            canSetVolume = now.canSetVolume,
            syncMaster = master,
            syncSlaves = slaves
        )
    }

    fun parseNowPlaying(
        playerId: String,
        status: JSONObject,
        queueOverride: List<Track>? = null
    ): NowPlaying {
        val power = truthy(status, "power", default = true)
        val mode = stringAny(status, "mode").ifBlank { if (power) "stop" else "stop" }
        val currentIndex = intAny(status, "playlist_cur_index", "playlist index")
        val rawVolume = intAny(status, "mixer volume", "mixer_volume")
        val muted = rawVolume < 0 || truthy(status, "mixer muted", "mixer_muting", default = false)
        val volume = abs(rawVolume).coerceIn(0, 100)
        val loop = objectArray(status, "playlist_loop") ?: emptyList()
        val queue = queueOverride ?: loop.map { parseTrack(it, it.optInt("playlist index", 0)) }
        val fallbackTitle = stringAny(status, "current_title")
        val parsedTrack = queue.firstOrNull { it.index == currentIndex }
            ?: loop.firstOrNull { it.optInt("playlist index", -1) == currentIndex }?.let { parseTrack(it, currentIndex) }
            ?: loop.firstOrNull()?.let { parseTrack(it, it.optInt("playlist index", 0)) }
        val currentTrack = parsedTrack?.let { track ->
            if (track.title.isBlank() && fallbackTitle.isNotBlank()) track.copy(title = fallbackTitle) else track
        } ?: if (fallbackTitle.isNotBlank()) Track(title = fallbackTitle) else null
        val duration = doubleAny(status, "duration").takeIf { it > 0 } ?: currentTrack?.duration ?: 0.0
        return NowPlaying(
            playerId = playerId,
            power = power,
            mode = mode,
            time = doubleAny(status, "time"),
            duration = duration,
            volume = volume,
            muted = muted,
            repeat = intAny(status, "playlist repeat", "playlist_repeat"),
            shuffle = intAny(status, "playlist shuffle", "playlist_shuffle"),
            currentIndex = currentIndex,
            trackCount = intAny(status, "playlist_tracks").takeIf { it > 0 } ?: queue.size,
            track = currentTrack,
            queue = queue,
            canSeek = truthy(status, "can_seek", default = true),
            // 达菲的「音量控制」开关关闭时，digital_volume_control / use_volume_control 均为 0
            canSetVolume = truthy(status, "digital_volume_control", default = true) &&
                truthy(status, "use_volume_control", default = true),
            playlistTimestamp = stringAny(status, "playlist_timestamp")
        )
    }

    fun parseTrack(item: JSONObject, index: Int = 0): Track = Track(
        index = index,
        id = stringAny(item, "id", "track_id"),
        title = stringAny(item, "title", "track").ifBlank { stringAny(item, "current_title") },
        artist = stringAny(item, "artist"),
        album = stringAny(item, "album"),
        albumId = stringAny(item, "album_id"),
        duration = doubleAny(item, "duration"),
        coverId = stringAny(item, "coverid", "artwork_track_id"),
        type = stringAny(item, "type"),
        sampleRate = stringAny(item, "samplerate"),
        sampleSize = stringAny(item, "samplesize"),
        bitRate = stringAny(item, "bitrate"),
        trackNum = intAny(item, "tracknum"),
        disc = intAny(item, "disc"),
        url = stringAny(item, "url"),
        lossless = truthy(item, "lossless", "Q"),
        channels = intAny(item, "channels", "channelcount")
    )

    fun parseAlbums(result: JSONObject): Paged<Album> {
        val loop = objectArray(result, "albums_loop") ?: objectLoop(result)
        val items = loop.mapNotNull { item ->
            val id = stringAny(item, "id", "album_id")
            if (id.isBlank()) return@mapNotNull null
            Album(
                id = id,
                title = stringAny(item, "album", "title").ifBlank { "未命名专辑" },
                artist = stringAny(item, "artist", "artists", "albumartist"),
                year = stringAny(item, "year"),
                artworkId = stringAny(item, "artwork_track_id", "coverid").ifBlank { id },
                trackCount = intAny(item, "trackcount", "track_count")
            )
        }
        return Paged(intAny(result, "count", default = items.size), items)
    }

    fun parseArtists(result: JSONObject): Paged<Artist> {
        val loop = objectArray(result, "artists_loop") ?: objectLoop(result)
        val items = loop.mapNotNull { item ->
            val id = stringAny(item, "id", "artist_id")
            val name = stringAny(item, "artist", "name")
            if (id.isBlank() || name.isBlank()) null else Artist(id, name)
        }
        return Paged(intAny(result, "count", default = items.size), items)
    }

    fun parseGenres(result: JSONObject): Paged<Genre> {
        val loop = objectArray(result, "genres_loop") ?: objectLoop(result)
        val items = loop.mapNotNull { item ->
            val id = stringAny(item, "id", "genre_id")
            val name = stringAny(item, "genre", "name")
            if (id.isBlank() || name.isBlank()) null else Genre(id, name)
        }
        return Paged(intAny(result, "count", default = items.size), items)
    }

    fun parseYears(result: JSONObject): Paged<YearItem> {
        val items = objectLoop(result).mapNotNull { item ->
            val year = stringAny(item, "year")
            if (year.isBlank()) null else YearItem(year)
        }.distinctBy { it.year }
        return Paged(intAny(result, "count", default = items.size), items)
    }

    fun parsePlaylists(result: JSONObject): Paged<PlaylistSummary> {
        val loop = objectArray(result, "playlists_loop") ?: objectLoop(result)
        val items = loop.mapNotNull { item ->
            val id = stringAny(item, "id", "playlist_id")
            val name = stringAny(item, "name", "playlist")
            if (id.isBlank() || name.isBlank()) null else PlaylistSummary(id, name)
        }
        return Paged(intAny(result, "count", default = items.size), items)
    }

    fun parseTracks(result: JSONObject): Paged<Track> {
        val loop = objectArray(result, "titles_loop")
            ?: objectArray(result, "tracks_loop")
            ?: objectArray(result, "songs_loop")
            ?: objectLoop(result)
        val items = loop.mapIndexed { index, item -> parseTrack(item, index) }
        return Paged(intAny(result, "count", default = items.size), items)
    }

    fun parseSearch(query: String, result: JSONObject): SearchResults {
        val songs = (objectArray(result, "tracks_loop")
            ?: objectArray(result, "songs_loop")
            ?: objectArray(result, "titles_loop")
            ?: emptyList())
            .map { item ->
                Track(
                    id = stringAny(item, "track_id", "id"),
                    title = stringAny(item, "track", "title"),
                    artist = stringAny(item, "artist"),
                    album = stringAny(item, "album"),
                    duration = doubleAny(item, "duration")
                )
            }
        val albums = (objectArray(result, "albums_loop") ?: emptyList()).mapNotNull { item ->
            val id = stringAny(item, "album_id", "id")
            if (id.isBlank()) null else Album(
                id = id,
                title = stringAny(item, "album", "title"),
                artist = stringAny(item, "artist"),
                year = stringAny(item, "year"),
                artworkId = stringAny(item, "artwork_track_id", "coverid").ifBlank { id }
            )
        }
        val artists = (objectArray(result, "artists_loop") ?: emptyList()).mapNotNull { item ->
            val id = stringAny(item, "artist_id", "id")
            val name = stringAny(item, "artist", "name")
            if (id.isBlank() || name.isBlank()) null else Artist(id, name)
        }
        val playlists = (objectArray(result, "playlists_loop") ?: emptyList()).mapNotNull { item ->
            val id = stringAny(item, "playlist_id", "id")
            val name = stringAny(item, "playlist", "name")
            if (id.isBlank() || name.isBlank()) null else PlaylistSummary(id, name)
        }
        return SearchResults(query, songs, albums, artists, playlists)
    }

    fun parseBrowserItems(result: JSONObject): List<BrowserItem> {
        val loop = objectArray(result, "item_loop")
            ?: objectArray(result, "folder_loop")
            ?: objectLoop(result)
        return loop.mapNotNull { item ->
            val id = stringAny(item, "id", "item_id")
            if (id.isBlank()) return@mapNotNull null
            val type = stringAny(item, "type")
            val filename = stringAny(item, "filename")
            val hasItems = intAny(item, "hasitems", "has_items") > 0 ||
                truthy(item, "hasitems") ||
                type == "folder" ||
                type == "dir"
            BrowserItem(
                id = id,
                name = stringAny(item, "name", "title")
                    .ifBlank { filename }
                    .ifBlank { id },
                hasItems = hasItems,
                type = type,
                url = stringAny(item, "url"),
                isAudio = truthy(item, "isaudio", "is_audio") ||
                    type == "audio" || type == "track" || type == "file",
                icon = stringAny(item, "icon"),
                description = stringAny(item, "description"),
                filename = filename,
                durationText = stringAny(item, "duration"),
                coverId = stringAny(item, "coverid", "artwork_track_id")
            )
        }
    }

    fun parseSongInfo(result: JSONObject): JSONObject {
        val loop = objectArray(result, "songinfo_loop") ?: return result
        val merged = JSONObject()
        loop.forEach { entry ->
            val keys = entry.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                merged.put(key, entry.opt(key))
            }
        }
        return merged
    }

    fun parseScanStatus(result: JSONObject): com.daphile.remote.data.model.ScanStatus {
        if (!truthy(result, "rescan")) {
            return com.daphile.remote.data.model.ScanStatus(false, "扫描完成", 100)
        }
        val phases = mutableListOf<String>()
        var done = 0
        var total = 0
        val keys = result.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            if (key in setOf("rescan", "totaltime", "steps", "lastscanfailed", "info")) continue
            val value = result.optInt(key, -1)
            if (value in 0..100) {
                phases += "$key $value%"
                done += value
                total += 100
            }
        }
        val percent = if (total > 0) (done * 100 / total) else null
        val time = stringAny(result, "totaltime")
        val info = stringAny(result, "info")
        val message = buildString {
            append("正在扫描音乐库")
            if (time.isNotBlank()) append(" · $time")
            if (phases.isNotEmpty()) append(" · ${phases.take(3).joinToString(" ")}")
            if (info.isNotBlank()) append(" · $info")
        }
        return com.daphile.remote.data.model.ScanStatus(true, message, percent)
    }

    fun parsePlugins(result: JSONObject): List<PluginEntry> {
        val loop = objectArray(result, "radios_loop")
            ?: objectArray(result, "apps_loop")
            ?: objectLoop(result)
        return loop.mapNotNull { item ->
            val cmd = stringAny(item, "cmd")
            if (cmd.isBlank()) return@mapNotNull null
            PluginEntry(
                cmd = cmd,
                name = stringAny(item, "name").ifBlank { cmd },
                type = stringAny(item, "type"),
                icon = stringAny(item, "icon")
            )
        }
    }

    private fun objectArray(obj: JSONObject, key: String): List<JSONObject>? {
        val value = obj.opt(key) ?: return null
        if (value is JSONObject) return listOf(value)
        val array = value as? JSONArray ?: return null
        return (0 until array.length()).mapNotNull { array.optJSONObject(it) }
    }

    private fun objectLoop(obj: JSONObject): List<JSONObject> {
        val keys = obj.keys()
        while (keys.hasNext()) {
            val value = obj.opt(keys.next())
            if (value is JSONObject) return listOf(value)
            if (value is JSONArray) {
                if (value.length() == 0) return emptyList()
                if (value.opt(0) is JSONObject) {
                    return (0 until value.length()).mapNotNull { value.optJSONObject(it) }
                }
            }
        }
        return emptyList()
    }

    private fun stringAny(obj: JSONObject, vararg keys: String): String {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)?.toString().orEmpty()
            if (value.isNotBlank() && value != "null") return value
        }
        return ""
    }

    private fun intAny(obj: JSONObject, vararg keys: String, default: Int = 0): Int {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key)
            val value = when (raw) {
                is Number -> raw.toInt()
                else -> raw?.toString()?.toDoubleOrNull()?.toInt()
            }
            if (value != null) return value
        }
        return default
    }

    private fun doubleAny(obj: JSONObject, vararg keys: String): Double {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key)
            val value = when (raw) {
                is Number -> raw.toDouble()
                else -> raw?.toString()?.toDoubleOrNull()
            }
            if (value != null) return value
        }
        return 0.0
    }

    private fun truthy(obj: JSONObject, vararg keys: String, default: Boolean = false): Boolean {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val raw = obj.opt(key)
            return when (raw) {
                is Boolean -> raw
                is Number -> raw.toInt() != 0
                else -> raw?.toString()?.equals("true", true) == true || raw?.toString() == "1"
            }
        }
        return default
    }
}
