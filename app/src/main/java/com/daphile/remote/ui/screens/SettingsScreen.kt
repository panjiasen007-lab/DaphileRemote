@file:OptIn(coil.annotation.ExperimentalCoilApi::class)

package com.daphile.remote.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.imageLoader
import com.daphile.remote.BuildConfig
import com.daphile.remote.data.model.ConnectionState
import com.daphile.remote.data.model.AppLanguage
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.i18n.languageDisplayName
import com.daphile.remote.ui.i18n.strings
import com.daphile.remote.ui.navigation.Routes
import com.daphile.remote.ui.theme.AppTheme
import com.daphile.remote.ui.theme.colorSchemeFor
import com.daphile.remote.ui.theme.themeDisplayName
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: SettingsViewModel = viewModel()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val settings by viewModel.appSettings.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val t = strings

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showRescanDialog by remember { mutableStateOf(false) }
    var showClearLyricsDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var powerAction by remember { mutableStateOf<HostPowerAction?>(null) }
    var lyricsCount by remember { mutableIntStateOf(viewModel.lyricsLibraryCount()) }

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PageHeader(title = t.settingsTitle)

            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Speaker,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = server?.name ?: t.noServerAdded,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = server?.let { "${it.host}:${it.port}" } ?: t.addServerHint,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (serverInfo != null) {
                                    Text(
                                        text = t.serverSummary.format(
                                            serverInfo?.version.orEmpty(),
                                            players.size,
                                            server?.daphilePort ?: 80
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = when (connection) {
                                    ConnectionState.Connected -> t.online
                                    ConnectionState.Connecting -> t.connecting
                                    ConnectionState.Reconnecting -> t.reconnecting
                                    ConnectionState.Lost -> t.unreachable
                                    else -> t.notConnected
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (connection) {
                                    ConnectionState.Connected -> MaterialTheme.colorScheme.primary
                                    ConnectionState.Lost -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingActionButton(
                                text = t.serverManage,
                                icon = Icons.Rounded.Wifi,
                                modifier = Modifier.weight(1f)
                            ) { navController.navigate(Routes.SERVERS) }
                            SettingActionButton(
                                text = t.daphileManage,
                                icon = Icons.Rounded.OpenInNew,
                                enabled = server != null,
                                modifier = Modifier.weight(1f)
                            ) {
                                val current = server
                                if (current == null) return@SettingActionButton
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(current.daphileBaseUrl))
                                    )
                                } catch (error: Throwable) {
                                    scope.launch {
                                        snackbar.showSnackbar(friendlyErrorMessage(error))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            SettingActionButton(
                                text = t.lmsWeb,
                                icon = Icons.Rounded.MusicNote,
                                enabled = server != null,
                                modifier = Modifier.weight(1f)
                            ) {
                                val current = server
                                if (current == null) return@SettingActionButton
                                try {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(current.baseUrl))
                                    )
                                } catch (error: Throwable) {
                                    scope.launch {
                                        snackbar.showSnackbar(friendlyErrorMessage(error))
                                    }
                                }
                            }
                            SettingActionButton(
                                text = t.systemInfo,
                                icon = Icons.Rounded.Info,
                                modifier = Modifier.weight(1f)
                            ) { navController.navigate(Routes.SYSTEM_INFO) }
                        }
                    }
                }
            }

            SettingsSectionTitle(t.playbackSection)
            SettingsRow(
                title = t.pollInterval,
                subtitle = t.pollIntervalSub,
                value = t.seconds.format(settings.pollSeconds),
                onClick = { showPollDialog = true }
            )
            SettingsRow(
                title = t.mediaNotification,
                subtitle = t.mediaNotificationSub,
                trailing = {
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            runCommand { viewModel.setNotificationsEnabled(enabled) }
                        }
                    )
                }
            )
            SettingsRow(
                title = t.lockScreen,
                subtitle = t.lockScreenSub,
                trailing = {
                    Switch(
                        checked = settings.lockScreenControls,
                        onCheckedChange = { enabled ->
                            runCommand { viewModel.setLockScreenControls(enabled) }
                        }
                    )
                }
            )
            SettingsRow(
                title = t.onlineMeta,
                subtitle = t.onlineMetaSub,
                trailing = {
                    Switch(
                        checked = settings.onlineMetadataEnabled,
                        onCheckedChange = { enabled ->
                            runCommand { viewModel.setOnlineMetadataEnabled(enabled) }
                        }
                    )
                }
            )

            SettingsSectionTitle(t.appearanceSection)
            SettingsRow(
                title = t.theme,
                value = themeDisplayName(AppTheme.fromKey(settings.theme), t),
                icon = Icons.Rounded.Palette,
                onClick = { showThemeDialog = true }
            )
            SettingsRow(
                title = t.language,
                value = languageDisplayName(AppLanguage.fromKey(settings.language), t),
                icon = Icons.Rounded.Language,
                onClick = { showLanguageDialog = true }
            )

            SettingsSectionTitle(t.storageSection)
            SettingsRow(
                title = t.lyricsLibrary,
                subtitle = viewModel.lyricsLibraryPath(),
                value = t.lyricsFileCount.format(lyricsCount),
                icon = Icons.Rounded.Lyrics,
                onClick = { showClearLyricsDialog = true }
            )
            SettingsRow(
                title = t.clearCoverCache,
                subtitle = t.clearCoverCacheSub,
                icon = Icons.Rounded.Delete,
                onClick = {
                    scope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            context.imageLoader.memoryCache?.clear()
                            context.imageLoader.diskCache?.clear()
                            File(context.cacheDir, "widget_cover.png").delete()
                        }
                        snackbar.showSnackbar(t.cacheCleared)
                    }
                }
            )

            SettingsSectionTitle(t.systemSection)
            SettingsRow(
                title = t.systemInfo,
                subtitle = t.systemInfoSub,
                icon = Icons.Rounded.Info,
                onClick = { navController.navigate(Routes.SYSTEM_INFO) }
            )
            SettingsRow(
                title = t.rescan,
                subtitle = t.rescanSub,
                icon = Icons.Rounded.Refresh,
                enabled = server != null,
                onClick = { showRescanDialog = true }
            )
            SettingsRow(
                title = t.hostPower,
                subtitle = t.manageAddress.format(server?.daphileBaseUrl ?: "—"),
                showChevron = false
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = t.hostPowerHint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PowerButton(t.reboot, Modifier.weight(1f)) {
                        powerAction = HostPowerAction.Restart
                    }
                    PowerButton(t.shutdown, Modifier.weight(1f), error = true) {
                        powerAction = HostPowerAction.Shutdown
                    }
                    PowerButton(t.suspend, Modifier.weight(1f)) {
                        powerAction = HostPowerAction.Suspend
                    }
                }
            }

            SettingsSectionTitle(t.aboutSection)
            SettingsRow(
                title = t.version,
                value = BuildConfig.VERSION_NAME,
                icon = Icons.Rounded.Info,
                onClick = { showAboutDialog = true }
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(
                    text = t.aboutLicense,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(32.dp))
        }

        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }

    if (showThemeDialog) {
        val themes = AppTheme.entries
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(t.theme) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    themes.forEach { theme ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showThemeDialog = false
                                    runCommand { viewModel.setTheme(theme.key) }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.theme == theme.key,
                                onClick = {
                                    showThemeDialog = false
                                    runCommand { viewModel.setTheme(theme.key) }
                                }
                            )
                            ThemeSwatch(theme)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = themeDisplayName(theme, t),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text(t.close) }
            }
        )
    }

    if (showPollDialog) {
        AlertDialog(
            onDismissRequest = { showPollDialog = false },
            title = { Text(t.pollInterval) },
            text = {
                Column {
                    listOf(2, 3, 5, 10).forEach { seconds ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showPollDialog = false
                                    runCommand { viewModel.setPollSeconds(seconds) }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.pollSeconds == seconds,
                                onClick = {
                                    showPollDialog = false
                                    runCommand { viewModel.setPollSeconds(seconds) }
                                }
                            )
                            Text(t.seconds.format(seconds))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPollDialog = false }) { Text(t.close) }
            }
        )
    }

    if (showRescanDialog) {
        AlertDialog(
            onDismissRequest = { showRescanDialog = false },
            title = { Text(t.rescanTitle) },
            text = { Text(t.rescanBody) },
            confirmButton = {
                TextButton(onClick = {
                    showRescanDialog = false
                    runCommand(t.scanStarted) { viewModel.rescan() }
                }) { Text(t.scan) }
            },
            dismissButton = {
                TextButton(onClick = { showRescanDialog = false }) { Text(t.cancel) }
            }
        )
    }

    if (showClearLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showClearLyricsDialog = false },
            title = { Text(t.clearLyricsTitle) },
            text = { Text(t.clearLyricsBody.format(lyricsCount)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearLyricsDialog = false
                    scope.launch {
                        runCatching { viewModel.clearLyricsLibrary() }
                        lyricsCount = viewModel.lyricsLibraryCount()
                        snackbar.showSnackbar(t.lyricsCleared)
                    }
                }) { Text(t.clear) }
            },
            dismissButton = {
                TextButton(onClick = { showClearLyricsDialog = false }) { Text(t.cancel) }
            }
        )
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(t.language) },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    AppLanguage.entries.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showLanguageDialog = false
                                    runCommand { viewModel.setLanguage(option) }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = AppLanguage.fromKey(settings.language) == option,
                                onClick = {
                                    showLanguageDialog = false
                                    runCommand { viewModel.setLanguage(option) }
                                }
                            )
                            Text(languageDisplayName(option, t))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text(t.close) }
            }
        )
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    powerAction?.let { action ->
        HostPowerConfirmDialog(
            action = action,
            hostAddress = server?.daphileBaseUrl.orEmpty(),
            onDismiss = { powerAction = null },
            onConfirm = {
                powerAction = null
                runCommand(t.hostCommandSent) { viewModel.hostAction(action) }
            }
        )
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val t = strings
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text(t.aboutTitle) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = t.aboutAppName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${t.version} ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = t.aboutIntro,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = t.aboutFeatures,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                listOf(
                    t.aboutFeature1,
                    t.aboutFeature2,
                    t.aboutFeature3,
                    t.aboutFeature4
                ).forEach { feature ->
                    Text(
                        text = "· $feature",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = t.aboutAuthor,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = t.aboutLicense,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t.close) }
        }
    )
}

@Composable
private fun SettingsSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String? = null,
    value: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    enabled: Boolean = true,
    showChevron: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled && onClick != null) { onClick?.invoke() }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.width(6.dp))
        }
        if (trailing != null) {
            trailing()
        } else if (showChevron && onClick != null) {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PowerButton(
    text: String,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = if (error) {
            MaterialTheme.colorScheme.errorContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.PowerSettingsNew,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun ThemeSwatch(theme: AppTheme) {
    val scheme = remember(theme) { colorSchemeFor(theme, theme.isDarkFamily) }
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        listOf(
            scheme.primary,
            scheme.secondary,
            scheme.surfaceVariant,
            scheme.background
        ).forEach { color ->
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
