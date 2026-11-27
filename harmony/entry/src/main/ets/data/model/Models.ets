/**
 * 数据模型：与 Android 版字段保持一致，便于对照调试。
 */

export interface ServerConfig {
  id: string;
  name: string;
  host: string;
  port: number;
  daphilePort: number;
  username: string;
  password: string;
  useHttps: boolean;
  /** 网卡 MAC 地址，用于网络唤醒；为空表示未配置。 */
  macAddress: string;
}

export function serverBaseUrl(server: ServerConfig): string {
  const scheme: string = server.useHttps ? 'https' : 'http';
  return `${scheme}://${server.host}:${server.port}`;
}

export function daphileBaseUrl(server: ServerConfig): string {
  return `http://${server.host}:${server.daphilePort}`;
}

export function coverUrl(server: ServerConfig, coverId: string, playerId: string): string {
  if (coverId.length > 0) {
    return `${serverBaseUrl(server)}/music/${encodeURIComponent(coverId)}/cover.jpg`;
  }
  if (playerId.length > 0) {
    return `${serverBaseUrl(server)}/music/current/cover.jpg?player=${encodeURIComponent(playerId)}`;
  }
  return '';
}

export interface Player {
  id: string;
  name: string;
  model: string;
  connected: boolean;
  power: boolean;
  volume: number;
  muted: boolean;
  mode: string;
  currentTitle: string;
  currentArtist: string;
  canSetVolume: boolean;
  syncMaster: string;
  syncSlaves: string[];
}

export function playerIsPlaying(player: Player): boolean {
  return player.mode === 'play';
}

export interface Track {
  index: number;
  id: string;
  title: string;
  artist: string;
  album: string;
  albumId: string;
  duration: number;
  coverId: string;
  type: string;
  sampleRate: string;
  sampleSize: string;
  bitRate: string;
  trackNum: number;
  disc: number;
  url: string;
  lossless: boolean;
  channels: number;
}

export interface NowPlaying {
  playerId: string;
  power: boolean;
  mode: string;
  time: number;
  duration: number;
  volume: number;
  muted: boolean;
  repeat: number;
  shuffle: number;
  currentIndex: number;
  trackCount: number;
  track: Track | null;
  queue: Track[];
  canSeek: boolean;
  canSetVolume: boolean;
  playlistTimestamp: string;
}

export function nowPlayingIsPlaying(state: NowPlaying): boolean {
  return state.mode === 'play';
}

export function nowPlayingIsPaused(state: NowPlaying): boolean {
  return state.mode === 'pause';
}

export interface Album {
  id: string;
  title: string;
  artist: string;
  year: string;
  artworkId: string;
  trackCount: number;
}

export interface Artist {
  id: string;
  name: string;
}

export interface Genre {
  id: string;
  name: string;
}

export interface YearItem {
  year: string;
}

export interface PlaylistSummary {
  id: string;
  name: string;
}

/** 保存在本机的自定义歌单。 */
export interface CustomPlaylist {
  id: string;
  name: string;
  tracks: Track[];
  createdAt: number;
  updatedAt: number;
}

export interface DailyRecommendation {
  date: string;
  tracks: Track[];
}

export interface SearchResults {
  query: string;
  songs: Track[];
  albums: Album[];
  artists: Artist[];
  playlists: PlaylistSummary[];
}

export interface Paged<T> {
  total: number;
  items: T[];
  hasMore: boolean;
}

export interface ServerInfo {
  version: string;
  ip: string;
  httpPort: number;
  playerCount: number;
  albumCount: number;
  artistCount: number;
  genreCount: number;
  songCount: number;
  scanning: boolean;
  lastScan: string;
}

export interface BrowserItem {
  id: string;
  name: string;
  hasItems: boolean;
  type: string;
  url: string;
  isAudio: boolean;
  icon: string;
  description: string;
  filename: string;
  durationText: string;
  coverId: string;
}

export interface PluginEntry {
  cmd: string;
  name: string;
  type: string;
  icon: string;
}

export interface DiscoveredServer {
  host: string;
  port: number;
  version: string;
  name: string;
}

