package com.daphile.remote.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * 根据主题与系统设置解析最终配色。
 */
@Composable
fun resolveThemeDark(themeKey: String): Boolean {
    val theme = AppTheme.fromKey(themeKey)
    return when {
        theme == AppTheme.Light -> false
        theme != AppTheme.System -> theme.isDarkFamily
        else -> isSystemInDarkTheme()
    }
}

@Composable
fun DaphileRemoteTheme(
    theme: String = AppTheme.Dark.key,
    content: @Composable () -> Unit
) {
    val appTheme = remember(theme) { AppTheme.fromKey(theme) }
    val dark = resolveThemeDark(theme)
    val scheme = remember(appTheme, dark) { colorSchemeFor(appTheme, dark) }
    MaterialTheme(
        colorScheme = scheme,
        typography = Typography(),
        content = content
    )
}

fun isDarkTheme(theme: String): Boolean = AppTheme.fromKey(theme).isDarkFamily
