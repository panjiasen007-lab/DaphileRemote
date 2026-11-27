/**
 * 主题运行时：14 套配色 + 元素化配置。
 * 配色值从 Android 版 AppThemes.kt 逐一移植。
 */

export interface ThemePalette {
  isDark: boolean;
  background: string;
  surface: string;
  surfaceHigh: string;
  primary: string;
  onPrimary: string;
  primaryContainer: string;
  onPrimaryContainer: string;
  secondary: string;
  tertiary: string;
  text: string;
  textSecondary: string;
  outline: string;
  error: string;
}

export enum ThemeBackdropKind {
  PULSE = 'pulse',
  LATTICE = 'lattice',
  WIND_FIELD = 'windField',
  BLOSSOM = 'blossom',
  SNOWFALL = 'snowfall',
  MOON_SKY = 'moonSky',
  STARFIELD = 'starfield',
  PAPER_GRID = 'paperGrid',
  WILLOW_BREEZE = 'willowBreeze',
  OCEAN_WAVES = 'oceanWaves',
  MAPLE_FALL = 'mapleFall',
  FROST_BLOOM = 'frostBloom',
  YIN_YANG = 'yinYang',
  BAMBOO_SHADE = 'bambooShade'
}

export enum ThemeSceneKind {
  STICK_FIGHT = 'stickFight',
  SHADOW_PUPPET = 'shadowPuppet',
  KITES = 'kites',
  BUTTERFLIES = 'butterflies',
  SNOW_CHASE = 'snowChase',
  MOON_SWORD = 'moonSword',
  SOUND_WAVE = 'soundWave',
  PAPER_PLANE = 'paperPlane',
  SWALLOWS = 'swallows',
  SAILBOAT = 'sailboat',
  GEESE = 'geese',
  SKATER = 'skater',
  PUSH_HANDS = 'pushHands',
  PANDA_BAMBOO = 'pandaBamboo'
}

export enum ThemeCoverFrame {
  NONE = 'none',
  LATTICE_RING = 'latticeRing',
  WINDMILL = 'windmill',
  BLOSSOM_WINDOW = 'blossomWindow',
  FROST = 'frost',
  MOON_PHASE = 'moonPhase',
  HALO_RING = 'haloRing',
  THIN_FRAME = 'thinFrame',
  BAMBOO_RING = 'bambooRing',
  WAVE_CREST = 'waveCrest',
  MAPLE_WREATH = 'mapleWreath',
  ICE_CRYSTAL = 'iceCrystal',
  BAGUA = 'bagua',
  BAMBOO_LEAF = 'bambooLeaf'
}

export enum ThemeSliderStyle {
  DEFAULT = 'default',
  RUYI_BAR = 'ruyiBar',
  SEED_THUMB = 'seedThumb',
  BUD_THUMB = 'budThumb',
  ICICLE = 'icicle',
  SILVER_LINE = 'silverLine',
  GLOW_DOT = 'glowDot',
  HAIR_LINE = 'hairLine',
  LEAF_THUMB = 'leafThumb',
  SHELL_THUMB = 'shellThumb',
  MAPLE_THUMB = 'mapleThumb',
  FROST_NEEDLE = 'frostNeedle',
  YIN_YANG_THUMB = 'yinYangThumb',
  PAW_THUMB = 'pawThumb'
}

export interface ThemeArt {
  backdrop: ThemeBackdropKind;
  scene: ThemeSceneKind;
  cover: ThemeCoverFrame;
  slider: ThemeSliderStyle;
  metal: string;
}

const DARK_PALETTE: ThemePalette = {
  isDark: true,
  background: '#0B0F14',
  surface: '#131A21',
  surfaceHigh: '#1B242D',
  primary: '#38D6C4',
  onPrimary: '#04110F',
  primaryContainer: '#143F3B',
  onPrimaryContainer: '#B8FFF5',
  secondary: '#8FC7D9',
  tertiary: '#38D6C4',
  text: '#F2F6F8',
  textSecondary: '#9AA6B2',
  outline: '#2B3641',
  error: '#FF6B6B'
};

const LIGHT_PALETTE: ThemePalette = {
  isDark: false,
  background: '#F5F7F9',
  surface: '#FFFFFF',
  surfaceHigh: '#ECF1F4',
  primary: '#1FA99A',
  onPrimary: '#FFFFFF',
  primaryContainer: '#C8F5EF',
  onPrimaryContainer: '#003731',
  secondary: '#3E6572',
  tertiary: '#1FA99A',
  text: '#111820',
  textSecondary: '#5B6874',
  outline: '#D3DCE3',
  error: '#BA1A1A'
};

