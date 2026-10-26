package com.daphile.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.ConnectionState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.strings
import kotlinx.coroutines.launch

@Composable
fun SystemInfoScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val info by viewModel.serverInfo.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()

    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val t = strings

    val connectionLabel = when (connection) {
        ConnectionState.Connected -> t.online
        ConnectionState.Connecting -> t.connecting
        ConnectionState.Reconnecting -> t.reconnecting
        ConnectionState.Lost -> t.unreachable
        else -> t.notConnected
    }

    val copyText = buildString {
        appendLine(t.systemInfoCopyTitle)
        appendLine(
            t.systemInfoCopyServer.format(
                server?.name.orEmpty(),
                server?.host.orEmpty(),
                server?.port ?: 0
            )
        )
        appendLine(t.systemInfoCopyVersion.format(info?.version.orEmpty()))
        appendLine(t.systemInfoCopyState.format(connectionLabel))
        appendLine(t.systemInfoCopyPlayers.format(players.size))
        appendLine(
            t.systemInfoCopyLibrary.format(
                info?.albumCount ?: 0,
                info?.songCount ?: 0,
                info?.artistCount ?: 0
            )
        )
        append(t.systemInfoCopyNetwork.format(info?.ip.orEmpty(), info?.httpPort ?: 0))
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PageHeader(title = t.systemInfo, onBack = { navController.popBackStack() })
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = server?.name ?: t.notConnected,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = server?.let { "${it.host}:${it.port}" } ?: "—",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        InfoRow(t.lmsVersion, info?.version.orEmpty().ifBlank { "—" })
                        InfoRow(t.serverStatus, connectionLabel)
                        InfoRow(t.networkAddress, "${info?.ip.orEmpty().ifBlank { "—" }}:${info?.httpPort ?: 0}")
                        InfoRow(t.playerCount, "${players.size}")
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = t.libraryTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(10.dp))
                        InfoRow(t.albums, "${info?.albumCount ?: 0}")
                        InfoRow(t.tracks, "${info?.songCount ?: 0}")
                        InfoRow(t.artists, "${info?.artistCount ?: 0}")
                        InfoRow(t.genres, "${info?.genreCount ?: 0}")
                        if (info?.scanning == true) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = t.scanningLibrary,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = t.playersTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        if (players.isEmpty()) {
                            Text(
                                text = t.noPlayers,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            players.forEach { player ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = CircleShape,
                                        color = if (player.connected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    ) {}
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = player.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = player.model.ifBlank { "Squeezelite" },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = if (player.connected) t.online else t.offline,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (player.connected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(copyText))
                        scope.launch { snackbar.showSnackbar(t.copiedToClipboard) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(t.copyInfo)
                }
                Spacer(Modifier.height(28.dp))
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
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
