import {
  Album, Artist, BrowserItem, Genre, NowPlaying, Paged, Player, PlaylistSummary,
  PluginEntry, ScanStatus, SearchResults, ServerInfo, Track, YearItem,
  emptyTrack
} from '../model/Models';

/** JSON 对象的统一表示。ArkTS 不支持 org.json，统一用 Record 访问。 */
export type JsonMap = Record<string, Object>;

function asMap(value: Object | undefined): JsonMap | null {
  if (value === undefined || value === null) {
    return null;
  }
  if (typeof value === 'object' && !Array.isArray(value)) {
    return value as JsonMap;
  }
  return null;
}

function asArray(value: Object | undefined): Object[] {
  if (value !== undefined && value !== null && Array.isArray(value)) {
    return value as Object[];
  }
  return [];
}

export function readRaw(obj: JsonMap, key: string): Object | undefined {
  return obj[key];
}

export function readMap(obj: JsonMap, key: string): JsonMap | null {
  return asMap(obj[key]);
}

function stringAny(obj: JsonMap, keys: string[]): string {
  for (const key of keys) {
    const raw = obj[key];
    if (raw === undefined || raw === null) {
      continue;
    }
    const value: string = raw.toString();
    if (value.length > 0 && value !== 'null' && value !== 'undefined') {
      return value;
    }
  }
  return '';
}

function intAny(obj: JsonMap, keys: string[], defaultValue: number = 0): number {
  for (const key of keys) {
    const raw = obj[key];
    if (raw === undefined || raw === null) {
      continue;
    }
    if (typeof raw === 'number') {
      return Math.trunc(raw as number);
    }
    const parsed = Number.parseFloat(raw.toString());
    if (!Number.isNaN(parsed)) {
      return Math.trunc(parsed);
    }
  }
  return defaultValue;
}

function doubleAny(obj: JsonMap, keys: string[]): number {
  for (const key of keys) {
    const raw = obj[key];
    if (raw === undefined || raw === null) {
      continue;
    }
    if (typeof raw === 'number') {
      return raw as number;
    }
    const parsed = Number.parseFloat(raw.toString());
    if (!Number.isNaN(parsed)) {
      return parsed;
    }
  }
  return 0;
}

function truthy(obj: JsonMap, keys: string[], defaultValue: boolean = false): boolean {
  for (const key of keys) {
    const raw = obj[key];
    if (raw === undefined || raw === null) {
      continue;
    }
    if (typeof raw === 'boolean') {
      return raw as boolean;
    }
    if (typeof raw === 'number') {
      return (raw as number) !== 0;
    }
    const text = raw.toString().toLowerCase();
    return text === 'true' || text === '1';
  }
  return defaultValue;
}

function objectArray(obj: JsonMap, key: string): Object[] | null {
  const raw = obj[key];
  if (raw === undefined || raw === null) {
    return null;
  }
  const map = asMap(raw);
  if (map !== null) {
    return [map];
  }
  if (Array.isArray(raw)) {
    return asArray(raw);
  }
  return null;
}

/** LMS 部分接口不返回具名 loop，取第一个对象数组作为列表。 */
function objectLoop(obj: JsonMap): JsonMap[] {
  const keys = Object.keys(obj);
  for (const key of keys) {
    const raw = obj[key];
    if (raw === undefined || raw === null) {
      continue;
    }
    const map = asMap(raw);
    if (map !== null) {
      return [map];
    }
    const array = asArray(raw);
    if (array.length > 0) {
      const first = asMap(array[0]);
      if (first !== null) {
        const result: JsonMap[] = [];
        for (const item of array) {
          const entry = asMap(item);
          if (entry !== null) {
            result.push(entry);
          }
        }
        return result;
      }
    }
  }
  return [];
}

function mapsFrom(obj: JsonMap, arrayKeys: string[]): JsonMap[] {
  for (const key of arrayKeys) {
    const array = objectArray(obj, key);
    if (array !== null) {
      const result: JsonMap[] = [];
      for (const item of array) {
        const entry = asMap(item);
        if (entry !== null) {
          result.push(entry);
        }
      }
      return result;
    }
  }
  return objectLoop(obj);
}

export class LmsParser {
  static readonly TRACK_TAGS: string = 'cdelgoaIirstTyquQ';