const SPRING_DARK: ThemePalette = {
  isDark: true,
  background: '#0C1510',
  surface: '#132018',
  surfaceHigh: '#1D2E22',
  primary: '#8FE3A6',
  onPrimary: '#06110F',
  primaryContainer: '#1F4A2C',
  onPrimaryContainer: '#D6FFE0',
  secondary: '#BBD9A9',
  tertiary: '#D98BAA',
  text: '#EAF6EC',
  textSecondary: '#A3BBA8',
  outline: '#324A38',
  error: '#FF6B6B'
};

const SPRING_LIGHT: ThemePalette = {
  isDark: false,
  background: '#F4FBF3',
  surface: '#FFFFFF',
  surfaceHigh: '#E3F1E3',
  primary: '#2E7D4F',
  onPrimary: '#FFFFFF',
  primaryContainer: '#C9F5D4',
  onPrimaryContainer: '#08351A',
  secondary: '#5C7A52',
  tertiary: '#D98BAA',
  text: '#122016',
  textSecondary: '#4F6353',
  outline: '#C9DECB',
  error: '#BA1A1A'
};

const SUMMER_DARK: ThemePalette = {
  isDark: true,
  background: '#06131A',
  surface: '#0D1E28',
  surfaceHigh: '#162C39',
  primary: '#5FD6E8',
  onPrimary: '#06110F',
  primaryContainer: '#0F4A5C',
  onPrimaryContainer: '#C9F5FF',
  secondary: '#7FC9E8',
  tertiary: '#F2C14E',
  text: '#E6F7FF',
  textSecondary: '#9FBDCB',
  outline: '#274455',
  error: '#FF6B6B'
};

const SUMMER_LIGHT: ThemePalette = {
  isDark: false,
  background: '#F1FAFD',
  surface: '#FFFFFF',
  surfaceHigh: '#DDF1F7',
  primary: '#007C91',
  onPrimary: '#FFFFFF',
  primaryContainer: '#C4EEF8',
  onPrimaryContainer: '#00323D',
  secondary: '#2D7EA8',
  tertiary: '#F2C14E',
  text: '#0A1C24',
  textSecondary: '#4B6572',
  outline: '#BFDCE8',
  error: '#BA1A1A'
};

const AUTUMN_DARK: ThemePalette = {
  isDark: true,
  background: '#150F0B',
  surface: '#201710',
  surfaceHigh: '#2C2117',
  primary: '#F0A45B',
  onPrimary: '#06110F',
  primaryContainer: '#5A3413',
  onPrimaryContainer: '#FFDCBF',
  secondary: '#E07A5F',
  tertiary: '#C99A2E',
  text: '#FFF1E4',
  textSecondary: '#CDB49B',
  outline: '#4A3A2A',
  error: '#FF6B6B'
};

const AUTUMN_LIGHT: ThemePalette = {
  isDark: false,
  background: '#FFF8F1',
  surface: '#FFFFFF',
  surfaceHigh: '#F6E5D4',
  primary: '#A94F14',
  onPrimary: '#FFFFFF',
  primaryContainer: '#FFDCC2',
  onPrimaryContainer: '#371200',
  secondary: '#B3562F',
  tertiary: '#C99A2E',
  text: '#241407',
  textSecondary: '#6E5643',
  outline: '#E7D0B8',
  error: '#BA1A1A'
};

const WINTER_DARK: ThemePalette = {
  isDark: true,
  background: '#0A1018',
  surface: '#111A24',
  surfaceHigh: '#1A2733',
  primary: '#B9D8F5',
  onPrimary: '#06110F',
  primaryContainer: '#274358',
  onPrimaryContainer: '#DCEEFF',
  secondary: '#9FC0DC',
  tertiary: '#7FA9C9',
  text: '#EDF5FF',
  textSecondary: '#A6BACB',
  outline: '#31465A',
  error: '#FF6B6B'
};

const WINTER_LIGHT: ThemePalette = {
  isDark: false,
  background: '#F6FAFF',
  surface: '#FFFFFF',
  surfaceHigh: '#E4EEF7',
  primary: '#2F6389',
  onPrimary: '#FFFFFF',
  primaryContainer: '#D5E8F8',
  onPrimaryContainer: '#06283F',
  secondary: '#54748E',
  tertiary: '#7FA9C9',
  text: '#0E1A24',
  textSecondary: '#51616F',
  outline: '#C9D9E7',
  error: '#BA1A1A'
};

