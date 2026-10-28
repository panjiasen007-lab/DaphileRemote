package com.daphile.remote.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import com.daphile.remote.ui.theme.ThemeSceneKind
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * 特别版主题的播放场景，取代通用火柴人对战。
 * 所有场景统一接收 [progress] 与 [playing]：进度推动角色位移，暂停时动作定格。
 */
@Composable
fun ThemeScene(
    kind: ThemeSceneKind,
    progress: Float,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    when (kind) {
        ThemeSceneKind.ShadowPuppet -> ShadowPuppetScene(progress, playing, modifier)
        ThemeSceneKind.Kites -> KiteScene(progress, playing, modifier)
        ThemeSceneKind.Butterflies -> ButterflyScene(progress, playing, modifier)
        ThemeSceneKind.SnowChase -> SnowChaseScene(progress, playing, modifier)
        ThemeSceneKind.MoonSword -> MoonSwordScene(progress, playing, modifier)
        ThemeSceneKind.StickFight -> StickFightScene(progress, playing, modifier)
        ThemeSceneKind.SoundWave -> SoundWaveScene(progress, playing, modifier)
        ThemeSceneKind.PaperPlane -> PaperPlaneScene(progress, playing, modifier)
        ThemeSceneKind.Swallows -> SwallowsScene(progress, playing, modifier)
        ThemeSceneKind.Sailboat -> SailboatScene(progress, playing, modifier)
        ThemeSceneKind.Geese -> GeeseScene(progress, playing, modifier)
        ThemeSceneKind.Skater -> SkaterScene(progress, playing, modifier)
        ThemeSceneKind.PushHands -> PushHandsScene(progress, playing, modifier)
        ThemeSceneKind.PandaBamboo -> PandaBambooScene(progress, playing, modifier)
    }
}

@Composable
private fun sceneClock(playing: Boolean, duration: Int, label: String): Float {
    val transition = rememberInfiniteTransition(label = label)
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "$label-clock"
    )
    return if (playing) clock else 0.32f
}

/** 中国红：皮影戏剪影对战，暖光背屏、关节金线，出招盖「福」印。 */
@Composable
private fun ShadowPuppetScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val ink = Color(0xFF140A0B)
    val gold = Color(0xFFD4AF37)
    val hero = Color(0xFF8E1B22)
    val clock = sceneClock(playing, 1700, "puppet")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.88f
        // 暖光背屏
        drawRect(
            brush = Brush.horizontalGradient(
                listOf(
                    Color(0xFFB6402A).copy(alpha = 0.10f),
                    Color(0xFFE9A55A).copy(alpha = 0.26f),
                    Color(0xFFB6402A).copy(alpha = 0.10f)
                )
            )
        )
        drawLine(gold.copy(alpha = 0.22f), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 1.4f)

        val travel = progress.coerceIn(0f, 1f) * w * 0.34f
        val fighterH = h * 0.72f
        val heroX = w * 0.28f + travel
        val rivalX = heroX + fighterH * 0.44f
        val beat = clock
        val swing = sin(beat * 2 * PI).toFloat()

        drawPuppet(heroX, groundY, fighterH, ink, gold, hero, facing = 1f, swing = swing, move = (beat * 3).toInt() % 3)
        drawPuppet(
            rivalX, groundY, fighterH, ink, gold, Color(0xFF33170F),
            facing = -1f, swing = -swing, move = (beat * 3 + 1).toInt() % 3
        )

        // 出招时的「福」字印
        if (playing && abs(swing) > 0.72f) {
            val strength = (abs(swing) - 0.72f) / 0.28f
            val sealSize = fighterH * 0.30f * (0.8f + strength * 0.3f)
            val cx = (heroX + rivalX) / 2f
            val cy = groundY - fighterH * 0.66f
            rotate(8f, pivot = Offset(cx, cy)) {
                drawRect(
                    color = gold.copy(alpha = 0.55f * strength),
                    topLeft = Offset(cx - sealSize / 2f, cy - sealSize / 2f),
                    size = Size(sealSize, sealSize),
                    style = Stroke(width = 2f)
                )
                drawLine(
                    color = gold.copy(alpha = 0.45f * strength),
                    Offset(cx - sealSize * 0.28f, cy - sealSize * 0.28f),
                    Offset(cx + sealSize * 0.28f, cy + sealSize * 0.28f),
                    strokeWidth = 1.6f
                )
                drawLine(
                    color = gold.copy(alpha = 0.45f * strength),
                    Offset(cx + sealSize * 0.28f, cy - sealSize * 0.28f),
                    Offset(cx - sealSize * 0.28f, cy + sealSize * 0.28f),
                    strokeWidth = 1.6f
                )
            }
        }
    }
}

private fun DrawScope.drawPuppet(
    cx: Float,
    groundY: Float,
    height: Float,
    ink: Color,
    gold: Color,
    robe: Color,
    facing: Float,
    swing: Float,
    move: Int
) {
    val headR = height * 0.095f
    val headY = groundY - height * 0.90f
    val shoulderY = groundY - height * 0.72f
    val hipY = groundY - height * 0.44f
    val limb = height * 0.075f

    // 袍身
    val cape = Path().apply {
        moveTo(cx - height * 0.09f, shoulderY)
        lineTo(cx + height * 0.09f, shoulderY)
        lineTo(cx + height * 0.14f, hipY + height * 0.05f)
        lineTo(cx - height * 0.14f, hipY + height * 0.05f)
        close()
    }
    drawPath(cape, robe)
    // 躯干
    drawLine(
        color = ink,
        start = Offset(cx, headY + headR * 0.8f),
        end = Offset(cx, hipY),
        strokeWidth = limb,
        cap = StrokeCap.Round
    )
    // 头与冠
    drawCircle(ink, radius = headR, center = Offset(cx, headY))
    drawLine(
        color = gold.copy(alpha = 0.55f),
        start = Offset(cx - headR, headY - headR * 0.62f),
        end = Offset(cx + headR, headY - headR * 0.62f),
        strokeWidth = 1.3f
    )
    // 双腿：交叉步
    val stride = height * 0.15f * swing
    drawLine(ink, Offset(cx, hipY), Offset(cx - stride, groundY), strokeWidth = limb * 0.8f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx, hipY), Offset(cx + stride * 0.55f, groundY), strokeWidth = limb * 0.8f, cap = StrokeCap.Round)
    // 双臂：按招式摆动
    val armLen = height * 0.32f
    val attack = if (move % 2 == 0) swing else -swing
    val frontHandX = cx + facing * armLen * (0.62f + 0.38f * attack)
    val frontHandY = shoulderY + armLen * (0.42f - 0.55f * attack)
    drawLine(ink, Offset(cx, shoulderY), Offset(frontHandX, frontHandY), strokeWidth = limb * 0.7f, cap = StrokeCap.Round)
    drawLine(
        ink,
        Offset(cx, shoulderY),
        Offset(cx - facing * armLen * 0.35f, shoulderY + armLen * 0.62f),
        strokeWidth = limb * 0.7f,
        cap = StrokeCap.Round
    )
    // 关节金线
    drawCircle(gold.copy(alpha = 0.75f), radius = 2.2f, center = Offset(cx, shoulderY))
    drawCircle(gold.copy(alpha = 0.70f), radius = 2.2f, center = Offset(cx, hipY))
    drawCircle(gold.copy(alpha = 0.65f), radius = 1.8f, center = Offset(frontHandX, frontHandY))
}

