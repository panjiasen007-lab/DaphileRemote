package com.daphile.remote.ui.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daphile.remote.data.model.Lyrics
import com.daphile.remote.ui.i18n.strings
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

private val METADATA_LINE = Regex(
    """^(作词|作曲|编曲|制作人|制作|监制|混音|母带|母带处理|人声编辑|人声|录音|和声|吉他|贝斯|鼓|弦乐|出品|发行|策划|统筹|演唱|原唱|词|曲)(\s*[:：]|\s*$)"""
)

/** 形如「人声编辑：李博」的短制作信息行，通常在歌词开头，展示时一并隐藏。 */
private fun isCreditLine(text: String): Boolean {
    if (text.length > 24) return false
    return METADATA_LINE.containsMatchIn(text.trim())
}

/**
 * 正在播放页右侧的歌词卡：半透明玻璃质感，显示当前行与相邻两句。
 * 有同步歌词时随播放进度切换；没有歌词时给出提示。
 */
@Composable
fun LyricSpotlight(
    lyrics: Lyrics?,
    loading: Boolean,
    currentIndex: Int,
    modifier: Modifier = Modifier,
    onOpenLyrics: () -> Unit
) {
    val t = strings
    val lines = remember(lyrics) {
        lyrics?.lines
            ?.filterNot { isCreditLine(it.text) }
            ?.ifEmpty { lyrics.lines }
            .orEmpty()
    }
    val index = remember(lines, currentIndex) {
        if (lines.isEmpty()) 0 else currentIndex.coerceIn(0, lines.lastIndex)
    }
    Surface(
        modifier = modifier.clickable(onClick = onOpenLyrics),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            when {
                loading -> LyricHint(t.matchingLyrics)
                lyrics == null || lines.isEmpty() -> LyricHint(t.noLyricsHint)
                !lyrics.synced -> {
                    Text(
                        text = lines.joinToString("\n") { it.text },
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                        maxLines = 6,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                else -> {
                    AnimatedContent(
                        targetState = index,
                        transitionSpec = {
                            (fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 3 })
                                .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 3 })
                        },
                        label = "lyricLine"
                    ) { target ->
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val prev = lines.getOrNull(target - 1)?.text
                            val cur = lines[target].text
                            val nxt = lines.getOrNull(target + 1)?.text
                            prev?.let { LyricLineText(it, 0.38f, 14.sp, false, maxLines = 1) }
                            LyricLineText(cur, 1f, 18.sp, true, maxLines = 2)
                            nxt?.let { LyricLineText(it, 0.45f, 14.sp, false, maxLines = 1) }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.GraphicEq,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = lyrics?.source?.let { t.lyricsWithSource.format(it) } ?: t.lyrics,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LyricLineText(
    text: String,
    alpha: Float,
    size: androidx.compose.ui.unit.TextUnit,
    bold: Boolean,
    maxLines: Int = 2
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = size,
            lineHeight = size * 1.45f
        ),
        fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
        color = if (bold) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        },
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun LyricHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        textAlign = TextAlign.Start
    )
}

/**
 * 实时频谱（遥控端模拟）：
 * 遥控端拿不到解码后的音频，因此用固定节拍时钟驱动：每拍能量冲高后缓慢衰减，
 * 低频段起伏大、高频段起伏小，整体变化平缓，避免闪烁晃眼。
 */
@Composable
fun SpectrumBars(
    playing: Boolean,
    seed: Int,
    modifier: Modifier = Modifier,
    bars: Int = 32
) {
    val transition = rememberInfiniteTransition(label = "spectrum")
    // 一个完整节拍循环：约 1.6 秒两拍，接近中慢速歌曲
    val beatPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spectrumBeat"
    )
    val random = remember(seed) { Random(seed) }
    val weights = remember(seed) {
        List(bars) { index ->
            // 中间频段稍高，两端稍低，形成自然的频响轮廓
            val shape = sin((index + 1f) / (bars + 1f) * Math.PI).toFloat()
            0.35f + 0.45f * shape + random.nextFloat() * 0.18f
        }
    }
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary

    Canvas(modifier = modifier) {
        val step = size.width / bars
        val barWidth = step * 0.46f
        // 每一拍的冲击强度缓慢衰减：0 为刚击拍，1 为拍尾
        val beat = beatPhase % 1f
        val attack = (1f - beat).let { it * it }          // 平方衰减，回落柔和
        val energy = if (playing) 0.22f + 0.42f * attack else 0.16f

        weights.forEachIndexed { index, weight ->
            val x = index * step + (step - barWidth) / 2f
            // 低频（左侧）响应更明显，高频（右侧）更稳定
            val lowBias = 1f - index.toFloat() / bars * 0.55f
            val slowWave = 0.06f * sin(beatPhase * 1.4f + index * 0.35f)
            val target = if (playing) {
                (weight * (0.55f + energy * lowBias) + slowWave).coerceIn(0.10f, 1f)
            } else {
                (weight * 0.30f).coerceIn(0.08f, 0.35f)
            }
            val h = size.height * target
            val top = size.height - h
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.90f),
                        secondary.copy(alpha = 0.50f),
                        primary.copy(alpha = 0.15f)
                    ),
                    startY = top,
                    endY = size.height
                ),
                topLeft = Offset(x, top),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

/**
 * 播放/暂停切换时的柔和呼吸动画（用于播放按钮外圈）。
 */
@Composable
fun PulseRing(modifier: Modifier = Modifier, color: Color) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseProgress"
    )
    Canvas(modifier = modifier) {
        val radius = size.minDimension / 2f
        drawCircle(
            color = color.copy(alpha = (1f - progress) * 0.25f),
            radius = radius * (0.82f + progress * 0.18f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
    }
}
