package com.daphile.remote.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.daphile.remote.ui.theme.ThemeCoverFrame
import com.daphile.remote.ui.theme.ThemeSliderStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 带主题外框的唱片封面：普通主题直接使用旋转黑胶，
 * 特别版主题在唱片外圈叠加回纹金环、风车、花窗、霜边或月相环。
 */
@Composable
fun ThemedCover(
    coverUrl: String?,
    playing: Boolean,
    style: ThemeCoverFrame,
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    if (style == ThemeCoverFrame.None) {
        SpinningVinyl(
            coverUrl = coverUrl,
            playing = playing,
            modifier = modifier,
            accent = accent
        )
        return
    }
    val transition = rememberInfiniteTransition(label = "themedCover")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "themedCoverClock"
    )
    // 风主题整体轻微摆动
    val sway = if (style == ThemeCoverFrame.Windmill && playing) {
        sin(clock * 2 * PI).toFloat() * 1.5f
    } else {
        0f
    }
    val gold = Color(0xFFD4AF37)
    val ice = Color(0xFFBFE9F2)
    val petal = Color(0xFFE8A0B4)
    val moon = Color(0xFFEAE6DA)
    val silver = Color(0xFFC9CEDA)
    val background = MaterialTheme.colorScheme.background
    val secondary = MaterialTheme.colorScheme.secondary

    Box(modifier = modifier.rotate(sway), contentAlignment = Alignment.Center) {
        SpinningVinyl(
            coverUrl = coverUrl,
            playing = playing,
            modifier = Modifier.fillMaxSize(0.80f),
            accent = accent
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val c = Offset(size.width / 2f, size.height / 2f)
            when (style) {
                ThemeCoverFrame.LatticeRing -> {
                    drawCircle(gold.copy(alpha = 0.85f), radius = r * 0.98f, center = c, style = Stroke(width = 2.4f))
                    drawCircle(gold.copy(alpha = 0.28f), radius = r * 0.90f, center = c, style = Stroke(width = 1f))
                    repeat(16) { index ->
                        rotate(index * 22.5f, pivot = c) {
                            drawLine(
                                color = gold.copy(alpha = 0.45f),
                                start = Offset(c.x, c.y - r * 0.99f),
                                end = Offset(c.x, c.y - r * 0.90f),
                                strokeWidth = 1.6f
                            )
                            if (index % 4 == 0) {
                                drawRect(
                                    color = gold.copy(alpha = 0.40f),
                                    topLeft = Offset(c.x - r * 0.035f, c.y - r * 1.05f),
                                    size = Size(r * 0.07f, r * 0.07f),
                                    style = Stroke(width = 1f)
                                )
                            }
                        }
                    }
                }
                ThemeCoverFrame.Windmill -> {
                    drawCircle(accent.copy(alpha = 0.30f), radius = r * 0.92f, center = c, style = Stroke(width = 1f))
                    val spin = if (playing) clock * 360f else 0f
                    rotate(spin, pivot = c) {
                        repeat(6) { index ->
                            rotate(index * 60f, pivot = c) {
                                val blade = Path().apply {
                                    moveTo(c.x, c.y - r * 0.88f)
                                    cubicTo(
                                        c.x + r * 0.18f, c.y - r * 1.02f,
                                        c.x + r * 0.30f, c.y - r * 0.98f,
                                        c.x + r * 0.20f, c.y - r * 0.80f
                                    )
                                }
                                drawPath(
                                    blade,
                                    accent.copy(alpha = 0.75f),
                                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                                )
                                drawCircle(
                                    color = secondary.copy(alpha = 0.55f),
                                    radius = r * 0.018f,
                                    center = Offset(c.x + r * 0.09f, c.y - r * 0.87f)
                                )
                            }
                        }
                    }
                }
                ThemeCoverFrame.BlossomWindow -> {
                    drawCircle(petal.copy(alpha = 0.30f), radius = r * 0.98f, center = c, style = Stroke(width = 1.2f))
                    repeat(6) { index ->
                        rotate(index * 60f, pivot = c) {
                            drawOval(
                                color = petal.copy(alpha = 0.60f),
                                topLeft = Offset(c.x - r * 0.10f, c.y - r * 1.00f),
                                size = Size(r * 0.20f, r * 0.30f),
                                style = Stroke(width = 1.6f)
                            )
                        }
                    }
                    repeat(9) { index ->
                        val angle = (index * 40f) * PI.toFloat() / 180f
                        val radius = r * (0.86f + (index % 3) * 0.05f)
                        drawOval(
                            color = petal.copy(alpha = 0.45f),
                            topLeft = Offset(
                                c.x + cos(angle) * radius - r * 0.022f,
                                c.y + sin(angle) * radius - r * 0.012f
                            ),
                            size = Size(r * 0.044f, r * 0.024f)
                        )
                    }
                }
                ThemeCoverFrame.Frost -> {
                    drawCircle(ice.copy(alpha = 0.55f), radius = r * 0.97f, center = c, style = Stroke(width = 2f))
                    repeat(20) { index ->
                        rotate(index * 18f, pivot = c) {
                            val spike = Path().apply {
                                moveTo(c.x - r * 0.032f, c.y - r * 0.96f)
                                lineTo(c.x, c.y - r * (1.02f + (index % 3) * 0.025f))
                                lineTo(c.x + r * 0.032f, c.y - r * 0.96f)
                                close()
                            }
                            drawPath(spike, ice.copy(alpha = 0.45f))
                        }
                    }
                    drawCircle(ice.copy(alpha = 0.25f), radius = r * 0.90f, center = c, style = Stroke(width = 1f))
                }
                ThemeCoverFrame.MoonPhase -> {
                    drawCircle(silver.copy(alpha = 0.45f), radius = r * 0.98f, center = c, style = Stroke(width = 1.4f))
                    repeat(8) { index ->
                        rotate(index * 45f, pivot = c) {
                            drawLine(
                                color = silver.copy(alpha = 0.35f),
                                start = Offset(c.x, c.y - r * 1.00f),
                                end = Offset(c.x, c.y - r * 0.94f),
                                strokeWidth = 1.2f
                            )
                        }
                    }
                    // 月相环：进度映射新月 → 满月 → 残月
                    val moonCenter = Offset(c.x, c.y - r * 0.90f)
                    val moonR = r * 0.085f
                    val p = progress.coerceIn(0f, 1f)
                    val lit = 1f - kotlin.math.abs(2f * p - 1f)
                    drawCircle(
                        color = moon.copy(alpha = 0.22f + 0.72f * lit),
                        radius = moonR,
                        center = moonCenter
                    )
                    drawCircle(
                        color = moon.copy(alpha = 0.55f),
                        radius = moonR,
                        center = moonCenter,
                        style = Stroke(width = 1f)
                    )
                }
                ThemeCoverFrame.HaloRing -> {
                    // 薄光环：外圈弱光 + 缓慢自转的刻度
                    drawCircle(accent.copy(alpha = 0.06f), radius = r * 0.99f, center = c, style = Stroke(width = 10f))
                    drawCircle(accent.copy(alpha = 0.20f), radius = r * 0.96f, center = c, style = Stroke(width = 5f))
                    drawCircle(accent.copy(alpha = 0.70f), radius = r * 0.96f, center = c, style = Stroke(width = 1.4f))
                    val spin = if (playing) clock * 360f else 0f
                    rotate(spin, pivot = c) {
                        repeat(24) { index ->
                            rotate(index * 15f, pivot = c) {
                                val long = index % 6 == 0
                                drawLine(
                                    color = accent.copy(alpha = if (long) 0.55f else 0.24f),
                                    start = Offset(c.x, c.y - r * 0.96f),
                                    end = Offset(c.x, c.y - r * (if (long) 1.00f else 0.98f)),
                                    strokeWidth = 1.2f,
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }
                }
                ThemeCoverFrame.ThinFrame -> {
                    // 极简细线圆角框，不做任何装饰
                    val half = r * 0.88f
                    drawRoundRect(
                        color = accent.copy(alpha = 0.45f),
                        topLeft = Offset(c.x - half, c.y - half),
                        size = Size(half * 2f, half * 2f),
                        cornerRadius = CornerRadius(r * 0.05f),
                        style = Stroke(width = 1f)
                    )
                }
                ThemeCoverFrame.BambooRing -> {
                    // 竹节环：短弧分段，外侧冒出三枚嫩芽
                    val ringTop = Offset(c.x - r * 0.96f, c.y - r * 0.96f)
                    val ringSize = Size(r * 1.92f, r * 1.92f)
                    repeat(28) { index ->
                        drawArc(
                            color = accent.copy(alpha = if (index % 2 == 0) 0.55f else 0.32f),
                            startAngle = index * (360f / 28f) + 1.5f,
                            sweepAngle = 360f / 28f - 4f,
                            useCenter = false,
                            topLeft = ringTop,
                            size = ringSize,
                            style = Stroke(width = 2f, cap = StrokeCap.Round)
                        )
                    }
                    repeat(3) { index ->
                        rotate(index * 120f + 30f, pivot = c) {
                            drawOval(
                                color = accent.copy(alpha = 0.60f),
                                topLeft = Offset(c.x - r * 0.022f, c.y - r * 0.99f),
                                size = Size(r * 0.044f, r * 0.10f)
                            )
                        }
                    }
                }
                ThemeCoverFrame.WaveCrest -> {
                    // 浪花卷边：环向弧段 + 卷起的浪尖
                    val crestTop = Offset(c.x - r * 0.94f, c.y - r * 0.94f)
                    val crestSize = Size(r * 1.88f, r * 1.88f)
                    repeat(12) { index ->
                        rotate(index * 30f, pivot = c) {
                            drawArc(
                                color = accent.copy(alpha = 0.38f),
                                startAngle = 196f,
                                sweepAngle = 148f,
                                useCenter = false,
                                topLeft = crestTop,
                                size = crestSize,
                                style = Stroke(width = 1.8f, cap = StrokeCap.Round)
                            )
                            drawArc(
                                color = accent.copy(alpha = 0.60f),
                                startAngle = 150f,
                                sweepAngle = 240f,
                                useCenter = false,
                                topLeft = Offset(c.x - r * 0.07f, c.y - r * 1.01f),
                                size = Size(r * 0.14f, r * 0.14f),
                                style = Stroke(width = 1.4f, cap = StrokeCap.Round)
                            )
                        }
                    }
                }
                ThemeCoverFrame.MapleWreath -> {
                    // 枫叶花环，叶脉朝向圆心
                    drawCircle(accent.copy(alpha = 0.20f), radius = r * 0.90f, center = c, style = Stroke(width = 1f))
                    repeat(8) { index ->
                        rotate(index * 45f, pivot = c) {
                            drawMapleLeaf(
                                cx = c.x,
                                cy = c.y - r * 0.91f,
                                r = r * 0.10f,
                                color = accent.copy(alpha = 0.55f)
                            )
                        }
                    }
                }
                ThemeCoverFrame.IceCrystal -> {
                    // 六棱冰晶：外尖内收的三角晶片 + 细内环
                    drawCircle(ice.copy(alpha = 0.35f), radius = r * 0.90f, center = c, style = Stroke(width = 1f))
                    repeat(6) { index ->
                        rotate(index * 60f, pivot = c) {
                            val shard = Path().apply {
                                moveTo(c.x - r * 0.13f, c.y - r * 0.88f)
                                lineTo(c.x, c.y - r * 1.00f)
                                lineTo(c.x + r * 0.13f, c.y - r * 0.88f)
                                lineTo(c.x, c.y - r * 0.84f)
                                close()
                            }
                            drawPath(shard, ice.copy(alpha = 0.28f))
                            drawPath(shard, ice.copy(alpha = 0.65f), style = Stroke(width = 1.2f))
                            // 晶面折线
                            drawLine(
                                color = ice.copy(alpha = 0.45f),
                                start = Offset(c.x, c.y - r * 0.84f),
                                end = Offset(c.x, c.y - r * 1.00f),
                                strokeWidth = 1f
                            )
                        }
                    }
                }
                ThemeCoverFrame.Bagua -> {
                    // 八卦：细圆环外环绕八组三爻，阴阳爻交替
                    drawCircle(accent.copy(alpha = 0.35f), radius = r * 0.90f, center = c, style = Stroke(width = 1.2f))
                    repeat(8) { index ->
                        rotate(index * 45f, pivot = c) {
                            repeat(3) { line ->
                                val y = c.y - r * (0.95f + line * 0.032f)
                                val broken = (index + line) % 2 == 1
                                if (broken) {
                                    drawLine(
                                        color = accent.copy(alpha = 0.55f),
                                        start = Offset(c.x - r * 0.085f, y),
                                        end = Offset(c.x - r * 0.018f, y),
                                        strokeWidth = 1.6f
                                    )
                                    drawLine(
                                        color = accent.copy(alpha = 0.55f),
                                        start = Offset(c.x + r * 0.018f, y),
                                        end = Offset(c.x + r * 0.085f, y),
                                        strokeWidth = 1.6f
                                    )
                                } else {
                                    drawLine(
                                        color = accent.copy(alpha = 0.55f),
                                        start = Offset(c.x - r * 0.085f, y),
                                        end = Offset(c.x + r * 0.085f, y),
                                        strokeWidth = 1.6f
                                    )
                                }
                            }
                        }
                    }
                }
                ThemeCoverFrame.BambooLeaf -> {
                    // 竹叶环：细长叶片沿圆周切向排布
                    drawCircle(accent.copy(alpha = 0.18f), radius = r * 0.92f, center = c, style = Stroke(width = 1f))
                    repeat(12) { index ->
                        rotate(index * 30f, pivot = c) {
                            val leaf = Path().apply {
                                moveTo(c.x - r * 0.13f, c.y - r * 0.95f)
                                cubicTo(
                                    c.x - r * 0.04f, c.y - r * 1.01f,
                                    c.x + r * 0.04f, c.y - r * 1.01f,
                                    c.x + r * 0.13f, c.y - r * 0.95f
                                )
                                cubicTo(
                                    c.x + r * 0.04f, c.y - r * 0.89f,
                                    c.x - r * 0.04f, c.y - r * 0.89f,
                                    c.x - r * 0.13f, c.y - r * 0.95f
                                )
                                close()
                            }
                            drawPath(leaf, accent.copy(alpha = 0.45f))
                            drawLine(
                                color = accent.copy(alpha = 0.55f),
                                start = Offset(c.x - r * 0.11f, c.y - r * 0.95f),
                                end = Offset(c.x + r * 0.11f, c.y - r * 0.95f),
                                strokeWidth = 0.9f
                            )
                        }
                    }
                }
                ThemeCoverFrame.None -> Unit
            }
        }
    }
}

/**
 * 特别版主题的进度条。造型随主题变化：朱漆如意杆、蒲公英种子、花苞、冰凌、银线。
 * 保持 40dp 触控高度，横向拖动与点击均可定位。
 */
@Composable
fun ThemedSeekBar(
    value: Float,
    max: Float,
    style: ThemeSliderStyle,
    enabled: Boolean,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val track = MaterialTheme.colorScheme.surfaceVariant
    val metal = MaterialTheme.colorScheme.tertiary
    val density = LocalDensity.current
    val inset = with(density) { 14.dp.toPx() }
    var width by remember { mutableFloatStateOf(0f) }
    val fraction = if (max > 0f) (value / max).coerceIn(0f, 1f) else 0f

    fun valueAt(x: Float): Float {
        val span = (width - inset * 2f).coerceAtLeast(1f)
        return ((x - inset) / span).coerceIn(0f, 1f) * max
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .onSizeChanged { width = it.width.toFloat() }
            .pointerInput(enabled, style, max) {
                if (!enabled) return@pointerInput
                detectTapGestures { offset ->
                    onValueChange(valueAt(offset.x))
                    onValueChangeFinished()
                }
            }
            .pointerInput(enabled, style, max) {
                if (!enabled) return@pointerInput
                detectHorizontalDragGestures(
                    onDragStart = { offset -> onValueChange(valueAt(offset.x)) },
                    onDragEnd = { onValueChangeFinished() },
                    onDragCancel = { onValueChangeFinished() },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        onValueChange(valueAt(change.position.x))
                    }
                )
            }
    ) {
        val cy = size.height / 2f
        val startX = inset
        val endX = size.width - inset
        val knobX = startX + (endX - startX) * fraction
        val readable = if (enabled) 1f else 0.45f
        when (style) {
            ThemeSliderStyle.RuyiBar -> {
                drawLine(
                    color = Color(0xFF5E1119).copy(alpha = 0.55f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawRuyiKnob(knobX, cy, metal.copy(alpha = readable))
            }
            ThemeSliderStyle.SeedThumb -> {
                drawLine(
                    color = track.copy(alpha = 0.75f),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.80f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawDandelion(knobX, cy, accent.copy(alpha = readable))
            }
            ThemeSliderStyle.BudThumb -> {
                drawLine(
                    color = Color(0xFF7FA679).copy(alpha = 0.55f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.80f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawBud(knobX, cy, fraction, accent.copy(alpha = readable), secondary.copy(alpha = readable))
            }
            ThemeSliderStyle.Icicle -> {
                drawLine(
                    color = Color(0xFFA9C7DE).copy(alpha = 0.30f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.75f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // 冰凌小刺
                var tickX = startX + 12.dp.toPx()
                while (tickX < endX) {
                    drawLine(
                        color = Color(0xFFBFE9F2).copy(alpha = 0.35f * readable),
                        start = Offset(tickX, cy - 4.dp.toPx()),
                        end = Offset(tickX + 2.dp.toPx(), cy + 5.dp.toPx()),
                        strokeWidth = 1.2.dp.toPx()
                    )
                    tickX += 16.dp.toPx()
                }
                drawCircle(
                    color = Color(0xFFF4F9FF).copy(alpha = 0.95f * readable),
                    radius = 6.5.dp.toPx(),
                    center = Offset(knobX, cy)
                )
                drawCircle(
                    color = Color(0xFFBFE9F2).copy(alpha = 0.75f * readable),
                    radius = 6.5.dp.toPx(),
                    center = Offset(knobX, cy),
                    style = Stroke(width = 1.4.dp.toPx())
                )
            }
            ThemeSliderStyle.SilverLine -> {
                drawLine(
                    color = Color(0xFFC9CEDA).copy(alpha = 0.30f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 1.4.dp.toPx()
                )
                drawLine(
                    color = Color(0xFFEAE6DA).copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 1.4.dp.toPx()
                )
                drawCircle(
                    color = Color(0xFFEAE6DA).copy(alpha = 0.12f * readable),
                    radius = 10.dp.toPx(),
                    center = Offset(knobX, cy)
                )
                drawCircle(
                    color = Color(0xFFF7F3E6).copy(alpha = 0.95f * readable),
                    radius = 3.6.dp.toPx(),
                    center = Offset(knobX, cy)
                )
            }
            ThemeSliderStyle.GlowDot -> {
                drawLine(
                    color = accent.copy(alpha = 0.16f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // 已播放部分叠加柔光
                drawLine(
                    color = accent.copy(alpha = 0.18f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.95f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawGlowDot(knobX, cy, metal.copy(alpha = readable))
            }
            ThemeSliderStyle.HairLine -> {
                drawLine(
                    color = track.copy(alpha = 0.85f),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = accent.copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 1.2.dp.toPx()
                )
                drawFlatDot(knobX, cy, accent.copy(alpha = readable))
            }
            ThemeSliderStyle.LeafThumb -> {
                drawLine(
                    color = Color(0xFF7FA679).copy(alpha = 0.45f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawWillowLeaf(knobX, cy, accent.copy(alpha = 0.90f * readable))
            }
            ThemeSliderStyle.ShellThumb -> {
                drawLine(
                    color = metal.copy(alpha = 0.28f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = metal.copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // 浪纹刻度
                var tickX = startX + 10.dp.toPx()
                while (tickX < endX) {
                    drawCircle(
                        color = metal.copy(alpha = 0.22f * readable),
                        radius = 1.dp.toPx(),
                        center = Offset(tickX, cy - 3.5.dp.toPx())
                    )
                    drawCircle(
                        color = metal.copy(alpha = 0.22f * readable),
                        radius = 1.dp.toPx(),
                        center = Offset(tickX + 4.dp.toPx(), cy + 3.5.dp.toPx())
                    )
                    tickX += 18.dp.toPx()
                }
                drawShellThumb(knobX, cy, metal.copy(alpha = readable))
            }
            ThemeSliderStyle.MapleThumb -> {
                drawLine(
                    color = Color(0xFFC46A2A).copy(alpha = 0.30f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = accent.copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawMapleThumb(knobX, cy, 5.5.dp.toPx(), metal.copy(alpha = readable))
            }
            ThemeSliderStyle.FrostNeedle -> {
                drawLine(
                    color = Color(0xFFA9C7DE).copy(alpha = 0.30f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFDFF3FF).copy(alpha = 0.80f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                // 霜面淡刻
                var tickX = startX + 12.dp.toPx()
                while (tickX < endX) {
                    drawLine(
                        color = Color(0xFFBFE9F2).copy(alpha = 0.22f * readable),
                        start = Offset(tickX, cy - 2.5.dp.toPx()),
                        end = Offset(tickX + 1.5.dp.toPx(), cy + 3.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                    tickX += 20.dp.toPx()
                }
                drawFrostNeedle(knobX, cy, Color(0xFFF4F9FF).copy(alpha = 0.92f * readable))
            }
            ThemeSliderStyle.YinYangThumb -> {
                drawLine(
                    color = accent.copy(alpha = 0.18f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = accent.copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.dp.toPx()
                )
                drawTaijiThumb(
                    knobX,
                    cy,
                    6.dp.toPx(),
                    accent.copy(alpha = readable),
                    Color(0xFFF3E7DA).copy(alpha = readable)
                )
            }
            ThemeSliderStyle.PawThumb -> {
                drawLine(
                    color = metal.copy(alpha = 0.25f * readable),
                    start = Offset(startX, cy),
                    end = Offset(endX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = metal.copy(alpha = 0.85f * readable),
                    start = Offset(startX, cy),
                    end = Offset(knobX, cy),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawPawThumb(knobX, cy, metal.copy(alpha = readable))
            }
            ThemeSliderStyle.Default -> Unit
        }
    }
}

private fun DrawScope.drawRuyiKnob(cx: Float, cy: Float, color: Color) {
    val r = 5.5.dp.toPx()
    drawCircle(color.copy(alpha = 0.95f), radius = r, center = Offset(cx, cy))
    drawCircle(color.copy(alpha = 0.80f), radius = r * 0.72f, center = Offset(cx - r * 1.05f, cy))
    drawCircle(color.copy(alpha = 0.80f), radius = r * 0.72f, center = Offset(cx + r * 1.05f, cy))
    drawCircle(Color(0xFF2A0508).copy(alpha = 0.40f), radius = r * 0.30f, center = Offset(cx, cy))
}

private fun DrawScope.drawDandelion(cx: Float, cy: Float, color: Color) {
    val r = 4.2.dp.toPx()
    val hair = 6.5.dp.toPx()
    repeat(8) { index ->
        val angle = index * PI.toFloat() / 4f
        drawLine(
            color = color.copy(alpha = 0.55f),
            start = Offset(cx + cos(angle) * r * 0.6f, cy + sin(angle) * r * 0.6f),
            end = Offset(cx + cos(angle) * hair, cy + sin(angle) * hair),
            strokeWidth = 0.9.dp.toPx()
        )
        drawCircle(
            color = Color(0xFFF3F6F7).copy(alpha = 0.55f),
            radius = 0.9.dp.toPx(),
            center = Offset(cx + cos(angle) * hair, cy + sin(angle) * hair)
        )
    }
    drawCircle(Color(0xFFF3F6F7).copy(alpha = 0.95f), radius = r, center = Offset(cx, cy))
    drawCircle(color.copy(alpha = 0.70f), radius = r * 0.45f, center = Offset(cx, cy))
}

private fun DrawScope.drawBud(
    cx: Float,
    cy: Float,
    bloom: Float,
    petal: Color,
    leaf: Color
) {
    val open = bloom > 0.5f
    if (open) {
        val r = 4.4.dp.toPx()
        repeat(5) { index ->
            val angle = index * 2 * PI / 5
            drawCircle(
                color = petal.copy(alpha = 0.80f),
                radius = r * 0.62f,
                center = Offset(
                    cx + cos(angle).toFloat() * r * 0.62f,
                    cy + sin(angle).toFloat() * r * 0.62f
                )
            )
        }
        drawCircle(Color(0xFFFFF6E8).copy(alpha = 0.90f), radius = 1.4.dp.toPx(), center = Offset(cx, cy))
    } else {
        drawOval(
            color = petal.copy(alpha = 0.85f),
            topLeft = Offset(cx - 3.dp.toPx(), cy - 5.dp.toPx()),
            size = Size(6.dp.toPx(), 10.dp.toPx())
        )
    }
    // 叶片
    drawOval(
        color = leaf.copy(alpha = 0.55f),
        topLeft = Offset(cx - 6.dp.toPx(), cy + 1.dp.toPx()),
        size = Size(7.dp.toPx(), 3.4.dp.toPx())
    )
}

/** 枫叶花环用的单片叶：轮廓 + 主脉与两侧支脉。 */
private fun DrawScope.drawMapleLeaf(cx: Float, cy: Float, r: Float, color: Color) {
    val leaf = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.30f, cy - r * 0.62f)
        lineTo(cx + r * 0.78f, cy - r * 0.78f)
        lineTo(cx + r * 0.50f, cy - r * 0.20f)
        lineTo(cx + r * 0.92f, cy + r * 0.10f)
        lineTo(cx + r * 0.34f, cy + r * 0.18f)
        lineTo(cx + r * 0.24f, cy + r * 0.86f)
        lineTo(cx, cy + r * 0.30f)
        lineTo(cx - r * 0.24f, cy + r * 0.86f)
        lineTo(cx - r * 0.34f, cy + r * 0.18f)
        lineTo(cx - r * 0.92f, cy + r * 0.10f)
        lineTo(cx - r * 0.50f, cy - r * 0.20f)
        lineTo(cx - r * 0.78f, cy - r * 0.78f)
        lineTo(cx - r * 0.30f, cy - r * 0.62f)
        close()
    }
    drawPath(leaf, color, style = Stroke(width = 1.2f))
    // 叶脉
    drawLine(color, start = Offset(cx, cy - r * 0.80f), end = Offset(cx, cy + r * 0.70f), strokeWidth = 0.9f)
    drawLine(
        color.copy(alpha = color.alpha * 0.7f),
        start = Offset(cx, cy - r * 0.10f),
        end = Offset(cx + r * 0.55f, cy - r * 0.35f),
        strokeWidth = 0.7f
    )
    drawLine(
        color.copy(alpha = color.alpha * 0.7f),
        start = Offset(cx, cy - r * 0.10f),
        end = Offset(cx - r * 0.55f, cy - r * 0.35f),
        strokeWidth = 0.7f
    )
}

/** 深色主题的发光圆钮：多层柔光 + 实心内芯。 */
private fun DrawScope.drawGlowDot(cx: Float, cy: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.14f), radius = 11.dp.toPx(), center = Offset(cx, cy))
    drawCircle(color.copy(alpha = 0.22f), radius = 8.dp.toPx(), center = Offset(cx, cy))
    drawCircle(color.copy(alpha = 0.90f), radius = 4.6.dp.toPx(), center = Offset(cx, cy))
    drawCircle(Color(0xFFF4FBF9).copy(alpha = 0.95f), radius = 2.dp.toPx(), center = Offset(cx, cy))
}

/** 浅色主题的小扁圆点。 */
private fun DrawScope.drawFlatDot(cx: Float, cy: Float, color: Color) {
    drawCircle(color.copy(alpha = 0.28f), radius = 5.dp.toPx(), center = Offset(cx, cy))
    drawCircle(color.copy(alpha = 0.95f), radius = 3.2.dp.toPx(), center = Offset(cx, cy))
}

/** 柳叶形滑块：两端收尖的椭圆，略带倾斜。 */
private fun DrawScope.drawWillowLeaf(cx: Float, cy: Float, color: Color) {
    val half = 5.6.dp.toPx()
    val waist = 2.6.dp.toPx()
    rotate(-24f, pivot = Offset(cx, cy)) {
        val leaf = Path().apply {
            moveTo(cx - half, cy)
            cubicTo(cx - half * 0.45f, cy - waist, cx + half * 0.45f, cy - waist, cx + half, cy)
            cubicTo(cx + half * 0.45f, cy + waist, cx - half * 0.45f, cy + waist, cx - half, cy)
            close()
        }
        drawPath(leaf, color.copy(alpha = 0.92f))
        drawLine(
            color = Color(0xFFF2F7F0).copy(alpha = 0.55f),
            start = Offset(cx - half * 0.72f, cy),
            end = Offset(cx + half * 0.72f, cy),
            strokeWidth = 0.8.dp.toPx()
        )
    }
}

/** 贝壳形滑块：向上张开的扇面 + 两道放射纹。 */
private fun DrawScope.drawShellThumb(cx: Float, cy: Float, color: Color) {
    val r = 5.8.dp.toPx()
    val pivot = Offset(cx, cy + r * 0.45f)
    val topLeft = Offset(pivot.x - r, pivot.y - r)
    val shellSize = Size(r * 2f, r * 2f)
    drawArc(
        color = color.copy(alpha = 0.92f),
        startAngle = 198f,
        sweepAngle = 144f,
        useCenter = true,
        topLeft = topLeft,
        size = shellSize
    )
    // 壳口
    drawArc(
        color = Color(0xFF7A4A2B).copy(alpha = 0.45f),
        startAngle = 198f,
        sweepAngle = 144f,
        useCenter = false,
        topLeft = topLeft,
        size = shellSize,
        style = Stroke(width = 1.dp.toPx())
    )
    // 两道放射纹
    repeat(2) { index ->
        val angle = (216f + index * 54f) * PI.toFloat() / 180f
        drawLine(
            color = Color(0xFF7A4A2B).copy(alpha = 0.45f),
            start = pivot,
            end = Offset(pivot.x + cos(angle) * r * 0.88f, pivot.y + sin(angle) * r * 0.88f),
            strokeWidth = 0.9.dp.toPx()
        )
    }
}

/** 三瓣枫叶形滑块。 */
private fun DrawScope.drawMapleThumb(cx: Float, cy: Float, r: Float, color: Color) {
    val leaf = Path().apply {
        moveTo(cx, cy - r)
        lineTo(cx + r * 0.34f, cy - r * 0.44f)
        lineTo(cx + r, cy - r * 0.56f)
        lineTo(cx + r * 0.52f, cy + r * 0.02f)
        lineTo(cx + r * 0.88f, cy + r * 0.62f)
        lineTo(cx, cy + r * 0.34f)
        lineTo(cx - r * 0.88f, cy + r * 0.62f)
        lineTo(cx - r * 0.52f, cy + r * 0.02f)
        lineTo(cx - r, cy - r * 0.56f)
        lineTo(cx - r * 0.34f, cy - r * 0.44f)
        close()
    }
    drawPath(leaf, color.copy(alpha = 0.92f))
    // 三瓣叶脉
    drawLine(
        Color(0xFF6B3B1E).copy(alpha = 0.45f),
        start = Offset(cx, cy + r * 0.30f),
        end = Offset(cx, cy - r * 0.75f),
        strokeWidth = 0.8.dp.toPx()
    )
    drawLine(
        Color(0xFF6B3B1E).copy(alpha = 0.45f),
        start = Offset(cx, cy + r * 0.30f),
        end = Offset(cx + r * 0.60f, cy - r * 0.40f),
        strokeWidth = 0.7.dp.toPx()
    )
    drawLine(
        Color(0xFF6B3B1E).copy(alpha = 0.45f),
        start = Offset(cx, cy + r * 0.30f),
        end = Offset(cx - r * 0.60f, cy - r * 0.40f),
        strokeWidth = 0.7.dp.toPx()
    )
}

/** 冰锥形滑块：上宽下尖 + 一道高光。 */
private fun DrawScope.drawFrostNeedle(cx: Float, cy: Float, color: Color) {
    val top = cy - 6.dp.toPx()
    val tip = cy + 7.dp.toPx()
    val w = 1.8.dp.toPx()
    val needle = Path().apply {
        moveTo(cx - w, top)
        lineTo(cx + w, top)
        lineTo(cx, tip)
        close()
    }
    drawPath(needle, color.copy(alpha = 0.92f))
    drawLine(
        color = Color(0xFFBFE9F2).copy(alpha = 0.75f),
        start = Offset(cx - w * 0.25f, top + 1.dp.toPx()),
        end = Offset(cx, tip - 2.dp.toPx()),
        strokeWidth = 0.8.dp.toPx()
    )
}

/** 小太极钮：左半填色、右半留白，两侧各一点。 */
private fun DrawScope.drawTaijiThumb(cx: Float, cy: Float, r: Float, ink: Color, paper: Color) {
    val center = Offset(cx, cy)
    drawCircle(paper.copy(alpha = 0.95f), radius = r, center = center)
    drawArc(
        color = ink.copy(alpha = 0.90f),
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2f, r * 2f)
    )
    // 阴阳两点
    drawCircle(ink.copy(alpha = 0.55f), radius = r * 0.22f, center = Offset(cx - r * 0.5f, cy))
    drawCircle(paper.copy(alpha = 0.95f), radius = r * 0.22f, center = Offset(cx + r * 0.5f, cy))
    drawCircle(ink.copy(alpha = 0.90f), radius = r, center = center, style = Stroke(width = 1.1.dp.toPx()))
}

/** 熊猫爪印：一枚掌垫 + 三个趾点。 */
private fun DrawScope.drawPawThumb(cx: Float, cy: Float, color: Color) {
    drawOval(
        color = color.copy(alpha = 0.92f),
        topLeft = Offset(cx - 3.2.dp.toPx(), cy + 0.4.dp.toPx()),
        size = Size(6.4.dp.toPx(), 5.2.dp.toPx())
    )
    val toeY = cy - 4.2.dp.toPx()
    repeat(3) { index ->
        val dx = (index - 1) * 2.9.dp.toPx()
        val dy = if (index == 1) -1.2.dp.toPx() else 0f
        drawCircle(
            color = color.copy(alpha = 0.85f),
            radius = 1.6.dp.toPx(),
            center = Offset(cx + dx, toeY + dy)
        )
    }
}