export interface AppSettings {
  theme: string;
  pollSeconds: number;
  notificationsEnabled: boolean;
  hostPowerEnabled: boolean;
  coverCacheMb: number;
  onlineMetadataEnabled: boolean;
  lockScreenControls: boolean;
  lyricsSource: string;
  language: string;
  layoutMode: string;
  autoWake: boolean;
  useFileNameAsTitle: boolean;
  fixGarbledText: boolean;
}

export function defaultAppSettings(): AppSettings {
  return {
    theme: 'dark',
    pollSeconds: 3,
    notificationsEnabled: true,
    hostPowerEnabled: true,
    coverCacheMb: 200,
    onlineMetadataEnabled: true,
    lockScreenControls: true,
    lyricsSource: 'auto',
    language: 'auto',
    layoutMode: 'auto',
    autoWake: false,
    useFileNameAsTitle: false,
    fixGarbledText: true
  };
}

export enum AppLanguage {
  AUTO = 'auto',
  ZH = 'zh',
  ZH_TW = 'zh-TW',
  EN = 'en',
  JA = 'ja',
  KO = 'ko',
  KO_KP = 'ko-KP',
  RU = 'ru',
  DE = 'de',
  FR = 'fr',
  ES = 'es',
  IT = 'it',
  PT = 'pt',
  NL = 'nl',
  EL = 'el',
  VI = 'vi',
  TH = 'th',
  AR = 'ar',
  AZ = 'az',
  FA = 'fa'
}

export function languageFromKey(key: string): AppLanguage {
  const values: AppLanguage[] = [
    AppLanguage.AUTO, AppLanguage.ZH, AppLanguage.ZH_TW, AppLanguage.EN,
    AppLanguage.JA, AppLanguage.KO, AppLanguage.KO_KP, AppLanguage.RU,
    AppLanguage.DE, AppLanguage.FR, AppLanguage.ES, AppLanguage.IT,
    AppLanguage.PT, AppLanguage.NL, AppLanguage.EL, AppLanguage.VI,
    AppLanguage.TH, AppLanguage.AR, AppLanguage.AZ, AppLanguage.FA
  ];
  for (const item of values) {
    if (item === key) {
      return item;
    }
  }
  return AppLanguage.AUTO;
}

export function languageLabel(language: AppLanguage): string {
  switch (language) {
    case AppLanguage.AUTO: return '跟随系统';
    case AppLanguage.ZH: return '简体中文';
    case AppLanguage.ZH_TW: return '繁體中文';
    case AppLanguage.EN: return 'English';
    case AppLanguage.JA: return '日本語';
    case AppLanguage.KO: return '한국어';
    case AppLanguage.KO_KP: return '조선어';
    case AppLanguage.RU: return 'Русский';
    case AppLanguage.DE: return 'Deutsch';
    case AppLanguage.FR: return 'Français';
    case AppLanguage.ES: return 'Español';
    case AppLanguage.IT: return 'Italiano';
    case AppLanguage.PT: return 'Português';
    case AppLanguage.NL: return 'Nederlands';
    case AppLanguage.EL: return 'Ελληνικά';
    case AppLanguage.VI: return 'Tiếng Việt';
    case AppLanguage.TH: return 'ไทย';
    case AppLanguage.AR: return 'العربية';
    case AppLanguage.AZ: return 'Azərbaycan dili';
    case AppLanguage.FA: return 'فارسی';
    default: return '跟随系统';
  }
}

export enum LyricsSource {
  AUTO = 'auto',
  NETEASE = 'netease',
  QQ = 'qq',
  LRCLIB = 'lrclib'
}

export function lyricsSourceFromKey(key: string): LyricsSource {
  switch (key) {
    case 'netease': return LyricsSource.NETEASE;
    case 'qq': return LyricsSource.QQ;
    case 'lrclib': return LyricsSource.LRCLIB;
    default: return LyricsSource.AUTO;
  }
}

export enum HostPowerAction {
  RESTART = 'restart',
  SHUTDOWN = 'shutdown',
  SUSPEND = 'suspend'
}