  /**
   * 计算分页边界。
   * LMS 的 count 在 albums/titles 等命令里只表示本次返回条数，
   * 因此按「请求条数是否全部返回」判断是否还有下一页。
   */
  static pageInfo(result: JsonMap, rawCount: number, start: number, requested: number): Paged<Object> {
    const reported = intAny(result, ['count'], rawCount);
    if (requested <= 0) {
      return { total: Math.max(reported, rawCount), items: [], hasMore: false };
    }
    const more = rawCount >= requested;
    const known = start + rawCount;
    return {
      total: Math.max(more ? known + 1 : known, reported),
      items: [],
      hasMore: more
    };
  }

  static parseServerInfo(result: JsonMap): ServerInfo {
    return {
      version: stringAny(result, ['version']),
      ip: stringAny(result, ['ip']),
      httpPort: intAny(result, ['httpport'], 9000),
      playerCount: intAny(result, ['player count', 'player_count']),
      albumCount: intAny(result, ['info total albums', 'info_total_albums']),
      artistCount: intAny(result, ['info total artists', 'info_total_artists']),
      genreCount: intAny(result, ['info total genres', 'info_total_genres']),
      songCount: intAny(result, ['info total songs', 'info_total_songs']),
      scanning: truthy(result, ['rescan']),
      lastScan: stringAny(result, ['lastscan'])
    };
  }

  static parsePlayers(result: JsonMap): Player[] {
    const loop = mapsFrom(result, ['players_loop']);
    const players: Player[] = [];
    for (const item of loop) {
      const id = stringAny(item, ['playerid', 'id']);
      if (id.length === 0) {
        continue;
      }
      players.push({
        id: id,
        name: stringAny(item, ['name']).length > 0 ? stringAny(item, ['name']) : id,
        model: stringAny(item, ['model']),
        connected: truthy(item, ['connected'], true),
        power: truthy(item, ['power'], true),
        volume: 0,
        muted: false,
        mode: 'stop',
        currentTitle: '',
        currentArtist: '',
        canSetVolume: true,
        syncMaster: '',
        syncSlaves: []
      });
    }
    return players;
  }

  static parsePlayerStatus(base: Player, status: JsonMap): Player {
    const now = LmsParser.parseNowPlaying(base.id, status, null);
    const masterRaw = stringAny(status, ['sync_master']);
    const master = masterRaw.length > 0 && masterRaw !== '-' ? masterRaw : '';
    const slavesRaw = stringAny(status, ['sync_slaves']);
    const slaves: string[] = [];
    if (slavesRaw.length > 0) {
      for (const part of slavesRaw.split(',')) {
        const trimmed = part.trim();
        if (trimmed.length > 0 && trimmed !== '-') {
          slaves.push(trimmed);
        }
      }
    }
    return {
      id: base.id,
      name: base.name,
      model: base.model,
      connected: truthy(status, ['player_connected'], base.connected),
      power: now.power,
      volume: now.volume,
      muted: now.muted,
      mode: now.mode,
      currentTitle: now.track !== null ? now.track.title : stringAny(status, ['current_title']),
      currentArtist: now.track !== null ? now.track.artist : '',
      canSetVolume: now.canSetVolume,
      syncMaster: master,
      syncSlaves: slaves
    };
  }

