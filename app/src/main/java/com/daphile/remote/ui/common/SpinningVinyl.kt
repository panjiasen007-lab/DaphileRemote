package com.daphile.remote.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest

/**
 * 旋转黑胶唱片：封面裁成圆形，外圈唱片纹路与高光盘面，中心轴孔。
 * [playing] 为 false 时停止转动并保留当前角度。
 */
@Composable
fun SpinningVinyl(
    coverUrl: String?,
    playing: Boolean,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary
) {
    val transition = rememberInfiniteTransition(label = "vinyl")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "vinylAngle"
    )
    val rotation = if (playing) angle else 0f

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            rotate(rotation, pivot = center) {
                // 盘面
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF202226), Color(0xFF0B0C0E)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius
                )
                // 唱片纹路
                val grooves = 26
                for (index in 1..grooves) {
                    val grooveRadius = radius * (0.35f + 0.62f * index / grooves)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.045f),
                        radius = grooveRadius,
                        style = Stroke(width = 1.1f)
                    )
                }
                // 高光
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.10f),
                            Color.Transparent,
                            Color.Transparent
                        )
                    ),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = radius * 0.16f)
                )
            }
        }

        // 封面
        val coverSize = 0.72f
        Box(
            modifier = Modifier
                .fillMaxSize(coverSize)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .rotate(rotation),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl.isNullOrBlank()) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxSize(0.42f)
                )
            } else {
                SubcomposeAsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // 中心轴孔
        Box(
            modifier = Modifier
                .fillMaxSize(0.075f)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.9f))
                .padding(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.background)
            )
        }
    }
}

/**
 * 静态小圆盘，用于列表/卡片装饰。
 */
@Composable
fun VinylBadge(
    coverUrl: String?,
    modifier: Modifier = Modifier,
    text: String? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            drawCircle(color = Color(0xFF15161A), radius = radius)
            for (index in 1..10) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = radius * (0.42f + 0.055f * index),
                    style = Stroke(width = 1f)
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize(0.58f)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (coverUrl.isNullOrBlank()) {
                Text(
                    text = text?.take(1) ?: "♪",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp
                )
            } else {
                SubcomposeAsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
