package com.daphile.remote.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.daphile.remote.ui.theme.ThemeBackdropKind
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 特别版主题的整页背景。所有绘制都在一张 Canvas 上完成，
 * 用「播放时加速、暂停时减速」的时钟驱动，不使用模糊与阴影，保证 60fps。
 */
@Composable
fun ThemeBackdrop(
    kind: ThemeBackdropKind,
    accent: Color,
    active: Boolean,
    progress: Float,
    modifier: Modifier = Modifier
) {
    val fullModifier = modifier.fillMaxSize()
    when (kind) {
        ThemeBackdropKind.Lattice -> LatticeBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.WindField -> WindFieldBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.Blossom -> BlossomBackdrop(accent, active, progress, fullModifier)
        ThemeBackdropKind.Snowfall -> SnowfallBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.MoonSky -> MoonSkyBackdrop(accent, active, progress, fullModifier)
        ThemeBackdropKind.Starfield -> StarfieldBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.PaperGrid -> PaperGridBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.WillowBreeze -> WillowBreezeBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.OceanWaves -> OceanWavesBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.MapleFall -> MapleFallBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.FrostBloom -> FrostBloomBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.YinYang -> YinYangBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.BambooShade -> BambooShadeBackdrop(accent, active, fullModifier)
        ThemeBackdropKind.Pulse -> Unit
    }
}

/** 中国红：六抹隔扇竖窗棂与两盏摇摆宫灯。 */
@Composable
private fun LatticeBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val gold = Color(0xFFD4AF37)
    val transition = rememberInfiniteTransition(label = "lattice")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 5200 else 14000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "latticeClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF2B080B), bg),
                startY = 0f,
                endY = h
            )
        )

        // 竖窗棂：六抹隔扇
        val columns = 6
        val columnW = w / columns
        for (index in 0 until columns) {
            val left = index * columnW
            drawRect(
                color = gold.copy(alpha = 0.10f),
                topLeft = Offset(left, 0f),
                size = Size(columnW * 0.018f, h * 0.72f)
            )
            drawRect(
                color = gold.copy(alpha = 0.10f),
                topLeft = Offset(left + columnW - columnW * 0.018f, 0f),
                size = Size(columnW * 0.018f, h * 0.72f)
            )
            // 内层细棂
            for (bar in 1..3) {
                val x = left + columnW * bar / 4f
                drawLine(
                    color = gold.copy(alpha = 0.07f),
                    start = Offset(x, h * 0.04f),
                    end = Offset(x, h * 0.70f),
                    strokeWidth = 1.2f
                )
            }
            // 横向窗棂与回纹小格
            for (row in 1..3) {
                val y = h * 0.70f * row / 4f
                drawLine(
                    color = gold.copy(alpha = 0.08f),
                    start = Offset(left, y),
                    end = Offset(left + columnW, y),
                    strokeWidth = 1.2f
                )
                drawRect(
                    color = gold.copy(alpha = 0.06f),
                    topLeft = Offset(left + columnW * 0.18f, y - columnW * 0.10f),
                    size = Size(columnW * 0.22f, columnW * 0.20f),
                    style = Stroke(width = 1f)
                )
            }
        }

        // 宫灯：烛光呼吸、缓慢摆动（只保留右侧一盏，避开左上的标题区）
        val glow = 0.16f + 0.10f * sin(clock.toFloat() * 2 * PI).toFloat().let { (it + 1f) / 2f }
        drawLantern(
            x = w * 0.87f,
            topY = h * 0.13f,
            bodyW = w * 0.15f,
            bodyH = h * 0.12f,
            angle = sin((clock + 0.55f) * 2 * PI).toFloat() * 3.6f,
            gold = gold,
            glow = Color(0xFFE23B4A),
            glowStrength = glow * 0.9f
        )

        // 底部朱墙压边
        drawRect(
            color = accent.copy(alpha = 0.10f),
            topLeft = Offset(0f, h * 0.88f),
            size = Size(w, h * 0.12f)
        )
    }
}

