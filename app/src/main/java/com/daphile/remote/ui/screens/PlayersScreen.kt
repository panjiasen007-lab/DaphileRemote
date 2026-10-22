package com.daphile.remote.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.Player
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.PageHeader
import kotlinx.coroutines.launch

@Composable
fun PlayersScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: PlayersViewModel = viewModel()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val currentPlayerId by viewModel.currentPlayerId.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

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

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            PageHeader(title = "播放器")
            if (players.isEmpty()) {
                EmptyState(
                    icon = Icons.Rounded.Speaker,
                    title = "没有可用播放器",
                    body = "请在 Daphile 音频设置中启用播放器，或检查 USB DAC 连接"
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(items = players, key = { it.id }) { player ->
                        PlayerCard(
                            player = player,
                            players = players,
                            isSelected = player.id == currentPlayerId,
                            onSelect = {
                                viewModel.selectPlayer(player.id)
                                scope.launch { snackbar.showSnackbar("已切换到 ${player.name}") }
                            },
                            onPowerToggle = {
                                runCommand { viewModel.setPower(player.id, !player.power) }
                            },
                            onSyncTo = { masterId ->
                                runCommand("同步设置已生效") { viewModel.syncTo(player.id, masterId) }
                            },
                            onUnsync = {
                                runCommand("已取消同步") { viewModel.unsync(player.id) }
                            },
                            onVolumeChange = { viewModel.onVolumeChange(player.id, it) },
                            onVolumeFinished = { viewModel.flushVolume(player.id) }
                        )
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun PlayerCard(
    player: Player,
    players: List<Player>,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onPowerToggle: () -> Unit,
    onSyncTo: (String) -> Unit,
    onUnsync: () -> Unit,
    onVolumeChange: (Int) -> Unit,
    onVolumeFinished: () -> Unit
) {
    val others = players.filter { it.id != player.id && it.connected }
    val master = players.firstOrNull { it.id == player.syncMaster }
    var syncMenu by remember { mutableStateOf(false) }
    var adjustingVolume by remember { mutableStateOf(false) }
    var volumeValue by remember { mutableFloatStateOf(player.volume.toFloat()) }
    LaunchedEffect(player.volume) {
        if (!adjustingVolume) volumeValue = player.volume.toFloat()
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = player.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelected) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "当前",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Text(
                        text = listOf(player.model, if (player.connected) "在线" else "离线")
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (player.connected) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        }
                    )
                }
                IconButton(onClick = onPowerToggle, enabled = player.connected) {
                    Icon(
                        imageVector = if (player.connected) Icons.Rounded.PowerSettingsNew else Icons.Rounded.WifiOff,
                        contentDescription = if (player.power) "关闭电源" else "开启电源",
                        tint = if (player.connected && player.power) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            if (player.currentTitle.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = player.currentTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = player.currentArtist,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (player.muted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Slider(
                    value = if (adjustingVolume) volumeValue else player.volume.toFloat(),
                    onValueChange = {
                        adjustingVolume = true
                        volumeValue = it
                        onVolumeChange(it.toInt().coerceIn(0, 100))
                    },
                    onValueChangeFinished = {
                        adjustingVolume = false
                        onVolumeFinished()
                    },
                    valueRange = 0f..100f,
                    enabled = player.connected,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (adjustingVolume) volumeValue.toInt().toString() else player.volume.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    master != null -> {
                        Text(
                            text = "已同步到 ${master.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onUnsync, enabled = player.connected) {
                            Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("取消同步")
                        }
                    }
                    player.syncSlaves.isNotEmpty() -> {
                        val names = player.syncSlaves.mapNotNull { id ->
                            players.firstOrNull { it.id == id }?.name
                        }
                        Text(
                            text = "同步主播放器" + if (names.isNotEmpty()) " · ${names.joinToString("、")}" else "",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    else -> {
                        Text(
                            text = "独立播放",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                if (master == null && others.isNotEmpty()) {
                    Box {
                        TextButton(onClick = { syncMenu = true }, enabled = player.connected) {
                            Icon(Icons.Rounded.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("同步到…")
                        }
                        DropdownMenu(expanded = syncMenu, onDismissRequest = { syncMenu = false }) {
                            others.forEach { target ->
                                DropdownMenuItem(
                                    text = { Text(target.name) },
                                    onClick = {
                                        syncMenu = false
                                        onSyncTo(target.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