const TAIJI_DARK: ThemePalette = {
  isDark: true,
  background: '#121211',
  surface: '#1A1A18',
  surfaceHigh: '#262622',
  primary: '#E8E6E1',
  onPrimary: '#111112',
  primaryContainer: '#2C2C29',
  onPrimaryContainer: '#F4F2ED',
  secondary: '#B9B6AE',
  tertiary: '#C8433C',
  text: '#F4F2ED',
  textSecondary: '#A9A6A0',
  outline: '#3C3B37',
  error: '#FF6B6B'
};

const TAIJI_LIGHT: ThemePalette = {
  isDark: false,
  background: '#F6F4EF',
  surface: '#FFFFFF',
  surfaceHigh: '#ECE9E2',
  primary: '#1B1B1D',
  onPrimary: '#FDFCF8',
  primaryContainer: '#E3E0D8',
  onPrimaryContainer: '#141416',
  secondary: '#55555A',
  tertiary: '#B33A33',
  text: '#141416',
  textSecondary: '#5C5C61',
  outline: '#D6D2C9',
  error: '#BA1A1A'
};

const PANDA_DARK: ThemePalette = {
  isDark: true,
  background: '#050505',
  surface: '#0D0D0D',
  surfaceHigh: '#191919',
  primary: '#F7F7F4',
  onPrimary: '#050505',
  primaryContainer: '#1C2A16',
  onPrimaryContainer: '#DDEBCF',
  secondary: '#7BA05B',
  tertiary: '#9BC46E',
  text: '#FBFBF8',
  textSecondary: '#A8A8A3',
  outline: '#303030',
  error: '#FF6B6B'
};

const PANDA_LIGHT: ThemePalette = {
  isDark: false,
  background: '#FDFDFA',
  surface: '#FFFFFF',
  surfaceHigh: '#EDEEE9',
  primary: '#0F0F0F',
  onPrimary: '#FFFFFF',
  primaryContainer: '#E4EEDC',
  onPrimaryContainer: '#0F0F0F',
  secondary: '#5E8C3E',
  tertiary: '#6F9E52',
  text: '#0B0B0B',
  textSecondary: '#5A5A56',
  outline: '#D8D9D2',
  error: '#BA1A1A'
};

const CHINA_RED_DARK: ThemePalette = {
  isDark: true,
  background: '#120A0B',
  surface: '#1B0F11',
  surfaceHigh: '#2A1618',
  primary: '#E23B4A',
  onPrimary: '#2A0508',
  primaryContainer: '#5E1119',
  onPrimaryContainer: '#FFD9DC',
  secondary: '#D4AF37',
  tertiary: '#E9C46A',
  text: '#F8E9E4',
  textSecondary: '#C9A9A4',
  outline: '#4A2A2C',
  error: '#FF6B6B'
};

const CHINA_RED_LIGHT: ThemePalette = {
  isDark: false,
  background: '#FBF4EC',
  surface: '#FFFFFF',
  surfaceHigh: '#F2E4D8',
  primary: '#B3122B',
  onPrimary: '#FFFFFF',
  primaryContainer: '#FAD9DC',
  onPrimaryContainer: '#4A0C14',
  secondary: '#8C6A1F',
  tertiary: '#C8A24A',
  text: '#2A1214',
  textSecondary: '#6B4A44',
  outline: '#E2CDBB',
  error: '#BA1A1A'
};

const WIND_DARK: ThemePalette = {
  isDark: true,
  background: '#0B1620',
  surface: '#111F2B',
  surfaceHigh: '#1A2E3C',
  primary: '#A8C6C9',
  onPrimary: '#08151E',
  primaryContainer: '#1E3A48',
  onPrimaryContainer: '#D6F0F7',
  secondary: '#6E8B9B',
  tertiary: '#D9E6E8',
  text: '#E6F1F5',
  textSecondary: '#97AEBB',
  outline: '#2C4250',
  error: '#FF6B6B'
};

const WIND_LIGHT: ThemePalette = {
  isDark: false,
  background: '#F3F6F7',
  surface: '#FFFFFF',
  surfaceHigh: '#E1EBEE',
  primary: '#3D6B7E',
  onPrimary: '#FFFFFF',
  primaryContainer: '#D3E7EC',
  onPrimaryContainer: '#06262F',
  secondary: '#6E8B9B',
  tertiary: '#7FA3B2',
  text: '#0E1B22',
  textSecondary: '#4E6570',
  outline: '#C9D9DE',
  error: '#BA1A1A'
};