/** 风：双纸鸢竞飞，四种姿态由进度与节拍推动。 */
@Composable
private fun KiteScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val accent = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val clock = sceneClock(playing, 3200, "kite")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(
            brush = Brush.verticalGradient(
                listOf(accent.copy(alpha = 0.10f), Color.Transparent)
            )
        )
        val travel = progress.coerceIn(0f, 1f) * w * 0.36f
        val flap = sin(clock * 2 * PI).toFloat()
        val firstX = w * 0.34f + travel
        val firstY = h * 0.34f + flap * h * 0.10f
        // 线
        drawLine(
            color = accent.copy(alpha = 0.20f),
            start = Offset(firstX, firstY + h * 0.21f),
            end = Offset(w * 0.06f, h),
            strokeWidth = 1f
        )
        drawLine(
            color = secondary.copy(alpha = 0.18f),
            start = Offset(firstX + w * 0.18f, h * 0.56f - flap * h * 0.06f),
            end = Offset(w * 0.94f, h),
            strokeWidth = 1f
        )
        drawKite(firstX, firstY, h * 0.42f, accent, rotation = -16f + flap * 16f, tail = flap)
        drawKite(
            firstX + w * 0.18f,
            h * 0.56f - flap * h * 0.06f,
            h * 0.30f,
            secondary,
            rotation = 12f - flap * 12f,
            tail = -flap
        )
    }
}

private fun DrawScope.drawKite(
    cx: Float,
    cy: Float,
    height: Float,
    color: Color,
    rotation: Float,
    tail: Float
) {
    rotate(rotation, pivot = Offset(cx, cy)) {
        val halfW = height * 0.42f
        val path = Path().apply {
            moveTo(cx, cy - height * 0.5f)
            lineTo(cx + halfW, cy)
            lineTo(cx, cy + height * 0.5f)
            lineTo(cx - halfW, cy)
            close()
        }
        drawPath(path, color.copy(alpha = 0.22f))
        drawPath(path, color.copy(alpha = 0.75f), style = Stroke(width = 1.6f))
        drawLine(
            color = color.copy(alpha = 0.55f),
            start = Offset(cx, cy - height * 0.5f),
            end = Offset(cx, cy + height * 0.5f),
            strokeWidth = 1f
        )
        drawLine(
            color = color.copy(alpha = 0.55f),
            start = Offset(cx - halfW, cy),
            end = Offset(cx + halfW, cy),
            strokeWidth = 1f
        )
        // 飘带
        val tailPath = Path().apply {
            moveTo(cx, cy + height * 0.5f)
            cubicTo(
                cx + tail * height * 0.30f, cy + height * 0.72f,
                cx - tail * height * 0.26f, cy + height * 0.92f,
                cx + tail * height * 0.16f, cy + height * 1.14f
            )
        }
        drawPath(tailPath, color.copy(alpha = 0.45f), style = Stroke(width = 1.6f, cap = StrokeCap.Round))
    }
}

/** 花：双蝶绕花，翼拍随节拍、位置随进度右移。 */
@Composable
private fun ButterflyScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val petal = Color(0xFFE8A0B4)
    val leaf = Color(0xFF7FA679)
    val clock = sceneClock(playing, 900, "butterfly")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val travel = progress.coerceIn(0f, 1f) * w * 0.30f
        // 草丛与花
        repeat(6) { index ->
            val fx = w * (0.10f + index * 0.16f)
            drawCircle(leaf.copy(alpha = 0.30f), radius = h * 0.055f, center = Offset(fx, h * 0.92f))
            drawCircle(petal.copy(alpha = 0.42f), radius = h * 0.045f, center = Offset(fx, h * 0.86f))
        }
        val flap = abs(sin(clock * 2 * PI).toFloat())
        val beat = sin(clock * 2 * PI).toFloat()
        drawButterfly(
            w * 0.34f + travel,
            h * 0.36f + beat * h * 0.10f,
            h * 0.22f,
            petal,
            flap,
            -12f
        )
        drawButterfly(
            w * 0.52f + travel * 0.8f,
            h * 0.62f - beat * h * 0.08f,
            h * 0.17f,
            Color(0xFFF2C1C9),
            1f - flap,
            14f
        )
    }
}

