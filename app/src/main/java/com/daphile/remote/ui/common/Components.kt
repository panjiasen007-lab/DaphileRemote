package com.daphile.remote.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.daphile.remote.data.model.ConnectionState
import com.daphile.remote.data.model.Track
import com.daphile.remote.ui.i18n.strings
import java.util.Locale

fun formatDuration(seconds: Double): String {
    if (seconds.isNaN() || seconds <= 0) return "0:00"
    val total = seconds.toInt()
    val hours = total / 3600
    val minutes = (total % 3600) / 60
    val secs = total % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, secs)
    }
}

fun formatRemaining(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, secs)
}

fun displayArtist(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val artist = value.trim()
    if (artist == "无艺人名" || artist == "未知艺术家" ||
        artist.equals("unknown artist", true) || artist.equals("various artists", true)
    ) {
        return ""
    }
    return artist
}

fun trackSpecs(track: Track?): List<String> {
    if (track == null) return emptyList()
    val specs = mutableListOf<String>()
    val type = track.type.trim()
    val rate = track.sampleRate.trim()
    val numericRate = rate.toDoubleOrNull()
    val isDsd = type.contains("dsd", true) ||
        (numericRate != null && numericRate >= 2_000_000)
    if (isDsd) {
        val multiple = numericRate?.let { (it / 2_822_400.0).toInt() } ?: 0
        specs += when (multiple) {
            1 -> "DSD64"
            2 -> "DSD128"
            4 -> "DSD256"
            8 -> "DSD512"
            else -> if (type.isNotBlank()) type.substringBefore("@").uppercase(Locale.US) else "DSD"
        }
    } else if (type.isNotBlank()) {
        specs += formatType(track.type)
    }
    if (rate.isNotBlank()) {
        when {
            numericRate != null && numericRate >= 1_000_000 ->
                specs += String.format(Locale.US, "%.1f MHz", numericRate / 1_000_000)
            numericRate != null && numericRate >= 1000 -> {
                val khz = numericRate / 1000
                specs += if (khz % 1.0 == 0.0) "${khz.toInt()} kHz" else String.format(Locale.US, "%.1f kHz", khz)
            }
            numericRate != null -> specs += "$rate Hz"
            else -> specs += rate
        }
    }
    val bits = track.sampleSize.trim()
    if (bits.isNotBlank() && !(isDsd && bits == "1")) {
        specs += if (bits.all { it.isDigit() }) "$bits bit" else bits
    }
    val bitrate = track.bitRate.trim()
    if (bitrate.isNotBlank() && bitrate != "0" && bitrate != "0kbps" &&
        specs.none { it.contains("kbps", true) }
    ) {
        specs += bitrate
    }
    return specs.distinct().take(4)
}

fun formatType(type: String): String = when (type.substringBefore("@").lowercase(Locale.US)) {
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
    else -> type.substringBefore("@").uppercase(Locale.US)
}

/** 音质档位，priority 越大代表规格越高，用于专辑内取最佳音质。 */
enum class QualityTier(val priority: Int) {
    DSD(5),
    HI_RES(4),
    LOSSLESS(3),
    HIGH(2),
    STANDARD(1)
}

data class TrackQuality(val label: String, val tier: QualityTier)

private val DSD_TYPES = setOf("dsd", "dsf", "dff")
private val LOSSLESS_TYPES = setOf("flac", "flc", "wav", "wave", "aif", "aiff", "ape", "alac", "wv", "tta")
private val LOSSY_TYPES = setOf("mp3", "aac", "m4a", "m4b", "ogg", "opus", "wma", "mpc", "aac")

/**
 * 由 LMS 返回的格式 / 采样率 / 位深 / 码率推导音质标签。
 * 新扫描入库的曲目只要带有这些字段，就会自动获得标签。
 */
