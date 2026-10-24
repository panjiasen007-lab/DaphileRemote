@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.daphile.remote.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.ConnectionState
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.data.model.NowPlaying
import com.daphile.remote.data.model.Player
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.model.SleepAction
import com.daphile.remote.data.model.Track
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.CoverArt
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.LyricSpotlight
import com.daphile.remote.ui.common.PulseRing
import com.daphile.remote.ui.common.QualityBadge
import com.daphile.remote.ui.common.SpecChip
import com.daphile.remote.ui.common.SpinningVinyl
import com.daphile.remote.ui.common.StickFightScene
import com.daphile.remote.ui.common.TrackRow
import com.daphile.remote.ui.common.displayArtist
import com.daphile.remote.ui.common.formatDuration
import com.daphile.remote.ui.common.formatRemaining
import com.daphile.remote.ui.common.trackSpecs
import com.daphile.remote.ui.common.trackQuality
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NowPlayingScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: NowPlayingViewModel = viewModel()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val currentPlayerId by viewModel.currentPlayerId.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val sleepRemaining by viewModel.sleepRemaining.collectAsStateWithLifecycle()
    val coverOverride by viewModel.coverOverride.collectAsStateWithLifecycle()
    val lyrics by viewModel.lyrics.collectAsStateWithLifecycle()
    val lyricsLoading by viewModel.lyricsLoading.collectAsStateWithLifecycle()
    val favorite by viewModel.favorite.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val t = strings
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {}
    var askedNotificationPermission by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(nowPlaying?.isPlaying) {
        if (nowPlaying?.isPlaying == true &&
            !askedNotificationPermission &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askedNotificationPermission = true
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    var showQueue by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showSleepDialog by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPowerMenu by remember { mutableStateOf(false) }
    var pendingHostAction by remember { mutableStateOf<HostPowerAction?>(null) }
    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun runCommand(successMessage: String? = null, block: suspend () -> Unit) {
        scope.launch {
            try {
                block()
                if (successMessage != null) snackbar.showSnackbar(successMessage)
            } catch (error: Throwable) {
                snackbar.showSnackbar(friendlyErrorMessage(error))
            }
        }
    }

    if (server == null) {
        Box(modifier.fillMaxSize()) {
            EmptyState(
                icon = Icons.Rounded.WifiOff,
                title = t.noServerTitle,
                body = t.noServerBody,
                actionLabel = t.addServer,
                onAction = { navController.navigate(Routes.SERVERS) }
            )
        }
        return
    }

    val currentPlayer = players.firstOrNull { it.id == currentPlayerId }
    val track = nowPlaying?.track
    val localCoverUrl = server?.coverUrl(track?.coverId, nowPlaying?.playerId)
    val coverUrl = coverOverride ?: localCoverUrl
    LaunchedEffect(track?.id, localCoverUrl) {
        if (track != null && localCoverUrl == null) {
            runCatching { viewModel.matchCover(false) }
        }
    }

    // 本地推进播放位置，让歌词与频谱不必等待下一次轮询
    var positionMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(nowPlaying?.time) {
        positionMs = ((nowPlaying?.time ?: 0.0) * 1000).toLong()
    }
    LaunchedEffect(nowPlaying?.isPlaying, nowPlaying?.track?.id) {
        if (nowPlaying?.isPlaying == true) {
            while (true) {
                kotlinx.coroutines.delay(250)
                positionMs += 250
            }
        }
    }
    val lyricsData = lyrics
    val lyricIndex = remember(lyricsData, positionMs) {
        if (lyricsData?.synced == true && lyricsData.lines.isNotEmpty()) {
            lyricsData.lines.indexOfLast { it.timeMs <= positionMs }.coerceAtLeast(0)
        } else {
            0
        }
    }

    Box(modifier.fillMaxSize()) {
        PulsingBackdrop(
            accent = MaterialTheme.colorScheme.primary,
            active = nowPlaying?.isPlaying == true
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            PlayerTopBar(
                serverName = server?.name.orEmpty(),
                players = players,
                currentPlayer = currentPlayer,
                powerOn = nowPlaying?.power != false,
                onSelectPlayer = { viewModel.selectPlayer(it) },
                onOpenQueue = { showQueue = true },
                onTogglePower = { showPowerMenu = true },
                onLyrics = { navController.navigate(Routes.LYRICS) },
                onAudioOutput = { navController.navigate(Routes.AUDIO_OUTPUT) },
                onMatchCover = {
                    runCommand {
                        val matched = viewModel.matchCover(true)
                        snackbar.showSnackbar(if (matched) t.matchedCover else t.noOnlineCover)
                    }
                },
                onMenuClick = { showMenu = true },
                menuExpanded = showMenu,
                onDismissMenu = { showMenu = false },
                onSleep = {
                    showMenu = false
                    showSleepDialog = true
                },
                onFavorite = {
                    showMenu = false
                    runCommand("已加入收藏") { viewModel.addCurrentFavorite() }
                },
                onSaveQueue = {
                    showMenu = false
                    showSaveDialog = true
                },
                onClearQueue = {
                    showMenu = false
                    showClearDialog = true
                },
                onStop = {
                    showMenu = false
                    runCommand { viewModel.stop() }
                }
            )

            Spacer(Modifier.height(8.dp))
            NowPlayingHeader(track = track, nowPlaying = nowPlaying)

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val vinylSize = 196.dp
                SpinningVinyl(
                    coverUrl = coverUrl,
                    playing = nowPlaying?.isPlaying == true,
                    modifier = Modifier.size(vinylSize),
                    accent = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(12.dp))
                LyricSpotlight(
                    lyrics = lyricsData,
                    loading = lyricsLoading,
                    currentIndex = lyricIndex,
                    modifier = Modifier
                        .weight(1f)
                        .height(vinylSize),
                    onOpenLyrics = { navController.navigate(Routes.LYRICS) }
                )
            }

            Spacer(Modifier.height(8.dp))
            StickFightScene(
                progress = if ((nowPlaying?.duration ?: 0.0) > 0) {
                    (positionMs / 1000f / (nowPlaying?.duration ?: 1.0)).toFloat()
                } else {
                    0f
                },
                playing = nowPlaying?.isPlaying == true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            )

            Spacer(Modifier.height(4.dp))
            if (track != null) {
                SeekSection(
                    nowPlaying = nowPlaying,
                    track = track,
                    onSeek = { seconds -> runCommand { viewModel.seekTo(seconds) } }
                )
            }

            Spacer(Modifier.height(4.dp))
            QuickActionsRow(
                shuffleOn = (nowPlaying?.shuffle ?: 0) > 0,
                enabled = track != null,
                onShuffle = {
                    val next = !((nowPlaying?.shuffle ?: 0) > 0)
                    runCommand(if (next) t.shuffleOn else t.shuffleOff) {
                        viewModel.setShuffle(next)
                    }
                },
                onFavorite = {
                    runCommand(if (favorite) t.favorited else t.addedToFavorites) {
                        if (!favorite) viewModel.addCurrentFavorite()
                    }
                },
                favorite = favorite,
                onShare = {
                    val title = track?.title.orEmpty()
                    val artist = displayArtist(track?.artist)
                    val shareText = listOf(title, artist)
                        .filter { it.isNotBlank() }
                        .joinToString(" - ")
                    if (shareText.isBlank()) {
                        scope.launch { snackbar.showSnackbar(t.nothingToShare) }
                    } else {
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        runCatching {
                            context.startActivity(
                                android.content.Intent.createChooser(intent, t.shareTitle)
                            )
                        }
                        scope.launch { snackbar.showSnackbar(t.shareOpened) }
                    }
                },
                onSleep = {
                    scope.launch { snackbar.showSnackbar(t.sleepStarted) }
                    showSleepDialog = true
                },
                onAudioInfo = { navController.navigate(Routes.AUDIO_OUTPUT) }
            )

            Spacer(Modifier.height(10.dp))
            PlayerControls(
                nowPlaying = nowPlaying,
                enabled = track != null,
                onPrevious = {
                    runCommand(t.prevTrack) { viewModel.previous() }
                },
                onNext = {
                    runCommand(t.nextTrack) { viewModel.next() }
                },
                onPlayPause = {
                    val playing = nowPlaying?.isPlaying == true
                    runCommand(if (playing) t.pausedToast else t.playingToast) { viewModel.playPause() }
                },
                onRepeat = {
                    val next = when (nowPlaying?.repeat ?: 0) {
                        0 -> 2
                        2 -> 1
                        else -> 0
                    }
                    val label = when (next) {
                        2 -> t.listLoop
                        1 -> t.singleLoop
                        else -> t.repeatOff
                    }
                    runCommand(label) { viewModel.setRepeat(next) }
                },
                muted = nowPlaying?.muted == true,
                muteEnabled = nowPlaying?.canSetVolume != false,
                onToggleMute = {
                    val muted = nowPlaying?.muted == true
                    runCommand(if (muted) t.unmute else t.mute) { viewModel.toggleMute() }
                }
            )

            Spacer(Modifier.height(8.dp))
            VolumeSection(
                nowPlaying = nowPlaying,
                onVolumeChange = { viewModel.onVolumeChange(it) },
                onVolumeFinished = { viewModel.flushVolume() }
            )

            if (sleepRemaining != null) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = t.sleepRemaining.format(formatRemaining(sleepRemaining ?: 0)),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { runCommand(t.cancelTimer) { viewModel.cancelSleepTimer() } }) {
                        Text(t.cancel)
                    }
                }
            }

            if (players.isEmpty() && connection == ConnectionState.Connected) {
                EmptyState(
                    icon = Icons.Rounded.Speaker,
                    title = t.noPlayersTitle,
                    body = t.noPlayersBody,
                    actionLabel = t.viewPlayers,
                    onAction = { navController.navigate(Routes.PLAYERS) }
                )
            }

            Spacer(Modifier.height(16.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    if (showQueue) {
        ModalBottomSheet(
            onDismissRequest = { showQueue = false },
            sheetState = queueSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            QueueSheetContent(
                nowPlaying = nowPlaying,
                server = server,
                onPlay = { index -> runCommand { viewModel.playQueueIndex(index) } },
                onRemove = { index -> runCommand { viewModel.removeQueueItem(index) } },
                onClear = { showClearDialog = true },
                onGoLibrary = {
                    scope.launch {
                        runCatching { queueSheetState.hide() }
                        showQueue = false
                        kotlinx.coroutines.delay(160)
                        navController.navigate(Routes.LIBRARY) { launchSingleTop = true }
                    }
                }
            )
        }
    }

    if (showSleepDialog) {
        SleepTimerDialog(
            activeRemaining = sleepRemaining,
            onDismiss = { showSleepDialog = false },
            onStart = { minutes, action ->
                showSleepDialog = false
                runCommand { viewModel.startSleepTimer(minutes, action) }
            },
            onCancelTimer = {
                showSleepDialog = false
                runCommand { viewModel.cancelSleepTimer() }
            }
        )
    }

    if (showSaveDialog) {
        SaveQueueDialog(
            onDismiss = { showSaveDialog = false },
            onSave = { name ->
                showSaveDialog = false
                runCommand("已保存") { viewModel.saveQueue(name) }
            }
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(t.clearQueueTitle) },
            text = { Text(t.clearQueueBody) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    runCommand { viewModel.clearQueue() }
                }) { Text(t.clear) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text(t.cancel) }
            }
        )
    }

    if (showPowerMenu) {
        PowerCenterDialog(
            playerName = currentPlayer?.name ?: "播放器",
            playerPowerOn = nowPlaying?.power != false,
            hostAddress = server?.daphileBaseUrl.orEmpty(),
            onDismiss = { showPowerMenu = false },
            onTogglePlayerPower = {
                showPowerMenu = false
                runCommand {
                    viewModel.setPlayerPower(!(nowPlaying?.power ?: true))
                }
            },
            onSelectHostAction = { action ->
                showPowerMenu = false
                pendingHostAction = action
            }
        )
    }

    pendingHostAction?.let { action ->
        HostPowerConfirmDialog(
            action = action,
            hostAddress = server?.daphileBaseUrl.orEmpty(),
            onDismiss = { pendingHostAction = null },
            onConfirm = {
                pendingHostAction = null
                runCommand("指令已下发，服务器即将断开") {
                    viewModel.hostPower(action)
                }
            }
        )
    }
}

