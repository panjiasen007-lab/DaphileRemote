package com.daphile.remote.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * 主题的场景化配置。
 *
 * 每个主题都有独立的背景绘制、播放场景、封面外框与进度条造型，
 * 由 [ThemeArt] 描述并通过 [LocalThemeArt] 下发。
 */
enum class ThemeBackdropKind {
    Pulse,
    Lattice,
    WindField,
    Blossom,
    Snowfall,
    MoonSky,
    Starfield,
    PaperGrid,
    WillowBreeze,
    OceanWaves,
    MapleFall,
    FrostBloom,
    YinYang,
    BambooShade
}

enum class ThemeSceneKind {
    StickFight,
    ShadowPuppet,
    Kites,
    Butterflies,
    SnowChase,
    MoonSword,
    SoundWave,
    PaperPlane,
    Swallows,
    Sailboat,
    Geese,
    Skater,
    PushHands,
    PandaBamboo
}

enum class ThemeCoverFrame {
    None,
    LatticeRing,
    Windmill,
    BlossomWindow,
    Frost,
    MoonPhase,
    HaloRing,
    ThinFrame,
    BambooRing,
    WaveCrest,
    MapleWreath,
    IceCrystal,
    Bagua,
    BambooLeaf
}

enum class ThemeSliderStyle {
    Default,
    RuyiBar,
    SeedThumb,
    BudThumb,
    Icicle,
    SilverLine,
    GlowDot,
    HairLine,
    LeafThumb,
    ShellThumb,
    MapleThumb,
    FrostNeedle,
    YinYangThumb,
    PawThumb
}

data class ThemeArt(
    val backdrop: ThemeBackdropKind = ThemeBackdropKind.Pulse,
    val scene: ThemeSceneKind = ThemeSceneKind.StickFight,
    val cover: ThemeCoverFrame = ThemeCoverFrame.None,
    val slider: ThemeSliderStyle = ThemeSliderStyle.Default,
    /** 该主题专属的点缀色（金、银等），为空时使用配色方案中的 tertiary。 */
    val metal: Color? = null
)

/**
 * 取得主题的场景化配置。[dark] 仅用于「跟随系统」：它按系统明暗沿用深色或浅色主题的画面。
 */
fun themeArtOf(theme: AppTheme, dark: Boolean = true): ThemeArt = when (theme) {
    AppTheme.System -> if (dark) DarkArt else LightArt
    AppTheme.Dark -> DarkArt
    AppTheme.Light -> LightArt
    AppTheme.ChinaRed -> ThemeArt(
        backdrop = ThemeBackdropKind.Lattice,
        scene = ThemeSceneKind.ShadowPuppet,
        cover = ThemeCoverFrame.LatticeRing,
        slider = ThemeSliderStyle.RuyiBar,
        metal = Color(0xFFD4AF37)
    )
    AppTheme.Wind -> ThemeArt(
        backdrop = ThemeBackdropKind.WindField,
        scene = ThemeSceneKind.Kites,
        cover = ThemeCoverFrame.Windmill,
        slider = ThemeSliderStyle.SeedThumb
    )
    AppTheme.Flower -> ThemeArt(
        backdrop = ThemeBackdropKind.Blossom,
        scene = ThemeSceneKind.Butterflies,
        cover = ThemeCoverFrame.BlossomWindow,
        slider = ThemeSliderStyle.BudThumb
    )
    AppTheme.Snow -> ThemeArt(
        backdrop = ThemeBackdropKind.Snowfall,
        scene = ThemeSceneKind.SnowChase,
        cover = ThemeCoverFrame.Frost,
        slider = ThemeSliderStyle.Icicle
    )
    AppTheme.Moon -> ThemeArt(
        backdrop = ThemeBackdropKind.MoonSky,
        scene = ThemeSceneKind.MoonSword,
        cover = ThemeCoverFrame.MoonPhase,
        slider = ThemeSliderStyle.SilverLine,
        metal = Color(0xFFD9C58A)
    )
    AppTheme.Spring -> ThemeArt(
        backdrop = ThemeBackdropKind.WillowBreeze,
        scene = ThemeSceneKind.Swallows,
        cover = ThemeCoverFrame.BambooRing,
        slider = ThemeSliderStyle.LeafThumb,
        metal = Color(0xFF7BC96F)
    )
    AppTheme.Summer -> ThemeArt(
        backdrop = ThemeBackdropKind.OceanWaves,
        scene = ThemeSceneKind.Sailboat,
        cover = ThemeCoverFrame.WaveCrest,
        slider = ThemeSliderStyle.ShellThumb,
        metal = Color(0xFFF2C14E)
    )
    AppTheme.Autumn -> ThemeArt(
        backdrop = ThemeBackdropKind.MapleFall,
        scene = ThemeSceneKind.Geese,
        cover = ThemeCoverFrame.MapleWreath,
        slider = ThemeSliderStyle.MapleThumb,
        metal = Color(0xFFC46A2A)
    )
    AppTheme.Winter -> ThemeArt(
        backdrop = ThemeBackdropKind.FrostBloom,
        scene = ThemeSceneKind.Skater,
        cover = ThemeCoverFrame.IceCrystal,
        slider = ThemeSliderStyle.FrostNeedle,
        metal = Color(0xFFDFF3FF)
    )
    AppTheme.Taiji -> ThemeArt(
        backdrop = ThemeBackdropKind.YinYang,
        scene = ThemeSceneKind.PushHands,
        cover = ThemeCoverFrame.Bagua,
        slider = ThemeSliderStyle.YinYangThumb,
        metal = Color(0xFFC8433C)
    )
    AppTheme.Panda -> ThemeArt(
        backdrop = ThemeBackdropKind.BambooShade,
        scene = ThemeSceneKind.PandaBamboo,
        cover = ThemeCoverFrame.BambooLeaf,
        slider = ThemeSliderStyle.PawThumb,
        metal = Color(0xFF7BA05B)
    )
}

/** 深色：星域 + 声波共鸣 + 细光环。 */
private val DarkArt = ThemeArt(
    backdrop = ThemeBackdropKind.Starfield,
    scene = ThemeSceneKind.SoundWave,
    cover = ThemeCoverFrame.HaloRing,
    slider = ThemeSliderStyle.GlowDot,
    metal = Color(0xFF6FE3D4)
)

/** 浅色：纸面网格 + 纸飞机 + 细线框。 */
private val LightArt = ThemeArt(
    backdrop = ThemeBackdropKind.PaperGrid,
    scene = ThemeSceneKind.PaperPlane,
    cover = ThemeCoverFrame.ThinFrame,
    slider = ThemeSliderStyle.HairLine,
    metal = Color(0xFF2F8F86)
)

val LocalThemeArt = staticCompositionLocalOf { ThemeArt() }