private fun DrawScope.drawLantern(
    x: Float,
    topY: Float,
    bodyW: Float,
    bodyH: Float,
    angle: Float,
    gold: Color,
    glow: Color,
    glowStrength: Float
) {
    rotate(angle, pivot = Offset(x, topY - bodyH * 0.18f)) {
        drawLine(
            color = gold.copy(alpha = 0.40f),
            start = Offset(x, topY - bodyH * 0.18f),
            end = Offset(x, topY),
            strokeWidth = 1.4f
        )
        // 光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(glow.copy(alpha = glowStrength), Color.Transparent),
                center = Offset(x, topY + bodyH * 0.42f),
                radius = bodyW * 1.5f
            ),
            radius = bodyW * 1.5f,
            center = Offset(x, topY + bodyH * 0.42f)
        )
        // 上下灯托
        drawRect(
            color = gold.copy(alpha = 0.75f),
            topLeft = Offset(x - bodyW * 0.30f, topY),
            size = Size(bodyW * 0.60f, bodyH * 0.09f)
        )
        drawRect(
            color = gold.copy(alpha = 0.70f),
            topLeft = Offset(x - bodyW * 0.26f, topY + bodyH * 0.86f),
            size = Size(bodyW * 0.52f, bodyH * 0.07f)
        )
        // 灯身
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    glow.copy(alpha = 0.55f + glowStrength * 0.35f),
                    glow.copy(alpha = 0.28f)
                ),
                center = Offset(x - bodyW * 0.10f, topY + bodyH * 0.36f),
                radius = bodyW * 0.62f
            ),
            topLeft = Offset(x - bodyW / 2f, topY + bodyH * 0.08f),
            size = Size(bodyW, bodyH * 0.80f)
        )
        // 灯骨
        for (rib in 1..3) {
            val rx = x - bodyW / 2f + bodyW * rib / 4f
            drawLine(
                color = gold.copy(alpha = 0.18f),
                start = Offset(rx, topY + bodyH * 0.12f),
                end = Offset(rx, topY + bodyH * 0.84f),
                strokeWidth = 1f
            )
        }
        drawLine(
            color = gold.copy(alpha = 0.55f),
            start = Offset(x, topY + bodyH * 0.92f),
            end = Offset(x, topY + bodyH * 1.18f),
            strokeWidth = 2f
        )
    }
}

/** 风：三层不同速度的风场流线，播放时整体加速。 */
@Composable
private fun WindFieldBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "wind")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "windClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.09f), bg, bg),
                startY = 0f,
                endY = h
            )
        )
        val speed = clock * if (active) 2.4f else 0.8f
        for (layer in 0..2) {
            val baseY = h * (0.18f + layer * 0.26f)
            val amp = h * (0.014f + layer * 0.010f)
            val alpha = 0.10f + layer * 0.045f
            val streak = 5 + layer * 2
            for (index in 0 until streak) {
                val phase = (phase01(speed * (1f + layer * 0.45f) + index / streak.toFloat() + layer * 0.17f))
                val x = (-0.35f + phase * 1.7f) * w
                val y = baseY + sin((phase * 4f + index) * PI).toFloat() * amp
                val path = Path().apply {
                    moveTo(x, y)
                    cubicTo(
                        x + w * 0.12f, y - amp * 2.2f,
                        x + w * 0.24f, y + amp * 2.2f,
                        x + w * 0.38f, y
                    )
                }
                drawPath(
                    path = path,
                    color = accent.copy(alpha = alpha),
                    style = Stroke(width = 1.8f + layer * 0.8f, cap = StrokeCap.Round)
                )
            }
        }
        // 被风带起的花粉/飞絮
        repeat(8) { index ->
            val p = phase01(speed * 0.6f + index * 0.13f)
            val x = (-0.1f + p * 1.2f) * w
            val y = h * (0.25f + (index % 4) * 0.15f) +
                sin((p * 5f + index) * PI).toFloat() * h * 0.02f
            drawCircle(
                color = accent.copy(alpha = 0.22f),
                radius = 2.2f + (index % 3),
                center = Offset(x, y)
            )
        }
    }
}

/** 取小数部分，避免相位不断增大后精度下降。 */
private fun phase01(value: Float): Float {
    val fraction = value - value.toInt()
    return if (fraction < 0f) fraction + 1f else fraction
}

