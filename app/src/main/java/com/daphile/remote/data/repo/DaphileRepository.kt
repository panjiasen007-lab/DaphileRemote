package com.daphile.remote.data.repo

import android.content.Context
import android.os.SystemClock
import com.daphile.remote.data.local.SettingsStore
import com.daphile.remote.data.model.Album
import com.daphile.remote.data.model.AppSettings
import com.daphile.remote.data.model.Artist
import com.daphile.remote.data.model.BrowserItem
import com.daphile.remote.data.model.ConnectionState
import com.daphile.remote.data.model.DiscoveredServer
import com.daphile.remote.data.model.Genre
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.data.model.LmsError
import com.daphile.remote.data.model.LmsException
import com.daphile.remote.data.model.Lyrics
import com.daphile.remote.data.model.NowPlaying
import com.daphile.remote.data.model.Paged
import com.daphile.remote.data.model.Player
import com.daphile.remote.data.model.PlaylistSummary
import com.daphile.remote.data.model.PluginEntry
import com.daphile.remote.data.model.ScanStatus
import com.daphile.remote.data.model.SearchResults
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.model.ServerInfo
import com.daphile.remote.data.model.SleepAction
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.YearItem
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.data.online.OnlineMetadataService
import com.daphile.remote.data.remote.LmsClient
import com.daphile.remote.data.remote.LmsParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.math.min