private fun DrawScope.drawButterfly(
    cx: Float,
    cy: Float,
    size: Float,
    color: Color,
    flap: Float,
    rotation: Float
) {
    rotate(rotation, pivot = Offset(cx, cy)) {
        val wingW = size * (0.35f + 0.85f * flap)
        // 上翼
        drawOval(
            color = color.copy(alpha = 0.72f),
            topLeft = Offset(cx - wingW, cy - size * 0.85f),
            size = Size(wingW * 2f, size * 0.90f)
        )
        // 下翼
        drawOval(
            color = color.copy(alpha = 0.52f),
            topLeft = Offset(cx - wingW * 0.75f, cy - size * 0.08f),
            size = Size(wingW * 1.5f, size * 0.65f)
        )
        // 身体
        drawLine(
            color = Color(0xFF4A2A32).copy(alpha = 0.8f),
            start = Offset(cx, cy - size * 0.45f),
            end = Offset(cx, cy + size * 0.45f),
            strokeWidth = size * 0.12f,
            cap = StrokeCap.Round
        )
        // 触须
        drawLine(
            color = Color(0xFF4A2A32).copy(alpha = 0.6f),
            Offset(cx, cy - size * 0.45f), Offset(cx - size * 0.30f, cy - size * 0.85f),
            strokeWidth = 1f
        )
        drawLine(
            color = Color(0xFF4A2A32).copy(alpha = 0.6f),
            Offset(cx, cy - size * 0.45f), Offset(cx + size * 0.30f, cy - size * 0.85f),
            strokeWidth = 1f
        )
    }
}

/** 雪：两个剪影在雪原追逐，雪痕即进度轨迹。 */
@Composable
private fun SnowChaseScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val snow = Color(0xFFF4F9FF)
    val ice = MaterialTheme.colorScheme.primary
    val clock = sceneClock(playing, 700, "snowChase")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.86f
        drawRect(
            brush = Brush.verticalGradient(
                listOf(ice.copy(alpha = 0.10f), Color.Transparent),
                startY = 0f,
                endY = h
            )
        )
        drawLine(snow.copy(alpha = 0.18f), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 1.2f)

        val clamped = progress.coerceIn(0f, 1f)
        // 雪痕轨迹
        val steps = 16
        for (index in 0 until steps) {
            val t = index / steps.toFloat()
            if (t > clamped) break
            drawCircle(
                color = snow.copy(alpha = 0.28f * (0.4f + t)),
                radius = 1.8f + index % 2,
                center = Offset(w * 0.06f + t * w * 0.80f, groundY + (index % 3) * 2f - 1f)
            )
        }
        val stride = sin(clock * 2 * PI).toFloat()
        val leaderX = w * 0.16f + clamped * w * 0.56f
        val chaserX = leaderX - h * 0.30f
        drawRunner(leaderX, groundY, h * 0.56f, snow.copy(alpha = 0.85f), stride, 1f)
        drawRunner(chaserX, groundY, h * 0.50f, ice.copy(alpha = 0.85f), -stride, 1f)

        // 飘雪
        repeat(9) { index ->
            val p = ((clock * 1.4f + index * 0.11f) % 1f)
            val x = w * (0.05f + (index * 0.103f)) + sin((p * 3f + index) * PI).toFloat() * w * 0.02f
            drawCircle(
                color = snow.copy(alpha = 0.45f),
                radius = 1.6f + index % 2,
                center = Offset(x, p * groundY)
            )
        }
    }
}

private fun DrawScope.drawRunner(
    cx: Float,
    groundY: Float,
    height: Float,
    color: Color,
    stride: Float,
    facing: Float
) {
    val headR = height * 0.11f
    val headY = groundY - height * 0.90f
    val hipY = groundY - height * 0.46f
    drawCircle(color, radius = headR, center = Offset(cx + facing * height * 0.03f, headY))
    drawLine(color, Offset(cx, headY + headR * 0.6f), Offset(cx - facing * height * 0.04f, hipY), strokeWidth = height * 0.10f, cap = StrokeCap.Round)
    // 腿
    drawLine(
        color,
        Offset(cx - facing * height * 0.04f, hipY),
        Offset(cx - facing * height * 0.04f + stride * height * 0.22f, groundY),
        strokeWidth = height * 0.075f,
        cap = StrokeCap.Round
    )
    drawLine(
        color,
        Offset(cx - facing * height * 0.04f, hipY),
        Offset(cx - facing * height * 0.04f - stride * height * 0.18f, groundY),
        strokeWidth = height * 0.075f,
        cap = StrokeCap.Round
    )
    // 手臂
    drawLine(
        color,
        Offset(cx, headY + headR * 1.4f),
        Offset(cx + facing * height * 0.24f, headY + headR * 1.2f + stride * height * 0.10f),
        strokeWidth = height * 0.065f,
        cap = StrokeCap.Round
    )
}

/** 月：山脊剪影对剑，逆光巨月。 */
@Composable
private fun MoonSwordScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val moon = Color(0xFFEAE6DA)
    val ink = Color(0xFF070A14)
    val silver = Color(0xFFC9CEDA)
    val clock = sceneClock(playing, 1500, "moonSword")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.90f
        val moonX = w * (0.30f + 0.40f * progress.coerceIn(0f, 1f))
        val moonY = h * 0.42f
        val moonR = h * 0.34f
        drawCircle(moon.copy(alpha = 0.06f), radius = moonR * 1.7f, center = Offset(moonX, moonY))
        drawCircle(moon.copy(alpha = 0.10f), radius = moonR * 1.25f, center = Offset(moonX, moonY))
        drawCircle(
            brush = Brush.radialGradient(
                listOf(moon, moon.copy(alpha = 0.75f)),
                center = Offset(moonX - moonR * 0.2f, moonY - moonR * 0.2f),
                radius = moonR * 1.1f
            ),
            radius = moonR,
            center = Offset(moonX, moonY)
        )
        // 山脊
        val ridge = Path().apply {
            moveTo(0f, groundY)
            lineTo(w * 0.16f, groundY - h * 0.18f)
            lineTo(w * 0.30f, groundY - h * 0.08f)
            lineTo(w * 0.46f, groundY - h * 0.20f)
            lineTo(w * 0.62f, groundY - h * 0.10f)
            lineTo(w * 0.80f, groundY - h * 0.22f)
            lineTo(w, groundY - h * 0.06f)
            lineTo(w, h)
            lineTo(0f, h)
            close()
        }
        drawPath(ridge, ink.copy(alpha = 0.92f))

        val swing = sin(clock * 2 * PI).toFloat()
        val heroX = w * 0.30f
        val rivalX = w * 0.66f
        drawSwordsman(heroX, groundY - h * 0.10f, h * 0.50f, ink, silver, 1f, swing)
        drawSwordsman(rivalX, groundY - h * 0.14f, h * 0.46f, ink, silver, -1f, -swing)

        // 剑光
        if (playing && abs(swing) > 0.55f) {
            val strength = (abs(swing) - 0.55f) / 0.45f
            drawCircle(
                color = silver.copy(alpha = 0.30f * strength),
                radius = h * 0.06f,
                center = Offset(w * 0.48f, groundY - h * 0.42f)
            )
        }
    }
}