/** 花：底部两角枝桠，花朵随播放进度开合。 */
@Composable
private fun BlossomBackdrop(
    accent: Color,
    active: Boolean,
    progress: Float,
    modifier: Modifier
) {
    val bg = MaterialTheme.colorScheme.background
    val leaf = Color(0xFF7FA679)
    val petal = Color(0xFFE8A0B4)
    val transition = rememberInfiniteTransition(label = "blossom")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 9000 else 20000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "blossomClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(bg, accent.copy(alpha = 0.07f), bg),
                startY = 0f,
                endY = h
            )
        )
        val bloom = 0.42f + 0.58f * progress.coerceIn(0f, 1f)

        // 左下与右下枝桠
        val leftBranch = Path().apply {
            moveTo(-w * 0.04f, h)
            cubicTo(w * 0.06f, h * 0.84f, w * 0.12f, h * 0.66f, w * 0.28f, h * 0.50f)
            cubicTo(w * 0.33f, h * 0.45f, w * 0.36f, h * 0.38f, w * 0.39f, h * 0.31f)
        }
        val leftTwig = Path().apply {
            moveTo(w * 0.13f, h * 0.74f)
            cubicTo(w * 0.19f, h * 0.72f, w * 0.23f, h * 0.68f, w * 0.26f, h * 0.62f)
        }
        val rightBranch = Path().apply {
            moveTo(w * 1.04f, h)
            cubicTo(w * 0.94f, h * 0.86f, w * 0.88f, h * 0.70f, w * 0.74f, h * 0.54f)
            cubicTo(w * 0.69f, h * 0.46f, w * 0.65f, h * 0.40f, w * 0.61f, h * 0.34f)
        }
        val rightTwig = Path().apply {
            moveTo(w * 0.87f, h * 0.76f)
            cubicTo(w * 0.81f, h * 0.74f, w * 0.78f, h * 0.70f, w * 0.75f, h * 0.64f)
        }
        drawPath(leftBranch, leaf.copy(alpha = 0.50f), style = Stroke(width = 6f, cap = StrokeCap.Round))
        drawPath(leftTwig, leaf.copy(alpha = 0.42f), style = Stroke(width = 4f, cap = StrokeCap.Round))
        drawPath(rightBranch, leaf.copy(alpha = 0.44f), style = Stroke(width = 5f, cap = StrokeCap.Round))
        drawPath(rightTwig, leaf.copy(alpha = 0.36f), style = Stroke(width = 3.5f, cap = StrokeCap.Round))

        // 枝上花朵
        drawBlossom(w * 0.28f, h * 0.50f, w * 0.055f, petal, bloom, 5)
        drawBlossom(w * 0.39f, h * 0.31f, w * 0.045f, petal, bloom * 0.9f, 5)
        drawBlossom(w * 0.26f, h * 0.62f, w * 0.042f, petal, bloom * 0.85f, 5)
        drawBlossom(w * 0.19f, h * 0.69f, w * 0.036f, petal, bloom * 0.8f, 5)
        drawBlossom(w * 0.11f, h * 0.79f, w * 0.032f, petal, bloom * 0.7f, 5)
        drawBlossom(w * 0.74f, h * 0.54f, w * 0.052f, petal, bloom, 5)
        drawBlossom(w * 0.61f, h * 0.34f, w * 0.042f, petal, bloom * 0.9f, 5)
        drawBlossom(w * 0.75f, h * 0.64f, w * 0.040f, petal, bloom * 0.8f, 5)
        drawBlossom(w * 0.82f, h * 0.71f, w * 0.035f, petal, bloom * 0.75f, 5)
        drawBlossom(w * 0.90f, h * 0.81f, w * 0.030f, petal, bloom * 0.65f, 5)

        // 飘落花瓣
        repeat(10) { index ->
            val p = phase01(clock * 0.7f + index * 0.1f)
            val x = w * (0.10f + (index * 0.087f)) + sin((p * 3f + index) * PI).toFloat() * w * 0.05f
            val y = h * (0.08f + p * 0.85f)
            drawOval(
                color = petal.copy(alpha = 0.30f),
                topLeft = Offset(x, y),
                size = Size(w * 0.020f, w * 0.010f)
            )
        }
    }
}

private fun DrawScope.drawBlossom(
    cx: Float,
    cy: Float,
    radius: Float,
    petal: Color,
    bloom: Float,
    petalCount: Int
) {
    repeat(petalCount) { index ->
        val angle = index * 2 * PI / petalCount
        val px = cx + cos(angle).toFloat() * radius * 0.6f * bloom
        val py = cy + sin(angle).toFloat() * radius * 0.6f * bloom
        drawCircle(
            color = petal.copy(alpha = 0.62f),
            radius = radius * 0.48f * bloom,
            center = Offset(px, py)
        )
    }
    drawCircle(
        color = Color(0xFFFFF6E8).copy(alpha = 0.80f),
        radius = radius * 0.16f * bloom,
        center = Offset(cx, cy)
    )
}

