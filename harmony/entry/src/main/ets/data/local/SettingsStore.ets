import { preferences } from '@kit.ArkData';
import { common } from '@kit.AbilityKit';
import {
  AppSettings, CustomPlaylist, DailyRecommendation, ServerConfig, Track,
  defaultAppSettings
} from '../model/Models';
import { CryptoStore } from './CryptoStore';

type JsonMap = Record<string, Object>;

function asMap(value: Object | undefined): JsonMap | null {
  if (value === undefined || value === null || typeof value !== 'object' || Array.isArray(value)) {
    return null;
  }
  return value as JsonMap;
}

function str(map: JsonMap, key: string): string {
  const value = map[key];
  return value === undefined || value === null ? '' : value.toString();
}

function num(map: JsonMap, key: string, fallback: number): number {
  const value = map[key];
  if (value === undefined || value === null) {
    return fallback;
  }
  const parsed = typeof value === 'number' ? value as number : Number.parseFloat(value.toString());
  return Number.isNaN(parsed) ? fallback : parsed;
}

function bool(map: JsonMap, key: string, fallback: boolean): boolean {
  const value = map[key];
  if (value === undefined || value === null) {
    return fallback;
  }
  if (typeof value === 'boolean') {
    return value as boolean;
  }
  return value.toString() === 'true' || value.toString() === '1';
}

/**
 * 本地设置存储。键名与 Android 版保持一致，便于对照排查。
 */
export class SettingsStore {
  private static readonly STORE_NAME: string = 'daphile_settings';
  private context: common.Context;
  private crypto: CryptoStore;
  private store: preferences.Preferences | null = null;

  constructor(context: common.Context, crypto: CryptoStore) {
    this.context = context;
    this.crypto = crypto;
  }

  async init(): Promise<void> {
    if (this.store === null) {
      this.store = await preferences.getPreferences(this.context, SettingsStore.STORE_NAME);
    }
  }

  private async ready(): Promise<preferences.Preferences> {
    await this.init();
    return this.store as preferences.Preferences;
  }

  async loadServers(): Promise<ServerConfig[]> {
    const store = await this.ready();
    const raw = await store.get('servers', '') as string;
    if (raw.length === 0) {
      return [];
    }
    try {
      const array = JSON.parse(raw) as Object[];
      const result: ServerConfig[] = [];
      for (const item of array) {
        const map = asMap(item);
        if (map === null) {
          continue;
        }
        const id = str(map, 'id');
        const host = str(map, 'host');
        if (id.length === 0 || host.length === 0) {
          continue;
        }
        const password = await this.crypto.decrypt(str(map, 'password'));
        result.push({
          id: id,
          name: str(map, 'name').length > 0 ? str(map, 'name') : host,
          host: host,
          port: Math.trunc(num(map, 'port', 9000)),
          daphilePort: Math.trunc(num(map, 'daphilePort', 80)),
          username: str(map, 'username'),
          password: password,
          useHttps: bool(map, 'useHttps', false),
          macAddress: str(map, 'macAddress')
        });
      }
      return result;
    } catch (error) {
      return [];
    }
  }

  async saveServers(servers: ServerConfig[]): Promise<void> {
    const store = await this.ready();
    const array: Object[] = [];
    for (const server of servers) {
      const encrypted = await this.crypto.encrypt(server.password);
      array.push({
        'id': server.id,
        'name': server.name,
        'host': server.host,
        'port': server.port,
        'daphilePort': server.daphilePort,
        'username': server.username,
        'password': encrypted,
        'useHttps': server.useHttps,
        'macAddress': server.macAddress
      } as JsonMap);
    }
    await store.put('servers', JSON.stringify(array));
    await store.flush();
  }

  async loadCurrentServerId(): Promise<string> {
    const store = await this.ready();
    return await store.get('current_server_id', '') as string;
  }

