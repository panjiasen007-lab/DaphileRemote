import { http } from '@kit.NetworkKit';
import { fileIo as fs } from '@kit.CoreFileKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { LyricLine, Lyrics, LyricsSource, Track } from '../model/Models';

type MapJson = Record<string, Object>;

function asMap(value: Object | undefined): MapJson | null {
  if (value === undefined || value === null || typeof value !== 'object' || Array.isArray(value)) {
    return null;
  }
  return value as MapJson;
}

function asArray(value: Object | undefined): Object[] {
  if (value === undefined || value === null || !Array.isArray(value)) {
    return [];
  }
  return value as Object[];
}

function str(map: MapJson, key: string): string {
  const value = map[key];
  return value === undefined || value === null ? '' : value.toString();
}

function num(map: MapJson, key: string): number {
  const value = map[key];
  if (value === undefined || value === null) {
    return 0;
  }
  const parsed = typeof value === 'number' ? value as number : Number.parseFloat(value.toString());
  return Number.isNaN(parsed) ? 0 : parsed;
}

/**
 * 在线歌词与封面。
 * 歌词源：网易云 → QQ 音乐 → LRCLIB（自动模式）；本地 .lrc 优先。
 * 封面目前先沿用 LMS 自带封面，在线匹配在后续阶段接入。
 */
export class OnlineMetadataService {
  private static readonly USER_AGENT: string =
    'DaphileRemote/1.9.2 (HarmonyOS; https://github.com/panjiasen007-lab/daphile-remote)';
  private static readonly DESKTOP_UA: string =
    'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 ' +
    '(KHTML, like Gecko) Chrome/120.0 Safari/537.36';
  private static readonly CACHE_DAYS: number = 30;

  private filesDir: string;
  private lyricsDir: string;
  private lyricsLibraryDir: string;
  private memory: Map<string, Lyrics> = new Map<string, Lyrics>();

  constructor(filesDir: string) {
    this.filesDir = filesDir;
    this.lyricsDir = `${filesDir}/lyrics_cache`;
    this.lyricsLibraryDir = `${filesDir}/lyrics`;
    this.ensureDir(this.lyricsDir);
    this.ensureDir(this.lyricsLibraryDir);
  }

  private ensureDir(path: string): void {
    try {
      fs.mkdirSync(path, true);
    } catch (error) {
      // 目录已存在时忽略
    }
  }

  private readFile(path: string): string {
    try {
      return fs.readTextSync(path);
    } catch (error) {
      return '';
    }
  }

  private writeFile(path: string, text: string): void {
    const index = path.lastIndexOf('/');
    if (index > 0) {
      this.ensureDir(path.substring(0, index));
    }
    try {
      const file = fs.openSync(path,
        fs.OpenMode.CREATE | fs.OpenMode.READ_WRITE | fs.OpenMode.TRUNC);
      fs.writeSync(file.fd, text);
      fs.closeSync(file);
    } catch (error) {
      // 歌词写入失败不影响展示
    }
  }

  private exists(path: string): boolean {
    try {
      return fs.accessSync(path);
    } catch (error) {
      return false;
    }
  }

  private async get(url: string, headers: Record<string, string>): Promise<string> {
    const task = http.createHttp();
    try {
      const response = await task.request(url, {
        method: http.RequestMethod.GET,
        header: headers,
        connectTimeout: 6000,
        readTimeout: 12000,
        expectDataType: http.HttpDataType.STRING
      });
      if (response.responseCode < 200 || response.responseCode >= 300) {
        return '';
      }
      if (typeof response.result === 'string') {
        return response.result as string;
      }
      return JSON.stringify(response.result);
    } catch (error) {
      return '';
    } finally {
      task.destroy();
    }
  }

  private hashKey(value: string): string {
    let hash = 0;
    for (let i = 0; i < value.length; i++) {
      hash = ((hash << 5) - hash + value.charCodeAt(i)) | 0;
    }
    const positive = hash === -2147483648 ? 2147483647 : Math.abs(hash);
    return positive.toString(16);
  }