/** 雪：雪原、远近两层视差落雪与角落霜花。 */
@Composable
private fun SnowfallBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "snow")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snowClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0C1B29), bg),
                startY = 0f,
                endY = h
            )
        )
        // 远山雪脊
        val farDune = Path().apply {
            moveTo(0f, h * 0.76f)
            cubicTo(w * 0.16f, h * 0.71f, w * 0.30f, h * 0.79f, w * 0.48f, h * 0.77f)
            cubicTo(w * 0.66f, h * 0.75f, w * 0.80f, h * 0.81f, w, h * 0.78f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(farDune, accent.copy(alpha = 0.06f))
        val nearDune = Path().apply {
            moveTo(0f, h * 0.90f)
            cubicTo(w * 0.22f, h * 0.86f, w * 0.42f, h * 0.92f, w * 0.62f, h * 0.89f)
            cubicTo(w * 0.80f, h * 0.87f, w * 0.92f, h * 0.91f, w, h * 0.89f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(nearDune, Color(0xFFF4F9FF).copy(alpha = 0.05f))

        val speed = if (active) 1f else 0.35f
        snowLayer(
            count = 26,
            width = w,
            height = h,
            clock = clock * speed,
            radiusRange = 1.0f..2.4f,
            alpha = 0.30f,
            drift = w * 0.012f
        )
        snowLayer(
            count = 16,
            width = w,
            height = h,
            clock = clock * speed * 1.35f,
            radiusRange = 2.2f..3.6f,
            alpha = 0.50f,
            drift = w * 0.025f
        )

        // 左下角霜花
        drawFrost(w * 0.03f, h * 0.97f, w * 0.22f, Color(0xFFBFE9F2))
        drawFrost(w * 0.97f, h * 0.99f, w * 0.16f, Color(0xFFBFE9F2))
    }
}

private fun DrawScope.snowLayer(
    count: Int,
    width: Float,
    height: Float,
    clock: Float,
    radiusRange: ClosedFloatingPointRange<Float>,
    alpha: Float,
    drift: Float
) {
    repeat(count) { index ->
        val seedX = ((index * 37) % 100) / 100f
        val speed = 0.45f + (index % 5) * 0.13f
        val y = phase01((index * 53 % 100) / 100f + clock * speed) * height
        val x = seedX * width + sin((clock * 2f + index * 0.3f) * PI).toFloat() * drift
        val radius = radiusRange.start + (index % 3) * (radiusRange.endInclusive - radiusRange.start) / 3f
        drawCircle(
            color = Color(0xFFF4F9FF).copy(alpha = alpha),
            radius = radius,
            center = Offset(x, y)
        )
    }
}

private fun DrawScope.drawFrost(originX: Float, originY: Float, length: Float, color: Color) {
    val direction = if (originX < 1f) 1f else -1f
    repeat(5) { index ->
        val angle = (-0.35f - index * 0.22f) * PI.toFloat()
        val ex = originX + cos(angle) * length * direction
        val ey = originY + sin(angle) * length
        drawLine(
            color = color.copy(alpha = 0.13f),
            start = Offset(originX, originY),
            end = Offset(ex, ey),
            strokeWidth = 1.4f
        )
        // 冰晶分叉
        repeat(3) { twig ->
            val t = 0.35f + twig * 0.22f
            val bx = originX + (ex - originX) * t
            val by = originY + (ey - originY) * t
            drawLine(
                color = color.copy(alpha = 0.10f),
                start = Offset(bx, by),
                end = Offset(
                    bx + cos(angle - 0.7f) * length * 0.16f * direction,
                    by + sin(angle - 0.7f) * length * 0.16f
                ),
                strokeWidth = 1f
            )
        }
    }
}

/** 月：夜空、远山、随进度移动的巨月与横掠薄云。 */
@Composable
private fun MoonSkyBackdrop(
    accent: Color,
    active: Boolean,
    progress: Float,
    modifier: Modifier
) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "moonSky")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 16000 else 30000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "moonClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF070B16), Color(0xFF0E1526), bg),
                startY = 0f,
                endY = h
            )
        )
        // 星
        repeat(42) { index ->
            val sx = ((index * 41) % 97) / 97f * w
            val sy = ((index * 29) % 61) / 61f * h * 0.62f
            val twinkle = 0.35f + 0.35f * (sin((clock * 1.7f + index * 0.11f) * 2 * PI).toFloat() + 1f) / 2f
            drawCircle(
                color = Color(0xFFEAE6DA).copy(alpha = twinkle),
                radius = 0.9f + (index % 3) * 0.5f,
                center = Offset(sx, sy)
            )
        }
        // 巨月：悬在右上方的夜空，随进度缓缓西移
        val moonX = w * (0.60f + 0.18f * progress.coerceIn(0f, 1f))
        val moonY = h * 0.085f
        val moonR = h * 0.075f
        drawCircle(
            color = Color(0xFFEAE6DA).copy(alpha = 0.03f),
            radius = moonR * 1.70f,
            center = Offset(moonX, moonY)
        )
        drawCircle(
            color = Color(0xFFEAE6DA).copy(alpha = 0.05f),
            radius = moonR * 1.25f,
            center = Offset(moonX, moonY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF7F3E6), Color(0xFFD9D3C2)),
                center = Offset(moonX - moonR * 0.25f, moonY - moonR * 0.25f),
                radius = moonR * 1.2f
            ),
            radius = moonR,
            center = Offset(moonX, moonY)
        )
        // 月面淡影
        drawCircle(
            color = Color(0xFFBDB7A6).copy(alpha = 0.20f),
            radius = moonR * 0.22f,
            center = Offset(moonX + moonR * 0.30f, moonY - moonR * 0.24f)
        )
        drawCircle(
            color = Color(0xFFBDB7A6).copy(alpha = 0.16f),
            radius = moonR * 0.15f,
            center = Offset(moonX - moonR * 0.34f, moonY + moonR * 0.30f)
        )

        // 薄云横掠，遮月时压低亮度
        for (index in 0 until 3) {
            val p = phase01(clock * (0.25f + index * 0.08f) + index * 0.4f)
            val cx = (-0.3f + p * 1.6f) * w
            val cy = h * (0.22f + index * 0.045f)
            drawOval(
                color = Color(0xFFC9CEDA).copy(alpha = 0.10f),
                topLeft = Offset(cx, cy),
                size = Size(w * 0.40f, h * 0.030f)
            )
            drawOval(
                color = Color(0xFFC9CEDA).copy(alpha = 0.06f),
                topLeft = Offset(cx + w * 0.10f, cy + h * 0.018f),
                size = Size(w * 0.24f, h * 0.022f)
            )
        }

        // 远山
        val far = Path().apply {
            moveTo(0f, h * 0.88f)
            lineTo(w * 0.16f, h * 0.79f)
            lineTo(w * 0.30f, h * 0.86f)
            lineTo(w * 0.46f, h * 0.77f)
            lineTo(w * 0.62f, h * 0.85f)
            lineTo(w * 0.80f, h * 0.79f)
            lineTo(w, h * 0.87f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(far, accent.copy(alpha = 0.09f))
        val near = Path().apply {
            moveTo(0f, h * 0.95f)
            lineTo(w * 0.22f, h * 0.90f)
            lineTo(w * 0.40f, h * 0.96f)
            lineTo(w * 0.60f, h * 0.91f)
            lineTo(w * 0.78f, h * 0.97f)
            lineTo(w, h * 0.93f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(near, bg.copy(alpha = 0.55f))
    }
}

/** 星域：缓缓漂移的星点与两道极细轨道弧，只做极轻的闪烁。 */
@Composable
private fun StarfieldBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "starfield")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 18000 else 36000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "starClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF050912), bg),
                startY = 0f,
                endY = h
            )
        )
        // 星点：大小与透明度分级，整体只做极小幅度的横向漂移
        repeat(54) { index ->
            val seedX = ((index * 43) % 97) / 97f
            val seedY = ((index * 31) % 89) / 89f
            val x = seedX * w + sin((clock * 1.4f + index * 0.07f) * 2 * PI).toFloat() * w * 0.012f
            val y = seedY * h * 0.94f + cos((clock * 1.1f + index * 0.05f) * 2 * PI).toFloat() * h * 0.010f
            val twinkle = 0.20f + 0.32f * ((sin((clock * 2f + index * 0.09f) * 2 * PI).toFloat() + 1f) / 2f)
            drawCircle(
                color = accent.copy(alpha = twinkle),
                radius = 0.8f + (index % 4) * 0.45f,
                center = Offset(x, y)
            )
        }
        // 两道极细轨道弧，压在星点上更显安静
        for (ring in 0 until 2) {
            val rx = w * (0.58f + ring * 0.15f)
            val ry = h * (0.22f + ring * 0.09f)
            drawOval(
                color = accent.copy(alpha = 0.06f),
                topLeft = Offset(w * 0.52f - rx, h * 0.30f - ry),
                size = Size(rx * 2f, ry * 2f),
                style = Stroke(width = 1f)
            )
        }
    }
}