@Composable
private fun PlayerTopBar(
    serverName: String,
    players: List<Player>,
    currentPlayer: Player?,
    powerOn: Boolean,
    onSelectPlayer: (String) -> Unit,
    onOpenQueue: () -> Unit,
    onTogglePower: () -> Unit,
    onLyrics: () -> Unit,
    onAudioOutput: () -> Unit,
    onMatchCover: () -> Unit,
    onMenuClick: () -> Unit,
    menuExpanded: Boolean,
    onDismissMenu: () -> Unit,
    onSleep: () -> Unit,
    onFavorite: () -> Unit,
    onSaveQueue: () -> Unit,
    onClearQueue: () -> Unit,
    onStop: () -> Unit
) {
    var playerMenu by remember { mutableStateOf(false) }
    val t = strings
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = serverName.ifBlank { "达菲遥控" },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box {
                TextButton(
                    onClick = { if (players.isNotEmpty()) playerMenu = true },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                ) {
                    Icon(
                        Icons.Rounded.Speaker,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = currentPlayer?.name ?: t.choosePlayer,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                }
                DropdownMenu(expanded = playerMenu, onDismissRequest = { playerMenu = false }) {
                    players.forEach { player ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(player.name)
                                    Text(
                                        text = if (player.connected) "在线 · ${player.model.ifBlank { "播放器" }}" else "离线",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Rounded.Speaker,
                                    contentDescription = null,
                                    tint = if (player.id == currentPlayer?.id) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            },
                            onClick = {
                                playerMenu = false
                                onSelectPlayer(player.id)
                            }
                        )
                    }
                }
            }
        }
        IconButton(onClick = onOpenQueue) {
            Icon(
                Icons.Rounded.QueueMusic,
                contentDescription = t.queue,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onLyrics) {
            Icon(
                Icons.Rounded.Subtitles,
                contentDescription = t.lyrics,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onTogglePower) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = t.power,
                tint = if (powerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.MoreVert, contentDescription = t.more)
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text(t.audioOutput) },
                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onAudioOutput()
                    }
                )
                DropdownMenuItem(
                    text = { Text(t.matchCover) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onMatchCover()
                    }
                )
                DropdownMenuItem(
                    text = { Text(t.sleepTimer) },
                    leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                    onClick = onSleep
                )
                DropdownMenuItem(
                    text = { Text(t.favoriteTrack) },
                    leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = null) },
                    onClick = onFavorite
                )
                DropdownMenuItem(
                    text = { Text(t.saveQueue) },
                    leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                    onClick = onSaveQueue
                )
                DropdownMenuItem(
                    text = { Text(t.stop) },
                    leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null) },
                    onClick = onStop
                )
                DropdownMenuItem(
                    text = { Text(t.clearQueue) },
                    leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) },
                    onClick = onClearQueue
                )
            }
        }
    }
}