  static parseNowPlaying(playerId: string, status: JsonMap, queueOverride: Track[] | null): NowPlaying {
    const power = truthy(status, ['power'], true);
    const modeRaw = stringAny(status, ['mode']);
    const mode = modeRaw.length > 0 ? modeRaw : 'stop';
    const currentIndex = intAny(status, ['playlist_cur_index', 'playlist index']);
    const rawVolume = intAny(status, ['mixer volume', 'mixer_volume']);
    const muted = rawVolume < 0 || truthy(status, ['mixer muted', 'mixer_muting']);
    const volume = Math.min(100, Math.max(0, Math.abs(rawVolume)));
    const loop = mapsFrom(status, ['playlist_loop']);
    let queue: Track[] = [];
    if (queueOverride !== null) {
      queue = queueOverride;
    } else {
      for (let index = 0; index < loop.length; index++) {
        const item = loop[index];
        queue.push(LmsParser.parseTrack(item, intAny(item, ['playlist index'], index)));
      }
    }
    const fallbackTitle = stringAny(status, ['current_title']);
    let currentTrack: Track | null = null;
    for (const track of queue) {
      if (track.index === currentIndex) {
        currentTrack = track;
        break;
      }
    }
    if (currentTrack === null) {
      for (const item of loop) {
        if (intAny(item, ['playlist index'], -1) === currentIndex) {
          currentTrack = LmsParser.parseTrack(item, currentIndex);
          break;
        }
      }
    }
    if (currentTrack === null && loop.length > 0) {
      const first = loop[0];
      currentTrack = LmsParser.parseTrack(first, intAny(first, ['playlist index'], 0));
    }
    if (currentTrack !== null && currentTrack.title.length === 0 && fallbackTitle.length > 0) {
      currentTrack.title = fallbackTitle;
    }
    if (currentTrack === null && fallbackTitle.length > 0) {
      const track = emptyTrack();
      track.title = fallbackTitle;
      currentTrack = track;
    }
    const rawDuration = doubleAny(status, ['duration']);
    const duration = rawDuration > 0 ? rawDuration : (currentTrack !== null ? currentTrack.duration : 0);
    const trackCountRaw = intAny(status, ['playlist_tracks']);
    return {
      playerId: playerId,
      power: power,
      mode: mode,
      time: doubleAny(status, ['time']),
      duration: duration,
      volume: volume,
      muted: muted,
      repeat: intAny(status, ['playlist repeat', 'playlist_repeat']),
      shuffle: intAny(status, ['playlist shuffle', 'playlist_shuffle']),
      currentIndex: currentIndex,
      trackCount: trackCountRaw > 0 ? trackCountRaw : queue.length,
      track: currentTrack,
      queue: queue,
      canSeek: truthy(status, ['can_seek'], true),
      canSetVolume: truthy(status, ['digital_volume_control'], true) &&
        truthy(status, ['use_volume_control'], true),
      playlistTimestamp: stringAny(status, ['playlist_timestamp'])
    };
  }

  static parseTrack(item: JsonMap, index: number = 0): Track {
    let title = stringAny(item, ['title', 'track']);
    if (title.length === 0) {
      title = stringAny(item, ['current_title']);
    }
    return {
      index: index,
      id: stringAny(item, ['id', 'track_id']),
      title: title,
      artist: stringAny(item, ['artist']),
      album: stringAny(item, ['album']),
      albumId: stringAny(item, ['album_id']),
      duration: doubleAny(item, ['duration']),
      coverId: stringAny(item, ['coverid', 'artwork_track_id']),
      type: stringAny(item, ['type']),
      sampleRate: stringAny(item, ['samplerate']),
      sampleSize: stringAny(item, ['samplesize']),
      bitRate: stringAny(item, ['bitrate']),
      trackNum: intAny(item, ['tracknum']),
      disc: intAny(item, ['disc']),
      url: stringAny(item, ['url']),
      lossless: truthy(item, ['lossless', 'Q']),
      channels: intAny(item, ['channels', 'channelcount'])
    };
  }

  static parseAlbums(result: JsonMap, start: number = 0, requested: number = -1): Paged<Album> {
    const array = objectArray(result, 'albums_loop');
    const loop = mapsFrom(result, ['albums_loop']);
    const items: Album[] = [];
    for (const item of loop) {
      const id = stringAny(item, ['id', 'album_id']);
      if (id.length === 0) {
        continue;
      }
      const title = stringAny(item, ['album', 'title']);
      items.push({
        id: id,
        title: title.length > 0 ? title : '未命名专辑',
        artist: stringAny(item, ['artist', 'artists', 'albumartist']),
        year: stringAny(item, ['year']),
        artworkId: stringAny(item, ['artwork_track_id', 'coverid']).length > 0
          ? stringAny(item, ['artwork_track_id', 'coverid']) : id,
        trackCount: intAny(item, ['trackcount', 'track_count'])
      });
    }
    const rawCount = array !== null ? array.length : items.length;
    const page = LmsParser.pageInfo(result, rawCount, start, requested);
    return { total: page.total, items: items, hasMore: page.hasMore };
  }

  static parseArtists(result: JsonMap, start: number = 0, requested: number = -1): Paged<Artist> {
    const array = objectArray(result, 'artists_loop');
    const loop = mapsFrom(result, ['artists_loop']);
    const items: Artist[] = [];
    for (const item of loop) {
      const id = stringAny(item, ['id', 'artist_id']);
      const name = stringAny(item, ['artist', 'name']);
      if (id.length > 0 && name.length > 0) {
        items.push({ id: id, name: name });
      }
    }
    const rawCount = array !== null ? array.length : items.length;
    const page = LmsParser.pageInfo(result, rawCount, start, requested);
    return { total: page.total, items: items, hasMore: page.hasMore };
  }