private fun DrawScope.drawSwordsman(
    cx: Float,
    baseY: Float,
    height: Float,
    ink: Color,
    silver: Color,
    facing: Float,
    swing: Float
) {
    val headR = height * 0.10f
    val headY = baseY - height * 0.88f
    val hipY = baseY - height * 0.44f
    drawCircle(ink, radius = headR, center = Offset(cx, headY))
    // 斗篷
    val cape = Path().apply {
        moveTo(cx - height * 0.06f, headY + headR)
        lineTo(cx + height * 0.10f, headY + headR)
        lineTo(cx + facing * height * 0.18f, baseY)
        lineTo(cx - facing * height * 0.12f, baseY)
        close()
    }
    drawPath(cape, ink)
    drawLine(ink, Offset(cx, headY + headR * 0.8f), Offset(cx, hipY), strokeWidth = height * 0.08f, cap = StrokeCap.Round)
    // 剑
    val hand = Offset(cx + facing * height * 0.14f, headY + headR * 2.0f + swing * height * 0.06f)
    drawLine(silver.copy(alpha = 0.75f), hand, Offset(hand.x + facing * height * 0.42f, hand.y - height * 0.30f), strokeWidth = 2f, cap = StrokeCap.Round)
    drawLine(ink, Offset(cx, headY + headR * 1.6f), hand, strokeWidth = height * 0.06f, cap = StrokeCap.Round)
}

/** 深色：声波共鸣——同心弧自中心扩散，右侧均衡器随节拍起伏。 */
@Composable
private fun SoundWaveScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val glow = MaterialTheme.colorScheme.primary
    val clock = sceneClock(playing, 2400, "soundWave")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val cx = w * (0.24f + 0.10f * progress.coerceIn(0f, 1f))
        val cy = h * 0.50f

        // 同心弧：由内向外扩散并淡出
        repeat(4) { ring ->
            val p = (clock + ring / 4f) % 1f
            val r = h * (0.10f + p * 0.36f)
            drawArc(
                color = glow.copy(alpha = 0.34f * (1f - p)),
                startAngle = -58f,
                sweepAngle = 116f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = 1.4f)
            )
        }
        drawCircle(glow.copy(alpha = 0.55f), radius = 2.4f, center = Offset(cx, cy))

        // 均衡器：竖条高度取自正弦，逐条错相
        val bars = 14
        val gap = w * 0.40f / bars
        repeat(bars) { index ->
            val amp = abs(sin(clock * 2 * PI + index * 0.62f)).toFloat()
            val barH = h * (0.10f + 0.34f * amp)
            drawLine(
                color = glow.copy(alpha = 0.28f + 0.42f * amp),
                start = Offset(w * 0.56f + index * gap, cy - barH / 2f),
                end = Offset(w * 0.56f + index * gap, cy + barH / 2f),
                strokeWidth = w * 0.009f,
                cap = StrokeCap.Round
            )
        }

        // 浮尘
        repeat(6) { index ->
            val p = (clock * 0.8f + index * 0.17f) % 1f
            val x = w * (0.06f + index * 0.15f) + sin((p + index) * PI).toFloat() * w * 0.02f
            drawCircle(
                color = glow.copy(alpha = 0.20f),
                radius = 1.2f + index % 2,
                center = Offset(x, cy - p * h * 0.30f + h * 0.02f)
            )
        }
    }
}

/** 浅色：纸飞机沿上升弧滑翔，身后拖出虚线航迹。 */
@Composable
private fun PaperPlaneScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val line = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val clock = sceneClock(playing, 3600, "paperPlane")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        // 点阵天空
        repeat(18) { index ->
            val col = index % 6
            val row = index / 6
            drawCircle(
                color = line.copy(alpha = 0.16f),
                radius = 1.1f,
                center = Offset(w * (0.08f + col * 0.16f), h * (0.16f + row * 0.26f))
            )
        }

        val clamped = progress.coerceIn(0f, 1f)
        val lane = { t: Float ->
            val u = t.coerceIn(0f, 1f)
            Offset(w * (0.08f + u * 0.62f), h * (0.76f - u * 0.42f))
        }
        val bob = sin(clock * 2 * PI).toFloat() * h * 0.04f

        // 虚线航迹：沿同一弧线回望
        repeat(14) { index ->
            val t = clamped - index * 0.022f
            if (t >= 0f) {
                val p = lane(t)
                drawCircle(
                    color = accent.copy(alpha = 0.34f * (1f - index / 14f)),
                    radius = 1.3f,
                    center = Offset(p.x, p.y + bob)
                )
            }
        }
        val head = lane(clamped)
        drawPaperPlane(head.x, head.y + bob, h * 0.30f, line, accent)
    }
}

private fun DrawScope.drawPaperPlane(cx: Float, cy: Float, size: Float, line: Color, accent: Color) {
    val nose = Offset(cx + size * 0.55f, cy - size * 0.06f)
    val tail = Offset(cx - size * 0.48f, cy + size * 0.10f)
    val wing = Offset(cx - size * 0.06f, cy - size * 0.40f)
    val keel = Offset(cx - size * 0.10f, cy + size * 0.34f)
    val body = Path().apply {
        moveTo(nose.x, nose.y)
        lineTo(tail.x, tail.y)
        lineTo(wing.x, wing.y)
        close()
    }
    drawPath(body, accent.copy(alpha = 0.20f))
    drawPath(body, accent.copy(alpha = 0.72f), style = Stroke(width = 1.4f))
    // 折线：机背与机身
    drawLine(accent.copy(alpha = 0.60f), nose, keel, strokeWidth = 1.2f)
    drawLine(line.copy(alpha = 0.24f), tail, keel, strokeWidth = 1f)
}

