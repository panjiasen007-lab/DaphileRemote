package com.daphile.remote.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.daphile.remote.ui.common.CoverArt
import com.daphile.remote.ui.common.PageHeader
import com.daphile.remote.ui.common.formatDuration
import com.daphile.remote.ui.common.trackSpecs
import com.daphile.remote.ui.i18n.UiStrings
import com.daphile.remote.ui.i18n.strings
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.Locale

@Composable
fun AudioOutputScreen(navController: NavController, modifier: Modifier = Modifier) {
    val viewModel: NowPlayingViewModel = viewModel()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()
    val players by viewModel.players.collectAsStateWithLifecycle()
    val currentPlayerId by viewModel.currentPlayerId.collectAsStateWithLifecycle()
    val server by viewModel.server.collectAsStateWithLifecycle()
    val serverInfo by viewModel.serverInfo.collectAsStateWithLifecycle()

    val track = nowPlaying?.track
    val player = players.firstOrNull { it.id == currentPlayerId }
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val t = strings
    val path = remember(track?.url) { decodeLmsPath(track?.url.orEmpty()) }
    val coverUrl = server?.coverUrl(track?.coverId, nowPlaying?.playerId)
    val dsdLabel = dsdLabel(track?.type.orEmpty(), track?.sampleRate.orEmpty())
    val formatLabel = formatTypeLabel(track?.type.orEmpty())

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PageHeader(
                title = t.audioOutputScreenTitle,
                onBack = { navController.popBackStack() }
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CoverArt(url = coverUrl, modifier = Modifier.size(64.dp), cornerRadius = 12.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track?.title?.ifBlank { t.notPlayingTitle } ?: t.notPlayingTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = listOfNotNull(track?.artist, track?.album)
                                    .filter { it.isNotBlank() }
                                    .joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(5.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(
                                            if (nowPlaying?.isPlaying == true) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            CircleShape
                                        )
                                )
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = playbackLabel(nowPlaying?.mode, nowPlaying?.power, t),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                AudioSectionTitle(t.audioSource)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AudioInfoRow(
                            icon = Icons.Rounded.AudioFile,
                            label = t.path,
                            value = path.ifBlank { "—" },
                            monospace = true,
                            action = if (path.isNotBlank()) {
                                {
                                    clipboard.setText(AnnotatedString(path))
                                    scope.launch { snackbar.showSnackbar(t.pathCopied) }
                                }
                            } else null
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = t.formatLabel,
                            value = listOf(formatLabel, dsdLabel)
                                .filter { it.isNotBlank() }
                                .joinToString(" / ")
                                .ifBlank { "—" }
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = t.sourceSampleRate,
                            value = formatRate(track?.sampleRate.orEmpty())
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = t.bitDepth,
                            value = track?.sampleSize?.takeIf { it.isNotBlank() }
                                ?.let { if (it == "1" && dsdLabel.isNotBlank()) t.bitDsd else "$it bit" }
                                ?: "—"
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = t.bitRate,
                            value = track?.bitRate?.takeIf { it.isNotBlank() && it != "0" } ?: "—"
                        )
                        if (track?.channels != null && track.channels > 0) {
                            AudioInfoRow(
                                icon = Icons.Rounded.GraphicEq,
                                label = t.channels,
                                value = track.channels.toString()
                            )
                        }
                        AudioInfoRow(
                            icon = Icons.Rounded.Info,
                            label = t.lossless,
                            value = if (track?.lossless == true) t.yesValue else t.noOrUnknown
                        )
                    }
                }

                AudioSectionTitle(t.playersAndOutput)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AudioInfoRow(
                            icon = Icons.Rounded.Speaker,
                            label = t.outputDevice,
                            value = player?.name ?: "—"
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.Speaker,
                            label = t.deviceModel,
                            value = player?.model?.ifBlank { "Squeezelite / Daphile" } ?: "—"
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.Speaker,
                            label = t.outputEngine,
                            value = t.outputEngineValue
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = t.outputSpec,
                            value = if (dsdLabel.isNotBlank()) {
                                t.outputSpecDsd
                            } else if (track != null) {
                                t.outputSpecAuto
                            } else {
                                "—"
                            }
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.Speaker,
                            label = t.volume,
                            value = when {
                                nowPlaying == null -> "—"
                                nowPlaying?.canSetVolume == false ->
                                    t.fixedLevel.format(nowPlaying?.volume ?: 100)
                                else -> "${nowPlaying?.volume ?: 0} / 100"
                            }
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.Info,
                            label = t.lmsVersion,
                            value = serverInfo?.version?.ifBlank { "—" } ?: "—"
                        )
                    }
                }

                AudioSectionTitle(t.realtimeStatus)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AudioInfoRow(
                            icon = Icons.Rounded.Timer,
                            label = t.elapsed,
                            value = formatDuration(
                                nowPlaying?.let { np ->
                                    if (np.duration > 0) np.time.coerceIn(0.0, np.duration)
                                    else np.time
                                } ?: 0.0
                            )
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.Timer,
                            label = t.totalDuration,
                            value = formatDuration(nowPlaying?.duration ?: 0.0)
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.GraphicEq,
                            label = t.queuePosition,
                            value = if (nowPlaying != null && (nowPlaying?.trackCount ?: 0) > 0) {
                                "${(nowPlaying?.currentIndex ?: 0) + 1} / ${nowPlaying?.trackCount}"
                            } else {
                                "—"
                            }
                        )
                        AudioInfoRow(
                            icon = Icons.Rounded.Info,
                            label = t.dataSource,
                            value = if (track?.url?.startsWith("http") == true) t.networkStream else t.localFile
                        )
                        val specs = trackSpecs(track)
                        if (specs.isNotEmpty()) {
                            AudioInfoRow(
                                icon = Icons.Rounded.GraphicEq,
                                label = t.specSummary,
                                value = specs.joinToString(" · ")
                            )
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                ) {
                    Row(modifier = Modifier.padding(14.dp)) {
                        Icon(
                            Icons.Rounded.Info,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = t.audioOutputNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(28.dp))
            }
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}