  static parseGenres(result: JsonMap, start: number = 0, requested: number = -1): Paged<Genre> {
    const array = objectArray(result, 'genres_loop');
    const loop = mapsFrom(result, ['genres_loop']);
    const items: Genre[] = [];
    for (const item of loop) {
      const id = stringAny(item, ['id', 'genre_id']);
      const name = stringAny(item, ['genre', 'name']);
      if (id.length > 0 && name.length > 0) {
        items.push({ id: id, name: name });
      }
    }
    const rawCount = array !== null ? array.length : items.length;
    const page = LmsParser.pageInfo(result, rawCount, start, requested);
    return { total: page.total, items: items, hasMore: page.hasMore };
  }

  static parseYears(result: JsonMap, start: number = 0, requested: number = -1): Paged<YearItem> {
    const loop = objectLoop(result);
    const items: YearItem[] = [];
    const seen: Set<string> = new Set<string>();
    for (const item of loop) {
      const year = stringAny(item, ['year']);
      if (year.length > 0 && !seen.has(year)) {
        seen.add(year);
        items.push({ year: year });
      }
    }
    const page = LmsParser.pageInfo(result, items.length, start, requested);
    return { total: page.total, items: items, hasMore: page.hasMore };
  }

  static parsePlaylists(result: JsonMap, start: number = 0, requested: number = -1): Paged<PlaylistSummary> {
    const array = objectArray(result, 'playlists_loop');
    const loop = mapsFrom(result, ['playlists_loop']);
    const items: PlaylistSummary[] = [];
    for (const item of loop) {
      const id = stringAny(item, ['id', 'playlist_id']);
      const name = stringAny(item, ['name', 'playlist']);
      if (id.length > 0 && name.length > 0) {
        items.push({ id: id, name: name });
      }
    }
    const rawCount = array !== null ? array.length : items.length;
    const page = LmsParser.pageInfo(result, rawCount, start, requested);
    return { total: page.total, items: items, hasMore: page.hasMore };
  }

  static parseTracks(result: JsonMap, start: number = 0, requested: number = -1): Paged<Track> {
    const array = objectArray(result, 'titles_loop') !== null
      ? objectArray(result, 'titles_loop')
      : (objectArray(result, 'tracks_loop') !== null
        ? objectArray(result, 'tracks_loop')
        : objectArray(result, 'songs_loop'));
    const loop = mapsFrom(result, ['titles_loop', 'tracks_loop', 'songs_loop']);
    const items: Track[] = [];
    for (let index = 0; index < loop.length; index++) {
      items.push(LmsParser.parseTrack(loop[index], index));
    }
    const rawCount = array !== null ? array.length : items.length;
    const page = LmsParser.pageInfo(result, rawCount, start, requested);
    return { total: page.total, items: items, hasMore: page.hasMore };
  }

  static parseSearch(query: string, result: JsonMap): SearchResults {
    const songs: Track[] = [];
    for (const item of mapsFrom(result, ['tracks_loop', 'songs_loop', 'titles_loop'])) {
      songs.push({
        index: 0,
        id: stringAny(item, ['track_id', 'id']),
        title: stringAny(item, ['track', 'title']),
        artist: stringAny(item, ['artist']),
        album: stringAny(item, ['album']),
        albumId: '',
        duration: doubleAny(item, ['duration']),
        coverId: '',
        type: '',
        sampleRate: '',
        sampleSize: '',
        bitRate: '',
        trackNum: 0,
        disc: 0,
        url: '',
        lossless: false,
        channels: 0
      });
    }
    const albums: Album[] = [];
    for (const item of mapsFrom(result, ['albums_loop'])) {
      const id = stringAny(item, ['album_id', 'id']);
      if (id.length === 0) {
        continue;
      }
      albums.push({
        id: id,
        title: stringAny(item, ['album', 'title']),
        artist: stringAny(item, ['artist']),
        year: stringAny(item, ['year']),
        artworkId: stringAny(item, ['artwork_track_id', 'coverid']).length > 0
          ? stringAny(item, ['artwork_track_id', 'coverid']) : id,
        trackCount: 0
      });
    }
    const artists: Artist[] = [];
    for (const item of mapsFrom(result, ['artists_loop'])) {
      const id = stringAny(item, ['artist_id', 'id']);
      const name = stringAny(item, ['artist', 'name']);
      if (id.length > 0 && name.length > 0) {
        artists.push({ id: id, name: name });
      }
    }
    const playlists: PlaylistSummary[] = [];
    for (const item of mapsFrom(result, ['playlists_loop'])) {
      const id = stringAny(item, ['playlist_id', 'id']);
      const name = stringAny(item, ['playlist', 'name']);
      if (id.length > 0 && name.length > 0) {
        playlists.push({ id: id, name: name });
      }
    }
    return { query: query, songs: songs, albums: albums, artists: artists, playlists: playlists };
  }