/** 春：双燕追逐，一只俯身掠过水面，点开涟漪。 */
@Composable
private fun SwallowsScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val leader = MaterialTheme.colorScheme.primary
    val chaser = MaterialTheme.colorScheme.secondary
    val water = MaterialTheme.colorScheme.onSurfaceVariant
    val clock = sceneClock(playing, 1500, "swallows")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val waterY = h * 0.78f
        val travel = progress.coerceIn(0f, 1f) * w * 0.26f
        drawLine(water.copy(alpha = 0.30f), Offset(0f, waterY), Offset(w, waterY), strokeWidth = 1.2f)

        val dip = 0.5f + 0.5f * sin(clock * 2 * PI).toFloat()
        val leaderX = w * 0.30f + travel
        val leaderY = h * (0.28f + dip * 0.44f)
        val chaserX = w * 0.12f + travel * 0.75f
        val chaserY = h * (0.34f + (1f - dip) * 0.20f)

        // 领头燕触水时的涟漪
        val ripple = ((leaderY - h * 0.56f) / (h * 0.20f)).coerceIn(0f, 1f)
        if (ripple > 0f) {
            repeat(2) { index ->
                drawOval(
                    color = water.copy(alpha = 0.36f * ripple),
                    topLeft = Offset(leaderX - w * (0.032f + index * 0.020f), waterY - h * 0.022f),
                    size = Size(w * (0.064f + index * 0.040f), h * 0.044f),
                    style = Stroke(width = 1f)
                )
            }
        }
        drawSwallow(chaserX, chaserY, h * 0.24f, chaser, 1f - dip, 10f)
        drawSwallow(leaderX, leaderY, h * 0.30f, leader, dip, -6f)
    }
}

private fun DrawScope.drawSwallow(
    cx: Float,
    cy: Float,
    size: Float,
    color: Color,
    flap: Float,
    rotation: Float
) {
    rotate(rotation, pivot = Offset(cx, cy)) {
        // 身体
        val body = Path().apply {
            moveTo(cx + size * 0.50f, cy - size * 0.02f)
            cubicTo(
                cx + size * 0.22f, cy - size * 0.20f,
                cx - size * 0.18f, cy - size * 0.14f,
                cx - size * 0.44f, cy
            )
            cubicTo(
                cx - size * 0.16f, cy + size * 0.14f,
                cx + size * 0.20f, cy + size * 0.18f,
                cx + size * 0.50f, cy - size * 0.02f
            )
            close()
        }
        drawPath(body, color.copy(alpha = 0.85f))
        // 分叉尾
        val tail = Path().apply {
            moveTo(cx - size * 0.36f, cy)
            lineTo(cx - size * 0.78f, cy - size * 0.26f)
            lineTo(cx - size * 0.56f, cy)
            lineTo(cx - size * 0.76f, cy + size * 0.24f)
            close()
        }
        drawPath(tail, color.copy(alpha = 0.85f))
        // 双翼：随节拍扇动
        val lift = size * (0.10f + 0.42f * flap)
        val wing = Path().apply {
            moveTo(cx + size * 0.10f, cy - size * 0.06f)
            cubicTo(
                cx - size * 0.02f, cy - lift - size * 0.14f,
                cx - size * 0.26f, cy - lift - size * 0.02f,
                cx - size * 0.40f, cy - lift * 0.55f
            )
            cubicTo(
                cx - size * 0.16f, cy + size * 0.02f,
                cx, cy + size * 0.10f,
                cx + size * 0.10f, cy - size * 0.06f
            )
            close()
        }
        drawPath(wing, color.copy(alpha = 0.50f))
        drawCircle(Color(0xFF0E1418), radius = size * 0.035f, center = Offset(cx + size * 0.42f, cy - size * 0.06f))
    }
}

/** 夏：小帆船驶过两道层叠的浪，船身随波轻斜。 */
@Composable
private fun SailboatScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val sea = MaterialTheme.colorScheme.primary
    val sail = MaterialTheme.colorScheme.tertiary
    val hull = Color(0xFF2C3B44)
    val clock = sceneClock(playing, 3000, "sailboat")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val boatX = w * (0.30f + progress.coerceIn(0f, 1f) * 0.36f)
        val waveY = h * 0.74f

        // 远浪
        drawWave(h * 0.60f, w, h, sea.copy(alpha = 0.16f), clock * 0.4f)
        // 船随波倾斜
        rotate(sin(clock * 2 * PI).toFloat() * 5f, pivot = Offset(boatX, waveY)) {
            drawSailboat(boatX, waveY, h * 0.52f, hull, sail)
        }
        // 近浪压住船底
        drawWave(waveY, w, h, sea.copy(alpha = 0.30f), clock)
        // 船首浪花
        repeat(4) { index ->
            val x = boatX + h * (0.18f + index * 0.045f)
            drawLine(
                color = sea.copy(alpha = 0.40f - index * 0.06f),
                start = Offset(x, waveY - h * 0.02f - index * 0.014f * h),
                end = Offset(x + h * (0.04f + index * 0.015f), waveY - h * (0.10f + index * 0.02f)),
                strokeWidth = 1f,
                cap = StrokeCap.Round
            )
        }
    }
}

private fun DrawScope.drawWave(baseY: Float, width: Float, height: Float, color: Color, phase: Float) {
    val wave = Path().apply {
        moveTo(0f, baseY)
        val steps = 24
        for (index in 1..steps) {
            val x = width * index / steps.toFloat()
            val y = baseY - sin((index / steps.toFloat() + phase) * 2 * PI).toFloat() * height * 0.035f
            lineTo(x, y)
        }
        lineTo(width, baseY + height * 0.20f)
        lineTo(0f, baseY + height * 0.20f)
        close()
    }
    drawPath(wave, color)
}