/** 纸面：极淡的方格纸与两团缓慢游移的柔光。 */
@Composable
private fun PaperGridBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "paperGrid")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 24000 else 48000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "gridClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(color = bg)
        // 方格：竖 14 列，横按同一格宽推算，保证格子是方的
        val columns = 14
        val cell = w / columns
        for (index in 1 until columns) {
            val x = cell * index
            drawLine(
                color = accent.copy(alpha = 0.045f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1f
            )
        }
        val rows = if (cell <= 0f) 0 else (h / cell).toInt().coerceIn(1, 48)
        for (index in 1 until rows) {
            val y = cell * index
            drawLine(
                color = accent.copy(alpha = 0.045f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }
        // 柔光：径向渐变的淡色光团，缓慢游移
        repeat(2) { index ->
            val p = phase01(clock + index * 0.5f)
            val cx = w * (0.26f + index * 0.36f) + sin((p * 2f + index) * PI).toFloat() * w * 0.07f
            val cy = h * (0.28f + index * 0.36f) + cos((p * 2f + index) * PI).toFloat() * h * 0.06f
            val radius = w * 0.42f
            val alpha = if (index == 0) 0.09f else 0.06f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.copy(alpha = alpha), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = radius
                ),
                radius = radius,
                center = Offset(cx, cy)
            )
        }
    }
}