  private safeName(value: string): string {
    let cleaned = value.replace(/[\\/:*?"<>|]/g, '_').replace(/\s+/g, ' ').trim();
    while (cleaned.endsWith('.')) {
      cleaned = cleaned.substring(0, cleaned.length - 1);
    }
    return cleaned.length > 0 ? cleaned.substring(0, 80) : '未命名';
  }

  private cleanTitle(title: string, artist: string): string {
    let value = title.replace(/^\s*\d{1,3}[\s.\-]+/, '')
      .replace(/\s*-\s*cnhifi\.com\s*$/i, '')
      .trim();
    if (artist.length > 0 && artist !== '无艺人名') {
      if (value.startsWith(`${artist} - `)) {
        value = value.substring(artist.length + 3);
      } else if (value.startsWith(`${artist}-`)) {
        value = value.substring(artist.length + 1);
      } else if (value.startsWith(`${artist} `)) {
        value = value.substring(artist.length + 1);
      }
      value = value.trim();
    }
    return value;
  }

  /** 本地歌词库路径：艺术家/专辑/曲名.lrc */
  lyricsFileFor(track: Track): string {
    const artist = this.safeName(track.artist.length > 0 ? track.artist : '未知艺术家');
    const album = this.safeName(track.album.length > 0 ? track.album : '未知专辑');
    const title = this.safeName(track.title.length > 0 ? track.title : '未知曲目');
    return `${this.lyricsLibraryDir}/${artist}/${album}/${title}.lrc`;
  }

  savedLyricsPath(track: Track): string {
    const path = this.lyricsFileFor(track);
    return this.exists(path) ? path : '';
  }

  lyricsLibraryPath(): string {
    return this.lyricsLibraryDir;
  }

  lyricsLibraryCount(): number {
    let count = 0;
    try {
      count = this.countLrc(this.lyricsLibraryDir);
    } catch (error) {
      count = 0;
    }
    return count;
  }

  private countLrc(dir: string): number {
    let count = 0;
    let entries: string[] = [];
    try {
      entries = fs.listFileSync(dir);
    } catch (error) {
      return 0;
    }
    for (const entry of entries) {
      const path = `${dir}/${entry}`;
      try {
        const stat = fs.statSync(path);
        if (stat.isDirectory()) {
          count += this.countLrc(path);
        } else if (entry.toLowerCase().endsWith('.lrc')) {
          count++;
        }
      } catch (error) {
        // 忽略单个条目失败
      }
    }
    return count;
  }

  clearLyricsLibrary(): number {
    this.removeDir(this.lyricsLibraryDir);
    this.removeDir(this.lyricsDir);
    this.ensureDir(this.lyricsLibraryDir);
    this.ensureDir(this.lyricsDir);
    this.memory.clear();
    return 0;
  }

  private removeDir(path: string): void {
    try {
      const entries = fs.listFileSync(path);
      for (const entry of entries) {
        const child = `${path}/${entry}`;
        try {
          const stat = fs.statSync(child);
          if (stat.isDirectory()) {
            this.removeDir(child);
          } else {
            fs.unlinkSync(child);
          }
        } catch (error) {
          // 忽略
        }
      }
      fs.rmdirSync(path);
    } catch (error) {
      // 目录不存在时忽略
    }
  }

  saveLyrics(track: Track, lyrics: Lyrics): string {
    const path = this.lyricsFileFor(track);
    this.writeFile(path, this.serializeLyrics(lyrics));
    return path;
  }

  private serializeLyrics(lyrics: Lyrics): string {
    if (lyrics.plainText.length > 0) {
      return lyrics.plainText;
    }
    const lines: string[] = [];
    for (const line of lyrics.lines) {
      lines.push(`[${this.formatLrcTime(line.timeMs)}]${line.text}`);
    }
    return lines.join('\n');
  }

  private formatLrcTime(timeMs: number): string {
    const minutes = Math.floor(timeMs / 60000);
    const seconds = Math.floor((timeMs % 60000) / 1000);
    const centis = Math.floor((timeMs % 1000) / 10);
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}.` +
      `${centis.toString().padStart(2, '0')}`;
  }

  parseLyricsText(raw: string, source: string): Lyrics | null {
    return this.buildLyrics(raw, source);
  }

  loadSavedLyrics(track: Track): Lyrics | null {
    return this.readSavedLyrics(track);
  }

  async lyricsFor(track: Track, force: boolean = false): Promise<Lyrics | null> {
    if (track.title.length === 0) {
      return null;
    }
    const key = this.hashKey(`${track.artist}|${track.title}|${track.album}`);
    if (!force) {
      const cachedMemory = this.memory.get(key);
      if (cachedMemory !== undefined) {
        return cachedMemory;
      }
      const saved = this.readSavedLyrics(track);
      if (saved !== null) {
        this.memory.set(key, saved);
        this.writeLyricsCache(key, saved);
        return saved;
      }
      const cache = this.readLyricsCache(key);
      if (cache !== null) {
        this.memory.set(key, cache);
        this.saveLyrics(track, cache);
        return cache;
      }
    }
    let online = await this.fetchNetease(track);
    if (online === null) {
      online = await this.fetchQqMusic(track);
    }
    if (online === null) {
      online = await this.fetchLrclib(track);
    }
    if (online !== null) {
      this.memory.set(key, online);
      this.writeLyricsCache(key, online);
      this.saveLyrics(track, online);
    }
    return online;
  }

  async lyricsFrom(track: Track, source: LyricsSource): Promise<Lyrics | null> {
    if (track.title.length === 0) {
      return null;
    }
    let fetched: Lyrics | null = null;
    if (source === LyricsSource.NETEASE) {
      fetched = await this.fetchNetease(track);
    } else if (source === LyricsSource.QQ) {
      fetched = await this.fetchQqMusic(track);
    } else if (source === LyricsSource.LRCLIB) {
      fetched = await this.fetchLrclib(track);
    } else {
      fetched = await this.fetchNetease(track);
      if (fetched === null) {
        fetched = await this.fetchQqMusic(track);
      }
      if (fetched === null) {
        fetched = await this.fetchLrclib(track);
      }
    }
    if (fetched !== null) {
      const key = this.hashKey(`${track.artist}|${track.title}|${track.album}`);
      this.memory.set(key, fetched);
      this.writeLyricsCache(key, fetched);
      this.saveLyrics(track, fetched);
    }
    return fetched;
  }

  private readSavedLyrics(track: Track): Lyrics | null {
    const path = this.lyricsFileFor(track);
    if (!this.exists(path)) {
      return null;
    }
    return this.buildLyrics(this.readFile(path), '本地歌词');
  }

  private readLyricsCache(key: string): Lyrics | null {
    const path = `${this.lyricsDir}/${key}.json`;
    if (!this.exists(path)) {
      return null;
    }
    try {
      const stat = fs.statSync(path);
      const age = Date.now() - stat.mtime * 1000;
      if (age > OnlineMetadataService.CACHE_DAYS * 24 * 60 * 60 * 1000) {
        return null;
      }
      const root = asMap(JSON.parse(this.readFile(path)) as Object);
      if (root === null) {
        return null;
      }
      const lines: LyricLine[] = [];
      for (const item of asArray(root['lines'])) {
        const map = asMap(item);
        if (map !== null) {
          lines.push({ timeMs: Math.trunc(num(map, 't')), text: str(map, 's') });
        }
      }
      return {
        lines: lines,
        plainText: str(root, 'plain'),
        synced: root['synced'] === true,
        source: str(root, 'source')
      };
    } catch (error) {
      return null;
    }
  }

  private writeLyricsCache(key: string, lyrics: Lyrics): void {
    const array: Object[] = [];
    for (const line of lyrics.lines) {
      array.push({ 't': line.timeMs, 's': line.text } as MapJson);
    }
    const root: MapJson = {
      'lines': array,
      'plain': lyrics.plainText,
      'synced': lyrics.synced,
      'source': lyrics.source
    };
    this.writeFile(`${this.lyricsDir}/${key}.json`, JSON.stringify(root));
  }

  private buildLyrics(raw: string, source: string): Lyrics | null {
    if (raw.trim().length === 0) {
      return null;
    }
    const parsed = this.parseLrc(raw);
    if (parsed.length === 0) {
      return null;
    }
    let synced = false;
    for (const line of parsed) {
      if (line.timeMs > 0) {
        synced = true;
        break;
      }
    }
    return { lines: parsed, plainText: raw.trim(), synced: synced, source: source };
  }

  private parseLrc(text: string): LyricLine[] {
    const timeTag = /\[(\d{1,2}):(\d{2})(?:[.:](\d{1,3}))?]/g;
    const metadataTag = new RegExp(
      '^(作词|作曲|编曲|制作人|制作|监制|混音|母带|母带处理|人声编辑|人声|录音|和声|吉他|贝斯|鼓|弦乐|出品|发行|策划|统筹|演唱|原唱|词|曲)\\s*[:：]');
    // 过滤 “歌曲名 - 歌手A/歌手B/...” 这类制作信息行，避免被当成当前歌词
    const creditTag = new RegExp('^[^\\-–—]{1,64}\\s[-–—]\\s.+\\s\\/\\s.+\\s\\/\\s.+$');
    const lines: LyricLine[] = [];
    for (const raw of text.split('\n')) {
      timeTag.lastIndex = 0;
      const matches: RegExpExecArray[] = [];
      let match = timeTag.exec(raw);
      while (match !== null) {
        matches.push(match);
        match = timeTag.exec(raw);
      }
      if (matches.length === 0) {
        continue;
      }
      const content = raw.replace(timeTag, '').trim();
      if (content.length === 0 || metadataTag.test(content) || creditTag.test(content)) {
        continue;
      }
      for (const item of matches) {
        const minutes = Number.parseInt(item[1]);
        const seconds = Number.parseInt(item[2]);
        const fractionRaw = item[3] === undefined ? '' : item[3];
        let fraction = 0;
        if (fractionRaw.length === 1) {
          fraction = Number.parseInt(fractionRaw) * 100;
        } else if (fractionRaw.length === 2) {
          fraction = Number.parseInt(fractionRaw) * 10;
        } else if (fractionRaw.length >= 3) {
          fraction = Number.parseInt(fractionRaw.substring(0, 3));
        }
        lines.push({
          timeMs: minutes * 60000 + seconds * 1000 + (Number.isNaN(fraction) ? 0 : fraction),
          text: content
        });
      }
    }
    if (lines.length === 0) {
      const plain: LyricLine[] = [];
      for (const raw of text.split('\n')) {
        const trimmed = raw.trim();
        if (trimmed.length > 0 && !trimmed.startsWith('[')) {
          plain.push({ timeMs: 0, text: trimmed });
        }
      }
      return plain;
    }
    lines.sort((left: LyricLine, right: LyricLine) => left.timeMs - right.timeMs);
    const unique: LyricLine[] = [];
    const seen: Set<string> = new Set<string>();
    for (const line of lines) {
      const key = `${line.timeMs}|${line.text}`;
      if (!seen.has(key)) {
        seen.add(key);
        unique.push(line);
      }
    }
    return unique;
  }

  private async fetchLrclib(track: Track): Promise<Lyrics | null> {
    const title = this.cleanTitle(track.title, track.artist);
    const params: string[] = [
      `track_name=${encodeURIComponent(title)}`,
      `artist_name=${encodeURIComponent(track.artist)}`
    ];
    if (track.album.length > 0) {
      params.push(`album_name=${encodeURIComponent(track.album)}`);
    }
    const body = await this.get(`https://lrclib.net/api/search?${params.join('&')}`,
      { 'User-Agent': OnlineMetadataService.USER_AGENT });
    if (body.length === 0) {
      return null;
    }
    try {
      const array = JSON.parse(body) as Object[];
      if (array.length === 0) {
        return null;
      }
      let best: MapJson | null = null;
      let bestScore = -1;
      for (const item of array) {
        const map = asMap(item);
        if (map === null) {
          continue;
        }
        let score = 0;
        if (str(map, 'syncedLyrics').length > 0) {
          score += 100;
        }
        if (str(map, 'plainLyrics').length > 0) {
          score += 10;
        }
        const duration = num(map, 'duration');
        if (track.duration > 0 && duration > 0) {
          const diff = Math.abs(duration - track.duration);
          if (diff < 3) {
            score += 30;
          } else if (diff < 15) {
            score += 10;
          }
        }
        if (score > bestScore) {
          bestScore = score;
          best = map;
        }
      }
      if (best === null) {
        return null;
      }
      const raw = str(best, 'syncedLyrics').length > 0
        ? str(best, 'syncedLyrics') : str(best, 'plainLyrics');
      return this.buildLyrics(raw, 'LRCLIB');
    } catch (error) {
      return null;
    }
  }

