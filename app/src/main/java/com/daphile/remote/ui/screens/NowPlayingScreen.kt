@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.daphile.remote.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.daphile.remote.ui.common.SpecChip
import com.daphile.remote.ui.common.TrackRow
import com.daphile.remote.ui.common.displayArtist
import com.daphile.remote.ui.common.formatDuration
import com.daphile.remote.ui.common.formatRemaining
import com.daphile.remote.ui.common.trackSpecs
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

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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
                title = "还没有连接 Daphile",
                body = "添加局域网中的 Daphile 主机，即可开始遥控播放",
                actionLabel = "添加服务器",
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

    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
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
                        snackbar.showSnackbar(if (matched) "已匹配在线封面" else "未找到在线封面")
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

            Spacer(Modifier.height(10.dp))
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val coverSize = minOf(maxWidth - 12.dp, 310.dp)
                CoverArt(
                    url = coverUrl,
                    modifier = Modifier.size(coverSize),
                    cornerRadius = 24.dp
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(
                text = track?.title?.ifBlank { "未知曲目" } ?: "暂无播放内容",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(6.dp))
            val subtitle = listOfNotNull(displayArtist(track?.artist), track?.album)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            Text(
                text = subtitle.ifBlank {
                    if (nowPlaying?.power == false) "播放器已关闭" else "选择音乐开始播放"
                },
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            val specs = trackSpecs(track)
            if (specs.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Routes.AUDIO_OUTPUT) },
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    specs.forEach { spec ->
                        SpecChip(spec)
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Text(
                    text = "点击音频规格查看实时输出",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }

            if (track != null) {
                Spacer(Modifier.height(18.dp))
                SeekSection(
                    nowPlaying = nowPlaying,
                    track = track,
                    onSeek = { seconds -> runCommand { viewModel.seekTo(seconds) } }
                )
            }

            Spacer(Modifier.height(12.dp))
            PlayerControls(
                nowPlaying = nowPlaying,
                enabled = track != null,
                onPrevious = { runCommand { viewModel.previous() } },
                onNext = { runCommand { viewModel.next() } },
                onPlayPause = { runCommand { viewModel.playPause() } },
                onShuffle = {
                    runCommand {
                        viewModel.setShuffle(!((nowPlaying?.shuffle ?: 0) > 0))
                    }
                },
                onRepeat = {
                    val next = when (nowPlaying?.repeat ?: 0) {
                        0 -> 2
                        2 -> 1
                        else -> 0
                    }
                    runCommand { viewModel.setRepeat(next) }
                }
            )

            Spacer(Modifier.height(18.dp))
            VolumeSection(
                nowPlaying = nowPlaying,
                onVolumeChange = { viewModel.onVolumeChange(it) },
                onVolumeFinished = { viewModel.flushVolume() },
                onToggleMute = { runCommand { viewModel.toggleMute() } }
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
                        text = "睡眠定时剩余 ${formatRemaining(sleepRemaining ?: 0)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { runCommand { viewModel.cancelSleepTimer() } }) {
                        Text("取消")
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = { navController.navigate(Routes.LYRICS) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Subtitles, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("歌词")
                }
                OutlinedButton(
                    onClick = { showQueue = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(19.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("队列 ${nowPlaying?.trackCount ?: 0}")
                }
            }

            if (players.isEmpty() && connection == ConnectionState.Connected) {
                EmptyState(
                    icon = Icons.Rounded.Speaker,
                    title = "服务器暂无可用播放器",
                    body = "请在 Daphile 音频设置中启用播放器，或检查 USB DAC 连接",
                    actionLabel = "查看播放器",
                    onAction = { navController.navigate(Routes.PLAYERS) }
                )
            }

            Spacer(Modifier.height(28.dp))
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
            title = { Text("清空播放队列") },
            text = { Text("清空后当前播放会停止，确定要继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    runCommand { viewModel.clearQueue() }
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
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
                        text = currentPlayer?.name ?: "选择播放器",
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
                contentDescription = "播放队列",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onTogglePower) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = "电源控制",
                tint = if (powerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "更多")
            }
            DropdownMenu(expanded = menuExpanded, onDismissRequest = onDismissMenu) {
                DropdownMenuItem(
                    text = { Text("歌词") },
                    leadingIcon = { Icon(Icons.Rounded.Subtitles, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onLyrics()
                    }
                )
                DropdownMenuItem(
                    text = { Text("音频输出信息") },
                    leadingIcon = { Icon(Icons.Rounded.GraphicEq, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onAudioOutput()
                    }
                )
                DropdownMenuItem(
                    text = { Text("在线匹配封面") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    onClick = {
                        onDismissMenu()
                        onMatchCover()
                    }
                )
                DropdownMenuItem(
                    text = { Text("睡眠定时") },
                    leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null) },
                    onClick = onSleep
                )
                DropdownMenuItem(
                    text = { Text("收藏当前曲目") },
                    leadingIcon = { Icon(Icons.Rounded.FavoriteBorder, contentDescription = null) },
                    onClick = onFavorite
                )
                DropdownMenuItem(
                    text = { Text("保存队列为播放列表") },
                    leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = null) },
                    onClick = onSaveQueue
                )
                DropdownMenuItem(
                    text = { Text("停止播放") },
                    leadingIcon = { Icon(Icons.Rounded.Stop, contentDescription = null) },
                    onClick = onStop
                )
                DropdownMenuItem(
                    text = { Text("清空队列") },
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
    onShuffle: () -> Unit,
    onRepeat: () -> Unit
) {
    val activeTint = MaterialTheme.colorScheme.primary
    val inactiveTint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onShuffle, enabled = enabled) {
            Icon(
                Icons.Rounded.Shuffle,
                contentDescription = "随机播放",
                tint = if ((nowPlaying?.shuffle ?: 0) > 0) activeTint else inactiveTint
            )
        }
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = onPrevious, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "上一首",
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        FilledIconButton(
            onClick = onPlayPause,
            modifier = Modifier.size(72.dp)
        ) {
            Icon(
                imageVector = if (nowPlaying?.isPlaying == true) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (nowPlaying?.isPlaying == true) "暂停" else "播放",
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onNext, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "下一首",
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = onRepeat, enabled = enabled) {
            Icon(
                imageVector = if ((nowPlaying?.repeat ?: 0) == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = "循环模式",
                tint = if ((nowPlaying?.repeat ?: 0) > 0) activeTint else inactiveTint
            )
        }
    }
}

@Composable
private fun VolumeSection(
    nowPlaying: NowPlaying?,
    onVolumeChange: (Int) -> Unit,
    onVolumeFinished: () -> Unit,
    onToggleMute: () -> Unit
) {
    var adjusting by remember { mutableStateOf(false) }
    var volumeValue by remember { mutableFloatStateOf((nowPlaying?.volume ?: 0).toFloat()) }
    LaunchedEffect(nowPlaying?.volume) {
        if (!adjusting) volumeValue = (nowPlaying?.volume ?: 0).toFloat()
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onToggleMute) {
            Icon(
                imageVector = if (nowPlaying?.muted == true) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                contentDescription = if (nowPlaying?.muted == true) "取消静音" else "静音",
                tint = if (nowPlaying?.muted == true) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
        if (nowPlaying?.canSetVolume == false) {
            Text(
                text = "固定音量",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "播放队列",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${queue.size} 首曲目",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onGoLibrary) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = "去音乐库")
            }
            IconButton(onClick = onClear, enabled = queue.isNotEmpty()) {
                Icon(Icons.Rounded.Delete, contentDescription = "清空队列")
            }
        }
        if (queue.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "队列为空",
                body = "去音乐库挑一张专辑开始播放",
                actionLabel = "去音乐库",
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("睡眠定时") },
        text = {
            Column {
                if (activeRemaining != null) {
                    Text(
                        text = "当前剩余 ${formatRemaining(activeRemaining)}",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = "倒计时时长",
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
                            label = { Text("$value 分钟") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "到点后",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        SleepAction.Stop to "停止播放",
                        SleepAction.Pause to "暂停播放",
                        SleepAction.PowerOff to "关闭播放器"
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
                Text(if (activeRemaining != null) "重新开始" else "开始")
            }
        },
        dismissButton = {
            Row {
                if (activeRemaining != null) {
                    TextButton(onClick = onCancelTimer) { Text("取消定时") }
                }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        }
    )
}

@Composable
private fun SaveQueueDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存队列") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("播放列表名称") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