/** 春：三缕柳丝自顶部垂落，随微风摆动，间有柳絮飘飞。 */
@Composable
private fun WillowBreezeBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "willow")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 11000 else 24000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "willowClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.07f), bg, bg),
                startY = 0f,
                endY = h
            )
        )
        // 三条柳丝：正弦摆动，越往下摆幅越大
        for (strand in 0 until 3) {
            val baseX = w * (0.15f + strand * 0.30f)
            val drop = h * (0.52f + strand * 0.10f)
            val sway = sin((clock * 2f + strand * 0.32f) * 2 * PI).toFloat() * w * (0.016f + strand * 0.006f)
            val path = Path().apply {
                moveTo(baseX, 0f)
                cubicTo(
                    baseX + sway * 0.35f, drop * 0.34f,
                    baseX + sway * 1.05f, drop * 0.66f,
                    baseX + sway * 1.6f, drop
                )
            }
            drawPath(
                path = path,
                color = accent.copy(alpha = 0.32f),
                style = Stroke(width = 2.2f, cap = StrokeCap.Round)
            )
            // 枝上细叶：沿柳丝等距取样
            for (leafIndex in 0 until 7) {
                val t = 0.14f + leafIndex * 0.12f
                val tilt = sin((clock * 3f + leafIndex * 0.4f + strand) * 2 * PI).toFloat() * 0.40f
                drawLeaf(
                    cx = baseX + sway * 1.6f * t * t,
                    cy = drop * t,
                    length = w * 0.020f,
                    color = accent.copy(alpha = 0.40f),
                    tilt = tilt + 0.15f
                )
            }
        }
        // 柳絮：轻亮点缓慢上浮
        repeat(9) { index ->
            val p = phase01(clock * 0.5f + index * 0.12f)
            val x = w * (0.08f + ((index * 29) % 83) / 83f * 0.86f) +
                sin((p * 3f + index) * PI).toFloat() * w * 0.04f
            val y = h * (0.88f - p * 0.74f)
            drawCircle(
                color = accent.copy(alpha = 0.26f),
                radius = 1.5f + (index % 3) * 0.7f,
                center = Offset(x, y)
            )
        }
    }
}

/** 一片细长小叶：以 (cx, cy) 为心，tilt 为倾斜力度。 */
private fun DrawScope.drawLeaf(cx: Float, cy: Float, length: Float, color: Color, tilt: Float) {
    rotate(tilt * 18f, pivot = Offset(cx, cy)) {
        drawOval(
            color = color,
            topLeft = Offset(cx - length * 0.22f, cy - length),
            size = Size(length * 0.44f, length * 2f)
        )
    }
}

/** 夏：三层正弦海浪自远及近铺开，浪尖点缀稀疏日光。 */
@Composable
private fun OceanWavesBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "ocean")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 9000 else 19000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "oceanClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(bg, accent.copy(alpha = 0.08f), bg),
                startY = 0f,
                endY = h
            )
        )
        // 三层海浪：层高、速度与浓度依次递增，铺满画面下部约四成
        for (layer in 0 until 3) {
            val baseY = h * (0.62f + layer * 0.09f)
            val amp = h * (0.026f + layer * 0.012f)
            val phase = phase01(clock * (0.8f + layer * 0.5f) + layer * 0.17f)
            val path = Path()
            for (step in 0..48) {
                val t = step / 48f
                val x = w * t
                val y = baseY + sin((phase + t * 1.6f) * 2 * PI).toFloat() * amp
                if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            path.lineTo(w, h)
            path.lineTo(0f, h)
            path.close()
            drawPath(
                path = path,
                color = accent.copy(alpha = 0.10f + layer * 0.06f)
            )
        }
        // 浪尖上的日光短划
        repeat(7) { index ->
            val p = phase01(clock * 1.2f + index * 0.14f)
            val gx = (-0.08f + p * 1.16f) * w
            val gy = h * 0.79f + sin((p * 2f + index) * PI).toFloat() * h * 0.05f - h * 0.015f
            drawLine(
                color = accent.copy(alpha = 0.22f),
                start = Offset(gx, gy),
                end = Offset(gx + w * 0.030f, gy - h * 0.004f),
                strokeWidth = 1.6f,
                cap = StrokeCap.Round
            )
        }
    }
}

