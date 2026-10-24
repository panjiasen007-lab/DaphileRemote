package com.daphile.remote.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 外观主题。除默认深色/浅色外，提供四季与两种东方风格主题。
 * 每个主题都带明暗两套配色，切换主题后整个应用（含系统栏）同步变化。
 */
enum class AppTheme(val key: String, val label: String) {
    System("system", "跟随系统"),
    Dark("dark", "深色"),
    Light("light", "浅色"),
    Spring("spring", "春 · 青芽"),
    Summer("summer", "夏 · 碧海"),
    Autumn("autumn", "秋 · 枫叶"),
    Winter("winter", "冬 · 霜雪"),
    Taiji("taiji", "太极 · 阴阳"),
    Panda("panda", "熊猫 · 黑白");

    val isDarkFamily: Boolean
        get() = when (this) {
            System, Dark, Spring, Taiji, Panda -> true
            Light, Summer, Autumn, Winter -> false
        }

    companion object {
        fun fromKey(key: String?): AppTheme =
            entries.firstOrNull { it.key == key } ?: Dark
    }
}

private fun scheme(
    dark: Boolean,
    primary: Color,
    secondary: Color,
    background: Color,
    surface: Color,
    surfaceVariant: Color,
    onBackground: Color,
    onSurfaceVariant: Color,
    outline: Color,
    primaryContainer: Color,
    onPrimaryContainer: Color,
    onPrimary: Color = if (dark) Color(0xFF06110F) else Color.White,
    tertiary: Color = secondary
): ColorScheme = if (dark) {
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        tertiary = tertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = surfaceVariant,
        error = ErrorRed,
        onError = Color(0xFF2A0505)
    )
} else {
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        tertiary = tertiary,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onBackground,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline,
        outlineVariant = surfaceVariant,
        error = Color(0xFFBA1A1A),
        onError = Color.White
    )
}

private val SpringDark = scheme(
    dark = true,
    primary = Color(0xFF8FE3A6),
    secondary = Color(0xFFBBD9A9),
    background = Color(0xFF0C1510),
    surface = Color(0xFF132018),
    surfaceVariant = Color(0xFF1D2E22),
    onBackground = Color(0xFFEAF6EC),
    onSurfaceVariant = Color(0xFFA3BBA8),
    outline = Color(0xFF324A38),
    primaryContainer = Color(0xFF1F4A2C),
    onPrimaryContainer = Color(0xFFD6FFE0)
)

private val SpringLight = scheme(
    dark = false,
    primary = Color(0xFF2E7D4F),
    secondary = Color(0xFF5C7A52),
    background = Color(0xFFF4FBF3),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE3F1E3),
    onBackground = Color(0xFF122016),
    onSurfaceVariant = Color(0xFF4F6353),
    outline = Color(0xFFC9DECB),
    primaryContainer = Color(0xFFC9F5D4),
    onPrimaryContainer = Color(0xFF08351A),
    tertiary = Color(0xFFD98BAA)
)

private val SummerDark = scheme(
    dark = true,
    primary = Color(0xFF5FD6E8),
    secondary = Color(0xFF7FC9E8),
    background = Color(0xFF06131A),
    surface = Color(0xFF0D1E28),
    surfaceVariant = Color(0xFF162C39),
    onBackground = Color(0xFFE6F7FF),
    onSurfaceVariant = Color(0xFF9FBDCB),
    outline = Color(0xFF274455),
    primaryContainer = Color(0xFF0F4A5C),
    onPrimaryContainer = Color(0xFFC9F5FF)
)

private val SummerLight = scheme(
    dark = false,
    primary = Color(0xFF007C91),
    secondary = Color(0xFF2D7EA8),
    background = Color(0xFFF1FAFD),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFDDF1F7),
    onBackground = Color(0xFF0A1C24),
    onSurfaceVariant = Color(0xFF4B6572),
    outline = Color(0xFFBFDCE8),
    primaryContainer = Color(0xFFC4EEF8),
    onPrimaryContainer = Color(0xFF00323D),
    tertiary = Color(0xFFF2C14E)
)

private val AutumnDark = scheme(
    dark = true,
    primary = Color(0xFFF0A45B),
    secondary = Color(0xFFE07A5F),
    background = Color(0xFF150F0B),
    surface = Color(0xFF201710),
    surfaceVariant = Color(0xFF2C2117),
    onBackground = Color(0xFFFFF1E4),
    onSurfaceVariant = Color(0xFFCDB49B),
    outline = Color(0xFF4A3A2A),
    primaryContainer = Color(0xFF5A3413),
    onPrimaryContainer = Color(0xFFFFDCBF)
)

