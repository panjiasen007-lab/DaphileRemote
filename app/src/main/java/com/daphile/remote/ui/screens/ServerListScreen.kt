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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.daphile.remote.data.model.ConnectionState
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.EmptyState
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch

@Composable
fun ServerListScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val currentServer by viewModel.server.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val t = strings
    var deleteTarget by remember { mutableStateOf<ServerConfig?>(null) }

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
            PageHeader(
                title = t.serverSection,
            onBack = if (navController.previousBackStackEntry != null) {
                { navController.popBackStack() }
            } else null,
            actions = {
                IconButton(onClick = {
                    navController.navigate(Routes.serverEdit(first = servers.isEmpty()))
                }) {
                    Icon(Icons.Rounded.Add, contentDescription = t.addServer)
                }
            }
        )

        if (servers.isEmpty()) {
            EmptyState(
                icon = Icons.Rounded.Speaker,
                title = t.noServerAdded,
                body = t.discoverHint,
                actionLabel = t.addServer,
                onAction = {
                    navController.navigate(Routes.serverEdit(first = servers.isEmpty()))
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(items = servers, key = { it.id }) { server ->
                    val isCurrent = server.id == currentServer?.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (!isCurrent) {
                                    runCommand(t.switchedTo.format(server.name)) {
                                        viewModel.switchServer(server.id)
                                    }
                                }
                            },
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = if (isCurrent) 3.dp else 1.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isCurrent && connection == ConnectionState.Connected) {
                                    Icons.Rounded.Wifi
                                } else {
                                    Icons.Rounded.WifiOff
                                },
                                contentDescription = null,
                                tint = if (isCurrent && connection == ConnectionState.Connected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = server.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isCurrent) {
                                        Spacer(Modifier.width(8.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = t.currentBadge,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${server.host}:${server.port}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                navController.navigate(Routes.serverEdit(server.id))
                            }) {
                                Icon(
                                    Icons.Rounded.Edit,
                                    contentDescription = t.edit,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            IconButton(onClick = { deleteTarget = server }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = t.delete,
                                    modifier = Modifier.size(19.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(t.deleteServerTitle) },
            text = { Text(t.deleteServerBody.format(target.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    runCommand(t.deleted) { viewModel.deleteServer(target.id) }
                }) { Text(t.delete) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(t.cancel) }
            }
        )
    }
}
