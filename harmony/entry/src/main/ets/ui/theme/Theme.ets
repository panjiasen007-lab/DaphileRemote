import { ThemeRuntime } from './ThemeRuntime';

/** 颜色统一从 ThemeRuntime 读取，切换主题时自动跟随。 */
export class AppColors {
  static get BACKGROUND(): string { return ThemeRuntime.instance.palette.background; }
  static get SURFACE(): string { return ThemeRuntime.instance.palette.surface; }
  static get SURFACE_HIGH(): string { return ThemeRuntime.instance.palette.surfaceHigh; }
  static get PRIMARY(): string { return ThemeRuntime.instance.palette.primary; }
  static get ON_PRIMARY(): string { return ThemeRuntime.instance.palette.onPrimary; }
  static get TEXT(): string { return ThemeRuntime.instance.palette.text; }
  static get TEXT_SECONDARY(): string { return ThemeRuntime.instance.palette.textSecondary; }
  static get OUTLINE(): string { return ThemeRuntime.instance.palette.outline; }
  static get ERROR(): string { return ThemeRuntime.instance.palette.error; }
  static get WARN(): string { return ThemeRuntime.instance.palette.tertiary; }
}

export interface ThemeOption {
  key: string;
  label: string;
}

/** 14 套主题的名称，Phase 6 再补齐各自配色与元素。 */
export const THEME_OPTIONS: ThemeOption[] = [
  { key: 'system', label: '跟随系统' },
  { key: 'dark', label: '深色' },
  { key: 'light', label: '浅色' },
  { key: 'spring', label: '春 · 青芽' },
  { key: 'summer', label: '夏 · 碧海' },
  { key: 'autumn', label: '秋 · 枫叶' },
  { key: 'winter', label: '冬 · 霜雪' },
  { key: 'taiji', label: '太极 · 阴阳' },
  { key: 'panda', label: '熊猫 · 黑白' },
  { key: 'china_red', label: '中国红 · 朱门' },
  { key: 'wind', label: '风 · 流风' },
  { key: 'flower', label: '花 · 繁花' },
  { key: 'snow', label: '雪 · 初雪' },
  { key: 'moon', label: '月 · 望月' }
];

export function themeOptionLabel(key: string): string {
  for (const option of THEME_OPTIONS) {
    if (option.key === key) {
      return option.label;
    }
  }
  return '深色';
}

/**
 * HarmonyOS 颜色字符串使用 #AARRGGBB，透明度必须放在最前面。
 * 直接拼接 '#RRGGBB' + '55' 会变成黄色等错误颜色。
 */
export function withAlpha(color: string, alpha: string): string {
  if (color.startsWith('#') && color.length === 7) {
    return `#${alpha}${color.substring(1)}`;
  }
  return color;
}

export function formatTime(seconds: number): string {
  if (!Number.isFinite(seconds) || seconds <= 0) {
    return '0:00';
  }
  const total = Math.trunc(seconds);
  const minutes = Math.floor(total / 60);
  const rest = total % 60;
  return `${minutes}:${rest.toString().padStart(2, '0')}`;
}
