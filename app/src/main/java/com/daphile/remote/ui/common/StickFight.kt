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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * 两个火柴人比武的过场动画，占据原来频谱的位置。
 * 站位随播放进度从左侧推进到右侧，招式循环切换（直拳、侧踹、回旋踢、上勾拳），
 * 配色与背景取自当前主题，因此切换主题后场景与人物颜色同步变化。
 */
@Composable
fun StickFightScene(
    progress: Float,
    playing: Boolean,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "stickFight")
    val clock by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stickFightClock"
    )

    val scheme = MaterialTheme.colorScheme
    val heroColor = scheme.primary
    val rivalColor = scheme.secondary
    val sceneColor = scheme.onSurfaceVariant
    val accentColor = scheme.tertiary

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val groundY = h * 0.84f

        // 场景：远山、落日与地面，颜色随主题变化
        drawCircle(
            color = accentColor.copy(alpha = 0.16f),
            radius = h * 0.26f,
            center = Offset(w * 0.80f, h * 0.26f)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.10f),
            radius = h * 0.38f,
            center = Offset(w * 0.80f, h * 0.26f)
        )
        drawMountain(w, h, groundY, sceneColor.copy(alpha = 0.10f), 0.02f, 0.30f, 0.46f)
        drawMountain(w, h, groundY, sceneColor.copy(alpha = 0.07f), 0.22f, 0.52f, 0.38f)
        drawLine(
            color = sceneColor.copy(alpha = 0.20f),
            start = Offset(0f, groundY),
            end = Offset(w, groundY),
            strokeWidth = 1.2f
        )

        // 随播放进度从左向右推进
        val clamped = progress.coerceIn(0f, 1f)
        val travel = clamped * (w * 0.46f)
        val fighterHeight = h * 0.62f
        val gap = fighterHeight * 0.52f
        val heroX = w * 0.22f + travel
        val rivalX = heroX + gap

        // 招式时钟：播放时推进，暂停时定格
        val beat = if (playing) clock else 0.35f
        val strike = (beat * 4f) % 1f
        val attackPower = if (playing) strikeCurve(strike) else 0.15f
        val heroMove = ((beat * 4f).toInt()) % 4
        val rivalMove = ((beat * 4f + 2).toInt()) % 4

        drawFighter(
            cx = heroX,
            groundY = groundY,
            height = fighterHeight,
            color = heroColor,
            facing = 1f,
            attack = attackPower,
            phase = beat,
            move = heroMove
        )
        drawFighter(
            cx = rivalX,
            groundY = groundY,
            height = fighterHeight,
            color = rivalColor,
            facing = -1f,
            attack = if (playing) strikeCurve((strike + 0.5f) % 1f) else 0.1f,
            phase = beat + 0.4f,
            move = rivalMove
        )

        // 交锋处的火花
        if (playing && attackPower > 0.55f) {
            val mid = Offset((heroX + rivalX) / 2f, groundY - fighterHeight * 0.62f)
            val strength = (attackPower - 0.55f) / 0.45f
            drawCircle(
                color = accentColor.copy(alpha = 0.35f * strength),
                radius = fighterHeight * 0.18f * (0.6f + strength),
                center = mid
            )
            repeat(5) { index ->
                val angle = (-0.9f + index * 0.45f) * PI.toFloat()
                val length = fighterHeight * 0.18f * (0.5f + strength)
                drawLine(
                    color = accentColor.copy(alpha = 0.55f * strength),
                    start = mid,
                    end = Offset(
                        mid.x + cos(angle) * length,
                        mid.y + sin(angle) * length
                    ),
                    strokeWidth = 1.4f,
                    cap = StrokeCap.Round
                )
            }
        }

        // 脚下扬尘
        if (playing) {
            val dustAlpha = 0.10f + 0.08f * sin((beat * 2f * PI).toFloat())
            drawOval(
                color = sceneColor.copy(alpha = dustAlpha.coerceAtLeast(0.04f)),
                topLeft = Offset(heroX - fighterHeight * 0.30f, groundY - 2f),
                size = Size(fighterHeight * 0.60f, fighterHeight * 0.10f)
            )
            drawOval(
                color = sceneColor.copy(alpha = dustAlpha.coerceAtLeast(0.04f)),
                topLeft = Offset(rivalX - fighterHeight * 0.30f, groundY - 2f),
                size = Size(fighterHeight * 0.60f, fighterHeight * 0.10f)
            )
        }
    }
}

/** 出招曲线：先蓄力再快速出击，回落平缓。 */
private fun strikeCurve(value: Float): Float {
    val v = value.coerceIn(0f, 1f)
    return when {
        v < 0.28f -> 0.12f + v * 0.5f
        v < 0.52f -> 0.26f + (v - 0.28f) * 2.9f
        else -> (1f - (v - 0.52f) / 0.48f).coerceIn(0.1f, 1f)
    }
}