private fun DrawScope.drawSailboat(cx: Float, waterY: Float, height: Float, hull: Color, sail: Color) {
    val deckY = waterY - height * 0.06f
    // 船体
    val body = Path().apply {
        moveTo(cx - height * 0.26f, deckY)
        lineTo(cx + height * 0.26f, deckY)
        lineTo(cx + height * 0.15f, waterY + height * 0.04f)
        lineTo(cx - height * 0.15f, waterY + height * 0.04f)
        close()
    }
    drawPath(body, hull.copy(alpha = 0.88f))
    // 桅杆
    drawLine(hull, Offset(cx, deckY), Offset(cx, waterY - height * 0.62f), strokeWidth = 1.6f, cap = StrokeCap.Round)
    // 主帆
    val main = Path().apply {
        moveTo(cx + height * 0.02f, waterY - height * 0.58f)
        lineTo(cx + height * 0.26f, deckY - height * 0.04f)
        lineTo(cx + height * 0.02f, deckY - height * 0.04f)
        close()
    }
    drawPath(main, sail.copy(alpha = 0.38f))
    drawPath(main, sail.copy(alpha = 0.78f), style = Stroke(width = 1.3f))
    // 前帆
    val jib = Path().apply {
        moveTo(cx - height * 0.02f, waterY - height * 0.60f)
        lineTo(cx - height * 0.22f, deckY - height * 0.04f)
        lineTo(cx - height * 0.02f, deckY - height * 0.04f)
        close()
    }
    drawPath(jib, sail.copy(alpha = 0.18f))
    drawPath(jib, sail.copy(alpha = 0.45f), style = Stroke(width = 1f))
}

/** 秋：雁阵排成人字南飞，整阵随进度横移，翼拍交替。 */
@Composable
private fun GeeseScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val lead = MaterialTheme.colorScheme.primary
    val wing = MaterialTheme.colorScheme.secondary
    val clock = sceneClock(playing, 1300, "geese")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val clamped = progress.coerceIn(0f, 1f)
        val leadX = w * 0.28f + clamped * w * 0.41f
        val leadY = h * 0.36f
        val flap = abs(sin(clock * 2 * PI).toFloat())

        drawGoose(leadX, leadY, h * 0.22f, lead, flap)
        repeat(3) { index ->
            val k = index + 1
            val dx = leadX - k * w * 0.068f
            drawGoose(dx, leadY - k * h * 0.09f, h * (0.19f - k * 0.012f), wing, flap)
            drawGoose(dx, leadY + k * h * 0.085f, h * (0.20f - k * 0.012f), lead, 1f - flap)
        }
    }
}

private fun DrawScope.drawGoose(cx: Float, cy: Float, size: Float, color: Color, flap: Float) {
    // 身体
    drawOval(
        color = color.copy(alpha = 0.82f),
        topLeft = Offset(cx - size * 0.44f, cy - size * 0.14f),
        size = Size(size * 0.78f, size * 0.30f)
    )
    // 长颈与头
    val neck = Path().apply {
        moveTo(cx + size * 0.24f, cy - size * 0.04f)
        cubicTo(
            cx + size * 0.44f, cy - size * 0.10f,
            cx + size * 0.40f, cy - size * 0.30f,
            cx + size * 0.54f, cy - size * 0.36f
        )
    }
    drawPath(neck, color.copy(alpha = 0.82f), style = Stroke(width = size * 0.10f, cap = StrokeCap.Round))
    drawCircle(color.copy(alpha = 0.82f), radius = size * 0.075f, center = Offset(cx + size * 0.54f, cy - size * 0.36f))
    drawLine(
        color = color.copy(alpha = 0.65f),
        start = Offset(cx + size * 0.60f, cy - size * 0.36f),
        end = Offset(cx + size * 0.72f, cy - size * 0.34f),
        strokeWidth = 1.4f
    )
    // 双翼：随节拍抬落
    val lift = cy - size * (0.04f + 0.34f * flap)
    val wing = Path().apply {
        moveTo(cx + size * 0.08f, cy - size * 0.04f)
        lineTo(cx - size * 0.46f, lift)
        lineTo(cx - size * 0.14f, cy + size * 0.10f)
        close()
    }
    drawPath(wing, color.copy(alpha = 0.62f))
}

/** 冬：单人滑冰，沿两个交叠的圆滑出 8 字，冰屑随刃飞出。 */
@Composable
private fun SkaterScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val body = MaterialTheme.colorScheme.primary
    val blade = MaterialTheme.colorScheme.tertiary
    val ice = MaterialTheme.colorScheme.onSurfaceVariant
    val clock = sceneClock(playing, 2600, "skater")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val iceY = h * 0.88f
        drawLine(ice.copy(alpha = 0.28f), Offset(0f, iceY), Offset(w, iceY), strokeWidth = 1.2f)

        val cx = w * (0.32f + progress.coerceIn(0f, 1f) * 0.34f)
        val cy = h * 0.82f
        val a = w * 0.095f
        val b = h * 0.050f

        // 8 字冰痕
        val trace = Path()
        val steps = 40
        for (index in 0..steps) {
            val t = index / steps.toFloat() * 2 * PI
            val x = cx + a * cos(t).toFloat()
            val y = cy + b * sin(t).toFloat() * cos(t).toFloat()
            if (index == 0) trace.moveTo(x, y) else trace.lineTo(x, y)
        }
        drawPath(trace, ice.copy(alpha = 0.30f), style = Stroke(width = 1f))

        val t = clock * 2 * PI
        val footX = cx + a * cos(t).toFloat()
        val footY = cy + b * sin(t).toFloat() * cos(t).toFloat()

        // 冰屑：沿滑行反方向甩出
        val backT = t - 0.45
        val chipX = cx + a * cos(backT).toFloat()
        val chipY = cy + b * sin(backT).toFloat() * cos(backT).toFloat()
        repeat(4) { index ->
            drawLine(
                color = blade.copy(alpha = 0.40f - index * 0.06f),
                start = Offset(chipX - h * 0.02f, chipY),
                end = Offset(chipX - h * (0.06f + index * 0.02f), chipY - h * (0.05f + index * 0.03f)),
                strokeWidth = 1f,
                cap = StrokeCap.Round
            )
        }
        drawSkater(footX, footY, h * 0.55f, body, blade, sin(t).toFloat() * 6f)
    }
}