export function hostPowerTitle(action: HostPowerAction): string {
  switch (action) {
    case HostPowerAction.RESTART: return '重启主机';
    case HostPowerAction.SHUTDOWN: return '关闭主机';
    case HostPowerAction.SUSPEND: return '挂起主机';
    default: return '主机操作';
  }
}

export function hostPowerPath(action: HostPowerAction): string {
  switch (action) {
    case HostPowerAction.RESTART: return '/cgi-bin/Settings?ACTION=restart';
    case HostPowerAction.SHUTDOWN: return '/cgi-bin/Info?ACTION=shutdown';
    case HostPowerAction.SUSPEND: return '/cgi-bin/Info?ACTION=pmsuspend';
    default: return '/';
  }
}

export enum WakeState {
  IDLE = 'idle',
  WAKING = 'waking',
  ONLINE = 'online',
  FAILED = 'failed'
}

export interface LyricLine {
  timeMs: number;
  text: string;
}

export interface Lyrics {
  lines: LyricLine[];
  plainText: string;
  synced: boolean;
  source: string;
}

export interface ScanStatus {
  scanning: boolean;
  message: string;
  percent: number;
}

export enum ConnectionState {
  IDLE = 'idle',
  CONNECTING = 'connecting',
  CONNECTED = 'connected',
  RECONNECTING = 'reconnecting',
  LOST = 'lost'
}

export enum SleepAction {
  PAUSE = 'pause',
  STOP = 'stop',
  POWER_OFF = 'powerOff'
}

export enum LmsError {
  NETWORK = 'network',
  TIMEOUT = 'timeout',
  AUTH = 'auth',
  SERVER = 'server',
  PLAYER = 'player',
  UNSUPPORTED = 'unsupported',
  EMPTY = 'empty'
}

export class LmsException extends Error {
  kind: LmsError;

  constructor(kind: LmsError, message: string) {
    super(message);
    this.kind = kind;
    this.name = 'LmsException';
  }
}

export function friendlyErrorMessage(error: Error): string {
  if (error instanceof LmsException) {
    switch (error.kind) {
      case LmsError.AUTH: return '用户名或密码错误';
      case LmsError.TIMEOUT: return '连接超时，请重试';
      case LmsError.PLAYER: return '播放器不可达';
      case LmsError.EMPTY: return '队列为空，请先选择音乐';
      case LmsError.UNSUPPORTED: return '当前 Daphile 版本不支持该功能';
      default: return error.message.length > 0 ? error.message : '网络连接失败';
    }
  }
  return error.message.length > 0 ? error.message : '操作失败，请重试';
}

/** 空数据构造器，避免 ArkTS 对象字面量缺少字段。 */
export function emptyTrack(): Track {
  return {
    index: 0, id: '', title: '', artist: '', album: '', albumId: '',
    duration: 0, coverId: '', type: '', sampleRate: '', sampleSize: '',
    bitRate: '', trackNum: 0, disc: 0, url: '', lossless: false, channels: 0
  };
}

export function emptyNowPlaying(playerId: string): NowPlaying {
  return {
    playerId: playerId, power: true, mode: 'stop', time: 0, duration: 0,
    volume: 0, muted: false, repeat: 0, shuffle: 0, currentIndex: 0,
    trackCount: 0, track: null, queue: [], canSeek: true, canSetVolume: true,
    playlistTimestamp: ''
  };
}

/** 生成新的 NowPlaying 对象，便于状态管理感知引用变化。 */
export function cloneNowPlaying(source: NowPlaying): NowPlaying {
  return {
    playerId: source.playerId,
    power: source.power,
    mode: source.mode,
    time: source.time,
    duration: source.duration,
    volume: source.volume,
    muted: source.muted,
    repeat: source.repeat,
    shuffle: source.shuffle,
    currentIndex: source.currentIndex,
    trackCount: source.trackCount,
    track: source.track,
    queue: source.queue,
    canSeek: source.canSeek,
    canSetVolume: source.canSetVolume,
    playlistTimestamp: source.playlistTimestamp
  };
}

export function emptyServerInfo(): ServerInfo {
  return {
    version: '', ip: '', httpPort: 9000, playerCount: 0, albumCount: 0,
    artistCount: 0, genreCount: 0, songCount: 0, scanning: false, lastScan: ''
  };
}