/** 秋：底部两层低矮山影，枫叶边旋边摆地飘落。 */
@Composable
private fun MapleFallBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "maple")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 12000 else 26000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "mapleClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(bg, accent.copy(alpha = 0.06f), bg),
                startY = 0f,
                endY = h
            )
        )
        // 远山与近山：两层低矮山脊
        val far = Path().apply {
            moveTo(0f, h * 0.88f)
            cubicTo(w * 0.18f, h * 0.80f, w * 0.34f, h * 0.90f, w * 0.52f, h * 0.84f)
            cubicTo(w * 0.70f, h * 0.78f, w * 0.84f, h * 0.90f, w, h * 0.85f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(far, accent.copy(alpha = 0.06f))
        val near = Path().apply {
            moveTo(0f, h * 0.96f)
            cubicTo(w * 0.22f, h * 0.91f, w * 0.40f, h * 0.99f, w * 0.62f, h * 0.95f)
            cubicTo(w * 0.78f, h * 0.92f, w * 0.90f, h * 0.98f, w, h * 0.94f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(near, accent.copy(alpha = 0.10f))

        // 飘落的枫叶：各自的相位与旋转速度略有差异
        repeat(9) { index ->
            val p = phase01(clock * (0.55f + (index % 4) * 0.08f) + index * 0.13f)
            val cx = w * (0.08f + ((index * 31) % 79) / 79f * 0.84f) +
                sin((p * 3f + index) * PI).toFloat() * w * 0.05f
            val cy = h * (-0.08f + p * 1.02f)
            drawMaple(
                cx = cx,
                cy = cy,
                size = w * (0.014f + (index % 3) * 0.004f),
                angleDeg = clock * 360f + index * 37f,
                color = accent.copy(alpha = 0.26f + (index % 3) * 0.05f)
            )
        }
    }
}

/** 一片三裂枫叶：以 (cx, cy) 为心，size 为半径。 */
private fun DrawScope.drawMaple(cx: Float, cy: Float, size: Float, angleDeg: Float, color: Color) {
    val path = Path().apply {
        moveTo(cx, cy - size)
        lineTo(cx + size * 0.28f, cy - size * 0.30f)
        lineTo(cx + size * 0.95f, cy - size * 0.45f)
        lineTo(cx + size * 0.55f, cy + size * 0.20f)
        lineTo(cx + size * 0.22f, cy + size * 0.85f)
        lineTo(cx - size * 0.22f, cy + size * 0.85f)
        lineTo(cx - size * 0.55f, cy + size * 0.20f)
        lineTo(cx - size * 0.95f, cy - size * 0.45f)
        lineTo(cx - size * 0.28f, cy - size * 0.30f)
        close()
    }
    rotate(angleDeg, pivot = Offset(cx, cy)) {
        drawPath(path, color)
    }
}

/** 冬：霜晶自左右边缘向画面内侧生长，缓慢明灭。 */
@Composable
private fun FrostBloomBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "frostBloom")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 16000 else 32000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "frostClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.10f), bg, bg),
                startY = 0f,
                endY = h
            )
        )
        // 霜晶的缓慢明灭：0.55 → 1.0
        val shimmer = 0.55f + 0.45f * ((sin(clock * 2 * PI).toFloat() + 1f) / 2f)
        for (root in 0 until 3) {
            val length = w * (0.20f - root * 0.035f)
            drawFrostBranch(
                x = -w * 0.01f,
                y = h * (0.16f + root * 0.30f),
                angle = 0.14f + root * 0.20f,
                length = length,
                depth = 0,
                color = accent,
                shimmer = shimmer
            )
            drawFrostBranch(
                x = w * 1.01f,
                y = h * (0.28f + root * 0.28f),
                angle = PI.toFloat() - 0.14f - root * 0.20f,
                length = length,
                depth = 0,
                color = accent,
                shimmer = shimmer
            )
        }
    }
}

/** 霜晶枝：三层递归分叉，主干上带细小冰刺。 */
private fun DrawScope.drawFrostBranch(
    x: Float,
    y: Float,
    angle: Float,
    length: Float,
    depth: Int,
    color: Color,
    shimmer: Float
) {
    val endX = x + cos(angle) * length
    val endY = y + sin(angle) * length
    drawLine(
        color = color.copy(alpha = (0.06f + depth * 0.015f) * shimmer),
        start = Offset(x, y),
        end = Offset(endX, endY),
        strokeWidth = 1.4f - depth * 0.3f,
        cap = StrokeCap.Round
    )
    if (depth >= 3) return
    repeat(2) { barb ->
        val t = 0.36f + barb * 0.26f
        val bx = x + (endX - x) * t
        val by = y + (endY - y) * t
        drawLine(
            color = color.copy(alpha = 0.05f * shimmer),
            start = Offset(bx, by),
            end = Offset(
                bx + cos(angle - 1.05f) * length * 0.16f,
                by + sin(angle - 1.05f) * length * 0.16f
            ),
            strokeWidth = 0.9f
        )
    }
    drawFrostBranch(endX, endY, angle - 0.42f, length * 0.56f, depth + 1, color, shimmer)
    drawFrostBranch(endX, endY, angle + 0.36f, length * 0.50f, depth + 1, color, shimmer)
}