@Composable
private fun SeekSection(
    nowPlaying: NowPlaying?,
    track: Track,
    onSeek: (Double) -> Unit
) {
    var dragging by remember { mutableStateOf(false) }
    var dragValue by remember { mutableFloatStateOf(0f) }
    val duration = nowPlaying?.duration?.takeIf { it > 0 } ?: track.duration
    val max = duration.toFloat().coerceAtLeast(1f)
    var displayTime by remember { mutableFloatStateOf((nowPlaying?.time ?: 0.0).toFloat()) }
    LaunchedEffect(nowPlaying?.time) {
        if (!dragging) {
            displayTime = (nowPlaying?.time ?: 0.0).toFloat().coerceIn(0f, max)
            dragValue = displayTime
        }
    }
    LaunchedEffect(nowPlaying?.isPlaying) {
        if (nowPlaying?.isPlaying == true) {
            while (true) {
                kotlinx.coroutines.delay(500)
                if (!dragging) {
                    displayTime = (displayTime + 0.5f).coerceAtMost(max)
                }
            }
        }
    }
    val value = if (dragging) dragValue else displayTime.coerceIn(0f, max)
    val canSeek = nowPlaying?.canSeek != false

    Column(modifier = Modifier.fillMaxWidth()) {
        Slider(
            value = value,
            onValueChange = {
                if (canSeek) {
                    dragging = true
                    dragValue = it
                }
            },
            onValueChangeFinished = {
                if (dragging) {
                    dragging = false
                    onSeek(dragValue.toDouble())
                }
            },
            valueRange = 0f..max,
            enabled = canSeek
        )
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = formatDuration(if (dragging) dragValue.toDouble() else value.toDouble()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatDuration(duration),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!canSeek) {
            Text(
                text = "当前音源不支持定位",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PlayerControls(
    nowPlaying: NowPlaying?,
    enabled: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onPlayPause: () -> Unit,
    onRepeat: () -> Unit,
    muted: Boolean,
    muteEnabled: Boolean,
    onToggleMute: () -> Unit
) {
    val activeTint = MaterialTheme.colorScheme.primary
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant
    val t = strings
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = t.prevTrack,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(18.dp))
        Box(contentAlignment = Alignment.Center) {
            if (nowPlaying?.isPlaying == true) {
                PulseRing(
                    modifier = Modifier.size(86.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FilledIconButton(
                onClick = onPlayPause,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    imageVector = if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (nowPlaying?.isPlaying == true) t.pause else t.play,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Spacer(Modifier.width(18.dp))
        IconButton(onClick = onNext, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = t.nextTrack,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = onRepeat, enabled = enabled) {
            Icon(
                imageVector = if ((nowPlaying?.repeat ?: 0) == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = t.repeatMode,
                tint = if ((nowPlaying?.repeat ?: 0) > 0) activeTint else inactiveTint
            )
        }
        IconButton(onClick = onToggleMute, enabled = muteEnabled) {
            Icon(
                imageVector = if (muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                contentDescription = if (muted) t.unmute else t.mute,
                tint = when {
                    !muteEnabled -> inactiveTint.copy(alpha = 0.35f)
                    muted -> activeTint
                    else -> inactiveTint
                }
            )
        }
    }
}

/**
 * 顶部曲目信息：曲名、艺人、专辑与规格标签，左对齐，贴近参考样式。
 */
@Composable
private fun NowPlayingHeader(track: Track?, nowPlaying: NowPlaying?) {
    val t = strings
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = track?.title?.ifBlank { t.unknownTrack } ?: t.noContent,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val quality = nowPlaying?.track?.let { trackQuality(it) }
            if (quality != null) {
                Spacer(Modifier.width(8.dp))
                QualityBadge(quality)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = listOfNotNull(displayArtist(track?.artist), track?.album)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank {
                    if (nowPlaying?.power == false) t.playerOff else t.selectMusic
                },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val specs = trackSpecs(track)
        if (specs.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                specs.forEach { spec -> SpecChip(spec) }
            }
        }
    }
}

/**
 * 参考播放器的一排快捷操作：随机、收藏、分享、睡眠定时、音质信息。
 * 歌词与队列已在页面上有独立入口，这里不再重复。
 */
@Composable
private fun QuickActionsRow(
    shuffleOn: Boolean,
    enabled: Boolean,
    onShuffle: () -> Unit,
    onFavorite: () -> Unit,
    favorite: Boolean,
    onShare: () -> Unit,
    onSleep: () -> Unit,
    onAudioInfo: () -> Unit
) {
    val active = MaterialTheme.colorScheme.primary
    val idle = MaterialTheme.colorScheme.onSurfaceVariant
    val t = strings
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onShuffle, enabled = enabled) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = if (shuffleOn) t.shuffleOff else t.shuffleOn,
                tint = if (shuffleOn) active else idle
            )
        }
        IconButton(onClick = onFavorite, enabled = enabled) {
            Icon(
                imageVector = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (favorite) t.favorited else t.favorite,
                tint = if (favorite) Color(0xFFE85D75) else idle
            )
        }
        IconButton(onClick = onShare, enabled = enabled) {
            Icon(
                Icons.Rounded.IosShare,
                contentDescription = t.share,
                tint = idle
            )
        }
        IconButton(onClick = onSleep) {
            Icon(
                Icons.Rounded.Timer,
                contentDescription = t.sleepTimer,
                tint = idle
            )
        }
        IconButton(onClick = onAudioInfo, enabled = enabled) {
            Icon(
                Icons.Rounded.GraphicEq,
                contentDescription = t.audioInfo,
                tint = idle
            )
        }
    }
}

/**
 * 背景呼吸光：播放时轻微起伏，暂停时静止，营造沉浸感。
 */
@Composable
private fun PulsingBackdrop(accent: Color, active: Boolean) {
    val transition = rememberInfiniteTransition(label = "backdrop")
    val glow by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 5200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "backdropGlow"
    )
    val intensity = if (active) 0.10f + glow * 0.10f else 0.06f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = intensity),
                        MaterialTheme.colorScheme.background.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    )
}

@Composable
private fun VolumeSection(
    nowPlaying: NowPlaying?,
    onVolumeChange: (Int) -> Unit,
    onVolumeFinished: () -> Unit
) {
    var adjusting by remember { mutableStateOf(false) }
    var volumeValue by remember { mutableFloatStateOf((nowPlaying?.volume ?: 0).toFloat()) }
    LaunchedEffect(nowPlaying?.volume) {
        if (!adjusting) volumeValue = (nowPlaying?.volume ?: 0).toFloat()
    }
    val canSetVolume = nowPlaying?.canSetVolume != false
    val t = strings
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = when {
                !canSetVolume -> Icons.Rounded.VolumeOff
                nowPlaying?.muted == true -> Icons.Rounded.VolumeOff
                else -> Icons.Rounded.VolumeUp
            },
            contentDescription = null,
            modifier = Modifier.padding(start = 12.dp, end = 10.dp).size(20.dp),
            tint = if (canSetVolume) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
            }
        )
        if (!canSetVolume) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t.fixedVolume,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = t.volumeDisabledHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                )
            }
        } else {
            Slider(
                value = if (adjusting) volumeValue else (nowPlaying?.volume ?: 0).toFloat(),
                onValueChange = {
                    adjusting = true
                    volumeValue = it
                    onVolumeChange(it.toInt().coerceIn(0, 100))
                },
                onValueChangeFinished = {
                    adjusting = false
                    onVolumeFinished()
                },
                valueRange = 0f..100f,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "${if (adjusting) volumeValue.toInt() else nowPlaying?.volume ?: 0}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(30.dp),
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun QueueSheetContent(
    nowPlaying: NowPlaying?,
    server: ServerConfig?,
    onPlay: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onClear: () -> Unit,
    onGoLibrary: () -> Unit
) {
    val queue = nowPlaying?.queue.orEmpty()
    val t = strings
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = t.queue,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = t.queueTracks.format(queue.size),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onGoLibrary) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = t.goLibrary)
            }
            IconButton(onClick = onClear, enabled = queue.isNotEmpty()) {
                Icon(Icons.Rounded.Delete, contentDescription = t.clearQueue)
            }
        }
        if (queue.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = t.queueEmptyTitle,
                body = t.queueEmptyBody,
                actionLabel = t.goLibrary,
                onAction = onGoLibrary
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 540.dp)) {
                items(items = queue, key = { it.index }) { item ->
                    TrackRow(
                        track = item,
                        coverUrl = server?.coverUrl(item.coverId, nowPlaying?.playerId),
                        showCover = true,
                        isCurrent = item.index == nowPlaying?.currentIndex,
                        trailing = {
                            IconButton(onClick = { onRemove(item.index) }) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = "移除",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = { onPlay(item.index) }
                    )
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SleepTimerDialog(
    activeRemaining: Int?,
    onDismiss: () -> Unit,
    onStart: (Int, SleepAction) -> Unit,
    onCancelTimer: () -> Unit
) {
    var minutes by remember { mutableIntStateOf(30) }
    var action by remember { mutableStateOf(SleepAction.Stop) }
    val t = strings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.sleepTimer) },
        text = {
            Column {
                if (activeRemaining != null) {
                    Text(
                        text = t.sleepRemaining.format(formatRemaining(activeRemaining)),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = t.sleepMinutes.format(minutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 30, 45, 60, 90, 120).forEach { value ->
                        FilterChip(
                            selected = minutes == value,
                            onClick = { minutes = value },
                            label = { Text(t.sleepMinutes.format(value)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = t.sleepAfter,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SleepAction.Stop to t.sleepStop,
                        SleepAction.Pause to t.sleepPause,
                        SleepAction.PowerOff to t.sleepPowerOff
                    ).forEach { (value, label) ->
                        FilterChip(
                            selected = action == value,
                            onClick = { action = value },
                            label = { Text(label) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onStart(minutes, action) }) {
                Text(if (activeRemaining != null) t.restart else t.start)
            }
        },
        dismissButton = {
            Row {
                if (activeRemaining != null) {
                    TextButton(onClick = onCancelTimer) { Text(t.cancelTimer) }
                }
                TextButton(onClick = onDismiss) { Text(t.close) }
            }
        }
    )
}

@Composable
private fun SaveQueueDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    val t = strings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.saveQueue) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(t.playlistName) },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text(t.save) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t.cancel) }
        }
    )
}