  static parseBrowserItems(result: JsonMap): BrowserItem[] {
    const loop = mapsFrom(result, ['item_loop', 'folder_loop']);
    const items: BrowserItem[] = [];
    for (const item of loop) {
      const id = stringAny(item, ['id', 'item_id']);
      if (id.length === 0) {
        continue;
      }
      const type = stringAny(item, ['type']);
      const filename = stringAny(item, ['filename']);
      let name = stringAny(item, ['name', 'title']);
      if (name.length === 0) {
        name = filename;
      }
      if (name.length === 0) {
        name = id;
      }
      const hasItems = intAny(item, ['hasitems', 'has_items']) > 0 ||
        truthy(item, ['hasitems']) || type === 'folder' || type === 'dir';
      items.push({
        id: id,
        name: name,
        hasItems: hasItems,
        type: type,
        url: stringAny(item, ['url']),
        isAudio: truthy(item, ['isaudio', 'is_audio']) ||
          type === 'audio' || type === 'track' || type === 'file',
        icon: stringAny(item, ['icon']),
        description: stringAny(item, ['description']),
        filename: filename,
        durationText: stringAny(item, ['duration']),
        coverId: stringAny(item, ['coverid', 'artwork_track_id'])
      });
    }
    return items;
  }

  /** songinfo 返回的是键值对数组，这里合并成一个对象。 */
  static parseSongInfo(result: JsonMap): JsonMap {
    const loop = objectArray(result, 'songinfo_loop');
    if (loop === null) {
      return result;
    }
    const merged: JsonMap = {};
    for (const entry of loop) {
      const map = asMap(entry);
      if (map === null) {
        continue;
      }
      for (const key of Object.keys(map)) {
        merged[key] = map[key];
      }
    }
    return merged;
  }

  static parseScanStatus(result: JsonMap): ScanStatus {
    if (!truthy(result, ['rescan'])) {
      return { scanning: false, message: '扫描完成', percent: 100 };
    }
    const phases: string[] = [];
    let done = 0;
    let total = 0;
    for (const key of Object.keys(result)) {
      if (key === 'rescan' || key === 'totaltime' || key === 'steps' ||
        key === 'lastscanfailed' || key === 'info') {
        continue;
      }
      const value = result[key];
      if (value === undefined || value === null) {
        continue;
      }
      const parsed = typeof value === 'number'
        ? Math.trunc(value as number)
        : Math.trunc(Number.parseFloat(value.toString()));
      if (!Number.isNaN(parsed) && parsed >= 0 && parsed <= 100) {
        phases.push(`${key} ${parsed}%`);
        done += parsed;
        total += 100;
      }
    }
    const percent = total > 0 ? Math.trunc(done * 100 / total) : -1;
    const time = stringAny(result, ['totaltime']);
    const info = stringAny(result, ['info']);
    let message = '正在扫描音乐库';
    if (time.length > 0) {
      message += ` · ${time}`;
    }
    if (phases.length > 0) {
      message += ` · ${phases.slice(0, 3).join(' ')}`;
    }
    if (info.length > 0) {
      message += ` · ${info}`;
    }
    return { scanning: true, message: message, percent: percent };
  }

  static parsePlugins(result: JsonMap): PluginEntry[] {
    const loop = mapsFrom(result, ['radios_loop', 'apps_loop']);
    const items: PluginEntry[] = [];
    for (const item of loop) {
      const cmd = stringAny(item, ['cmd']);
      if (cmd.length === 0) {
        continue;
      }
      items.push({
        cmd: cmd,
        name: stringAny(item, ['name']).length > 0 ? stringAny(item, ['name']) : cmd,
        type: stringAny(item, ['type']),
        icon: stringAny(item, ['icon'])
      });
    }
    return items;
  }
}
