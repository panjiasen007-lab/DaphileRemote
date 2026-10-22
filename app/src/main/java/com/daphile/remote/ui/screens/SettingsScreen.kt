@file:OptIn(coil.annotation.ExperimentalCoilApi::class)

package com.daphile.remote.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.data.model.friendlyErrorMessage
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.navigation.Routes
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

    var showThemeDialog by remember { mutableStateOf(false) }
    var showPollDialog by remember { mutableStateOf(false) }
    var showRescanDialog by remember { mutableStateOf(false) }
    var powerAction by remember { mutableStateOf<HostPowerAction?>(null) }

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
            PageHeader(title = "设置")

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
                                    text = server?.name ?: "尚未添加服务器",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = server?.let { "${it.host}:${it.port}" } ?: "添加 Daphile 主机以开始使用",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (serverInfo != null) {
                                    Text(
                                        text = "LMS ${serverInfo?.version.orEmpty()} · ${players.size} 个播放器" +
                                            " · 管理端口 ${server?.daphilePort ?: 80}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Text(
                                text = when (connection) {
                                    ConnectionState.Connected -> "在线"
                                    ConnectionState.Connecting -> "连接中"
                                    ConnectionState.Reconnecting -> "重连中"
                                    ConnectionState.Lost -> "不可达"
                                    else -> "未连接"
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
                                text = "服务器管理",
                                icon = Icons.Rounded.Wifi,
                                modifier = Modifier.weight(1f)
                            ) { navController.navigate(Routes.SERVERS) }
                            SettingActionButton(
                                text = "Daphile 管理",
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
                                text = "LMS 网页端",
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
                                text = "系统信息",
                                icon = Icons.Rounded.Info,
                                modifier = Modifier.weight(1f)
                            ) { navController.navigate(Routes.SYSTEM_INFO) }
                        }
                    }
                }
            }

            SettingsSectionTitle("播放与同步")
            SettingsRow(
                title = "状态刷新间隔",
                subtitle = "推送不可用时的状态轮询频率",
                value = "${settings.pollSeconds} 秒",
                onClick = { showPollDialog = true }
            )
            SettingsRow(
                title = "媒体通知",
                subtitle = "在通知栏显示播放控制",
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
                title = "锁屏媒体控制",
                subtitle = "在锁屏界面显示封面与播放控制",
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
                title = "在线歌词与封面",
                subtitle = "LRCLIB、MusicBrainz、网易云与 iTunes",
                trailing = {
                    Switch(
                        checked = settings.onlineMetadataEnabled,
                        onCheckedChange = { enabled ->
                            runCommand { viewModel.setOnlineMetadataEnabled(enabled) }
                        }
                    )
                }
            )

            SettingsSectionTitle("外观")
            SettingsRow(
                title = "主题",
                subtitle = "深色模式适合夜间听音",
                value = themeLabel(settings.theme),
                icon = Icons.Rounded.Palette,
                onClick = { showThemeDialog = true }
            )

            SettingsSectionTitle("存储")
            SettingsRow(
                title = "清除封面缓存",
                subtitle = "封面图片可随时重新加载",
                icon = Icons.Rounded.Delete,
                onClick = {
                    scope.launch {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            context.imageLoader.memoryCache?.clear()
                            context.imageLoader.diskCache?.clear()
                            File(context.cacheDir, "widget_cover.png").delete()
                        }
                        snackbar.showSnackbar("缓存已清除")
                    }
                }
            )

            SettingsSectionTitle("系统")
            SettingsRow(
                title = "系统信息",
                subtitle = "服务器版本、播放器与音乐库统计",
                icon = Icons.Rounded.Info,
                onClick = { navController.navigate(Routes.SYSTEM_INFO) }
            )
            SettingsRow(
                title = "重新扫描音乐库",
                subtitle = "让 Daphile 重新索引音乐目录",
                icon = Icons.Rounded.Refresh,
                enabled = server != null,
                onClick = { showRescanDialog = true }
            )
            SettingsRow(
                title = "主机电源控制",
                subtitle = "管理地址 ${server?.daphileBaseUrl ?: "—"}",
                showChevron = false
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "重启与挂起使用 Daphile 官方 Web 快捷方式，关机使用 /cgi-bin/Info?ACTION=shutdown；每次操作都需要二次确认。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PowerButton("重启", Modifier.weight(1f)) {
                        powerAction = HostPowerAction.Restart
                    }
                    PowerButton("关机", Modifier.weight(1f), error = true) {
                        powerAction = HostPowerAction.Shutdown
                    }
                    PowerButton("挂起", Modifier.weight(1f)) {
                        powerAction = HostPowerAction.Suspend
                    }
                }
            }

            SettingsSectionTitle("关于")
            SettingsRow(
                title = "版本",
                value = BuildConfig.VERSION_NAME,
                showChevron = false
            )
            SettingsRow(
                title = "语言",
                value = "简体中文",
                showChevron = false
            )
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                Text(
                    text = "达菲遥控 · Daphile Remote\n面向 Daphile 25.05 的第三方遥控应用。应用不收集个人数据，所有服务器信息与凭据仅保存在本机。",
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
        val themes = listOf(
            "system" to "跟随系统",
            "dark" to "深色",
            "light" to "浅色"
        )
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("主题") },
            text = {
                Column {
                    themes.forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showThemeDialog = false
                                    runCommand { viewModel.setTheme(value) }
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.theme == value,
                                onClick = {
                                    showThemeDialog = false
                                    runCommand { viewModel.setTheme(value) }
                                }
                            )
                            Text(label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("关闭") }
            }
        )
    }

    if (showPollDialog) {
        AlertDialog(
            onDismissRequest = { showPollDialog = false },
            title = { Text("状态刷新间隔") },
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
                            Text("$seconds 秒")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPollDialog = false }) { Text("关闭") }
            }
        )
    }

    if (showRescanDialog) {
        AlertDialog(
            onDismissRequest = { showRescanDialog = false },
            title = { Text("重新扫描音乐库") },
            text = { Text("触发服务器重新扫描音乐库？扫描期间音乐库可能不完整。") },
            confirmButton = {
                TextButton(onClick = {
                    showRescanDialog = false
                    runCommand("已触发音乐库扫描") { viewModel.rescan() }
                }) { Text("扫描") }
            },
            dismissButton = {
                TextButton(onClick = { showRescanDialog = false }) { Text("取消") }
            }
        )
    }

    powerAction?.let { action ->
        HostPowerConfirmDialog(
            action = action,
            hostAddress = server?.daphileBaseUrl.orEmpty(),
            onDismiss = { powerAction = null },
            onConfirm = {
                powerAction = null
                runCommand("指令已下发，服务器即将断开") { viewModel.hostAction(action) }
            }
        )
    }
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

private fun themeLabel(theme: String): String = when (theme) {
    "light" -> "浅色"
    "system" -> "跟随系统"
    else -> "深色"
}
