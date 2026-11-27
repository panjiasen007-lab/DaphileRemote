import { util } from '@kit.ArkTS';
import {
  Album, AppSettings, Artist, BrowserItem, ConnectionState, CustomPlaylist,
  DailyRecommendation, DiscoveredServer, Genre, HostPowerAction, LmsError,
  LmsException, Lyrics, LyricsSource, NowPlaying, Paged, Player, PlaylistSummary,
  PluginEntry, ScanStatus, SearchResults, ServerConfig, ServerInfo, SleepAction,
  Track, WakeState, YearItem, cloneNowPlaying, defaultAppSettings,
  emptyNowPlaying, friendlyErrorMessage, nowPlayingIsPlaying
} from '../model/Models';
import { LmsClient } from '../remote/LmsClient';
import { LanDiscovery } from '../remote/LanDiscovery';
import { JsonMap, LmsParser, readMap } from '../remote/LmsParser';
import { WolClient } from '../remote/WolClient';
import { SettingsStore } from '../local/SettingsStore';
import { TrackDisplay } from '../model/TrackDisplay';
import { OnlineMetadataService } from '../online/OnlineMetadataService';
import { ThemeRuntime } from '../../ui/theme/ThemeRuntime';
import { CardBridge } from '../../app/CardBridge';

function delay(ms: number): Promise<void> {
  return new Promise<void>((resolve) => {
    setTimeout(() => resolve(), ms);
  });
}

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

function int(map: JsonMap, key: string): number {
  const value = map[key];
  if (value === undefined || value === null) {
    return 0;
  }
  const parsed = typeof value === 'number' ? value as number : Number.parseFloat(value.toString());
  return Number.isNaN(parsed) ? 0 : Math.trunc(parsed);
}

function todayKey(): string {
  const now = new Date();
  const month = (now.getMonth() + 1).toString().padStart(2, '0');
  const day = now.getDate().toString().padStart(2, '0');
  return `${now.getFullYear()}-${month}-${day}`;
}

@ObservedV2
export class RepoState {
  @Trace connection: ConnectionState = ConnectionState.IDLE;
  @Trace servers: ServerConfig[] = [];
  @Trace server: ServerConfig | null = null;
  @Trace players: Player[] = [];
  @Trace currentPlayerId: string = '';
  @Trace nowPlaying: NowPlaying | null = null;
  @Trace serverInfo: ServerInfo | null = null;
  @Trace lastError: string = '';
  @Trace settings: AppSettings = defaultAppSettings();
  @Trace customPlaylists: CustomPlaylist[] = [];
  @Trace dailyRecommendation: DailyRecommendation | null = null;
  @Trace sleepRemaining: number = -1;
  @Trace scanStatus: ScanStatus | null = null;
  @Trace wakeState: WakeState = WakeState.IDLE;
}

/**
 * 业务中枢：轮询、状态机、LMS 命令编排。
 * 与 Android 版 DaphileRepository 一一对应。
 */
export class DaphileRepository {
  readonly state: RepoState = new RepoState();

  private store: SettingsStore;
  private client: LmsClient;
  private online: OnlineMetadataService;
  private sessionToken: number = 0;
  private sleepToken: number = 0;
  private scanToken: number = 0;
  private started: boolean = false;

  constructor(store: SettingsStore, client: LmsClient, online: OnlineMetadataService) {
    this.store = store;
    this.client = client;
    this.online = online;
  }

  async start(): Promise<void> {
    if (this.started) {
      return;
    }
    this.started = true;
    this.state.settings = await this.store.loadSettings();
    TrackDisplay.configure(this.state.settings);
    ThemeRuntime.instance.apply(this.state.settings.theme);
    this.state.servers = await this.store.loadServers();
    this.state.customPlaylists = await this.store.loadCustomPlaylists();
    this.state.dailyRecommendation = await this.store.loadDailyRecommendation();
    if (this.state.servers.length === 0) {
      this.state.connection = ConnectionState.IDLE;
      return;
    }
    const currentId = await this.store.loadCurrentServerId();
    let server: ServerConfig = this.state.servers[0];
    for (const item of this.state.servers) {
      if (item.id === currentId) {
        server = item;
        break;
      }
    }
    this.startSession(server);
  }

  retryConnection(): void {
    if (this.state.server !== null) {
      this.startSession(this.state.server);
    }
  }

  /** 处理桌面卡片按钮转发的指令。 */
  private async applyCardCommand(command: string): Promise<void> {
    try {
      if (command === 'playPause') {
        await this.playPause();
      } else if (command === 'next') {
        await this.next();
      } else if (command === 'previous') {
        await this.previous();
      }
    } catch (error) {
      this.state.lastError = friendlyErrorMessage(error as Error);
    }
  }

  private startSession(server: ServerConfig): void {
    this.sessionToken++;
    const token = this.sessionToken;
    this.state.server = server;
    this.state.connection = ConnectionState.CONNECTING;
    this.state.serverInfo = null;
    this.state.nowPlaying = null;
    this.state.players = [];
    this.state.currentPlayerId = '';
    this.state.lastError = '';
    this.pollLoop(server, token).catch((error: Error) => {
      this.state.lastError = friendlyErrorMessage(error);
    });
  }