  private async fetchNetease(track: Track): Promise<Lyrics | null> {
    const title = this.cleanTitle(track.title, track.artist);
    const queryParts: string[] = [];
    if (track.artist.length > 0 && track.artist !== '无艺人名') {
      queryParts.push(track.artist);
    }
    if (title.length > 0) {
      queryParts.push(title);
    }
    const query = queryParts.join(' ');
    if (query.length === 0) {
      return null;
    }
    const headers: Record<string, string> = {
      'User-Agent': OnlineMetadataService.DESKTOP_UA,
      'Referer': 'https://music.163.com/'
    };
    const searchUrl = 'https://music.163.com/api/search/get/web' +
      `?csrf_token=&s=${encodeURIComponent(query)}&type=1&offset=0&total=true&limit=5`;
    const searchBody = await this.get(searchUrl, headers);
    if (searchBody.length === 0) {
      return null;
    }
    try {
      const root = asMap(JSON.parse(searchBody) as Object);
      const songs = root === null ? [] : asArray(asMap(root['result']) === null
        ? undefined : (asMap(root['result']) as MapJson)['songs']);
      if (songs.length === 0) {
        return null;
      }
      const song = asMap(songs[0]);
      if (song === null) {
        return null;
      }
      const songId = Math.trunc(num(song, 'id'));
      if (songId <= 0) {
        return null;
      }
      const lyricUrl = 'https://music.163.com/api/song/lyric' +
        `?id=${songId}&lv=1&kv=1&tv=-1`;
      const lyricBody = await this.get(lyricUrl, headers);
      if (lyricBody.length === 0) {
        return null;
      }
      const lyricRoot = asMap(JSON.parse(lyricBody) as Object);
      if (lyricRoot === null) {
        return null;
      }
      const lrc = asMap(lyricRoot['lrc']);
      let text = lrc === null ? '' : str(lrc, 'lyric');
      if (text.length === 0) {
        const tlyric = asMap(lyricRoot['tlyric']);
        text = tlyric === null ? '' : str(tlyric, 'lyric');
      }
      if (text.length === 0) {
        return null;
      }
      return this.buildLyrics(text, '网易云音乐');
    } catch (error) {
      return null;
    }
  }