  async setCurrentServerId(serverId: string): Promise<void> {
    const store = await this.ready();
    await store.put('current_server_id', serverId);
    await store.flush();
  }

  async loadSettings(): Promise<AppSettings> {
    const store = await this.ready();
    const settings = defaultAppSettings();
    settings.theme = await store.get('theme', settings.theme) as string;
    settings.pollSeconds = Math.trunc(await store.get('poll_seconds', settings.pollSeconds) as number);
    settings.notificationsEnabled = await store.get('notifications_enabled', settings.notificationsEnabled) as boolean;
    settings.hostPowerEnabled = await store.get('host_power_enabled', settings.hostPowerEnabled) as boolean;
    settings.coverCacheMb = Math.trunc(await store.get('cover_cache_mb', settings.coverCacheMb) as number);
    settings.onlineMetadataEnabled = await store.get('online_metadata_enabled', settings.onlineMetadataEnabled) as boolean;
    settings.lockScreenControls = await store.get('lock_screen_controls', settings.lockScreenControls) as boolean;
    settings.lyricsSource = await store.get('lyrics_source', settings.lyricsSource) as string;
    settings.language = await store.get('app_language', settings.language) as string;
    settings.layoutMode = await store.get('layout_mode', settings.layoutMode) as string;
    settings.autoWake = await store.get('auto_wake', settings.autoWake) as boolean;
    settings.useFileNameAsTitle = await store.get('use_file_name_as_title', settings.useFileNameAsTitle) as boolean;
    settings.fixGarbledText = await store.get('fix_garbled_text', settings.fixGarbledText) as boolean;
    return settings;
  }

  async setTheme(value: string): Promise<void> {
    await this.putValue('theme', value);
  }

  async setPollSeconds(value: number): Promise<void> {
    await this.putValue('poll_seconds', Math.min(30, Math.max(2, value)));
  }

  async setNotificationsEnabled(value: boolean): Promise<void> {
    await this.putValue('notifications_enabled', value);
  }

  async setHostPowerEnabled(value: boolean): Promise<void> {
    await this.putValue('host_power_enabled', value);
  }

  async setOnlineMetadataEnabled(value: boolean): Promise<void> {
    await this.putValue('online_metadata_enabled', value);
  }

  async setLockScreenControls(value: boolean): Promise<void> {
    await this.putValue('lock_screen_controls', value);
  }

  async setLyricsSource(value: string): Promise<void> {
    await this.putValue('lyrics_source', value);
  }

  async setLanguage(value: string): Promise<void> {
    await this.putValue('app_language', value);
  }

  async setLayoutMode(value: string): Promise<void> {
    await this.putValue('layout_mode', value);
  }

  async setAutoWake(value: boolean): Promise<void> {
    await this.putValue('auto_wake', value);
  }

  async setUseFileNameAsTitle(value: boolean): Promise<void> {
    await this.putValue('use_file_name_as_title', value);
  }

  async setFixGarbledText(value: boolean): Promise<void> {
    await this.putValue('fix_garbled_text', value);
  }

  private async putValue(key: string, value: preferences.ValueType): Promise<void> {
    const store = await this.ready();
    await store.put(key, value);
    await store.flush();
  }

  async playerForServer(serverId: string): Promise<string> {
    const store = await this.ready();
    return await store.get(`player_${serverId}`, '') as string;
  }

  async setPlayerForServer(serverId: string, playerId: string): Promise<void> {
    await this.putValue(`player_${serverId}`, playerId);
  }

  async loadCustomPlaylists(): Promise<CustomPlaylist[]> {
    const store = await this.ready();
    const raw = await store.get('custom_playlists', '') as string;
    if (raw.length === 0) {
      return [];
    }
    try {
      const array = JSON.parse(raw) as Object[];
      const result: CustomPlaylist[] = [];
      for (const item of array) {
        const map = asMap(item);
        if (map === null) {
          continue;
        }
        const id = str(map, 'id');
        if (id.length === 0) {
          continue;
        }
        result.push({
          id: id,
          name: str(map, 'name'),
          tracks: this.decodeTracks(map['tracks']),
          createdAt: Math.trunc(num(map, 'createdAt', 0)),
          updatedAt: Math.trunc(num(map, 'updatedAt', 0))
        });
      }
      return result;
    } catch (error) {
      return [];
    }
  }