private fun DrawScope.drawMountain(
    width: Float,
    height: Float,
    groundY: Float,
    color: Color,
    left: Float,
    peak: Float,
    top: Float
) {
    val path = Path().apply {
        moveTo(width * left, groundY)
        lineTo(width * (left + peak / 2f), height * top)
        lineTo(width * (left + peak), groundY)
        close()
    }
    drawPath(path, color)
}

/**
 * 绘制一个火柴人。[facing] 为 1 表示面向右侧，-1 面向左侧。
 * [move] 轮换四种招式：直拳、高踢、扫堂腿、腾空回旋踢；
 * [attack] 越大，出拳/踢腿越伸展。
 */
private fun DrawScope.drawFighter(
    cx: Float,
    groundY: Float,
    height: Float,
    color: Color,
    facing: Float,
    attack: Float,
    phase: Float,
    move: Int
) {
    val stroke = height * 0.075f
    val lift = if (move == 3) height * (0.10f + 0.12f * attack) else 0f
    val crouch = if (move == 2) height * 0.12f else 0f
    val hipY = groundY - height * 0.44f - lift + crouch
    val shoulderY = groundY - height * 0.80f - lift + crouch * 0.75f
    val headR = height * 0.11f
    val headY = groundY - height * 0.93f - lift + crouch * 0.75f

    // 重心随呼吸起伏
    val bob = sin((phase * 2f * PI).toFloat()) * height * 0.015f
    val hip = Offset(cx, hipY + bob)
    val shoulder = Offset(cx + facing * height * 0.03f, shoulderY + bob)

    // 头
    drawCircle(
        color = color,
        radius = headR,
        center = Offset(cx + facing * height * 0.04f, headY + bob)
    )
    drawLine(
        color = color,
        start = Offset(cx + facing * height * 0.03f, headY + headR + bob),
        end = shoulder,
        strokeWidth = stroke,
        cap = StrokeCap.Round
    )
    // 躯干
    drawLine(color = color, start = shoulder, end = hip, strokeWidth = stroke, cap = StrokeCap.Round)

    val legLen = height * 0.42f
    val armLen = height * 0.30f

    when (move) {
        // 高踢：单腿支撑，另一腿踢向对手胸口
        1 -> {
            drawLine(
                color = color,
                start = hip,
                end = Offset(hip.x - facing * legLen * 0.22f, groundY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = hip,
                end = Offset(
                    hip.x + facing * legLen * (0.55f + 0.55f * attack),
                    hip.y - legLen * (0.15f + 0.65f * attack)
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        // 扫堂腿：重心压低，一腿贴地扫出
        2 -> {
            drawLine(
                color = color,
                start = hip,
                end = Offset(hip.x - facing * legLen * 0.12f, groundY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = hip,
                end = Offset(hip.x + facing * legLen * (0.85f + 0.45f * attack), groundY - height * 0.03f),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        // 腾空回旋踢：双腿离地收拢
        3 -> {
            drawLine(
                color = color,
                start = hip,
                end = Offset(
                    hip.x + facing * legLen * 0.60f,
                    hip.y + legLen * (0.55f - 0.35f * attack)
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = hip,
                end = Offset(
                    hip.x - facing * legLen * 0.45f,
                    hip.y + legLen * 0.35f
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
        // 直拳：常规站架
        else -> {
            drawLine(
                color = color,
                start = hip,
                end = Offset(hip.x + facing * legLen * 0.34f, groundY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
            drawLine(
                color = color,
                start = hip,
                end = Offset(hip.x - facing * legLen * 0.30f, groundY),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }
    }

    // 手臂：腾空时双臂张开，其余为前手出击 + 后手护身
    if (move == 3) {
        drawLine(
            color = color,
            start = shoulder,
            end = Offset(shoulder.x + facing * armLen * 0.95f, shoulder.y - armLen * 0.55f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = shoulder,
            end = Offset(shoulder.x - facing * armLen * 0.95f, shoulder.y - armLen * 0.45f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    } else {
        val punchX = shoulder.x + facing * armLen * (0.25f + 0.95f * attack)
        val punchY = shoulder.y - height * 0.04f + (1f - attack) * height * 0.06f
        drawLine(
            color = color,
            start = shoulder,
            end = Offset(punchX, punchY),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = shoulder,
            end = Offset(
                shoulder.x - facing * armLen * 0.42f,
                shoulder.y + armLen * 0.28f
            ),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        // 出击瞬间的拳风
        if (attack > 0.75f) {
            val alpha = (attack - 0.75f) / 0.25f
            drawCircle(
                color = color.copy(alpha = 0.22f * alpha),
                radius = height * 0.05f * (1f + alpha),
                center = Offset(punchX, punchY),
                style = Stroke(width = height * 0.014f)
            )
        }
    }
}