private val AutumnLight = scheme(
    dark = false,
    primary = Color(0xFFA94F14),
    secondary = Color(0xFFB3562F),
    background = Color(0xFFFFF8F1),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF6E5D4),
    onBackground = Color(0xFF241407),
    onSurfaceVariant = Color(0xFF6E5643),
    outline = Color(0xFFE7D0B8),
    primaryContainer = Color(0xFFFFDCC2),
    onPrimaryContainer = Color(0xFF371200),
    tertiary = Color(0xFFC99A2E)
)

private val WinterDark = scheme(
    dark = true,
    primary = Color(0xFFB9D8F5),
    secondary = Color(0xFF9FC0DC),
    background = Color(0xFF0A1018),
    surface = Color(0xFF111A24),
    surfaceVariant = Color(0xFF1A2733),
    onBackground = Color(0xFFEDF5FF),
    onSurfaceVariant = Color(0xFFA6BACB),
    outline = Color(0xFF31465A),
    primaryContainer = Color(0xFF274358),
    onPrimaryContainer = Color(0xFFDCEEFF)
)

private val WinterLight = scheme(
    dark = false,
    primary = Color(0xFF2F6389),
    secondary = Color(0xFF54748E),
    background = Color(0xFFF6FAFF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE4EEF7),
    onBackground = Color(0xFF0E1A24),
    onSurfaceVariant = Color(0xFF51616F),
    outline = Color(0xFFC9D9E7),
    primaryContainer = Color(0xFFD5E8F8),
    onPrimaryContainer = Color(0xFF06283F),
    tertiary = Color(0xFF7FA9C9)
)

/** 太极：黑白为骨，一点朱砂为魂。 */
private val TaijiDark = scheme(
    dark = true,
    primary = Color(0xFFE8E6E1),
    secondary = Color(0xFFB9B6AE),
    background = Color(0xFF121211),
    surface = Color(0xFF1A1A18),
    surfaceVariant = Color(0xFF262622),
    onBackground = Color(0xFFF4F2ED),
    onSurfaceVariant = Color(0xFFA9A6A0),
    outline = Color(0xFF3C3B37),
    primaryContainer = Color(0xFF2C2C29),
    onPrimaryContainer = Color(0xFFF4F2ED),
    onPrimary = Color(0xFF111112),
    tertiary = Color(0xFFC8433C)
)

private val TaijiLight = scheme(
    dark = false,
    primary = Color(0xFF1B1B1D),
    secondary = Color(0xFF55555A),
    background = Color(0xFFF6F4EF),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECE9E2),
    onBackground = Color(0xFF141416),
    onSurfaceVariant = Color(0xFF5C5C61),
    outline = Color(0xFFD6D2C9),
    primaryContainer = Color(0xFFE3E0D8),
    onPrimaryContainer = Color(0xFF141416),
    onPrimary = Color(0xFFFDFCF8),
    tertiary = Color(0xFFB33A33)
)

/** 熊猫：纯黑与纯白，配竹青点缀。 */
private val PandaDark = scheme(
    dark = true,
    primary = Color(0xFFF7F7F4),
    secondary = Color(0xFF7BA05B),
    background = Color(0xFF050505),
    surface = Color(0xFF0D0D0D),
    surfaceVariant = Color(0xFF191919),
    onBackground = Color(0xFFFBFBF8),
    onSurfaceVariant = Color(0xFFA8A8A3),
    outline = Color(0xFF303030),
    primaryContainer = Color(0xFF1C2A16),
    onPrimaryContainer = Color(0xFFDDEBCF),
    onPrimary = Color(0xFF050505),
    tertiary = Color(0xFF9BC46E)
)

private val PandaLight = scheme(
    dark = false,
    primary = Color(0xFF0F0F0F),
    secondary = Color(0xFF5E8C3E),
    background = Color(0xFFFDFDFA),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFEDEEE9),
    onBackground = Color(0xFF0B0B0B),
    onSurfaceVariant = Color(0xFF5A5A56),
    outline = Color(0xFFD8D9D2),
    primaryContainer = Color(0xFFE4EEDC),
    onPrimaryContainer = Color(0xFF0F0F0F),
    onPrimary = Color(0xFFFFFFFF),
    tertiary = Color(0xFF6F9E52)
)

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

internal fun colorSchemeFor(theme: AppTheme, dark: Boolean): ColorScheme = when (theme) {
    AppTheme.Spring -> if (dark) SpringDark else SpringLight
    AppTheme.Summer -> if (dark) SummerDark else SummerLight
    AppTheme.Autumn -> if (dark) AutumnDark else AutumnLight
    AppTheme.Winter -> if (dark) WinterDark else WinterLight
    AppTheme.Taiji -> if (dark) TaijiDark else TaijiLight
    AppTheme.Panda -> if (dark) PandaDark else PandaLight
    AppTheme.Light -> LightScheme
    AppTheme.Dark, AppTheme.System -> if (dark) DarkScheme else LightScheme
}
