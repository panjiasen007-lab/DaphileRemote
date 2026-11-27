import { i18n } from '@kit.LocalizationKit';
import { AppLanguage, languageFromKey } from '../../data/model/Models';
import {
  UiStrings, ar, az, de, el, en, es, fa, fr, it, ja, ko, koKp, nl, pt, ru,
  th, vi, zh, zhTW
} from './GeneratedStrings';

export type { UiStrings };

function matchSystemLanguage(): AppLanguage {
  let locale = 'en';
  let language = 'en';
  try {
    locale = i18n.System.getSystemLocale().toLowerCase();
    language = i18n.System.getSystemLanguage().toLowerCase();
  } catch (error) {
    return AppLanguage.EN;
  }
  if (language.startsWith('zh')) {
    if (locale.includes('tw') || locale.includes('hk') || locale.includes('mo') ||
      locale.includes('hant')) {
      return AppLanguage.ZH_TW;
    }
    return AppLanguage.ZH;
  }
  const candidates: AppLanguage[] = [
    AppLanguage.EN, AppLanguage.JA, AppLanguage.KO, AppLanguage.KO_KP,
    AppLanguage.RU, AppLanguage.DE, AppLanguage.FR, AppLanguage.ES,
    AppLanguage.IT, AppLanguage.PT, AppLanguage.NL, AppLanguage.EL,
    AppLanguage.VI, AppLanguage.TH, AppLanguage.AR, AppLanguage.AZ,
    AppLanguage.FA
  ];
  for (const candidate of candidates) {
    const code = candidate as string;
    if (language === code || language.startsWith(code + '-')) {
      return candidate;
    }
  }
  return AppLanguage.EN;
}

export function resolveLanguage(setting: string): AppLanguage {
  if (setting === 'auto' || setting.length === 0) {
    return matchSystemLanguage();
  }
  return languageFromKey(setting);
}

/** 根据设置返回完整文案表；auto 时按系统语言匹配。 */
export function stringsFor(setting: string): UiStrings {
  const language = resolveLanguage(setting);
  switch (language) {
    case AppLanguage.ZH: return zh;
    case AppLanguage.ZH_TW: return zhTW;
    case AppLanguage.JA: return ja;
    case AppLanguage.KO: return ko;
    case AppLanguage.KO_KP: return koKp;
    case AppLanguage.RU: return ru;
    case AppLanguage.DE: return de;
    case AppLanguage.FR: return fr;
    case AppLanguage.ES: return es;
    case AppLanguage.IT: return it;
    case AppLanguage.PT: return pt;
    case AppLanguage.NL: return nl;
    case AppLanguage.EL: return el;
    case AppLanguage.VI: return vi;
    case AppLanguage.TH: return th;
    case AppLanguage.AR: return ar;
    case AppLanguage.AZ: return az;
    case AppLanguage.FA: return fa;
    case AppLanguage.EN: return en;
    default: return en;
  }
}