const FLOWER_DARK: ThemePalette = {
  isDark: true,
  background: '#201318',
  surface: '#2A1B21',
  surfaceHigh: '#38252C',
  primary: '#F0A9BC',
  onPrimary: '#2A0E17',
  primaryContainer: '#4A2431',
  onPrimaryContainer: '#FFD9E3',
  secondary: '#E8A0B4',
  tertiary: '#A8C79F',
  text: '#FBEDF0',
  textSecondary: '#C9A8B0',
  outline: '#4A3138',
  error: '#FF6B6B'
};

const FLOWER_LIGHT: ThemePalette = {
  isDark: false,
  background: '#FFF8F6',
  surface: '#FFFFFF',
  surfaceHigh: '#F7E6E8',
  primary: '#C25A78',
  onPrimary: '#FFFFFF',
  primaryContainer: '#FBDDE4',
  onPrimaryContainer: '#3E0E1C',
  secondary: '#7FA679',
  tertiary: '#E8A0B4',
  text: '#2A151A',
  textSecondary: '#6E4E56',
  outline: '#EAD2D6',
  error: '#BA1A1A'
};

const SNOW_DARK: ThemePalette = {
  isDark: true,
  background: '#0A1420',
  surface: '#101C2A',
  surfaceHigh: '#182838',
  primary: '#A9C7DE',
  onPrimary: '#06121C',
  primaryContainer: '#1E3548',
  onPrimaryContainer: '#DCEEFF',
  secondary: '#BFE9F2',
  tertiary: '#F4F9FF',
  text: '#EDF6FF',
  textSecondary: '#A0B7C8',
  outline: '#2C4152',
  error: '#FF6B6B'
};

const SNOW_LIGHT: ThemePalette = {
  isDark: false,
  background: '#F4F9FF',
  surface: '#FFFFFF',
  surfaceHigh: '#E2EFF8',
  primary: '#2F6C92',
  onPrimary: '#FFFFFF',
  primaryContainer: '#D6EAF8',
  onPrimaryContainer: '#042736',
  secondary: '#5A8CB2',
  tertiary: '#8FB3CC',
  text: '#0B1822',
  textSecondary: '#4E6272',
  outline: '#C7DBEA',
  error: '#BA1A1A'
};

const MOON_DARK: ThemePalette = {
  isDark: true,
  background: '#0B0F1C',
  surface: '#121A2B',
  surfaceHigh: '#1B2437',
  primary: '#D9C58A',
  onPrimary: '#0B0F1C',
  primaryContainer: '#26314C',
  onPrimaryContainer: '#F0EAD8',
  secondary: '#C9CEDA',
  tertiary: '#8FA4C8',
  text: '#EAE6DA',
  textSecondary: '#A8AFC0',
  outline: '#2C3650',
  error: '#FF6B6B'
};

const MOON_LIGHT: ThemePalette = {
  isDark: false,
  background: '#FBFAF5',
  surface: '#FFFFFF',
  surfaceHigh: '#EFEDE2',
  primary: '#7A6A3A',
  onPrimary: '#FFFFFF',
  primaryContainer: '#EDE6CE',
  onPrimaryContainer: '#32290E',
  secondary: '#5A6780',
  tertiary: '#4E5E80',
  text: '#16171F',
  textSecondary: '#555B68',
  outline: '#D9D5C6',
  error: '#BA1A1A'
};

const DARK_ART: ThemeArt = {
  backdrop: ThemeBackdropKind.STARFIELD,
  scene: ThemeSceneKind.SOUND_WAVE,
  cover: ThemeCoverFrame.HALO_RING,
  slider: ThemeSliderStyle.GLOW_DOT,
  metal: '#6FE3D4'
};

const LIGHT_ART: ThemeArt = {
  backdrop: ThemeBackdropKind.PAPER_GRID,
  scene: ThemeSceneKind.PAPER_PLANE,
  cover: ThemeCoverFrame.THIN_FRAME,
  slider: ThemeSliderStyle.HAIR_LINE,
  metal: '#2F8F86'
};

export function paletteFor(themeKey: string): ThemePalette {
  switch (themeKey) {
    case 'light': return LIGHT_PALETTE;
    case 'spring': return SPRING_DARK;
    case 'summer': return SUMMER_LIGHT;
    case 'autumn': return AUTUMN_LIGHT;
    case 'winter': return WINTER_LIGHT;
    case 'taiji': return TAIJI_DARK;
    case 'panda': return PANDA_DARK;
    case 'china_red': return CHINA_RED_DARK;
    case 'wind': return WIND_DARK;
    case 'flower': return FLOWER_LIGHT;
    case 'snow': return SNOW_DARK;
    case 'moon': return MOON_DARK;
    case 'system':
    case 'dark':
    default:
      return DARK_PALETTE;
  }
}