@Composable
private fun AudioSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun AudioInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    monospace: Boolean = false,
    action: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(17.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            modifier = Modifier.width(78.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            modifier = Modifier.weight(1f),
            style = if (monospace) {
                MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
            } else {
                MaterialTheme.typography.bodySmall
            },
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = if (monospace) 3 else 2,
            overflow = TextOverflow.Ellipsis
        )
        if (action != null) {
            IconButton(onClick = action, modifier = Modifier.size(28.dp)) {
                Icon(
                    Icons.Rounded.ContentCopy,
                    contentDescription = strings.copyAction,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun decodeLmsPath(url: String): String {
    if (url.isBlank()) return ""
    var value = url
    repeat(2) {
        value = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)
    }
    return value.removePrefix("file://")
}

private fun formatTypeLabel(type: String): String = when (type.lowercase(Locale.US)) {
    "flc" -> "FLAC"
    "dsf" -> "DSF"
    "dff" -> "DFF"
    "wav" -> "WAV"
    "aif", "aiff" -> "AIFF"
    "mp3" -> "MP3"
    "aac" -> "AAC"
    "m4a" -> "M4A"
    "ogg" -> "OGG"
    "opus" -> "OPUS"
    else -> type.uppercase(Locale.US)
}

private fun dsdLabel(type: String, rate: String): String {
    val numeric = rate.toDoubleOrNull()
    val isDsd = type.contains("dsd", true) || type.equals("dsf", true) ||
        type.equals("dff", true) || (numeric != null && numeric >= 2_000_000)
    if (!isDsd) return ""
    val multiple = numeric?.let { (it / 2_822_400.0).toInt() } ?: 0
    return when (multiple) {
        1 -> "DSD64"
        2 -> "DSD128"
        4 -> "DSD256"
        8 -> "DSD512"
        else -> "DSD"
    }
}

private fun formatRate(rate: String): String {
    val numeric = rate.toDoubleOrNull() ?: return rate.ifBlank { "—" }
    return when {
        numeric >= 1_000_000 -> String.format(Locale.US, "%.1f MHz", numeric / 1_000_000)
        numeric >= 1000 -> {
            val khz = numeric / 1000
            if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", khz)
        }
        else -> "$rate Hz"
    }
}

private fun playbackLabel(mode: String?, power: Boolean?, t: UiStrings): String = when {
    power == false -> t.playerOff
    mode == "play" -> t.playingState
    mode == "pause" -> t.pausedToast
    else -> t.stoppedState
}