private fun DrawScope.drawSkater(
    cx: Float,
    footY: Float,
    height: Float,
    body: Color,
    blade: Color,
    lean: Float
) {
    rotate(lean, pivot = Offset(cx, footY)) {
        val headR = height * 0.10f
        val headY = footY - height * 0.86f
        val hipY = footY - height * 0.44f
        drawCircle(body, radius = headR, center = Offset(cx + height * 0.04f, headY))
        // 躯干
        drawLine(
            color = body,
            start = Offset(cx + height * 0.03f, headY + headR),
            end = Offset(cx - height * 0.02f, hipY),
            strokeWidth = height * 0.10f,
            cap = StrokeCap.Round
        )
        // 支撑腿与后展腿
        drawLine(
            body,
            Offset(cx - height * 0.02f, hipY),
            Offset(cx + height * 0.06f, footY - height * 0.05f),
            strokeWidth = height * 0.065f,
            cap = StrokeCap.Round
        )
        drawLine(
            body,
            Offset(cx - height * 0.02f, hipY),
            Offset(cx - height * 0.24f, footY - height * 0.16f),
            strokeWidth = height * 0.060f,
            cap = StrokeCap.Round
        )
        // 冰刀
        drawLine(
            blade.copy(alpha = 0.85f),
            Offset(cx - height * 0.04f, footY - height * 0.04f),
            Offset(cx + height * 0.22f, footY),
            strokeWidth = 1.4f,
            cap = StrokeCap.Round
        )
        // 双臂：一前一后
        drawLine(
            body,
            Offset(cx + height * 0.02f, headY + headR * 1.5f),
            Offset(cx + height * 0.30f, headY + headR * 1.1f),
            strokeWidth = height * 0.055f,
            cap = StrokeCap.Round
        )
        drawLine(
            body,
            Offset(cx + height * 0.02f, headY + headR * 1.9f),
            Offset(cx - height * 0.28f, headY + headR * 2.4f),
            strokeWidth = height * 0.050f,
            cap = StrokeCap.Round
        )
    }
}

/** 太极：一黑一白两人推手，绕同一圆心缓转，中间悬一枚太极。 */
@Composable
private fun PushHandsScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.primary
    val paper = MaterialTheme.colorScheme.surface
    val accent = MaterialTheme.colorScheme.tertiary
    val clock = sceneClock(playing, 4200, "pushHands")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.88f
        val centerX = w * (0.38f + progress.coerceIn(0f, 1f) * 0.18f)
        val orbit = cos(clock * 2 * PI).toFloat()
        val lean = sin(clock * 2 * PI).toFloat()

        // 太极图：缓缓旋转
        drawTaiji(centerX, groundY - h * 0.62f, h * 0.10f, ink, paper, clock * 360f)

        // 两人绕中心推手：一进一退，进退互逆
        val gap = h * 0.38f
        val darkX = centerX + orbit * h * 0.12f - gap
        val lightX = centerX - orbit * h * 0.12f + gap
        drawTaijiFigure(darkX, groundY, h * 0.64f, ink, Color.Transparent, 1f, lean * 3f)
        drawTaijiFigure(lightX, groundY, h * 0.64f, paper, accent, -1f, -lean * 3f)

        // 地面
        drawLine(ink.copy(alpha = 0.14f), Offset(0f, groundY), Offset(w, groundY), strokeWidth = 1f)
    }
}

private fun DrawScope.drawTaijiFigure(
    cx: Float,
    groundY: Float,
    height: Float,
    fill: Color,
    outline: Color,
    facing: Float,
    lean: Float
) {
    rotate(lean, pivot = Offset(cx, groundY)) {
        val headR = height * 0.10f
        val headY = groundY - height * 0.86f
        val hipY = groundY - height * 0.42f
        // 头
        drawCircle(fill, radius = headR, center = Offset(cx, headY))
        if (outline.alpha > 0f) {
            drawCircle(
                color = outline.copy(alpha = 0.85f),
                radius = headR,
                center = Offset(cx, headY),
                style = Stroke(width = 1.2f)
            )
        }
        // 躯干
        val torso = Path().apply {
            moveTo(cx - height * 0.05f, headY + headR * 0.8f)
            lineTo(cx + height * 0.05f, headY + headR * 0.8f)
            lineTo(cx + height * 0.06f, hipY)
            lineTo(cx - height * 0.06f, hipY)
            close()
        }
        drawPath(torso, fill)
        if (outline.alpha > 0f) {
            drawPath(torso, outline.copy(alpha = 0.65f), style = Stroke(width = 1.1f))
        }
        // 弓步双腿
        drawLine(fill, Offset(cx, hipY), Offset(cx - height * 0.16f, groundY), strokeWidth = height * 0.05f, cap = StrokeCap.Round)
        drawLine(fill, Offset(cx, hipY), Offset(cx + height * 0.16f, groundY), strokeWidth = height * 0.05f, cap = StrokeCap.Round)
        // 推手双臂：前后手交替
        val hand = Offset(cx + facing * height * 0.26f, headY + headR * 2.0f)
        drawLine(fill, Offset(cx, headY + headR * 1.5f), hand, strokeWidth = height * 0.045f, cap = StrokeCap.Round)
        drawLine(
            fill,
            Offset(cx, headY + headR * 1.9f),
            Offset(hand.x - facing * height * 0.05f, hand.y + height * 0.09f),
            strokeWidth = height * 0.042f,
            cap = StrokeCap.Round
        )
        drawCircle(fill, radius = height * 0.035f, center = hand)
    }
}

private fun DrawScope.drawTaiji(cx: Float, cy: Float, r: Float, ink: Color, paper: Color, spin: Float) {
    rotate(spin, pivot = Offset(cx, cy)) {
        val topLeft = Offset(cx - r, cy - r)
        val diameter = Size(r * 2f, r * 2f)
        drawCircle(paper.copy(alpha = 0.65f), radius = r, center = Offset(cx, cy))
        // 阴鱼：右半圆 + 上方的半圆，合成 S 形分界
        drawArc(ink.copy(alpha = 0.55f), -90f, 180f, true, topLeft = topLeft, size = diameter)
        drawCircle(ink.copy(alpha = 0.55f), radius = r * 0.5f, center = Offset(cx, cy - r * 0.5f))
        drawCircle(paper.copy(alpha = 0.85f), radius = r * 0.5f, center = Offset(cx, cy + r * 0.5f))
        // 鱼眼
        drawCircle(paper.copy(alpha = 0.95f), radius = r * 0.16f, center = Offset(cx, cy - r * 0.5f))
        drawCircle(ink.copy(alpha = 0.70f), radius = r * 0.16f, center = Offset(cx, cy + r * 0.5f))
        drawCircle(ink.copy(alpha = 0.28f), radius = r, center = Offset(cx, cy), style = Stroke(width = 1f))
    }
}