  private async fetchQqMusic(track: Track): Promise<Lyrics | null> {
    const mid = await this.qqSongMid(track);
    if (mid.length === 0) {
      return null;
    }
    const url = 'https://c.y.qq.com/lyric/fcgi-bin/fcg_query_lyric_new.fcg' +
      `?songmid=${encodeURIComponent(mid)}&format=json&nobase64=1&g_tk=5381` +
      '&loginUin=0&hostUin=0&inCharset=utf8&outCharset=utf-8&notice=0' +
      '&platform=yqq.json&needNewCode=0';
    const body = await this.get(url, this.qqHeaders());
    if (body.length === 0) {
      return null;
    }
    try {
      const root = asMap(JSON.parse(body) as Object);
      const text = root === null ? '' : str(root, 'lyric');
      if (text.length === 0) {
        return null;
      }
      return this.buildLyrics(text, 'QQ 音乐');
    } catch (error) {
      return null;
    }
  }

  private qqHeaders(): Record<string, string> {
    return {
      'User-Agent': OnlineMetadataService.USER_AGENT,
      'Referer': 'https://y.qq.com/'
    };
  }

  private async qqSongMid(track: Track): Promise<string> {
    const title = this.cleanTitle(track.title, track.artist);
    const queryParts: string[] = [];
    if (track.artist.length > 0 && track.artist !== '无艺人名') {
      queryParts.push(track.artist);
    }
    if (title.length > 0) {
      queryParts.push(title);
    }
    const query = queryParts.join(' ');
    if (query.length === 0) {
      return '';
    }
    const url = 'https://c.y.qq.com/splcloud/fcgi-bin/smartbox_new.fcg' +
      `?key=${encodeURIComponent(query)}&format=json&g_tk=5381&loginUin=0` +
      '&hostUin=0&inCharset=utf8&outCharset=utf-8&notice=0' +
      '&platform=yqq.json&needNewCode=0';
    const body = await this.get(url, this.qqHeaders());
    if (body.length === 0) {
      return '';
    }
    try {
      const root = asMap(JSON.parse(body) as Object);
      if (root === null) {
        return '';
      }
      const data = asMap(root['data']);
      if (data === null) {
        return '';
      }
      const song = asMap(data['song']);
      if (song === null) {
        return '';
      }
      const songs = asArray(song['itemlist']);
      if (songs.length === 0) {
        return '';
      }
      const first = asMap(songs[0]);
      return first === null ? '' : str(first, 'mid');
    } catch (error) {
      return '';
    }
  }