  private async pollLoop(server: ServerConfig, token: number): Promise<void> {
    let failures = 0;
    let disconnectStartedAt = 0;
    let lastPlayersRefreshAt = 0;
    let lastQueueTimestamp = '';
    while (this.sessionToken === token) {
      try {
        const cardCommand = CardBridge.takeCommand();
        if (cardCommand.length > 0) {
          await this.applyCardCommand(cardCommand);
        }
        if (failures > 0 || this.state.serverInfo === null) {
          this.state.connection = failures === 0
            ? ConnectionState.CONNECTING : ConnectionState.RECONNECTING;
          this.state.serverInfo = await this.client.serverStatus(server);
        }

        const now = Date.now();
        if (this.state.players.length === 0 || now - lastPlayersRefreshAt >= 30000) {
          await this.refreshPlayers(server);
          lastPlayersRefreshAt = now;
        }

        const playerId = this.state.currentPlayerId;
        if (playerId.length > 0) {
          const quick = await this.client.quickStatus(server, playerId);
          const timestamp = str(quick, 'playlist_timestamp');
          const trackCount = int(quick, 'playlist_tracks');
          const existing = this.state.nowPlaying;
          const quickCurrentId = this.quickCurrentId(quick);
          const needsQueue = existing === null ||
            existing.queue.length === 0 ||
            existing.trackCount !== trackCount ||
            timestamp !== lastQueueTimestamp ||
            (quickCurrentId.length > 0 && existing.track !== null &&
              existing.track.id.length > 0 && quickCurrentId !== existing.track.id);

          let queue: Track[] = existing !== null ? existing.queue : [];
          if (needsQueue && trackCount > 0) {
            const queueResult = await this.client.queueStatus(server, playerId, Math.min(trackCount, 1000));
            queue = LmsParser.parseTracks(queueResult).items;
          }
          if (needsQueue) {
            lastQueueTimestamp = timestamp;
          }
          this.state.nowPlaying = LmsParser.parseNowPlaying(playerId, quick, queue);
        } else {
          this.state.nowPlaying = null;
        }

        this.state.connection = ConnectionState.CONNECTED;
        this.state.lastError = '';
        failures = 0;
        disconnectStartedAt = 0;

        const interval = Math.min(30, Math.max(2, this.state.settings.pollSeconds));
        await delay(interval * 1000);
      } catch (error) {
        failures++;
        if (disconnectStartedAt === 0) {
          disconnectStartedAt = Date.now();
        }
        const disconnectedFor = Date.now() - disconnectStartedAt;
        this.state.connection = disconnectedFor >= 300000
          ? ConnectionState.LOST : ConnectionState.RECONNECTING;
        this.state.lastError = friendlyErrorMessage(error as Error);
        this.state.serverInfo = null;
        lastPlayersRefreshAt = 0;
        if (this.state.settings.autoWake) {
          this.autoWakeIfNeeded(server, disconnectedFor);
        }
        await delay(Math.min(60000, 1000 * Math.pow(2, Math.min(failures, 6))));
      }
    }
  }

  private quickCurrentId(quick: JsonMap): string {
    const loop = quick['playlist_loop'];
    if (loop === undefined || loop === null || !Array.isArray(loop)) {
      return '';
    }
    const array = loop as Object[];
    if (array.length === 0) {
      return '';
    }
    const first = asMap(array[0]);
    return first === null ? '' : str(first, 'id');
  }

  private autoWakeIfNeeded(server: ServerConfig, disconnectedFor: number): void {
    if (disconnectedFor < 10000 || server.macAddress.length === 0) {
      return;
    }
    if (this.state.wakeState === WakeState.WAKING) {
      return;
    }
    this.wakeHost().catch(() => {
      // 自动唤醒失败由状态提示
    });
  }

  private async refreshPlayers(server: ServerConfig): Promise<void> {
    const basic = await this.client.players(server);
    const jobs: Promise<Player>[] = [];
    for (const player of basic) {
      jobs.push(this.client.playerBrief(server, player).catch((error: Error) => {
        player.connected = false;
        return player;
      }));
    }
    const detailed = await Promise.all(jobs);
    this.state.players = detailed;

    const saved = await this.store.playerForServer(server.id);
    let selected = '';
    for (const player of detailed) {
      if (player.id === this.state.currentPlayerId) {
        selected = player.id;
        break;
      }
    }
    if (selected.length === 0) {
      for (const player of detailed) {
        if (player.id === saved) {
          selected = player.id;
          break;
        }
      }
    }
    if (selected.length === 0) {
      for (const player of detailed) {
        if (player.connected) {
          selected = player.id;
          break;
        }
      }
    }
    if (selected.length === 0 && detailed.length > 0) {
      selected = detailed[0].id;
    }
    if (selected !== this.state.currentPlayerId) {
      this.state.currentPlayerId = selected;
      if (selected.length > 0) {
        await this.store.setPlayerForServer(server.id, selected);
      }
    }
  }

  private async refreshPlayerState(server: ServerConfig, playerId: string): Promise<void> {
    const quick = await this.client.quickStatus(server, playerId);
    const existing = this.state.nowPlaying;
    const now = LmsParser.parseNowPlaying(playerId, quick, existing !== null ? existing.queue : []);
    this.state.nowPlaying = now;
    this.updatePlayerFromStatus(playerId, quick);
  }

  private async refreshFullState(server: ServerConfig, playerId: string): Promise<void> {
    const quick = await this.client.quickStatus(server, playerId);
    const trackCount = int(quick, 'playlist_tracks');
    let queue: Track[] = [];
    if (trackCount > 0) {
      queue = LmsParser.parseTracks(
        await this.client.queueStatus(server, playerId, Math.min(trackCount, 1000))
      ).items;
    }
    this.state.nowPlaying = LmsParser.parseNowPlaying(playerId, quick, queue);
    this.updatePlayerFromStatus(playerId, quick);
  }

  private updatePlayerFromStatus(playerId: string, quick: JsonMap): void {
    const players: Player[] = [];
    for (const player of this.state.players) {
      players.push(player.id === playerId ? LmsParser.parsePlayerStatus(player, quick) : player);
    }
    this.state.players = players;
  }

  private async refreshAfterCommand(queueChanged: boolean = false): Promise<void> {
    if (this.state.server === null || this.state.currentPlayerId.length === 0) {
      return;
    }
    await delay(140);
    try {
      if (queueChanged) {
        await this.refreshFullState(this.state.server, this.state.currentPlayerId);
      } else {
        await this.refreshPlayerState(this.state.server, this.state.currentPlayerId);
      }
    } catch (error) {
      // 命令已发出，刷新失败不覆盖 UI
    }
  }