/** 熊猫：竹影下抱竹静坐，竹叶打着旋儿落下。 */
@Composable
private fun PandaBambooScene(progress: Float, playing: Boolean, modifier: Modifier) {
    val ink = MaterialTheme.colorScheme.onBackground
    val paper = MaterialTheme.colorScheme.background
    val bamboo = MaterialTheme.colorScheme.tertiary
    val clock = sceneClock(playing, 4600, "pandaBamboo")
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.86f
        val px = w * (0.28f + progress.coerceIn(0f, 1f) * 0.22f)

        // 背景竹竿与节
        val stalkX = w * 0.78f
        drawLine(
            color = bamboo.copy(alpha = 0.30f),
            start = Offset(stalkX, groundY),
            end = Offset(stalkX + h * 0.06f, h * 0.08f),
            strokeWidth = 2.2f
        )
        repeat(4) { index ->
            val t = 0.16f + index * 0.18f
            val nodeX = stalkX + h * 0.06f * t
            val nodeY = groundY - (groundY - h * 0.08f) * t
            drawLine(bamboo.copy(alpha = 0.24f), Offset(nodeX - 2f, nodeY), Offset(nodeX + 2f, nodeY), strokeWidth = 1.2f)
        }
        drawLeaf(stalkX + h * 0.06f, h * 0.12f, h * 0.16f, bamboo.copy(alpha = 0.42f), -24f)
        drawLeaf(stalkX + h * 0.02f, h * 0.18f, h * 0.14f, bamboo.copy(alpha = 0.32f), 20f)

        // 地影
        drawOval(
            color = ink.copy(alpha = 0.10f),
            topLeft = Offset(px - h * 0.26f, groundY - h * 0.03f),
            size = Size(h * 0.52f, h * 0.07f)
        )
        drawPanda(px, groundY, h * 0.62f, ink, paper, bamboo)

        // 飘落的竹叶
        repeat(5) { index ->
            val p = (clock * 0.6f + index * 0.19f) % 1f
            val lx = w * (0.56f + index * 0.075f) + sin((p * 2f + index) * PI).toFloat() * w * 0.018f
            val ly = h * 0.14f + p * h * 0.62f
            drawLeaf(lx, ly, h * 0.11f, bamboo.copy(alpha = 0.38f + 0.25f * (1f - p)), p * 300f + index * 36f)
        }
    }
}

private fun DrawScope.drawPanda(
    cx: Float,
    groundY: Float,
    height: Float,
    ink: Color,
    paper: Color,
    bamboo: Color
) {
    val bodyR = height * 0.30f
    val bodyCy = groundY - bodyR
    // 身体
    drawOval(
        color = ink,
        topLeft = Offset(cx - bodyR, bodyCy - bodyR * 1.05f),
        size = Size(bodyR * 2f, bodyR * 2.1f)
    )
    // 双脚
    drawOval(ink, topLeft = Offset(cx - bodyR * 0.95f, groundY - bodyR * 0.36f), size = Size(bodyR * 0.82f, bodyR * 0.38f))
    drawOval(ink, topLeft = Offset(cx + bodyR * 0.13f, groundY - bodyR * 0.36f), size = Size(bodyR * 0.82f, bodyR * 0.38f))
    // 头
    val headR = height * 0.19f
    val headCy = groundY - height * 0.66f
    drawCircle(ink, radius = headR, center = Offset(cx, headCy))
    // 耳
    drawCircle(ink, radius = headR * 0.38f, center = Offset(cx - headR * 0.74f, headCy - headR * 0.70f))
    drawCircle(ink, radius = headR * 0.38f, center = Offset(cx + headR * 0.74f, headCy - headR * 0.70f))
    // 眼斑与鼻
    drawOval(paper, topLeft = Offset(cx - headR * 0.66f, headCy - headR * 0.34f), size = Size(headR * 0.44f, headR * 0.54f))
    drawOval(paper, topLeft = Offset(cx + headR * 0.22f, headCy - headR * 0.34f), size = Size(headR * 0.44f, headR * 0.54f))
    drawOval(paper, topLeft = Offset(cx - headR * 0.16f, headCy + headR * 0.26f), size = Size(headR * 0.32f, headR * 0.22f))
    // 抱竹
    val stalkX = cx + bodyR * 0.9f
    val stalkTop = headCy - headR * 0.9f
    drawLine(bamboo.copy(alpha = 0.85f), Offset(stalkX, groundY), Offset(stalkX + height * 0.04f, stalkTop), strokeWidth = 2.4f)
    repeat(2) { index ->
        val t = 0.28f + index * 0.28f
        val ny = groundY - (groundY - stalkTop) * t
        drawLine(bamboo.copy(alpha = 0.55f), Offset(stalkX - 2f, ny), Offset(stalkX + 3f, ny), strokeWidth = 1f)
    }
    drawLeaf(stalkX + height * 0.04f, stalkTop, height * 0.22f, bamboo.copy(alpha = 0.60f), -30f)
    drawLeaf(stalkX + height * 0.02f, stalkTop + height * 0.04f, height * 0.18f, bamboo.copy(alpha = 0.45f), 25f)
    // 搭在竹上的手臂
    drawLine(
        ink,
        Offset(cx + bodyR * 0.6f, bodyCy - bodyR * 0.2f),
        Offset(stalkX, bodyCy + bodyR * 0.2f),
        strokeWidth = height * 0.06f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawLeaf(cx: Float, cy: Float, size: Float, color: Color, rotation: Float) {
    rotate(rotation, pivot = Offset(cx, cy)) {
        val leaf = Path().apply {
            moveTo(cx - size * 0.5f, cy)
            cubicTo(
                cx - size * 0.18f, cy - size * 0.32f,
                cx + size * 0.18f, cy - size * 0.32f,
                cx + size * 0.5f, cy
            )
            cubicTo(
                cx + size * 0.18f, cy + size * 0.32f,
                cx - size * 0.18f, cy + size * 0.32f,
                cx - size * 0.5f, cy
            )
            close()
        }
        drawPath(leaf, color)
    }
}