  private coverCachePath(): string {
    return `${this.filesDir}/covers_cache.json`;
  }

  /** 清空在线封面缓存，返回清空的条目数。 */
  clearCoverCache(): number {
    let count = 0;
    try {
      count = this.readCoverCache().size;
      fs.unlinkSync(this.coverCachePath());
    } catch (error) {
      // 缓存文件不存在时忽略
    }
    return count;
  }

  private readCoverCache(): Map<string, string> {
    const result: Map<string, string> = new Map<string, string>();
    const text = this.readFile(this.coverCachePath());
    if (text.length === 0) {
      return result;
    }
    try {
      const root = asMap(JSON.parse(text) as Object);
      if (root === null) {
        return result;
      }
      for (const key of Object.keys(root)) {
        const value = root[key];
        if (value !== undefined && value !== null) {
          result.set(key, value.toString());
        }
      }
    } catch (error) {
      // 缓存损坏时重新建立
    }
    return result;
  }

  private writeCoverCache(cache: Map<string, string>): void {
    const root: MapJson = {};
    cache.forEach((value: string, key: string) => {
      root[key] = value;
    });
    this.writeFile(this.coverCachePath(), JSON.stringify(root));
  }

  /** 在线封面：MusicBrainz/CAA → 网易云 → iTunes。 */
  async coverFor(artist: string, album: string, force: boolean = false): Promise<string> {
    const cleanArtist = artist.trim();
    const cleanAlbum = album.trim();
    if (cleanAlbum.length === 0) {
      return '';
    }
    const key = this.hashKey(`${cleanArtist}|${cleanAlbum}`);
    const cache = this.readCoverCache();
    if (!force) {
      const cached = cache.get(key);
      if (cached !== undefined && cached.length > 0) {
        return cached;
      }
    }

    let url = await this.coverFromMusicBrainz(cleanArtist, cleanAlbum);
    if (url.length === 0) {
      url = await this.coverFromNetease(cleanArtist, cleanAlbum);
    }
    if (url.length === 0) {
      url = await this.coverFromItunes(cleanArtist, cleanAlbum);
    }
    if (url.length > 0) {
      cache.set(key, url);
      this.writeCoverCache(cache);
    }
    return url;
  }

