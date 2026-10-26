package com.daphile.remote.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.data.model.DiscoveredServer
import com.daphile.remote.data.model.ServerConfig
import com.daphile.remote.data.model.ServerInfo
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun ServerEditScreen(
    navController: NavController,
    serverId: String?,
    first: Boolean,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel()
    val servers by viewModel.servers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val t = strings

    var name by remember { mutableStateOf("") }
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("9000") }
    var daphilePort by remember { mutableStateOf("80") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var initialized by remember { mutableStateOf(false) }

    var saving by remember { mutableStateOf(false) }
    var discovering by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var discovered by remember { mutableStateOf<List<DiscoveredServer>?>(null) }
    var lastInfo by remember { mutableStateOf<ServerInfo?>(null) }

    val existing = remember(serverId, servers) {
        servers.firstOrNull { it.id == serverId }
    }

    LaunchedEffect(serverId, servers) {
        if (initialized) return@LaunchedEffect
        if (serverId.isNullOrBlank()) {
            initialized = true
        } else if (existing != null) {
            name = existing.name
            host = existing.host
            port = existing.port.toString()
            daphilePort = existing.daphilePort.toString()
            username = existing.username
            password = existing.password
            initialized = true
        }
    }

    fun buildConfig(): ServerConfig? {
        val cleanHost = host.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .trimEnd('/')
        if (cleanHost.isBlank()) {
            errorText = t.hostRequired
            return null
        }
        val portValue = port.trim().toIntOrNull()
        if (portValue == null || portValue !in 1..65535) {
            errorText = t.lmsPortRange
            return null
        }
        val daphilePortValue = daphilePort.trim().toIntOrNull()
        if (daphilePortValue == null || daphilePortValue !in 1..65535) {
            errorText = t.daphilePortRange
            return null
        }
        return ServerConfig(
            id = existing?.id ?: UUID.randomUUID().toString(),
            name = name.trim().ifBlank { cleanHost },
            host = cleanHost,
            port = portValue,
            daphilePort = daphilePortValue,
            username = username.trim(),
            password = password,
            useHttps = existing?.useHttps ?: false
        )
    }

    fun save() {
        val config = buildConfig() ?: return
        errorText = null
        saving = true
        scope.launch {
            try {
                lastInfo = viewModel.testServer(config)
                viewModel.saveServer(config)
                if (first) {
                    navController.navigate(Routes.NOW) { launchSingleTop = true }
                } else {
                    navController.popBackStack()
                }
            } catch (error: Throwable) {
                errorText = friendlyErrorMessage(error)
            } finally {
                saving = false
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        PageHeader(
            title = if (first) t.connectYourDaphile else if (existing == null) t.addServer else t.editServer,
            onBack = if (!first && navController.previousBackStackEntry != null) {
                { navController.popBackStack() }
            } else null
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                bottom = 32.dp
            )
        ) {
            if (first) {
                item {
                    Text(
                        text = t.serverIntro,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(20.dp))
                }
            }
            item {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t.nameOptional) },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = host,
                    onValueChange = { host = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t.hostAddressLabel) },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t.lmsHttpPort) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = daphilePort,
                    onValueChange = { daphilePort = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t.daphilePortLabel) },
                    supportingText = { Text(t.daphilePortHint) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t.usernameOptional) },
                    singleLine = true
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(t.passwordOptional) },
                    supportingText = { Text(t.passwordHint) },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (passwordVisible) t.hidePassword else t.showPassword
                            )
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
                if (existing == null) {
                    OutlinedButton(
                        onClick = {
                            discovering = true
                            errorText = null
                            scope.launch {
                                try {
                                    discovered = viewModel.discoverServers()
                                } catch (error: Throwable) {
                                    errorText = friendlyErrorMessage(error)
                                } finally {
                                    discovering = false
                                }
                            }
                        },
                        enabled = !discovering && !saving,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (discovering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(t.scanningLan)
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(t.discoverServers)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                if (errorText != null) {
                    Text(
                        text = errorText.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }
                if (lastInfo != null) {
                    Text(
                        text = t.connectedToLms.format(lastInfo?.version.orEmpty()),
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Button(
                    onClick = { save() },
                    enabled = !saving && !discovering,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(t.connectingServer)
                    } else {
                        Text(t.testAndSave)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = t.serverEditNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    discovered?.let { list ->
        AlertDialog(
            onDismissRequest = { discovered = null },
            title = { Text(if (list.isEmpty()) t.noServersFound else t.serversFoundCount.format(list.size)) },
            text = {
                if (list.isEmpty()) {
                    Text(t.noServersFoundBody)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(items = list, key = { it.host }) { server ->
                            TextButton(
                                onClick = {
                                    host = server.host
                                    port = server.port.toString()
                                    if (name.isBlank()) name = server.name
                                    discovered = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(server.name, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        text = "${server.host}:${server.port} · LMS ${server.version}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { discovered = null }) { Text(t.close) }
            }
        )
    }
}