export function themeArtOf(themeKey: string): ThemeArt {
  switch (themeKey) {
    case 'light': return LIGHT_ART;
    case 'spring':
      return {
        backdrop: ThemeBackdropKind.WILLOW_BREEZE,
        scene: ThemeSceneKind.SWALLOWS,
        cover: ThemeCoverFrame.BAMBOO_RING,
        slider: ThemeSliderStyle.LEAF_THUMB,
        metal: '#7BC96F'
      };
    case 'summer':
      return {
        backdrop: ThemeBackdropKind.OCEAN_WAVES,
        scene: ThemeSceneKind.SAILBOAT,
        cover: ThemeCoverFrame.WAVE_CREST,
        slider: ThemeSliderStyle.SHELL_THUMB,
        metal: '#F2C14E'
      };
    case 'autumn':
      return {
        backdrop: ThemeBackdropKind.MAPLE_FALL,
        scene: ThemeSceneKind.GEESE,
        cover: ThemeCoverFrame.MAPLE_WREATH,
        slider: ThemeSliderStyle.MAPLE_THUMB,
        metal: '#C46A2A'
      };
    case 'winter':
      return {
        backdrop: ThemeBackdropKind.FROST_BLOOM,
        scene: ThemeSceneKind.SKATER,
        cover: ThemeCoverFrame.ICE_CRYSTAL,
        slider: ThemeSliderStyle.FROST_NEEDLE,
        metal: '#DFF3FF'
      };
    case 'taiji':
      return {
        backdrop: ThemeBackdropKind.YIN_YANG,
        scene: ThemeSceneKind.PUSH_HANDS,
        cover: ThemeCoverFrame.BAGUA,
        slider: ThemeSliderStyle.YIN_YANG_THUMB,
        metal: '#C8433C'
      };
    case 'panda':
      return {
        backdrop: ThemeBackdropKind.BAMBOO_SHADE,
        scene: ThemeSceneKind.PANDA_BAMBOO,
        cover: ThemeCoverFrame.BAMBOO_LEAF,
        slider: ThemeSliderStyle.PAW_THUMB,
        metal: '#7BA05B'
      };
    case 'china_red':
      return {
        backdrop: ThemeBackdropKind.LATTICE,
        scene: ThemeSceneKind.SHADOW_PUPPET,
        cover: ThemeCoverFrame.LATTICE_RING,
        slider: ThemeSliderStyle.RUYI_BAR,
        metal: '#D4AF37'
      };
    case 'wind':
      return {
        backdrop: ThemeBackdropKind.WIND_FIELD,
        scene: ThemeSceneKind.KITES,
        cover: ThemeCoverFrame.WINDMILL,
        slider: ThemeSliderStyle.SEED_THUMB,
        metal: '#A8C6C9'
      };
    case 'flower':
      return {
        backdrop: ThemeBackdropKind.BLOSSOM,
        scene: ThemeSceneKind.BUTTERFLIES,
        cover: ThemeCoverFrame.BLOSSOM_WINDOW,
        slider: ThemeSliderStyle.BUD_THUMB,
        metal: '#E8A0B4'
      };
    case 'snow':
      return {
        backdrop: ThemeBackdropKind.SNOWFALL,
        scene: ThemeSceneKind.SNOW_CHASE,
        cover: ThemeCoverFrame.FROST,
        slider: ThemeSliderStyle.ICICLE,
        metal: '#BFE9F2'
      };
    case 'moon':
      return {
        backdrop: ThemeBackdropKind.MOON_SKY,
        scene: ThemeSceneKind.MOON_SWORD,
        cover: ThemeCoverFrame.MOON_PHASE,
        slider: ThemeSliderStyle.SILVER_LINE,
        metal: '#D9C58A'
      };
    case 'system':
    case 'dark':
    default:
      return DARK_ART;
  }
}

@ObservedV2
export class ThemeRuntime {
  static readonly instance: ThemeRuntime = new ThemeRuntime();

  @Trace themeKey: string = 'dark';
  @Trace palette: ThemePalette = DARK_PALETTE;
  @Trace art: ThemeArt = DARK_ART;

  apply(themeKey: string): void {
    const key = themeKey.length > 0 ? themeKey : 'dark';
    this.themeKey = key;
    this.palette = paletteFor(key);
    this.art = themeArtOf(key);
  }
}