  private async coverFromMusicBrainz(artist: string, album: string): Promise<string> {
    const query = `release:"${album}"` + (artist.length > 0 ? ` AND artist:"${artist}"` : '');
    const url = 'https://musicbrainz.org/ws/2/release-group/?query=' +
      `${encodeURIComponent(query)}&fmt=json&limit=3`;
    const body = await this.get(url, { 'User-Agent': OnlineMetadataService.USER_AGENT });
    if (body.length === 0) {
      return '';
    }
    try {
      const root = asMap(JSON.parse(body) as Object);
      if (root === null) {
        return '';
      }
      const groups = asArray(root['release-groups']);
      if (groups.length === 0) {
        return '';
      }
      const first = asMap(groups[0]);
      if (first === null) {
        return '';
      }
      const id = str(first, 'id');
      if (id.length === 0) {
        return '';
      }
      return `https://coverartarchive.org/release-group/${id}/front-500`;
    } catch (error) {
      return '';
    }
  }

  private async coverFromNetease(artist: string, album: string): Promise<string> {
    const query = [artist, album].filter((value: string) => value.length > 0).join(' ');
    const url = 'https://music.163.com/api/search/get/web' +
      `?csrf_token=&s=${encodeURIComponent(query)}&type=10&offset=0&total=true&limit=3`;
    const body = await this.get(url, {
      'User-Agent': OnlineMetadataService.DESKTOP_UA,
      'Referer': 'https://music.163.com/'
    });
    if (body.length === 0) {
      return '';
    }
    try {
      const root = asMap(JSON.parse(body) as Object);
      if (root === null) {
        return '';
      }
      const result = asMap(root['result']);
      if (result === null) {
        return '';
      }
      const albums = asArray(result['albums']);
      if (albums.length === 0) {
        return '';
      }
      const first = asMap(albums[0]);
      if (first === null) {
        return '';
      }
      let pic = str(first, 'picUrl');
      if (pic.startsWith('http://')) {
        pic = 'https://' + pic.substring(7);
      }
      return pic;
    } catch (error) {
      return '';
    }
  }

  private async coverFromItunes(artist: string, album: string): Promise<string> {
    const term = [artist, album].filter((value: string) => value.length > 0).join(' ');
    const url = 'https://itunes.apple.com/search' +
      `?term=${encodeURIComponent(term)}&entity=album&limit=3`;
    const body = await this.get(url, { 'User-Agent': OnlineMetadataService.USER_AGENT });
    if (body.length === 0) {
      return '';
    }
    try {
      const root = asMap(JSON.parse(body) as Object);
      if (root === null) {
        return '';
      }
      const results = asArray(root['results']);
      if (results.length === 0) {
        return '';
      }
      const first = asMap(results[0]);
      if (first === null) {
        return '';
      }
      let cover = str(first, 'artworkUrl100');
      if (cover.length === 0) {
        return '';
      }
      cover = cover.replace('100x100bb', '600x600bb');
      return cover;
    } catch (error) {
      return '';
    }
  }
}
