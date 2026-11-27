import { util } from '@kit.ArkTS';
import { AppSettings, Track, defaultAppSettings } from './Models';

function looksMojibake(value: string): boolean {
  if (value.length < 2) {
    return false;
  }
  if (value.indexOf('\uFFFD') >= 0) {
    return true;
  }
  let markers = 0;
  for (let i = 0; i < value.length; i++) {
    const code = value.charCodeAt(i);
    if ((code >= 0x80 && code <= 0x9F) || (code >= 0xA1 && code <= 0xFF)) {
      markers++;
    }
    if (markers >= 2) {
      return true;
    }
  }
  return false;
}

function readability(value: string): number {
  let score = 0;
  for (let i = 0; i < value.length; i++) {
    const code = value.charCodeAt(i);
    if (code === 0xFFFD) {
      score -= 8;
    } else if (code < 0x20 && code !== 0x09) {
      score -= 6;
    } else if (code >= 0x80 && code <= 0x9F) {
      score -= 5;
    } else if (code >= 0xA1 && code <= 0xFF) {
      score -= 3;
    } else if ((code >= 0x3400 && code <= 0x4DBF) || (code >= 0x4E00 && code <= 0x9FFF) ||
      (code >= 0x3040 && code <= 0x30FF) || (code >= 0xAC00 && code <= 0xD7AF)) {
      score += 6;
    } else if ((code >= 0x30 && code <= 0x39) || (code >= 0x41 && code <= 0x5A) ||
      (code >= 0x61 && code <= 0x7A) || code === 0x20) {
      score += 1;
    }
  }
  return score;
}

/** 修复 UTF-8 被单字节编码误读产生的乱码；无法确认时原样返回。 */
export function repairMojibakeText(value: string): string {
  if (!looksMojibake(value)) {
    return value;
  }
  const baseScore = readability(value);
  const bytes = new Uint8Array(value.length);
  for (let i = 0; i < value.length; i++) {
    const code = value.charCodeAt(i);
    if (code > 0xFF) {
      return value;
    }
    bytes[i] = code;
  }
  try {
    const decoder = util.TextDecoder.create('utf-8', { fatal: true, ignoreBOM: true });
    const decoded = decoder.decodeToString(bytes);
    if (decoded.length > 0 && decoded !== value && readability(decoded) + 10 >= baseScore + 6) {
      return decoded;
    }
  } catch (error) {
    return value;
  }
  return value;
}

/** 从 LMS 返回的 url 中取出不带扩展名的文件名，用作备用曲名。 */
export function fileNameTitle(track: Track): string {
  if (track.url.length === 0) {
    return '';
  }
  let raw = track.url;
  const slash = Math.max(raw.lastIndexOf('/'), raw.lastIndexOf('\\'));
  if (slash >= 0) {
    raw = raw.substring(slash + 1);
  }
  if (raw.length === 0) {
    return '';
  }
  let decoded = raw;
  try {
    decoded = decodeURIComponent(raw);
  } catch (error) {
    decoded = raw;
  }
  const query = decoded.indexOf('?');
  const hash = decoded.indexOf('#');
  let cut = decoded.length;
  if (query >= 0 && query < cut) {
    cut = query;
  }
  if (hash >= 0 && hash < cut) {
    cut = hash;
  }
  decoded = decoded.substring(0, cut);
  const dot = decoded.lastIndexOf('.');
  const withoutExt = dot > 0 ? decoded.substring(0, dot) : decoded;
  return withoutExt.trim();
}

class TrackDisplayRuntime {
  private settings: AppSettings = defaultAppSettings();

  configure(settings: AppSettings): void {
    this.settings = settings;
  }

  text(value: string): string {
    return this.settings.fixGarbledText ? repairMojibakeText(value) : value;
  }

  title(track: Track): string {
    let base = track.title;
    if (this.settings.useFileNameAsTitle) {
      const fromFile = fileNameTitle(track);
      if (fromFile.length > 0) {
        base = fromFile;
      }
    }
    return this.text(base);
  }

  artist(track: Track): string {
    return this.text(track.artist);
  }

  album(track: Track): string {
    return this.text(track.album);
  }
}

export const TrackDisplay = new TrackDisplayRuntime();
