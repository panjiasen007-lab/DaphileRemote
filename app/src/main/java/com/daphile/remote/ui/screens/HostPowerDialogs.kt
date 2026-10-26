package com.daphile.remote.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.daphile.remote.data.model.HostPowerAction
import com.daphile.remote.ui.i18n.UiStrings
import com.daphile.remote.ui.i18n.strings

@Composable
fun PowerCenterDialog(
    playerName: String,
    playerPowerOn: Boolean,
    hostAddress: String,
    onDismiss: () -> Unit,
    onTogglePlayerPower: () -> Unit,
    onSelectHostAction: (HostPowerAction) -> Unit
) {
    val t = strings
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.power) },
        text = {
            Column {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(
                            text = t.playerWithName.format(playerName),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (playerPowerOn) t.poweredOn else t.poweredOff,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = onTogglePlayerPower,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Rounded.PowerSettingsNew,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(if (playerPowerOn) t.sleepPowerOff else t.turnOnPlayer)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Text(
                    text = t.daphileHost,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = hostAddress,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HostActionButton(
                        label = t.reboot,
                        icon = Icons.Rounded.RestartAlt,
                        modifier = Modifier.weight(1f)
                    ) { onSelectHostAction(HostPowerAction.Restart) }
                    HostActionButton(
                        label = t.shutdown,
                        icon = Icons.Rounded.PowerSettingsNew,
                        error = true,
                        modifier = Modifier.weight(1f)
                    ) { onSelectHostAction(HostPowerAction.Shutdown) }
                    HostActionButton(
                        label = t.suspend,
                        icon = Icons.Rounded.Settings,
                        modifier = Modifier.weight(1f)
                    ) { onSelectHostAction(HostPowerAction.Suspend) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = t.hostActionNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(t.cancel) }
        }
    )
}

@Composable
private fun HostActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (error) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun HostPowerConfirmDialog(
    action: HostPowerAction,
    hostAddress: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val t = strings
    val actionLabel = hostActionLabel(action, t)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t.confirmActionTitle.format(actionLabel)) },
        text = {
            Column {
                Text(
                    text = t.confirmActionBody.format(hostAddress, actionLabel),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = t.confirmActionNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Icon(
                    Icons.Rounded.PowerSettingsNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t.cancel) }
        }
    )
}

private fun hostActionLabel(action: HostPowerAction, t: UiStrings): String = when (action) {
    HostPowerAction.Restart -> t.hostRestart
    HostPowerAction.Shutdown -> t.hostShutdown
    HostPowerAction.Suspend -> t.hostSuspend
}