/** 太极：角落一枚极淡的太极盘缓慢旋转，两道薄云横掠。 */
@Composable
private fun YinYangBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "yinYang")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 30000 else 60000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "taijiClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(accent.copy(alpha = 0.05f), bg, bg),
                startY = 0f,
                endY = h
            )
        )
        // 角落太极盘：整圈缓慢旋转，透明度极低
        val cx = w * 0.86f
        val cy = h * 0.14f
        val radius = w * 0.30f
        val ink = accent.copy(alpha = 0.05f)
        rotate(clock * 360f, pivot = Offset(cx, cy)) {
            // 阴鱼：左半圆
            drawArc(
                color = ink,
                startAngle = 90f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(cx - radius, cy - radius),
                size = Size(radius * 2f, radius * 2f)
            )
            // 上下两个半圆盘与鱼眼
            drawCircle(color = ink, radius = radius * 0.5f, center = Offset(cx, cy - radius * 0.5f))
            drawCircle(color = bg.copy(alpha = 0.50f), radius = radius * 0.5f, center = Offset(cx, cy + radius * 0.5f))
            drawCircle(color = bg.copy(alpha = 0.60f), radius = radius * 0.11f, center = Offset(cx, cy - radius * 0.5f))
            drawCircle(color = ink.copy(alpha = 0.09f), radius = radius * 0.11f, center = Offset(cx, cy + radius * 0.5f))
            drawCircle(color = ink, radius = radius, center = Offset(cx, cy), style = Stroke(width = 1.2f))
        }
        // 薄云横掠
        for (index in 0 until 2) {
            val p = phase01(clock * (0.35f + index * 0.12f) + index * 0.4f)
            val x = (-0.3f + p * 1.6f) * w
            val y = h * (0.30f + index * 0.26f)
            drawOval(
                color = accent.copy(alpha = 0.06f),
                topLeft = Offset(x, y),
                size = Size(w * 0.42f, h * 0.026f)
            )
            drawOval(
                color = accent.copy(alpha = 0.04f),
                topLeft = Offset(x + w * 0.13f, y + h * 0.014f),
                size = Size(w * 0.20f, h * 0.018f)
            )
        }
    }
}

/** 竹影：五竿高低错落的墨竹，竹节与竹叶随风轻摆。 */
@Composable
private fun BambooShadeBackdrop(accent: Color, active: Boolean, modifier: Modifier) {
    val bg = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "bamboo")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (active) 10000 else 22000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "bambooClock"
    )
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(bg, accent.copy(alpha = 0.05f), bg),
                startY = 0f,
                endY = h
            )
        )
        // 五竿竹：粗细、位置、顶端高度各不相同
        for (stalk in 0 until 5) {
            val baseX = w * (0.10f + stalk * 0.20f)
            val stalkW = w * (0.018f + (stalk % 3) * 0.006f)
            val topY = h * (0.05f + (stalk % 3) * 0.05f)
            val sway = sin((clock * 2f + stalk * 0.22f) * 2 * PI).toFloat() * w * 0.010f
            drawLine(
                color = accent.copy(alpha = 0.16f),
                start = Offset(baseX, h * 1.02f),
                end = Offset(baseX + sway, topY),
                strokeWidth = stalkW,
                cap = StrokeCap.Round
            )
            // 竹节：用底色在竹竿上切出横线
            for (node in 0..3) {
                val t = 0.18f + node * 0.22f
                val nx = baseX + sway * t
                val ny = h * 1.02f + (topY - h * 1.02f) * t
                drawLine(
                    color = bg.copy(alpha = 0.55f),
                    start = Offset(nx - stalkW * 0.80f, ny),
                    end = Offset(nx + stalkW * 0.80f, ny),
                    strokeWidth = 1.1f
                )
            }
            // 叶簇：三片叶交错下垂，随竹竿同步轻摆
            for (leaf in 0 until 3) {
                val t = 0.10f + leaf * 0.13f
                val tilt = if (leaf % 2 == 0) 2.4f else -2.4f
                drawLeaf(
                    cx = baseX + sway * (1.2f + t),
                    cy = topY + h * t,
                    length = w * 0.030f,
                    color = accent.copy(alpha = 0.28f + leaf * 0.05f),
                    tilt = tilt + sway * 0.02f
                )
            }
        }
    }
}