class DaphileRepository(
    private val context: Context,
    private val settings: SettingsStore,
    private val client: LmsClient,
    private val onlineMetadata: OnlineMetadataService
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val servers: StateFlow<List<ServerConfig>> =
        settings.servers.stateIn(scope, SharingStarted.Eagerly, emptyList())

    val appSettings: StateFlow<AppSettings> =
        settings.appSettings.stateIn(scope, SharingStarted.Eagerly, AppSettings())

    private val _server = MutableStateFlow<ServerConfig?>(null)
    val server: StateFlow<ServerConfig?> = _server.asStateFlow()

    private val _connection = MutableStateFlow(ConnectionState.Idle)
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    private val _players = MutableStateFlow<List<Player>>(emptyList())
    val players: StateFlow<List<Player>> = _players.asStateFlow()

    private val _currentPlayerId = MutableStateFlow<String?>(null)
    val currentPlayerId: StateFlow<String?> = _currentPlayerId.asStateFlow()

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    private val _serverInfo = MutableStateFlow<ServerInfo?>(null)
    val serverInfo: StateFlow<ServerInfo?> = _serverInfo.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _sleepRemaining = MutableStateFlow<Int?>(null)
    val sleepRemaining: StateFlow<Int?> = _sleepRemaining.asStateFlow()

    private val _scanStatus = MutableStateFlow<ScanStatus?>(null)
    val scanStatus: StateFlow<ScanStatus?> = _scanStatus.asStateFlow()

    private var sessionJob: Job? = null
    private var sleepJob: Job? = null
    private var scanJob: Job? = null
    private var started = false
    private val playerRefreshMutex = Mutex()

    fun start() {
        if (started) return
        started = true
        scope.launch {
            settings.currentServer.collectLatest { serverConfig ->
                if (serverConfig == null) {
                    stopSession()
                } else {
                    _server.value = serverConfig
                    startSession(serverConfig)
                }
            }
        }
    }

    fun retryConnection() {
        _server.value?.let { startSession(it) }
    }

    private fun startSession(serverConfig: ServerConfig) {
        sessionJob?.cancel()
        _connection.value = ConnectionState.Connecting
        _serverInfo.value = null
        _nowPlaying.value = null
        _players.value = emptyList()
        _currentPlayerId.value = null
        _lastError.value = null
        sessionJob = scope.launch { pollLoop(serverConfig) }
    }

    private fun stopSession() {
        sessionJob?.cancel()
        sessionJob = null
        scanJob?.cancel()
        scanJob = null
        _scanStatus.value = null
        _server.value = null
        _connection.value = ConnectionState.Idle
        _players.value = emptyList()
        _currentPlayerId.value = null
        _nowPlaying.value = null
        _serverInfo.value = null
        _lastError.value = null
    }

    private suspend fun pollLoop(serverConfig: ServerConfig) {
        var failures = 0
        var disconnectStartedAt = 0L
        var lastPlayersRefreshAt = 0L
        var lastQueueTimestamp: String? = null

        while (currentCoroutineContext().isActive) {
            try {
                if (failures > 0 || _serverInfo.value == null) {
                    _connection.value = if (failures == 0) ConnectionState.Connecting else ConnectionState.Reconnecting
                    _serverInfo.value = client.serverStatus(serverConfig)
                }

                val now = SystemClock.elapsedRealtime()
                if (_players.value.isEmpty() || now - lastPlayersRefreshAt >= 30_000L) {
                    refreshPlayers(serverConfig)
                    lastPlayersRefreshAt = now
                }

                val playerId = _currentPlayerId.value
                if (playerId != null) {
                    val quick = client.quickStatus(serverConfig, playerId)
                    val timestamp = quick.optString("playlist_timestamp")
                    val trackCount = quick.optInt("playlist_tracks", 0)
                    val existing = _nowPlaying.value
                    val quickCurrentId = quick.optJSONArray("playlist_loop")
                        ?.optJSONObject(0)
                        ?.optString("id")
                        .orEmpty()
                    val needsQueue = existing == null ||
                        existing.queue.isEmpty() ||
                        existing.trackCount != trackCount ||
                        timestamp != lastQueueTimestamp ||
                        (quickCurrentId.isNotBlank() &&
                            existing.track?.id?.isNotBlank() == true &&
                            quickCurrentId != existing.track?.id)

                    val queue = if (needsQueue && trackCount > 0) {
                        LmsParser.parseTracks(
                            client.queueStatus(serverConfig, playerId, min(trackCount, 1000))
                        ).items
                    } else {
                        existing?.queue.orEmpty()
                    }
                    if (needsQueue) lastQueueTimestamp = timestamp
                    _nowPlaying.value = LmsParser.parseNowPlaying(playerId, quick, queue)
                } else {
                    _nowPlaying.value = null
                }

                _connection.value = ConnectionState.Connected
                _lastError.value = null
                failures = 0
                disconnectStartedAt = 0L

                val interval = settings.appSettings.first().pollSeconds.coerceIn(2, 30)
                delay(interval * 1000L)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (error: Throwable) {
                failures++
                if (disconnectStartedAt == 0L) disconnectStartedAt = SystemClock.elapsedRealtime()
                val disconnectedFor = SystemClock.elapsedRealtime() - disconnectStartedAt
                _connection.value = if (disconnectedFor >= 300_000L) {
                    ConnectionState.Lost
                } else {
                    ConnectionState.Reconnecting
                }
                _lastError.value = friendlyErrorMessage(error)
                _serverInfo.value = null
                lastPlayersRefreshAt = 0L
                delay(min(60_000L, 1_000L shl min(failures, 6)))
            }
        }
    }

    private suspend fun refreshPlayers(serverConfig: ServerConfig) {
        playerRefreshMutex.withLock {
            val basic = client.players(serverConfig)
            val detailed = supervisorScope {
                basic.map { player ->
                    async {
                        runCatching { client.playerBrief(serverConfig, player) }
                            .getOrElse { player.copy(connected = false) }
                    }
                }.awaitAll()
            }
            _players.value = detailed

            val saved = settings.playerForServer(serverConfig.id)
            val current = _currentPlayerId.value
            val selected = current?.takeIf { id -> detailed.any { it.id == id } }
                ?: saved?.takeIf { id -> detailed.any { it.id == id } }
                ?: detailed.firstOrNull { it.connected }?.id
                ?: detailed.firstOrNull()?.id
            if (selected != _currentPlayerId.value) {
                _currentPlayerId.value = selected
                if (selected != null) settings.setPlayerForServer(serverConfig.id, selected)
            }
        }
    }

    private suspend fun refreshPlayerState(serverConfig: ServerConfig, playerId: String) {
        val quick = client.quickStatus(serverConfig, playerId)
        val existing = _nowPlaying.value
        val now = LmsParser.parseNowPlaying(playerId, quick, existing?.queue.orEmpty())
        _nowPlaying.value = now
        _players.value = _players.value.map { player ->
            if (player.id == playerId) LmsParser.parsePlayerStatus(player, quick) else player
        }
    }

    private suspend fun refreshFullState(serverConfig: ServerConfig, playerId: String) {
        val quick = client.quickStatus(serverConfig, playerId)
        val trackCount = quick.optInt("playlist_tracks", 0)
        val queue = if (trackCount > 0) {
            LmsParser.parseTracks(
                client.queueStatus(serverConfig, playerId, min(trackCount, 1000))
            ).items
        } else {
            emptyList()
        }
        _nowPlaying.value = LmsParser.parseNowPlaying(playerId, quick, queue)
        _players.value = _players.value.map { player ->
            if (player.id == playerId) LmsParser.parsePlayerStatus(player, quick) else player
        }
    }

    private suspend fun refreshAfterCommand(queueChanged: Boolean = false) {
        val serverConfig = _server.value ?: return
        val playerId = _currentPlayerId.value ?: return
        delay(140)
        if (queueChanged) {
            runCatching { refreshFullState(serverConfig, playerId) }
        } else {
            runCatching { refreshPlayerState(serverConfig, playerId) }
        }
    }

    private suspend fun sendCurrent(vararg command: String) {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        val playerId = _currentPlayerId.value ?: throw LmsException(LmsError.Player, "没有可用播放器")
        client.command(serverConfig, playerId, *command)
    }

    private suspend fun sendTo(playerId: String, vararg command: String) {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        client.command(serverConfig, playerId, *command)
    }

    fun selectPlayer(playerId: String) {
        val serverConfig = _server.value ?: return
        _currentPlayerId.value = playerId
        _nowPlaying.value = null
        scope.launch {
            settings.setPlayerForServer(serverConfig.id, playerId)
            runCatching { refreshPlayerState(serverConfig, playerId) }
        }
    }

    suspend fun play() {
        val state = _nowPlaying.value ?: throw LmsException(LmsError.Player, "没有正在播放的内容")
        if (!state.power) {
            setPlayerPower(state.playerId, true)
            return
        }
        if (!state.isPlaying) {
            _nowPlaying.value = state.copy(mode = "play")
            if (state.isPaused) sendCurrent("pause", "0") else sendCurrent("play")
            refreshAfterCommand()
        }
    }

    suspend fun pause() {
        val state = _nowPlaying.value ?: throw LmsException(LmsError.Player, "没有正在播放的内容")
        if (state.isPlaying) {
            _nowPlaying.value = state.copy(mode = "pause")
            sendCurrent("pause", "1")
            refreshAfterCommand()
        }
    }

    suspend fun playPause() {
        val state = _nowPlaying.value ?: throw LmsException(LmsError.Player, "没有正在播放的内容")
        if (!state.power) {
            setPlayerPower(state.playerId, true)
            return
        }
        when {
            state.isPlaying -> {
                _nowPlaying.value = state.copy(mode = "pause")
                sendCurrent("pause", "1")
            }
            state.isPaused -> {
                _nowPlaying.value = state.copy(mode = "play")
                sendCurrent("pause", "0")
            }
            else -> {
                _nowPlaying.value = state.copy(mode = "play")
                sendCurrent("play")
            }
        }
        refreshAfterCommand()
    }

    suspend fun next() {
        _nowPlaying.value = _nowPlaying.value?.copy(
            currentIndex = (_nowPlaying.value?.currentIndex ?: 0) + 1
        )
        sendCurrent("playlist", "jump", "+1")
        refreshAfterCommand()
    }

    suspend fun previous() {
        sendCurrent("playlist", "jump", "-1")
        refreshAfterCommand()
    }

    suspend fun stop() {
        _nowPlaying.value = _nowPlaying.value?.copy(mode = "stop")
        sendCurrent("stop")
        refreshAfterCommand()
    }

    suspend fun seekTo(seconds: Double) {
        val state = _nowPlaying.value ?: return
        if (!state.canSeek) throw LmsException(LmsError.Unsupported, "当前音源不支持定位")
        _nowPlaying.value = state.copy(time = seconds.coerceIn(0.0, state.duration))
        sendCurrent("time", seconds.toInt().coerceAtLeast(0).toString())
        refreshAfterCommand()
    }

    suspend fun setVolume(value: Int) {
        val volume = value.coerceIn(0, 100)
        _nowPlaying.value = _nowPlaying.value?.copy(volume = volume)
        _players.value = _players.value.map {
            if (it.id == _currentPlayerId.value) it.copy(volume = volume) else it
        }
        sendCurrent("mixer", "volume", volume.toString())
        refreshAfterCommand()
    }

    suspend fun setPlayerVolume(playerId: String, value: Int) {
        val volume = value.coerceIn(0, 100)
        _players.value = _players.value.map { if (it.id == playerId) it.copy(volume = volume) else it }
        if (playerId == _currentPlayerId.value) {
            _nowPlaying.value = _nowPlaying.value?.copy(volume = volume)
        }
        sendTo(playerId, "mixer", "volume", volume.toString())
        scope.launch { delay(250); refreshPlayersQuietly() }
    }

    suspend fun toggleMute() {
        sendCurrent("mixer", "muting", "toggle")
        refreshAfterCommand()
    }

    suspend fun setRepeat(value: Int) {
        val repeat = value.coerceIn(0, 2)
        _nowPlaying.value = _nowPlaying.value?.copy(repeat = repeat)
        sendCurrent("playlist", "repeat", repeat.toString())
        refreshAfterCommand()
    }

    suspend fun setShuffle(enabled: Boolean) {
        val shuffle = if (enabled) 1 else 0
        _nowPlaying.value = _nowPlaying.value?.copy(shuffle = shuffle)
        sendCurrent("playlist", "shuffle", shuffle.toString())
        refreshAfterCommand()
    }

    suspend fun setPlayerPower(playerId: String, on: Boolean) {
        sendTo(playerId, "power", if (on) "1" else "0")
        if (playerId == _currentPlayerId.value) {
            _nowPlaying.value = _nowPlaying.value?.copy(
                power = on,
                mode = if (on) _nowPlaying.value?.mode.orEmpty().ifBlank { "stop" } else "stop"
            )
        }
        _players.value = _players.value.map {
            if (it.id == playerId) it.copy(power = on, mode = if (on) it.mode else "stop") else it
        }
        scope.launch { delay(250); refreshPlayersQuietly() }
    }

    suspend fun syncPlayerTo(playerId: String, masterId: String) {
        sendTo(playerId, "sync", masterId)
        scope.launch { delay(300); refreshPlayersQuietly() }
    }

    suspend fun unsyncPlayer(playerId: String) {
        sendTo(playerId, "sync", "-")
        scope.launch { delay(300); refreshPlayersQuietly() }
    }

    private suspend fun refreshPlayersQuietly() {
        val serverConfig = _server.value ?: return
        runCatching { refreshPlayers(serverConfig) }
    }

    suspend fun playQueueIndex(index: Int) {
        val state = _nowPlaying.value
        val track = state?.queue?.firstOrNull { it.index == index }
        if (state != null && track != null) {
            _nowPlaying.value = state.copy(currentIndex = index, track = track, mode = "play")
        }
        sendCurrent("playlist", "index", index.toString())
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun removeQueueItem(index: Int) {
        sendCurrent("playlist", "delete", index.toString())
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun clearQueue() {
        sendCurrent("playlist", "clear")
        _nowPlaying.value = _nowPlaying.value?.copy(queue = emptyList(), trackCount = 0, track = null, mode = "stop")
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun saveQueue(name: String) {
        val clean = name.trim()
        if (clean.isBlank()) throw LmsException(LmsError.Server, "播放列表名称不能为空")
        sendCurrent("playlist", "save", clean)
    }

    suspend fun addCurrentFavorite() {
        val track = _nowPlaying.value?.track ?: throw LmsException(LmsError.Empty, "没有正在播放的曲目")
        if (track.url.isBlank()) throw LmsException(LmsError.Server, "当前音源不支持收藏")
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        client.command(
            serverConfig,
            _currentPlayerId.value.orEmpty(),
            "favorites", "add", "url:${track.url}", "title:${track.title}"
        )
    }

    suspend fun startSleepTimer(minutes: Int, action: SleepAction) {
        val seconds = minutes.coerceIn(1, 24 * 60) * 60
        runCatching { sendCurrent("sleep", "0") }
        sleepJob?.cancel()
        if (action == SleepAction.PowerOff) {
            sendCurrent("sleep", seconds.toString())
        }
        _sleepRemaining.value = seconds
        sleepJob = scope.launch {
            var remaining = seconds
            while (remaining > 0 && isActive) {
                delay(1_000)
                remaining--
                _sleepRemaining.value = remaining
            }
            if (remaining <= 0) {
                when (action) {
                    SleepAction.Pause -> runCatching { sendCurrent("pause", "1") }
                    SleepAction.Stop -> runCatching { sendCurrent("stop") }
                    SleepAction.PowerOff -> runCatching { sendCurrent("power", "0") }
                }
                _sleepRemaining.value = null
                refreshAfterCommand()
            }
        }
    }

    suspend fun cancelSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        _sleepRemaining.value = null
        runCatching { sendCurrent("sleep", "0") }
    }

    suspend fun playAlbum(albumId: String, append: Boolean = false) {
        if (append) {
            sendCurrent("playlistcontrol", "cmd:add", "album_id:$albumId")
            refreshAfterCommand(queueChanged = true)
        } else {
            sendCurrent("playlistcontrol", "cmd:load", "album_id:$albumId")
            sendCurrent("play")
            refreshAfterCommand(queueChanged = true)
        }
    }

    suspend fun playTrack(trackId: String, append: Boolean = false) {
        if (append) {
            sendCurrent("playlistcontrol", "cmd:add", "track_id:$trackId")
            refreshAfterCommand(queueChanged = true)
        } else {
            sendCurrent("playlistcontrol", "cmd:load", "track_id:$trackId")
            sendCurrent("play")
            refreshAfterCommand(queueChanged = true)
        }
    }

    suspend fun queueTrack(trackId: String) {
        sendCurrent("playlistcontrol", "cmd:add", "track_id:$trackId")
    }

    suspend fun playPlaylist(playlistId: String) {
        sendCurrent("playlistcontrol", "cmd:load", "playlist_id:$playlistId")
        sendCurrent("play")
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun playArtist(artistId: String) {
        sendCurrent("playlistcontrol", "cmd:load", "artist_id:$artistId")
        sendCurrent("play")
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun playFavorite(itemId: String) {
        sendCurrent("favorites", "playlist", "play", "item_id:$itemId")
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun playBrowserItem(pluginCmd: String?, item: BrowserItem) {
        when {
            item.url.isNotBlank() && (item.isAudio || item.url.startsWith("http")) -> {
                sendCurrent("playlist", "play", item.url, item.name)
            }
            pluginCmd != null -> {
                sendCurrent(pluginCmd, "playlist", "play", "item_id:${item.id}")
            }
            else -> {
                sendCurrent("favorites", "playlist", "play", "item_id:${item.id}")
            }
        }
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun randomAlbum() {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        val albums = client.albums(serverConfig, 0, 1, "random")
        val album = albums.items.firstOrNull() ?: throw LmsException(LmsError.Empty, "音乐库为空")
        playAlbum(album.id)
    }

    suspend fun rescan() {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        client.command(serverConfig, "", "rescan")
    }

    suspend fun hostPower(action: HostPowerAction) {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        client.hostAction(serverConfig, action)
    }

    suspend fun startScan(full: Boolean) {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        client.startScan(serverConfig, full)
        _scanStatus.value = ScanStatus(
            scanning = true,
            message = if (full) "正在重建音乐库…" else "正在扫描更新…"
        )
        scanJob?.cancel()
        scanJob = scope.launch {
            var failures = 0
            while (isActive) {
                delay(2_000)
                val status = runCatching { client.scanProgress(serverConfig) }.getOrElse {
                    failures++
                    if (failures >= 6) {
                        _scanStatus.value = ScanStatus(false, "无法获取扫描状态")
                        return@launch
                    }
                    null
                } ?: continue
                _scanStatus.value = status
                if (!status.scanning) {
                    runCatching {
                        _serverInfo.value = client.serverStatus(serverConfig)
                    }
                    delay(4_000)
                    _scanStatus.value = null
                    return@launch
                }
            }
        }
    }

    suspend fun refreshServerInfo() {
        val serverConfig = _server.value ?: return
        _serverInfo.value = client.serverStatus(serverConfig)
    }

    suspend fun songs(start: Int, count: Int): Paged<Track> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.songs(serverConfig, start, count)
    }

    suspend fun musicFolders(parentId: String?, start: Int, count: Int): Paged<BrowserItem> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.musicFolder(serverConfig, parentId, start, count)
    }

    suspend fun playFolder(folderId: String) {
        sendCurrent("playlistcontrol", "cmd:load", "folder_id:$folderId")
        sendCurrent("play")
        refreshAfterCommand(queueChanged = true)
    }

    suspend fun playFolderItem(item: BrowserItem) {
        when {
            item.hasItems || item.type == "folder" -> playFolder(item.id)
            item.id.toLongOrNull() != null -> {
                sendCurrent("playlistcontrol", "cmd:load", "track_id:${item.id}")
                sendCurrent("play")
                refreshAfterCommand(queueChanged = true)
            }
            item.url.isNotBlank() -> {
                sendCurrent("playlist", "play", item.url, item.name)
                refreshAfterCommand(queueChanged = true)
            }
        }
    }

    suspend fun queueFolderItem(item: BrowserItem) {
        if (item.id.toLongOrNull() != null) {
            sendCurrent("playlistcontrol", "cmd:add", "track_id:${item.id}")
        }
    }

    suspend fun lyricsForTrack(track: Track): Lyrics? {
        val serverConfig = _server.value ?: return null
        if (track.id.toLongOrNull() != null) {
            val local = runCatching {
                client.songInfo(serverConfig, track.id).optString("lyrics")
            }.getOrNull()
            if (!local.isNullOrBlank()) {
                onlineMetadata.parseLyricsText(local, "Daphile 本地歌词")?.let { return it }
            }
        }
        if (!settings.appSettings.first().onlineMetadataEnabled) return null
        return runCatching { onlineMetadata.lyricsFor(track) }.getOrNull()
    }

    suspend fun resolveOnlineCover(track: Track, force: Boolean = false): String? {
        if (!settings.appSettings.first().onlineMetadataEnabled) return null
        return runCatching {
            onlineMetadata.coverFor(track.artist, track.album, force)
        }.getOrNull()
    }

    suspend fun resolveOnlineCover(artist: String, album: String, force: Boolean = false): String? {
        if (!settings.appSettings.first().onlineMetadataEnabled) return null
        return runCatching { onlineMetadata.coverFor(artist, album, force) }.getOrNull()
    }

    suspend fun setOnlineMetadataEnabled(enabled: Boolean) =
        settings.setOnlineMetadataEnabled(enabled)

    suspend fun setLockScreenControls(enabled: Boolean) =
        settings.setLockScreenControls(enabled)

    suspend fun testServer(serverConfig: ServerConfig): ServerInfo = client.serverStatus(serverConfig)

    suspend fun saveServer(serverConfig: ServerConfig, makeCurrent: Boolean = true) {
        settings.upsertServer(serverConfig, makeCurrent)
        if (makeCurrent) settings.setCurrentServer(serverConfig.id)
    }

    suspend fun deleteServer(serverId: String) {
        settings.removeServer(serverId)
    }

    suspend fun switchServer(serverId: String) {
        settings.setCurrentServer(serverId)
    }

    suspend fun setTheme(theme: String) = settings.setTheme(theme)

    suspend fun setPollSeconds(seconds: Int) = settings.setPollSeconds(seconds)

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        settings.setNotificationsEnabled(enabled)

    suspend fun setHostPowerEnabled(enabled: Boolean) =
        settings.setHostPowerEnabled(enabled)

    suspend fun albums(
        start: Int,
        count: Int,
        sort: String,
        artistId: String? = null,
        genreId: String? = null,
        year: String? = null
    ): Paged<Album> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.albums(serverConfig, start, count, sort, artistId, genreId, year)
    }

    suspend fun artists(start: Int, count: Int): Paged<Artist> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.artists(serverConfig, start, count)
    }

    suspend fun genres(start: Int, count: Int): Paged<Genre> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.genres(serverConfig, start, count)
    }

    suspend fun years(start: Int, count: Int): Paged<YearItem> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.years(serverConfig, start, count)
    }

    suspend fun playlists(start: Int, count: Int): Paged<PlaylistSummary> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.playlists(serverConfig, start, count)
    }

    suspend fun albumDetails(albumId: String): Pair<Album, List<Track>> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        val tracks = client.albumTracks(serverConfig, albumId).items
        val info = client.albumInfo(serverConfig, albumId)
            ?: Album(
                id = albumId,
                title = tracks.firstOrNull()?.album ?: "专辑",
                artist = tracks.firstOrNull()?.artist.orEmpty(),
                year = "",
                artworkId = tracks.firstOrNull()?.coverId.orEmpty()
            )
        return info to tracks
    }

    suspend fun playlistTracks(playlistId: String): List<Track> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.playlistTracks(serverConfig, playlistId).items
    }

    suspend fun search(query: String): SearchResults {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.search(serverConfig, query)
    }

    suspend fun favorites(parentId: String? = null): List<BrowserItem> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.favorites(serverConfig, parentId, _currentPlayerId.value.orEmpty())
    }

    suspend fun radios(): List<PluginEntry> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.radios(serverConfig)
    }

    suspend fun apps(): List<PluginEntry> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        return client.apps(serverConfig)
    }

    suspend fun pluginItems(pluginCmd: String, parentId: String): List<BrowserItem> {
        val serverConfig = _server.value ?: throw LmsException(LmsError.Network, "尚未连接服务器")
        val playerId = _currentPlayerId.value.orEmpty()
        return client.pluginItems(serverConfig, playerId, pluginCmd, parentId)
    }

    suspend fun discoverServers(): List<DiscoveredServer> = withContext(Dispatchers.IO) {
        val hosts = localNetworkHosts()
        if (hosts.isEmpty()) return@withContext emptyList()
        val semaphore = Semaphore(24)
        val results = mutableListOf<DiscoveredServer>()
        withTimeoutOrNull(30_000L) {
            coroutineScope {
                hosts.map { host ->
                    async {
                        semaphore.withPermit {
                            if (isPortOpen(host, 9000)) {
                                val temp = ServerConfig(id = "discovered-$host", name = host, host = host)
                                runCatching { client.serverStatus(temp) }.getOrNull()?.let { info ->
                                    synchronized(results) {
                                        results.add(
                                            DiscoveredServer(
                                                host = host,
                                                port = 9000,
                                                version = info.version,
                                                name = "Daphile ($host)"
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }.awaitAll()
            }
        }
        results.distinctBy { it.host }.sortedBy { it.host }
    }

    private fun localNetworkHosts(): List<String> {
        val prefixes = mutableSetOf<String>()
        val ownAddresses = mutableSetOf<String>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val network = interfaces.nextElement()
                if (!network.isUp || network.isLoopback) continue
                for (address in network.inetAddresses) {
                    val ipv4 = address as? Inet4Address ?: continue
                    val bytes = ipv4.address
                    if (bytes.size != 4) continue
                    val first = bytes[0].toInt() and 0xFF
                    val second = bytes[1].toInt() and 0xFF
                    val third = bytes[2].toInt() and 0xFF
                    val fourth = bytes[3].toInt() and 0xFF
                    if (first == 127 || (first == 169 && second == 254)) continue
                    ownAddresses.add(ipv4.hostAddress.orEmpty())
                    prefixes.add("$first.$second.$third")
                }
            }
        } catch (_: Throwable) {
            return emptyList()
        }
        val hosts = mutableListOf<String>()
        prefixes.take(3).forEach { prefix ->
            for (last in 1..254) {
                val host = "$prefix.$last"
                if (host !in ownAddresses) hosts.add(host)
            }
        }
        return hosts
    }

    private fun isPortOpen(host: String, port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 350)
            true
        }
    } catch (_: Throwable) {
        false
    }

}
