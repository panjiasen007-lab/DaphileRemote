package com.daphile.remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkScheme = darkColorScheme(
    primary = Teal,
    onPrimary = Color(0xFF04110F),
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFFB8FFF5),
    secondary = Color(0xFF8FC7D9),
    onSecondary = Color(0xFF06131A),
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = TextSecondaryDark,
    outline = DarkOutline,
    outlineVariant = Color(0xFF222C36),
    error = ErrorRed,
    onError = Color(0xFF2A0505)
)

private val LightScheme = lightColorScheme(
    primary = TealDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC8F5EF),
    onPrimaryContainer = Color(0xFF003731),
    secondary = Color(0xFF3E6572),
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceHigh,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightOutline,
    outlineVariant = Color(0xFFE2E9EE),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

@Composable
fun DaphileRemoteTheme(
    theme: String = "dark",
    content: @Composable () -> Unit
) {
    val dark = when (theme) {
        "light" -> false
        "system" -> isSystemInDarkTheme()
        else -> true
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        typography = Typography(),
        content = content
    )
}

fun isDarkTheme(theme: String): Boolean = when (theme) {
    "light" -> false
    "system" -> false
    else -> true
}