  private async sendCurrent(command: string[]): Promise<void> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    if (this.state.currentPlayerId.length === 0) {
      throw new LmsException(LmsError.PLAYER, '没有可用播放器');
    }
    await this.client.command(this.state.server, this.state.currentPlayerId, command);
  }

  private async sendTo(playerId: string, command: string[]): Promise<void> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    await this.client.command(this.state.server, playerId, command);
  }

  selectPlayer(playerId: string): void {
    const server = this.state.server;
    if (server === null) {
      return;
    }
    this.state.currentPlayerId = playerId;
    this.state.nowPlaying = null;
    this.store.setPlayerForServer(server.id, playerId).catch(() => {
    });
    this.refreshPlayerState(server, playerId).catch(() => {
    });
  }

  async play(): Promise<void> {
    const state = this.state.nowPlaying;
    if (state === null) {
      throw new LmsException(LmsError.PLAYER, '没有正在播放的内容');
    }
    if (!state.power) {
      await this.setPlayerPower(state.playerId, true);
      return;
    }
    if (!nowPlayingIsPlaying(state)) {
      const optimistic = cloneNowPlaying(state);
      optimistic.mode = 'play';
      this.state.nowPlaying = optimistic;
      if (state.mode === 'pause') {
        await this.sendCurrent(['pause', '0']);
      } else {
        await this.sendCurrent(['play']);
      }
      await this.refreshAfterCommand();
    }
  }

  async pause(): Promise<void> {
    const state = this.state.nowPlaying;
    if (state === null) {
      throw new LmsException(LmsError.PLAYER, '没有正在播放的内容');
    }
    if (nowPlayingIsPlaying(state)) {
      const optimistic = cloneNowPlaying(state);
      optimistic.mode = 'pause';
      this.state.nowPlaying = optimistic;
      await this.sendCurrent(['pause', '1']);
      await this.refreshAfterCommand();
    }
  }

  async playPause(): Promise<void> {
    const state = this.state.nowPlaying;
    if (state === null) {
      throw new LmsException(LmsError.PLAYER, '没有正在播放的内容');
    }
    if (!state.power) {
      await this.setPlayerPower(state.playerId, true);
      return;
    }
    const optimistic = cloneNowPlaying(state);
    if (state.mode === 'play') {
      optimistic.mode = 'pause';
      await this.sendCurrent(['pause', '1']);
    } else if (state.mode === 'pause') {
      optimistic.mode = 'play';
      await this.sendCurrent(['pause', '0']);
    } else {
      optimistic.mode = 'play';
      await this.sendCurrent(['play']);
    }
    this.state.nowPlaying = optimistic;
    await this.refreshAfterCommand();
  }

  async next(): Promise<void> {
    await this.sendCurrent(['playlist', 'jump', '+1']);
    await this.refreshAfterCommand();
  }

  async previous(): Promise<void> {
    await this.sendCurrent(['playlist', 'jump', '-1']);
    await this.refreshAfterCommand();
  }

  async stop(): Promise<void> {
    await this.sendCurrent(['stop']);
    await this.refreshAfterCommand();
  }

  async seekTo(seconds: number): Promise<void> {
    const state = this.state.nowPlaying;
    if (state === null) {
      return;
    }
    if (!state.canSeek) {
      throw new LmsException(LmsError.UNSUPPORTED, '当前音源不支持定位');
    }
    const optimistic = cloneNowPlaying(state);
    optimistic.time = Math.min(state.duration, Math.max(0, seconds));
    this.state.nowPlaying = optimistic;
    await this.sendCurrent(['time', Math.trunc(optimistic.time).toString()]);
    await this.refreshAfterCommand();
  }

  async setVolume(value: number): Promise<void> {
    const volume = Math.min(100, Math.max(0, Math.trunc(value)));
    const state = this.state.nowPlaying;
    if (state !== null) {
      const optimistic = cloneNowPlaying(state);
      optimistic.volume = volume;
      this.state.nowPlaying = optimistic;
    }
    const players: Player[] = [];
    for (const player of this.state.players) {
      if (player.id === this.state.currentPlayerId) {
        const copy = player;
        copy.volume = volume;
        players.push(copy);
      } else {
        players.push(player);
      }
    }
    this.state.players = players;
    await this.sendCurrent(['mixer', 'volume', volume.toString()]);
    await this.refreshAfterCommand();
  }

  async setPlayerVolume(playerId: string, value: number): Promise<void> {
    const volume = Math.min(100, Math.max(0, Math.trunc(value)));
    const players: Player[] = [];
    for (const player of this.state.players) {
      if (player.id === playerId) {
        const copy = player;
        copy.volume = volume;
        players.push(copy);
      } else {
        players.push(player);
      }
    }
    this.state.players = players;
    if (playerId === this.state.currentPlayerId && this.state.nowPlaying !== null) {
      const optimistic = cloneNowPlaying(this.state.nowPlaying);
      optimistic.volume = volume;
      this.state.nowPlaying = optimistic;
    }
    await this.sendTo(playerId, ['mixer', 'volume', volume.toString()]);
  }

  async toggleMute(): Promise<void> {
    await this.sendCurrent(['mixer', 'muting', 'toggle']);
    await this.refreshAfterCommand();
  }

  async setRepeat(value: number): Promise<void> {
    const repeat = Math.min(2, Math.max(0, Math.trunc(value)));
    if (this.state.nowPlaying !== null) {
      const optimistic = cloneNowPlaying(this.state.nowPlaying);
      optimistic.repeat = repeat;
      this.state.nowPlaying = optimistic;
    }
    await this.sendCurrent(['playlist', 'repeat', repeat.toString()]);
    await this.refreshAfterCommand();
  }

  async setShuffle(enabled: boolean): Promise<void> {
    const shuffle = enabled ? 1 : 0;
    if (this.state.nowPlaying !== null) {
      const optimistic = cloneNowPlaying(this.state.nowPlaying);
      optimistic.shuffle = shuffle;
      this.state.nowPlaying = optimistic;
    }
    await this.sendCurrent(['playlist', 'shuffle', shuffle.toString()]);
    await this.refreshAfterCommand();
  }

  async setPlayerPower(playerId: string, on: boolean): Promise<void> {
    await this.sendTo(playerId, ['power', on ? '1' : '0']);
    const players: Player[] = [];
    for (const player of this.state.players) {
      if (player.id === playerId) {
        const copy = player;
        copy.power = on;
        if (!on) {
          copy.mode = 'stop';
        }
        players.push(copy);
      } else {
        players.push(player);
      }
    }
    this.state.players = players;
    if (playerId === this.state.currentPlayerId && this.state.nowPlaying !== null) {
      const optimistic = cloneNowPlaying(this.state.nowPlaying);
      optimistic.power = on;
      if (!on) {
        optimistic.mode = 'stop';
      }
      this.state.nowPlaying = optimistic;
    }
  }

  async syncPlayerTo(playerId: string, masterId: string): Promise<void> {
    await this.sendTo(playerId, ['sync', masterId]);
  }

  /** 直接切换指定播放器的播放/暂停，不影响当前选中播放器。 */
  async togglePlayer(playerId: string): Promise<void> {
    await this.sendTo(playerId, ['pause']);
  }

  async unsyncPlayer(playerId: string): Promise<void> {
    await this.sendTo(playerId, ['sync', '-']);
  }

  async playQueueIndex(index: number): Promise<void> {
    const state = this.state.nowPlaying;
    if (state !== null) {
      const optimistic = cloneNowPlaying(state);
      optimistic.currentIndex = index;
      for (const track of state.queue) {
        if (track.index === index) {
          optimistic.track = track;
          break;
        }
      }
      optimistic.mode = 'play';
      this.state.nowPlaying = optimistic;
    }
    await this.sendCurrent(['playlist', 'index', index.toString()]);
    await this.refreshAfterCommand(true);
  }

  async removeQueueItem(index: number): Promise<void> {
    await this.sendCurrent(['playlist', 'delete', index.toString()]);
    await this.refreshAfterCommand(true);
  }

  async clearQueue(): Promise<void> {
    await this.sendCurrent(['playlist', 'clear']);
    if (this.state.nowPlaying !== null) {
      const optimistic = cloneNowPlaying(this.state.nowPlaying);
      optimistic.queue = [];
      optimistic.trackCount = 0;
      optimistic.track = null;
      optimistic.mode = 'stop';
      this.state.nowPlaying = optimistic;
    }
    await this.refreshAfterCommand(true);
  }

  async saveQueue(name: string): Promise<void> {
    const clean = name.trim();
    if (clean.length === 0) {
      throw new LmsException(LmsError.SERVER, '播放列表名称不能为空');
    }
    await this.sendCurrent(['playlist', 'save', clean]);
  }

  // ---------- 本机自定义歌单 ----------

  private trackKey(track: Track): string {
    return track.id.length > 0 ? track.id : track.url;
  }

  async createCustomPlaylist(name: string): Promise<CustomPlaylist> {
    const clean = name.trim();
    if (clean.length === 0) {
      throw new LmsException(LmsError.SERVER, '歌单名称不能为空');
    }
    const now = Date.now();
    const playlist: CustomPlaylist = {
      id: util.generateRandomUUID(true),
      name: clean,
      tracks: [],
      createdAt: now,
      updatedAt: now
    };
    const updated = this.state.customPlaylists.slice();
    updated.push(playlist);
    await this.store.saveCustomPlaylists(updated);
    this.state.customPlaylists = updated;
    return playlist;
  }

  async renameCustomPlaylist(playlistId: string, name: string): Promise<void> {
    const clean = name.trim();
    if (clean.length === 0) {
      throw new LmsException(LmsError.SERVER, '歌单名称不能为空');
    }
    const updated: CustomPlaylist[] = [];
    for (const playlist of this.state.customPlaylists) {
      if (playlist.id === playlistId) {
        const copy = playlist;
        copy.name = clean;
        copy.updatedAt = Date.now();
        updated.push(copy);
      } else {
        updated.push(playlist);
      }
    }
    await this.store.saveCustomPlaylists(updated);
    this.state.customPlaylists = updated;
  }

  async deleteCustomPlaylist(playlistId: string): Promise<void> {
    const updated: CustomPlaylist[] = [];
    for (const playlist of this.state.customPlaylists) {
      if (playlist.id !== playlistId) {
        updated.push(playlist);
      }
    }
    await this.store.saveCustomPlaylists(updated);
    this.state.customPlaylists = updated;
  }

  async addTracksToCustomPlaylist(playlistId: string, tracks: Track[]): Promise<number> {
    if (tracks.length === 0) {
      return 0;
    }
    let added = 0;
    const updated: CustomPlaylist[] = [];
    for (const playlist of this.state.customPlaylists) {
      if (playlist.id !== playlistId) {
        updated.push(playlist);
        continue;
      }
      const existing: Set<string> = new Set<string>();
      for (const track of playlist.tracks) {
        existing.add(this.trackKey(track));
      }
      const additions: Track[] = [];
      for (const track of tracks) {
        const key = this.trackKey(track);
        if (key.length > 0 && !existing.has(key)) {
          existing.add(key);
          additions.push(track);
        }
      }
      added = additions.length;
      const copy = playlist;
      copy.tracks = playlist.tracks.concat(additions);
      copy.updatedAt = Date.now();
      updated.push(copy);
    }
    await this.store.saveCustomPlaylists(updated);
    this.state.customPlaylists = updated;
    return added;
  }

  async removeTrackFromCustomPlaylist(playlistId: string, track: Track): Promise<void> {
    const key = this.trackKey(track);
    const updated: CustomPlaylist[] = [];
    for (const playlist of this.state.customPlaylists) {
      if (playlist.id !== playlistId) {
        updated.push(playlist);
        continue;
      }
      const tracks: Track[] = [];
      for (const item of playlist.tracks) {
        if (this.trackKey(item) !== key) {
          tracks.push(item);
        }
      }
      const copy = playlist;
      copy.tracks = tracks;
      copy.updatedAt = Date.now();
      updated.push(copy);
    }
    await this.store.saveCustomPlaylists(updated);
    this.state.customPlaylists = updated;
  }

  async saveTracksAsCustomPlaylist(name: string, tracks: Track[]): Promise<CustomPlaylist> {
    const clean = name.trim();
    if (clean.length === 0) {
      throw new LmsException(LmsError.SERVER, '歌单名称不能为空');
    }
    if (tracks.length === 0) {
      throw new LmsException(LmsError.EMPTY, '队列为空');
    }
    const now = Date.now();
    const seen: Set<string> = new Set<string>();
    const unique: Track[] = [];
    for (const track of tracks) {
      const key = this.trackKey(track);
      if (!seen.has(key)) {
        seen.add(key);
        unique.push(track);
      }
    }
    const playlist: CustomPlaylist = {
      id: util.generateRandomUUID(true),
      name: clean,
      tracks: unique,
      createdAt: now,
      updatedAt: now
    };
    const updated = this.state.customPlaylists.slice();
    updated.push(playlist);
    await this.store.saveCustomPlaylists(updated);
    this.state.customPlaylists = updated;
    return playlist;
  }

  async saveQueueAsCustomPlaylist(name: string): Promise<CustomPlaylist> {
    return this.saveTracksAsCustomPlaylist(name, this.state.nowPlaying !== null ? this.state.nowPlaying.queue : []);
  }

  private async enqueueTrack(track: Track): Promise<void> {
    if (track.id.length > 0 && !Number.isNaN(Number.parseInt(track.id))) {
      await this.sendCurrent(['playlistcontrol', 'cmd:add', `track_id:${track.id}`]);
    } else if (track.url.length > 0) {
      await this.sendCurrent(['playlist', 'add', track.url, track.title.length > 0 ? track.title : '曲目']);
    }
  }

  async playCustomPlaylist(playlist: CustomPlaylist, append: boolean = false): Promise<void> {
    const tracks: Track[] = [];
    for (const track of playlist.tracks) {
      if ((track.id.length > 0 && !Number.isNaN(Number.parseInt(track.id))) || track.url.length > 0) {
        tracks.push(track);
      }
    }
    if (tracks.length === 0) {
      throw new LmsException(LmsError.EMPTY, '歌单为空');
    }
    if (!append) {
      await this.sendCurrent(['playlist', 'clear']);
    }
    for (const track of tracks) {
      await this.enqueueTrack(track);
    }
    if (!append) {
      await this.sendCurrent(['play']);
    }
    await this.refreshAfterCommand(true);
  }

  async queueRandomTracks(count: number): Promise<number> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    const tracks = await this.client.randomTracks(this.state.server, Math.min(200, Math.max(1, count)));
    if (tracks.length === 0) {
      throw new LmsException(LmsError.EMPTY, '音乐库为空');
    }
    for (const track of tracks) {
      await this.enqueueTrack(track);
    }
    await this.refreshAfterCommand(true);
    return tracks.length;
  }

  async queueAllSongs(play: boolean): Promise<void> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    const roots = await this.client.musicFolder(this.state.server, '', 0, 50);
    const folders: BrowserItem[] = [];
    for (const item of roots.items) {
      if (item.hasItems || item.type === 'folder') {
        folders.push(item);
      }
    }
    if (folders.length === 0) {
      throw new LmsException(LmsError.EMPTY, '音乐库为空');
    }
    for (let index = 0; index < folders.length; index++) {
      await this.sendCurrent([
        'playlistcontrol',
        index === 0 ? 'cmd:load' : 'cmd:add',
        `folder_id:${folders[index].id}`
      ]);
    }
    if (play) {
      await this.sendCurrent(['play']);
    }
    await this.refreshAfterCommand(true);
  }

  async favoritesToQueue(play: boolean): Promise<void> {
    await this.sendCurrent(['favorites', 'playlist', play ? 'play' : 'add']);
    if (play) {
      await this.sendCurrent(['play']);
    }
    await this.refreshAfterCommand(true);
  }

  async refreshDailyRecommendation(force: boolean = false): Promise<DailyRecommendation> {
    const today = todayKey();
    const cached = this.state.dailyRecommendation;
    if (!force && cached !== null && cached.date === today && cached.tracks.length > 0) {
      return cached;
    }
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    const tracks = await this.client.randomTracks(this.state.server, 20);
    if (tracks.length === 0) {
      throw new LmsException(LmsError.EMPTY, '音乐库为空');
    }
    const recommendation: DailyRecommendation = { date: today, tracks: tracks };
    await this.store.saveDailyRecommendation(recommendation);
    this.state.dailyRecommendation = recommendation;
    return recommendation;
  }

  async addCurrentFavorite(): Promise<void> {
    const state = this.state.nowPlaying;
    if (state === null || state.track === null) {
      throw new LmsException(LmsError.EMPTY, '没有正在播放的曲目');
    }
    if (state.track.url.length === 0) {
      throw new LmsException(LmsError.SERVER, '当前音源不支持收藏');
    }
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    await this.client.command(this.state.server, this.state.currentPlayerId, [
      'favorites', 'add', `url:${state.track.url}`, `title:${state.track.title}`
    ]);
  }

  async startSleepTimer(minutes: number, action: SleepAction): Promise<void> {
    const seconds = Math.min(24 * 60, Math.max(1, minutes)) * 60;
    await this.cancelSleepTimer();
    this.sleepToken++;
    const token = this.sleepToken;
    if (action === SleepAction.POWER_OFF) {
      try {
        await this.sendCurrent(['sleep', seconds.toString()]);
      } catch (error) {
        // 定时仍由本地兜底
      }
    }
    this.state.sleepRemaining = seconds;
    this.runSleepCountdown(token, action);
  }

  private runSleepCountdown(token: number, action: SleepAction): void {
    const tick = () => {
      if (token !== this.sleepToken) {
        return;
      }
      if (this.state.sleepRemaining <= 0) {
        this.state.sleepRemaining = -1;
        this.executeSleepAction(action);
        return;
      }
      this.state.sleepRemaining = this.state.sleepRemaining - 1;
      setTimeout(tick, 1000);
    };
    setTimeout(tick, 1000);
  }

  private executeSleepAction(action: SleepAction): void {
    const run = async () => {
      try {
        if (action === SleepAction.PAUSE) {
          await this.sendCurrent(['pause', '1']);
        } else if (action === SleepAction.STOP) {
          await this.sendCurrent(['stop']);
        } else {
          await this.sendCurrent(['power', '0']);
        }
        await this.refreshAfterCommand();
      } catch (error) {
        // 忽略
      }
    };
    run();
  }

  async cancelSleepTimer(): Promise<void> {
    this.sleepToken++;
    this.state.sleepRemaining = -1;
    try {
      await this.sendCurrent(['sleep', '0']);
    } catch (error) {
      // 未连接服务器时仅清除本地定时
    }
  }

  async playAlbum(albumId: string, append: boolean = false): Promise<void> {
    if (append) {
      await this.sendCurrent(['playlistcontrol', 'cmd:add', `album_id:${albumId}`]);
    } else {
      await this.sendCurrent(['playlistcontrol', 'cmd:load', `album_id:${albumId}`]);
      await this.sendCurrent(['play']);
    }
    await this.refreshAfterCommand(true);
  }

  async playTrack(trackId: string, append: boolean = false): Promise<void> {
    if (append) {
      await this.sendCurrent(['playlistcontrol', 'cmd:add', `track_id:${trackId}`]);
    } else {
      await this.sendCurrent(['playlistcontrol', 'cmd:load', `track_id:${trackId}`]);
      await this.sendCurrent(['play']);
    }
    await this.refreshAfterCommand(true);
  }

  async queueTrack(trackId: string): Promise<void> {
    await this.sendCurrent(['playlistcontrol', 'cmd:add', `track_id:${trackId}`]);
  }

  async playPlaylist(playlistId: string): Promise<void> {
    await this.sendCurrent(['playlistcontrol', 'cmd:load', `playlist_id:${playlistId}`]);
    await this.sendCurrent(['play']);
    await this.refreshAfterCommand(true);
  }

  async playArtist(artistId: string): Promise<void> {
    await this.sendCurrent(['playlistcontrol', 'cmd:load', `artist_id:${artistId}`]);
    await this.sendCurrent(['play']);
    await this.refreshAfterCommand(true);
  }

  async playFavorite(itemId: string): Promise<void> {
    await this.sendCurrent(['favorites', 'playlist', 'play', `item_id:${itemId}`]);
    await this.refreshAfterCommand(true);
  }

  async playBrowserItem(pluginCmd: string, item: BrowserItem): Promise<void> {
    if (item.url.length > 0 && (item.isAudio || item.url.startsWith('http'))) {
      await this.sendCurrent(['playlist', 'play', item.url, item.name]);
    } else if (pluginCmd.length > 0) {
      await this.sendCurrent([pluginCmd, 'playlist', 'play', `item_id:${item.id}`]);
    } else {
      await this.sendCurrent(['favorites', 'playlist', 'play', `item_id:${item.id}`]);
    }
    await this.refreshAfterCommand(true);
  }

  async queueBrowserItem(pluginCmd: string, item: BrowserItem): Promise<void> {
    if (item.url.length > 0 && (item.isAudio || item.url.startsWith('http'))) {
      await this.sendCurrent(['playlist', 'add', item.url, item.name]);
    } else if (pluginCmd.length > 0) {
      await this.sendCurrent([pluginCmd, 'playlist', 'add', `item_id:${item.id}`]);
    } else {
      await this.sendCurrent(['favorites', 'playlist', 'add', `item_id:${item.id}`]);
    }
    await this.refreshAfterCommand(true);
  }

  async randomAlbum(): Promise<void> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    const album = await this.client.randomAlbum(this.state.server);
    if (album === null) {
      throw new LmsException(LmsError.EMPTY, '音乐库为空');
    }
    await this.playAlbum(album.id);
  }

  async hostPower(action: HostPowerAction): Promise<void> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    await this.client.hostAction(this.state.server, action);
  }

  async wakeHost(timeoutMs: number = 90000): Promise<void> {
    const server = this.state.server;
    if (server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    if (server.macAddress.length === 0) {
      throw new LmsException(LmsError.NETWORK, '请先在服务器设置中填写 MAC 地址');
    }
    this.state.wakeState = WakeState.WAKING;
    const sent = await WolClient.wake(server.macAddress, server.host);
    if (!sent) {
      this.state.wakeState = WakeState.FAILED;
      throw new LmsException(LmsError.NETWORK, '唤醒包发送失败，请确认手机与主机在同一局域网');
    }
    const deadline = Date.now() + timeoutMs;
    while (Date.now() < deadline) {
      await delay(2000);
      try {
        await this.client.players(server);
        this.state.wakeState = WakeState.ONLINE;
        this.startSession(server);
        return;
      } catch (error) {
        // 继续等待
      }
    }
    this.state.wakeState = WakeState.FAILED;
    throw new LmsException(LmsError.NETWORK, '主机未在 90 秒内响应，请确认主机 BIOS 与网卡已启用网络唤醒');
  }

  async startScan(full: boolean): Promise<void> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    await this.client.startScan(this.state.server, full);
    this.state.scanStatus = {
      scanning: true,
      message: full ? '正在重建音乐库…' : '正在扫描更新…',
      percent: -1
    };
    this.scanToken++;
    const token = this.scanToken;
    this.scanLoop(this.state.server, token);
  }

  private scanLoop(server: ServerConfig, token: number): void {
    const tick = async () => {
      if (token !== this.scanToken) {
        return;
      }
      await delay(2000);
      if (token !== this.scanToken) {
        return;
      }
      try {
        const status = await this.client.scanProgress(server);
        this.state.scanStatus = status;
        if (!status.scanning) {
          this.state.serverInfo = await this.client.serverStatus(server);
          await delay(4000);
          this.state.scanStatus = null;
          return;
        }
      } catch (error) {
        this.state.scanStatus = { scanning: false, message: '无法获取扫描状态', percent: -1 };
        return;
      }
      tick();
    };
    tick();
  }

  async refreshServerInfo(): Promise<void> {
    if (this.state.server === null) {
      return;
    }
    this.state.serverInfo = await this.client.serverStatus(this.state.server);
  }

  // ---------- 曲库 ----------

  async songs(start: number, count: number): Promise<Paged<Track>> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.songs(this.state.server, start, count);
  }

  async albums(start: number, count: number, sort: string,
    artistId: string = '', genreId: string = '', year: string = ''): Promise<Paged<Album>> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.albums(this.state.server, start, count, sort, artistId, genreId, year);
  }

  async artists(start: number, count: number): Promise<Paged<Artist>> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.artists(this.state.server, start, count);
  }

  async genres(start: number, count: number): Promise<Paged<Genre>> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.genres(this.state.server, start, count);
  }

  async years(start: number, count: number): Promise<Paged<YearItem>> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.years(this.state.server, start, count);
  }

  async playlists(start: number, count: number): Promise<Paged<PlaylistSummary>> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.playlists(this.state.server, start, count);
  }

  async albumDetails(albumId: string): Promise<Album> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    const tracks = await this.client.albumTracks(this.state.server, albumId);
    const info = await this.client.albumInfo(this.state.server, albumId);
    if (info !== null) {
      return info;
    }
    const first = tracks.items.length > 0 ? tracks.items[0] : null;
    return {
      id: albumId,
      title: first !== null && first.album.length > 0 ? first.album : '专辑',
      artist: first !== null ? first.artist : '',
      year: '',
      artworkId: first !== null ? first.coverId : '',
      trackCount: tracks.items.length
    };
  }

  async albumTracks(albumId: string): Promise<Track[]> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return (await this.client.albumTracks(this.state.server, albumId)).items;
  }

  async playlistTracks(playlistId: string): Promise<Track[]> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return (await this.client.playlistTracks(this.state.server, playlistId)).items;
  }

  async search(query: string): Promise<SearchResults> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.search(this.state.server, query);
  }

  async favorites(parentId: string = ''): Promise<BrowserItem[]> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.favorites(this.state.server, parentId, this.state.currentPlayerId);
  }

  async radios(): Promise<PluginEntry[]> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.radios(this.state.server);
  }

  async apps(): Promise<PluginEntry[]> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.apps(this.state.server);
  }

  async pluginItems(pluginCmd: string, parentId: string): Promise<BrowserItem[]> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.pluginItems(this.state.server, this.state.currentPlayerId, pluginCmd, parentId);
  }

  async musicFolders(parentId: string, start: number, count: number): Promise<Paged<BrowserItem>> {
    if (this.state.server === null) {
      throw new LmsException(LmsError.NETWORK, '尚未连接服务器');
    }
    return this.client.musicFolder(this.state.server, parentId, start, count);
  }

  async playFolder(folderId: string): Promise<void> {
    await this.sendCurrent(['playlistcontrol', 'cmd:load', `folder_id:${folderId}`]);
    await this.sendCurrent(['play']);
    await this.refreshAfterCommand(true);
  }

  async playFolderItem(item: BrowserItem): Promise<void> {
    if (item.hasItems || item.type === 'folder') {
      await this.playFolder(item.id);
    } else if (item.id.length > 0 && !Number.isNaN(Number.parseInt(item.id))) {
      await this.sendCurrent(['playlistcontrol', 'cmd:load', `track_id:${item.id}`]);
      await this.sendCurrent(['play']);
      await this.refreshAfterCommand(true);
    } else if (item.url.length > 0) {
      await this.sendCurrent(['playlist', 'play', item.url, item.name]);
      await this.refreshAfterCommand(true);
    }
  }

  async queueFolderItem(item: BrowserItem): Promise<void> {
    if (item.id.length > 0 && !Number.isNaN(Number.parseInt(item.id))) {
      await this.sendCurrent(['playlistcontrol', 'cmd:add', `track_id:${item.id}`]);
    }
  }

  // ---------- 歌词与封面 ----------

  async lyricsForTrack(track: Track, source: LyricsSource,
    forceOnline: boolean = false): Promise<Lyrics | null> {
    let fallback: Lyrics | null = null;
    if (!forceOnline && source === LyricsSource.AUTO) {
      const saved = this.online.loadSavedLyrics(track);
      if (saved !== null) {
        if (saved.synced) {
          return saved;
        }
        fallback = saved;
      }
      if (this.state.server !== null && track.id.length > 0 &&
        !Number.isNaN(Number.parseInt(track.id))) {
        try {
          const info = await this.client.songInfo(this.state.server, track.id);
          const raw = info['lyrics'];
          if (raw !== undefined && raw !== null && raw.toString().length > 0) {
            const parsed = this.online.parseLyricsText(raw.toString(), 'Daphile 本地歌词');
            if (parsed !== null) {
              if (parsed.synced) {
                this.online.saveLyrics(track, parsed);
                return parsed;
              }
              if (fallback === null) {
                fallback = parsed;
              }
            }
          }
        } catch (error) {
          // 继续走在线源
        }
      }
    }
    if (!this.state.settings.onlineMetadataEnabled) {
      return fallback;
    }
    try {
      const onlineLyrics = source === LyricsSource.AUTO
        ? await this.online.lyricsFor(track, forceOnline)
        : await this.online.lyricsFrom(track, source);
      if (onlineLyrics !== null) {
        // 在线同步歌词优先于本地未同步歌词
        if (onlineLyrics.synced || fallback === null) {
          return onlineLyrics;
        }
        return fallback;
      }
      return fallback;
    } catch (error) {
      return fallback;
    }
  }

  savedLyricsPath(track: Track): string {
    return this.online.savedLyricsPath(track);
  }

  saveLyrics(track: Track, lyrics: Lyrics): string {
    return this.online.saveLyrics(track, lyrics);
  }

  lyricsLibraryPath(): string {
    return this.online.lyricsLibraryPath();
  }

  lyricsLibraryCount(): number {
    return this.online.lyricsLibraryCount();
  }

  clearLyricsLibrary(): number {
    return this.online.clearLyricsLibrary();
  }

  clearCoverCache(): number {
    return this.online.clearCoverCache();
  }

  async resolveOnlineCover(track: Track, force: boolean = false): Promise<string> {
    if (!this.state.settings.onlineMetadataEnabled) {
      return '';
    }
    try {
      return await this.online.coverFor(track.artist, track.album, force);
    } catch (error) {
      return '';
    }
  }

  // ---------- 服务器与设置 ----------

  async testServer(server: ServerConfig): Promise<ServerInfo> {
    return this.client.serverStatus(server);
  }

  async upsertServer(server: ServerConfig, makeCurrent: boolean = true): Promise<void> {
    const servers = await this.store.loadServers();
    let found = false;
    for (let i = 0; i < servers.length; i++) {
      if (servers[i].id === server.id) {
        servers[i] = server;
        found = true;
        break;
      }
    }
    if (!found) {
      servers.push(server);
    }
    await this.store.saveServers(servers);
    this.state.servers = servers;
    if (makeCurrent) {
      await this.store.setCurrentServerId(server.id);
      this.startSession(server);
    }
  }

  async deleteServer(serverId: string): Promise<void> {
    const servers: ServerConfig[] = [];
    for (const server of await this.store.loadServers()) {
      if (server.id !== serverId) {
        servers.push(server);
      }
    }
    await this.store.saveServers(servers);
    this.state.servers = servers;
    const currentId = await this.store.loadCurrentServerId();
    if (currentId === serverId) {
      if (servers.length > 0) {
        await this.store.setCurrentServerId(servers[0].id);
        this.startSession(servers[0]);
      } else {
        this.sessionToken++;
        this.state.server = null;
        this.state.connection = ConnectionState.IDLE;
        this.state.nowPlaying = null;
        this.state.players = [];
        this.state.currentPlayerId = '';
      }
    }
  }

  async switchServer(serverId: string): Promise<void> {
    for (const server of await this.store.loadServers()) {
      if (server.id === serverId) {
        await this.store.setCurrentServerId(serverId);
        this.startSession(server);
        return;
      }
    }
  }

  async setTheme(theme: string): Promise<void> {
    await this.store.setTheme(theme);
    await this.reloadSettings();
  }

  async setPollSeconds(seconds: number): Promise<void> {
    await this.store.setPollSeconds(seconds);
    await this.reloadSettings();
  }

  async setNotificationsEnabled(enabled: boolean): Promise<void> {
    await this.store.setNotificationsEnabled(enabled);
    await this.reloadSettings();
  }

  async setHostPowerEnabled(enabled: boolean): Promise<void> {
    await this.store.setHostPowerEnabled(enabled);
    await this.reloadSettings();
  }

  async setOnlineMetadataEnabled(enabled: boolean): Promise<void> {
    await this.store.setOnlineMetadataEnabled(enabled);
    await this.reloadSettings();
  }

  async setLockScreenControls(enabled: boolean): Promise<void> {
    await this.store.setLockScreenControls(enabled);
    await this.reloadSettings();
  }

  async setLyricsSource(source: LyricsSource): Promise<void> {
    await this.store.setLyricsSource(source);
    await this.reloadSettings();
  }

  async setLanguage(language: string): Promise<void> {
    await this.store.setLanguage(language);
    await this.reloadSettings();
  }

  async setLayoutMode(mode: string): Promise<void> {
    await this.store.setLayoutMode(mode);
    await this.reloadSettings();
  }

  async setAutoWake(enabled: boolean): Promise<void> {
    await this.store.setAutoWake(enabled);
    await this.reloadSettings();
  }

  async setUseFileNameAsTitle(enabled: boolean): Promise<void> {
    await this.store.setUseFileNameAsTitle(enabled);
    await this.reloadSettings();
  }

  async setFixGarbledText(enabled: boolean): Promise<void> {
    await this.store.setFixGarbledText(enabled);
    await this.reloadSettings();
  }

  private async reloadSettings(): Promise<void> {
    this.state.settings = await this.store.loadSettings();
    TrackDisplay.configure(this.state.settings);
    ThemeRuntime.instance.apply(this.state.settings.theme);
  }

  async discoverServers(): Promise<DiscoveredServer[]> {
    const hintHost = this.state.server !== null ? this.state.server.host : '';
    const hosts = await LanDiscovery.scan(hintHost);
    const results: DiscoveredServer[] = [];
    await Promise.all(hosts.map(async (host: string) => {
      const temp: ServerConfig = {
        id: `discovered-${host}`,
        name: host,
        host: host,
        port: 9000,
        daphilePort: 80,
        username: '',
        password: '',
        useHttps: false,
        macAddress: ''
      };
      try {
        const info = await this.client.serverStatus(temp);
        results.push({
          host: host,
          port: 9000,
          version: info.version,
          name: `Daphile (${host})`
        });
      } catch (error) {
        // 端口开放但不是 LMS 服务，忽略
      }
    }));
    results.sort((left: DiscoveredServer, right: DiscoveredServer) =>
      left.host.localeCompare(right.host));
    return results;
  }
}