fun trackQuality(track: Track?): TrackQuality? {
    if (track == null) return null
    val rawType = track.type.trim().lowercase(Locale.US)
    val type = rawType.substringBefore("@").trim().substringAfterLast('.').trim()
    val rate = track.sampleRate.trim().toDoubleOrNull()
    val bits = track.sampleSize.trim().toDoubleOrNull()
    val bitrate = Regex("""(\d+)\s*kbps""", RegexOption.IGNORE_CASE)
        .find(track.bitRate)
        ?.groupValues?.getOrNull(1)
        ?.toIntOrNull()
        ?.takeIf { it > 0 }
    val isDsd = type in DSD_TYPES || (rate != null && rate >= 2_000_000)
    if (isDsd) {
        val multiple = rate?.let { (it / 2_822_400.0).toInt() } ?: 0
        val label = when (multiple) {
            1 -> "DSD64"
            2 -> "DSD128"
            4 -> "DSD256"
            8 -> "DSD512"
            else -> "DSD"
        }
        return TrackQuality(label, QualityTier.DSD)
    }
    if (type.isBlank() && rate == null && bitrate == null) return null
    val lossless = track.lossless || type in LOSSLESS_TYPES
    if (lossless) {
        val hiRes = (rate != null && rate >= 88_200) || (bits != null && bits > 16)
        return if (hiRes) {
            TrackQuality("Hi-Res", QualityTier.HI_RES)
        } else {
            TrackQuality("SQ", QualityTier.LOSSLESS)
        }
    }
    if (bitrate != null) {
        return if (bitrate >= 256) {
            TrackQuality("HQ", QualityTier.HIGH)
        } else {
            TrackQuality("标准", QualityTier.STANDARD)
        }
    }
    if (type in LOSSY_TYPES) return TrackQuality("标准", QualityTier.STANDARD)
    return null
}

/** 一组曲目中的最佳音质，用于专辑详情页展示。 */
fun bestTrackQuality(tracks: List<Track>): TrackQuality? =
    tracks.mapNotNull { trackQuality(it) }.maxByOrNull { it.tier.priority }

@Composable
fun QualityBadge(track: Track?, modifier: Modifier = Modifier) {
    val quality = trackQuality(track) ?: return
    QualityBadge(quality = quality, modifier = modifier)
}

@Composable
fun QualityBadge(quality: TrackQuality, modifier: Modifier = Modifier) {
    val color = when (quality.tier) {
        QualityTier.DSD -> Color(0xFFC08A20)
        QualityTier.HI_RES -> Color(0xFFC08A20)
        QualityTier.LOSSLESS -> Color(0xFF31A79B)
        QualityTier.HIGH -> Color(0xFF4C8DF6)
        QualityTier.STANDARD -> Color(0xFF8C939C)
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.16f),
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = if (quality.tier == QualityTier.STANDARD) strings.qualityStandard else quality.label,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            ),
            color = color,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun CoverArt(
    url: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(32.dp)
            )
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        }
    }
}

@Composable
fun SpecChip(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PageHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (onBack != null) 4.dp else 20.dp, end = 8.dp, top = 10.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = strings.back)
            }
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        actions()
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .padding(20.dp)
                    .size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(20.dp))
            androidx.compose.material3.Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            strokeWidth = 2.5.dp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    coverUrl: String? = null,
    showCover: Boolean = false,
    leadingText: String? = null,
    isCurrent: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val background = if (isCurrent) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showCover) {
            CoverArt(url = coverUrl, modifier = Modifier.size(44.dp), cornerRadius = 8.dp)
            Spacer(Modifier.width(12.dp))
        } else if (leadingText != null) {
            Text(
                text = leadingText,
                modifier = Modifier.width(30.dp),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End
            )
            Spacer(Modifier.width(10.dp))
        } else {
            Box(
                modifier = Modifier.width(30.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track.title.ifBlank { strings.unknownTrack },
                    modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val quality = trackQuality(track)
                if (quality != null) {
                    Spacer(Modifier.width(6.dp))
                    QualityBadge(quality)
                }
            }
            val subtitle = listOf(displayArtist(track.artist), track.album)
                .filter { it.isNotBlank() }
                .joinToString(" · ")
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (track.duration > 0) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = formatDuration(track.duration),
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(Modifier.width(4.dp))
            trailing()
        }
    }
}

@Composable
fun ConnectionBanner(
    state: ConnectionState,
    hasServer: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!hasServer || state == ConnectionState.Connected || state == ConnectionState.Idle) return
    val isLost = state == ConnectionState.Lost
    val text = when (state) {
        ConnectionState.Connecting -> strings.connectingDaphile
        ConnectionState.Reconnecting -> strings.reconnectingBanner
        ConnectionState.Lost -> strings.serverUnreachable
        else -> return
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = if (isLost) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!isLost) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (isLost) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            if (isLost) {
                TextButton(onClick = onRetry) {
                    Text(strings.reconnect)
                }
            }
        }
    }
}