  async saveCustomPlaylists(playlists: CustomPlaylist[]): Promise<void> {
    const store = await this.ready();
    const array: Object[] = [];
    for (const playlist of playlists) {
      array.push({
        'id': playlist.id,
        'name': playlist.name,
        'tracks': this.encodeTracks(playlist.tracks),
        'createdAt': playlist.createdAt,
        'updatedAt': playlist.updatedAt
      } as JsonMap);
    }
    await store.put('custom_playlists', JSON.stringify(array));
    await store.flush();
  }

  async loadDailyRecommendation(): Promise<DailyRecommendation | null> {
    const store = await this.ready();
    const raw = await store.get('daily_recommendation', '') as string;
    if (raw.length === 0) {
      return null;
    }
    try {
      const map = asMap(JSON.parse(raw) as Object);
      if (map === null) {
        return null;
      }
      return {
        date: str(map, 'date'),
        tracks: this.decodeTracks(map['tracks'])
      };
    } catch (error) {
      return null;
    }
  }

  async saveDailyRecommendation(recommendation: DailyRecommendation): Promise<void> {
    const store = await this.ready();
    const value: JsonMap = {
      'date': recommendation.date,
      'tracks': this.encodeTracks(recommendation.tracks)
    };
    await store.put('daily_recommendation', JSON.stringify(value));
    await store.flush();
  }

  async currentServerSnapshot(): Promise<ServerConfig | null> {
    const servers = await this.loadServers();
    if (servers.length === 0) {
      return null;
    }
    const id = await this.loadCurrentServerId();
    for (const server of servers) {
      if (server.id === id) {
        return server;
      }
    }
    return servers[0];
  }

  private encodeTracks(tracks: Track[]): Object[] {
    const array: Object[] = [];
    for (const track of tracks) {
      array.push({
        'id': track.id,
        'title': track.title,
        'artist': track.artist,
        'album': track.album,
        'albumId': track.albumId,
        'duration': track.duration,
        'coverId': track.coverId,
        'type': track.type,
        'sampleRate': track.sampleRate,
        'sampleSize': track.sampleSize,
        'bitRate': track.bitRate,
        'trackNum': track.trackNum,
        'disc': track.disc,
        'url': track.url,
        'lossless': track.lossless,
        'channels': track.channels
      } as JsonMap);
    }
    return array;
  }

  private decodeTracks(value: Object | undefined): Track[] {
    if (value === undefined || value === null || !Array.isArray(value)) {
      return [];
    }
    const array = value as Object[];
    const result: Track[] = [];
    for (let index = 0; index < array.length; index++) {
      const map = asMap(array[index]);
      if (map === null) {
        continue;
      }
      result.push({
        index: index,
        id: str(map, 'id'),
        title: str(map, 'title'),
        artist: str(map, 'artist'),
        album: str(map, 'album'),
        albumId: str(map, 'albumId'),
        duration: num(map, 'duration', 0),
        coverId: str(map, 'coverId'),
        type: str(map, 'type'),
        sampleRate: str(map, 'sampleRate'),
        sampleSize: str(map, 'sampleSize'),
        bitRate: str(map, 'bitRate'),
        trackNum: Math.trunc(num(map, 'trackNum', 0)),
        disc: Math.trunc(num(map, 'disc', 0)),
        url: str(map, 'url'),
        lossless: bool(map, 'lossless', false),
        channels: Math.trunc(num(map, 'channels', 0))
      });
    }
    return result;
  }
}
