package com.daphile.remote.ui.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import com.daphile.remote.data.model.AppLanguage

/**
 * 轻量界面文案表。语言可在设置中选择「跟随系统 / 简体中文 / English」。
 * 未覆盖的次要页面暂时保留中文，后续可继续补充键值。
 */
data class ExtraStrings(
    val connecting: String,
    val reconnecting: String,
    val unreachable: String,
    val notConnected: String,
    val connectingDaphile: String,
    val reconnectingBanner: String,
    val serverUnreachable: String,
    val reconnect: String,
    val noServerAdded: String,
    val addServerHint: String,
    val serverSummary: String,
    val lyricsFileCount: String,
    val cacheCleared: String,
    val manageAddress: String,
    val hostPowerHint: String,
    val hostCommandSent: String,
    val copiedToClipboard: String,
    val copyInfo: String,
    val deleteServerTitle: String,
    val deleteServerBody: String,
    val deleted: String,
    val edit: String,
    val delete: String,
    val switchedTo: String,
    val discoverHint: String,
    val connectYourDaphile: String,
    val editServer: String,
    val serverIntro: String,
    val hostRequired: String,
    val lmsPortRange: String,
    val daphilePortRange: String,
    val nameOptional: String,
    val hostAddressLabel: String,
    val lmsHttpPort: String,
    val daphilePortLabel: String,
    val daphilePortHint: String,
    val usernameOptional: String,
    val passwordOptional: String,
    val passwordHint: String,
    val hidePassword: String,
    val showPassword: String,
    val scanningLan: String,
    val discoverServers: String,
    val connectedToLms: String,
    val connectingServer: String,
    val testAndSave: String,
    val serverEditNote: String,
    val noServersFound: String,
    val serversFoundCount: String,
    val noServersFoundBody: String,
    val playerWithName: String,
    val poweredOn: String,
    val poweredOff: String,
    val turnOnPlayer: String,
    val daphileHost: String,
    val hostActionNote: String,
    val confirmActionTitle: String,
    val confirmActionBody: String,
    val confirmActionNote: String,
    val hostRestart: String,
    val hostShutdown: String,
    val hostSuspend: String,
    val themeSystem: String,
    val themeDark: String,
    val themeLight: String,
    val themeSpring: String,
    val themeSummer: String,
    val themeAutumn: String,
    val themeWinter: String,
    val themeTaiji: String,
    val themePanda: String,
    val qualityStandard: String,
    val systemInfoCopyTitle: String,
    val systemInfoCopyServer: String,
    val systemInfoCopyVersion: String,
    val systemInfoCopyState: String,
    val systemInfoCopyPlayers: String,
    val systemInfoCopyLibrary: String,
    val systemInfoCopyNetwork: String,
    val lmsVersion: String,
    val serverStatus: String,
    val networkAddress: String,
    val playerCount: String,
    val tracks: String,
    val scanningLibrary: String,
    val noPlayers: String,
    val audioOutputScreenTitle: String,
    val notPlayingTitle: String,
    val playingState: String,
    val stoppedState: String,
    val audioSource: String,
    val playersAndOutput: String,
    val realtimeStatus: String,
    val path: String,
    val formatLabel: String,
    val sourceSampleRate: String,
    val bitDepth: String,
    val bitRate: String,
    val channels: String,
    val lossless: String,
    val outputDevice: String,
    val deviceModel: String,
    val outputEngine: String,
    val outputSpec: String,
    val volume: String,
    val elapsed: String,
    val totalDuration: String,
    val queuePosition: String,
    val dataSource: String,
    val specSummary: String,
    val bitDsd: String,
    val yesValue: String,
    val noOrUnknown: String,
    val outputEngineValue: String,
    val outputSpecDsd: String,
    val outputSpecAuto: String,
    val fixedLevel: String,
    val networkStream: String,
    val localFile: String,
    val pathCopied: String,
    val audioOutputNote: String,
    val copyAction: String,
    val seekUnsupported: String,
    val appShortName: String,
    val onlineWithModel: String,
    val playerLabel: String,
    val remove: String,
    val scanLibrary: String,
    val clearInput: String,
    val scanFinished: String,
    val recentlyAddedSub: String,
    val scanDialogBody: String,
    val scanStartedUpdate: String,
    val scanUpdate: String,
    val rebuildStarted: String,
    val rebuildLibrary: String,
    val rebuildHint: String,
    val noSearchResultsTitle: String,
    val noSearchResultsBody: String,
    val playingAlbum: String,
    val noArtistsTitle: String,
    val noArtistsBody: String,
    val noGenresTitle: String,
    val noGenresBody: String,
    val noYearsTitle: String,
    val noYearsBody: String,
    val noPlaylistsTitle: String,
    val noPlaylistsBody: String,
    val noContentTitle: String,
    val noContentBody: String,
    val musicFolders: String,
    val playFolder: String,
    val folderEmptyTitle: String,
    val folderEmptyBody: String,
    val browseTitle: String,
    val noCurrentTrack: String,
    val matchingLyrics: String,
    val noLyricsHint: String,
    val lyricsWithSource: String,
    val noSongsTitle: String,
    val noSongsBody: String,
    val noTracksTitle: String,
    val noTracksBody: String,
    val playlistEmptyTitle: String,
    val playlistEmptyBody: String,
    val syncApplied: String,
    val syncCancelled: String,
    val powerOff: String,
    val powerOn: String,
    val syncMaster: String,
    val independentPlayer: String,
    val favorites: String,
    val radios: String,
    val streamingServices: String,
    val noRadiosTitle: String,
    val noRadiosBody: String,
    val noStreamingTitle: String,
    val noStreamingBody: String,
    val favoritesEmptyTitle: String,
    val favoritesEmptyBody: String,
    val playAllItems: String,
    val browseContents: String,
)

data class UiStrings(
    // 底部导航
    val navNow: String,
    val navLibrary: String,
    val navRadio: String,
    val navPlayers: String,
    val navSettings: String,

    // 通用
    val back: String,
    val cancel: String,
    val close: String,
    val confirm: String,
    val retry: String,
    val refresh: String,
    val search: String,
    val searchHint: String,
    val loading: String,
    val saved: String,

    // 正在播放
    val queue: String,
    val lyrics: String,
    val power: String,
    val more: String,
    val audioOutput: String,
    val matchCover: String,
    val sleepTimer: String,
    val favoriteTrack: String,
    val saveQueue: String,
    val stop: String,
    val clearQueue: String,
    val share: String,
    val audioInfo: String,
    val fixedVolume: String,
    val volumeDisabledHint: String,
    val mute: String,
    val unmute: String,
    val sleepRemaining: String,
    val unknownTrack: String,
    val noContent: String,
    val selectMusic: String,
    val playerOff: String,
    val prevTrack: String,
    val nextTrack: String,
    val play: String,
    val pause: String,
    val repeatMode: String,
    val listLoop: String,
    val singleLoop: String,
    val repeatOff: String,
    val shuffleOn: String,
    val shuffleOff: String,
    val favorite: String,
    val favorited: String,
    val addedToFavorites: String,
    val pausedToast: String,
    val playingToast: String,
    val noPlayersTitle: String,
    val noPlayersBody: String,
    val viewPlayers: String,
    val addServer: String,
    val noServerTitle: String,
    val noServerBody: String,
    val choosePlayer: String,
    val queueEmptyTitle: String,
    val queueEmptyBody: String,
    val goLibrary: String,
    val clearQueueTitle: String,
    val clearQueueBody: String,
    val queueTracks: String,
    val sleepMinutes: String,
    val sleepAfter: String,
    val sleepStop: String,
    val sleepPause: String,
    val sleepPowerOff: String,
    val start: String,
    val restart: String,
    val cancelTimer: String,
    val playlistName: String,
    val save: String,
    val audioOutputTitle: String,
    val matchedCover: String,
    val noOnlineCover: String,
    val shareTitle: String,
    val nothingToShare: String,
    val shareOpened: String,
    val sleepStarted: String,

    // 播放器
    val playersTitle: String,
    val currentBadge: String,
    val playingBadge: String,
    val openNowPlaying: String,
    val syncTo: String,
    val unsync: String,
    val syncedTo: String,
    val online: String,
    val offline: String,

    // 音乐库
    val libraryTitle: String,
    val albums: String,
    val songs: String,
    val artists: String,
    val genres: String,
    val years: String,
    val folders: String,
    val playlists: String,
    val randomAlbum: String,
    val randomAlbumSub: String,
    val albumUnit: String,
    val songUnit: String,
    val artistUnit: String,
    val genreUnit: String,
    val browseByYear: String,
    val browseByFolder: String,
    val serverPlaylists: String,
    val viewAlbums: String,
    val openPlaylist: String,
    val openYear: String,

    // 专辑 / 歌曲
    val albumsTitle: String,
    val songsTitle: String,
    val playAll: String,
    val addToQueue: String,
    val view: String,
    val sortBy: String,
    val sortAlbum: String,
    val sortArtist: String,
    val sortYear: String,
    val sortNew: String,
    val listView: String,
    val gridView: String,
    val noAlbumsTitle: String,
    val noAlbumsBody: String,
    val tracksUnit: String,
    val loadMore: String,
    val onlineCover: String,
    val matchedCoverToast: String,
    val noOnlineCoverToast: String,

    // 歌词
    val lyricsTitle: String,
    val copyLyrics: String,
    val exportLrc: String,
    val refreshLyrics: String,
    val lyricsSaved: String,
    val lyricsSourceLabel: String,
    val sourceAuto: String,
    val sourceNetease: String,
    val sourceQq: String,
    val sourceLrclib: String,
    val noLyricsTitle: String,
    val rematch: String,
    val noTimelineHint: String,
    val syncedLabel: String,
    val lyricsCopied: String,
    val lyricsExported: String,
    val exportFailed: String,
    val nothingToCopy: String,
    val nothingToExport: String,
    val lyricsFrom: String,

    // 歌单与每日推荐
    val myPlaylists: String,
    val myPlaylistsSub: String,
    val dailyRecommendation: String,
    val dailyRecommendationSub: String,
    val regenerate: String,
    val createPlaylist: String,
    val renamePlaylist: String,
    val deletePlaylist: String,
    val deletePlaylistTitle: String,
    val deletePlaylistBody: String,
    val playlistCreated: String,
    val playlistRenamed: String,
    val playlistDeleted: String,
    val addToPlaylist: String,
    val newPlaylistName: String,
    val createAndAdd: String,
    val noPlaylistsHint: String,
    val addedToPlaylist: String,
    val trackCount: String,
    val saveToMyPlaylists: String,
    val queueAddedCount: String,
    val removeFromPlaylist: String,
    val removedFromPlaylist: String,
    val emptyPlaylistsTitle: String,
    val emptyPlaylistsBody: String,
    val emptyPlaylistTitle: String,
    val emptyPlaylistBody: String,
    val emptyDailyTitle: String,
    val emptyDailyBody: String,
    val addAllToQueue: String,
    val allSongsToQueue: String,
    val allSongsPlayAll: String,
    val randomToQueue: String,
    val randomToQueueSub: String,
    val randomCountTitle: String,
    val randomCountHint: String,
    val randomQueued: String,

    // 设置
    val settingsTitle: String,
    val serverSection: String,
    val serverManage: String,
    val daphileManage: String,
    val lmsWeb: String,
    val systemInfo: String,
    val systemInfoSub: String,
    val playbackSection: String,
    val pollInterval: String,
    val pollIntervalSub: String,
    val mediaNotification: String,
    val mediaNotificationSub: String,
    val lockScreen: String,
    val lockScreenSub: String,
    val onlineMeta: String,
    val onlineMetaSub: String,
    val appearanceSection: String,
    val theme: String,
    val storageSection: String,
    val lyricsLibrary: String,
    val clearCoverCache: String,
    val clearCoverCacheSub: String,
    val systemSection: String,
    val rescan: String,
    val rescanSub: String,
    val hostPower: String,
    val aboutSection: String,
    val version: String,
    val language: String,
    val languageAuto: String,
    val languageZh: String,
    val languageEn: String,
    val reboot: String,
    val shutdown: String,
    val suspend: String,
    val seconds: String,
    val clearLyricsTitle: String,
    val clearLyricsBody: String,
    val clear: String,
    val lyricsCleared: String,
    val rescanTitle: String,
    val rescanBody: String,
    val scan: String,
    val scanStarted: String,

    // 关于
    val aboutTitle: String,
    val aboutAppName: String,
    val aboutIntro: String,
    val aboutFeatures: String,
    val aboutFeature1: String,
    val aboutFeature2: String,
    val aboutFeature3: String,
    val aboutFeature4: String,
    val aboutAuthor: String,
    val aboutLicense: String,

    // 连接状态

    // 电源

    // 主题

    // 音质

    // 系统信息

    // 音频输出

    // 正在播放补充

    // 音乐库补充

    // 浏览

    // 歌词补充

    // 歌曲与歌单

    // 播放器补充

    // 电台与收藏,
    var extra: ExtraStrings
) {

    // 扩展文案：委托到 ExtraStrings，避免单个方法参数超过 Android 上限
    val connecting: String get() = extra.connecting
    val reconnecting: String get() = extra.reconnecting
    val unreachable: String get() = extra.unreachable
    val notConnected: String get() = extra.notConnected
    val connectingDaphile: String get() = extra.connectingDaphile
    val reconnectingBanner: String get() = extra.reconnectingBanner
    val serverUnreachable: String get() = extra.serverUnreachable
    val reconnect: String get() = extra.reconnect
    val noServerAdded: String get() = extra.noServerAdded
    val addServerHint: String get() = extra.addServerHint
    val serverSummary: String get() = extra.serverSummary
    val lyricsFileCount: String get() = extra.lyricsFileCount
    val cacheCleared: String get() = extra.cacheCleared
    val manageAddress: String get() = extra.manageAddress
    val hostPowerHint: String get() = extra.hostPowerHint
    val hostCommandSent: String get() = extra.hostCommandSent
    val copiedToClipboard: String get() = extra.copiedToClipboard
    val copyInfo: String get() = extra.copyInfo
    val deleteServerTitle: String get() = extra.deleteServerTitle
    val deleteServerBody: String get() = extra.deleteServerBody
    val deleted: String get() = extra.deleted
    val edit: String get() = extra.edit
    val delete: String get() = extra.delete
    val switchedTo: String get() = extra.switchedTo
    val discoverHint: String get() = extra.discoverHint
    val connectYourDaphile: String get() = extra.connectYourDaphile
    val editServer: String get() = extra.editServer
    val serverIntro: String get() = extra.serverIntro
    val hostRequired: String get() = extra.hostRequired
    val lmsPortRange: String get() = extra.lmsPortRange
    val daphilePortRange: String get() = extra.daphilePortRange
    val nameOptional: String get() = extra.nameOptional
    val hostAddressLabel: String get() = extra.hostAddressLabel
    val lmsHttpPort: String get() = extra.lmsHttpPort
    val daphilePortLabel: String get() = extra.daphilePortLabel
    val daphilePortHint: String get() = extra.daphilePortHint
    val usernameOptional: String get() = extra.usernameOptional
    val passwordOptional: String get() = extra.passwordOptional
    val passwordHint: String get() = extra.passwordHint
    val hidePassword: String get() = extra.hidePassword
    val showPassword: String get() = extra.showPassword
    val scanningLan: String get() = extra.scanningLan
    val discoverServers: String get() = extra.discoverServers
    val connectedToLms: String get() = extra.connectedToLms
    val connectingServer: String get() = extra.connectingServer
    val testAndSave: String get() = extra.testAndSave
    val serverEditNote: String get() = extra.serverEditNote
    val noServersFound: String get() = extra.noServersFound
    val serversFoundCount: String get() = extra.serversFoundCount
    val noServersFoundBody: String get() = extra.noServersFoundBody
    val playerWithName: String get() = extra.playerWithName
    val poweredOn: String get() = extra.poweredOn
    val poweredOff: String get() = extra.poweredOff
    val turnOnPlayer: String get() = extra.turnOnPlayer
    val daphileHost: String get() = extra.daphileHost
    val hostActionNote: String get() = extra.hostActionNote
    val confirmActionTitle: String get() = extra.confirmActionTitle
    val confirmActionBody: String get() = extra.confirmActionBody
    val confirmActionNote: String get() = extra.confirmActionNote
    val hostRestart: String get() = extra.hostRestart
    val hostShutdown: String get() = extra.hostShutdown
    val hostSuspend: String get() = extra.hostSuspend
    val themeSystem: String get() = extra.themeSystem
    val themeDark: String get() = extra.themeDark
    val themeLight: String get() = extra.themeLight
    val themeSpring: String get() = extra.themeSpring
    val themeSummer: String get() = extra.themeSummer
    val themeAutumn: String get() = extra.themeAutumn
    val themeWinter: String get() = extra.themeWinter
    val themeTaiji: String get() = extra.themeTaiji
    val themePanda: String get() = extra.themePanda
    val qualityStandard: String get() = extra.qualityStandard
    val systemInfoCopyTitle: String get() = extra.systemInfoCopyTitle
    val systemInfoCopyServer: String get() = extra.systemInfoCopyServer
    val systemInfoCopyVersion: String get() = extra.systemInfoCopyVersion
    val systemInfoCopyState: String get() = extra.systemInfoCopyState
    val systemInfoCopyPlayers: String get() = extra.systemInfoCopyPlayers
    val systemInfoCopyLibrary: String get() = extra.systemInfoCopyLibrary
    val systemInfoCopyNetwork: String get() = extra.systemInfoCopyNetwork
    val lmsVersion: String get() = extra.lmsVersion
    val serverStatus: String get() = extra.serverStatus
    val networkAddress: String get() = extra.networkAddress
    val playerCount: String get() = extra.playerCount
    val tracks: String get() = extra.tracks
    val scanningLibrary: String get() = extra.scanningLibrary
    val noPlayers: String get() = extra.noPlayers
    val audioOutputScreenTitle: String get() = extra.audioOutputScreenTitle
    val notPlayingTitle: String get() = extra.notPlayingTitle
    val playingState: String get() = extra.playingState
    val stoppedState: String get() = extra.stoppedState
    val audioSource: String get() = extra.audioSource
    val playersAndOutput: String get() = extra.playersAndOutput
    val realtimeStatus: String get() = extra.realtimeStatus
    val path: String get() = extra.path
    val formatLabel: String get() = extra.formatLabel
    val sourceSampleRate: String get() = extra.sourceSampleRate
    val bitDepth: String get() = extra.bitDepth
    val bitRate: String get() = extra.bitRate
    val channels: String get() = extra.channels
    val lossless: String get() = extra.lossless
    val outputDevice: String get() = extra.outputDevice
    val deviceModel: String get() = extra.deviceModel
    val outputEngine: String get() = extra.outputEngine
    val outputSpec: String get() = extra.outputSpec
    val volume: String get() = extra.volume
    val elapsed: String get() = extra.elapsed
    val totalDuration: String get() = extra.totalDuration
    val queuePosition: String get() = extra.queuePosition
    val dataSource: String get() = extra.dataSource
    val specSummary: String get() = extra.specSummary
    val bitDsd: String get() = extra.bitDsd
    val yesValue: String get() = extra.yesValue
    val noOrUnknown: String get() = extra.noOrUnknown
    val outputEngineValue: String get() = extra.outputEngineValue
    val outputSpecDsd: String get() = extra.outputSpecDsd
    val outputSpecAuto: String get() = extra.outputSpecAuto
    val fixedLevel: String get() = extra.fixedLevel
    val networkStream: String get() = extra.networkStream
    val localFile: String get() = extra.localFile
    val pathCopied: String get() = extra.pathCopied
    val audioOutputNote: String get() = extra.audioOutputNote
    val copyAction: String get() = extra.copyAction
    val seekUnsupported: String get() = extra.seekUnsupported
    val appShortName: String get() = extra.appShortName
    val onlineWithModel: String get() = extra.onlineWithModel
    val playerLabel: String get() = extra.playerLabel
    val remove: String get() = extra.remove
    val scanLibrary: String get() = extra.scanLibrary
    val clearInput: String get() = extra.clearInput
    val scanFinished: String get() = extra.scanFinished
    val recentlyAddedSub: String get() = extra.recentlyAddedSub
    val scanDialogBody: String get() = extra.scanDialogBody
    val scanStartedUpdate: String get() = extra.scanStartedUpdate
    val scanUpdate: String get() = extra.scanUpdate
    val rebuildStarted: String get() = extra.rebuildStarted
    val rebuildLibrary: String get() = extra.rebuildLibrary
    val rebuildHint: String get() = extra.rebuildHint
    val noSearchResultsTitle: String get() = extra.noSearchResultsTitle
    val noSearchResultsBody: String get() = extra.noSearchResultsBody
    val playingAlbum: String get() = extra.playingAlbum
    val noArtistsTitle: String get() = extra.noArtistsTitle
    val noArtistsBody: String get() = extra.noArtistsBody
    val noGenresTitle: String get() = extra.noGenresTitle
    val noGenresBody: String get() = extra.noGenresBody
    val noYearsTitle: String get() = extra.noYearsTitle
    val noYearsBody: String get() = extra.noYearsBody
    val noPlaylistsTitle: String get() = extra.noPlaylistsTitle
    val noPlaylistsBody: String get() = extra.noPlaylistsBody
    val noContentTitle: String get() = extra.noContentTitle
    val noContentBody: String get() = extra.noContentBody
    val musicFolders: String get() = extra.musicFolders
    val playFolder: String get() = extra.playFolder
    val folderEmptyTitle: String get() = extra.folderEmptyTitle
    val folderEmptyBody: String get() = extra.folderEmptyBody
    val browseTitle: String get() = extra.browseTitle
    val noCurrentTrack: String get() = extra.noCurrentTrack
    val matchingLyrics: String get() = extra.matchingLyrics
    val noLyricsHint: String get() = extra.noLyricsHint
    val lyricsWithSource: String get() = extra.lyricsWithSource
    val noSongsTitle: String get() = extra.noSongsTitle
    val noSongsBody: String get() = extra.noSongsBody
    val noTracksTitle: String get() = extra.noTracksTitle
    val noTracksBody: String get() = extra.noTracksBody
    val playlistEmptyTitle: String get() = extra.playlistEmptyTitle
    val playlistEmptyBody: String get() = extra.playlistEmptyBody
    val syncApplied: String get() = extra.syncApplied
    val syncCancelled: String get() = extra.syncCancelled
    val powerOff: String get() = extra.powerOff
    val powerOn: String get() = extra.powerOn
    val syncMaster: String get() = extra.syncMaster
    val independentPlayer: String get() = extra.independentPlayer
    val favorites: String get() = extra.favorites
    val radios: String get() = extra.radios
    val streamingServices: String get() = extra.streamingServices
    val noRadiosTitle: String get() = extra.noRadiosTitle
    val noRadiosBody: String get() = extra.noRadiosBody
    val noStreamingTitle: String get() = extra.noStreamingTitle
    val noStreamingBody: String get() = extra.noStreamingBody
    val favoritesEmptyTitle: String get() = extra.favoritesEmptyTitle
    val favoritesEmptyBody: String get() = extra.favoritesEmptyBody
    val playAllItems: String get() = extra.playAllItems
    val browseContents: String get() = extra.browseContents
}


private val zhExtra = ExtraStrings(
    connecting = "连接中",
    reconnecting = "重连中",
    unreachable = "不可达",
    notConnected = "未连接",
    connectingDaphile = "正在连接 Daphile…",
    reconnectingBanner = "连接中断，正在重连…",
    serverUnreachable = "服务器不可达",
    reconnect = "重新连接",
    noServerAdded = "尚未添加服务器",
    addServerHint = "添加 Daphile 主机以开始使用",
    serverSummary = "LMS %s · %d 个播放器 · 管理端口 %d",
    lyricsFileCount = "%d 个 .lrc",
    cacheCleared = "缓存已清除",
    manageAddress = "管理地址 %s",
    hostPowerHint = "重启与挂起使用 Daphile 官方 Web 快捷方式，关机使用 /cgi-bin/Info?ACTION=shutdown；每次操作都需要二次确认。",
    hostCommandSent = "指令已下发，服务器即将断开",
    copiedToClipboard = "已复制到剪贴板",
    copyInfo = "复制信息",
    deleteServerTitle = "删除服务器",
    deleteServerBody = "删除「%s」？本地保存的凭据与缓存也会一并清除。",
    deleted = "已删除",
    edit = "编辑",
    delete = "删除",
    switchedTo = "已切换到 %s",
    discoverHint = "输入 Daphile 主机的 IP 地址，或使用局域网自动发现",
    connectYourDaphile = "连接你的 Daphile",
    editServer = "编辑服务器",
    serverIntro = "输入 Daphile 主机的地址，开始遥控你的数字音频系统。",
    hostRequired = "请输入主机地址",
    lmsPortRange = "LMS 端口必须在 1~65535 之间",
    daphilePortRange = "Daphile 管理端口必须在 1~65535 之间",
    nameOptional = "名称（可选）",
    hostAddressLabel = "主机地址（IP 或主机名）",
    lmsHttpPort = "LMS HTTP 端口",
    daphilePortLabel = "Daphile 管理端口",
    daphilePortHint = "用于主机电源与管理页，默认 80",
    usernameOptional = "用户名（可选）",
    passwordOptional = "密码（可选）",
    passwordHint = "留空表示不需要登录",
    hidePassword = "隐藏密码",
    showPassword = "显示密码",
    scanningLan = "正在扫描局域网…",
    discoverServers = "自动发现局域网服务器",
    connectedToLms = "连接成功 · LMS %s",
    connectingServer = "正在连接服务器…",
    testAndSave = "测试并保存",
    serverEditNote = "Daphile 默认使用局域网明文 HTTP：LMS 端口 9000，管理端口 80。跨网络访问时请使用 ZeroTier 等 VPN，并注意网络安全。",
    noServersFound = "未发现服务器",
    serversFoundCount = "发现 %d 台服务器",
    noServersFoundBody = "未发现服务器，请手动输入 IP 地址。",
    playerWithName = "播放器 · %s",
    poweredOn = "当前已开启",
    poweredOff = "当前已关闭",
    turnOnPlayer = "开启播放器",
    daphileHost = "Daphile 主机",
    hostActionNote = "主机操作会中断正在播放的音乐，且需要二次确认。",
    confirmActionTitle = "确认%s",
    confirmActionBody = "即将对 Daphile 主机（%s）执行「%s」。正在播放的音乐会中断，主机可能在数分钟内不可用。",
    confirmActionNote = "该操作依赖 Daphile 官方 Web 接口，仅在你确认要操作主机时执行。",
    hostRestart = "重启主机",
    hostShutdown = "关闭主机",
    hostSuspend = "挂起主机",
    themeSystem = "跟随系统",
    themeDark = "深色",
    themeLight = "浅色",
    themeSpring = "春 · 青芽",
    themeSummer = "夏 · 碧海",
    themeAutumn = "秋 · 枫叶",
    themeWinter = "冬 · 霜雪",
    themeTaiji = "太极 · 阴阳",
    themePanda = "熊猫 · 黑白",
    qualityStandard = "标准",
    systemInfoCopyTitle = "Daphile Remote 系统信息",
    systemInfoCopyServer = "服务器: %s (%s:%d)",
    systemInfoCopyVersion = "LMS 版本: %s",
    systemInfoCopyState = "状态: %s",
    systemInfoCopyPlayers = "播放器: %d",
    systemInfoCopyLibrary = "音乐库: %d 张专辑, %d 首曲目, %d 位艺术家",
    systemInfoCopyNetwork = "网络: %s:%d",
    lmsVersion = "LMS 版本",
    serverStatus = "服务器状态",
    networkAddress = "网络地址",
    playerCount = "播放器数量",
    tracks = "曲目",
    scanningLibrary = "正在扫描音乐库…",
    noPlayers = "没有可用播放器",
    audioOutputScreenTitle = "音频输出",
    notPlayingTitle = "未在播放",
    playingState = "正在播放",
    stoppedState = "已停止",
    audioSource = "音源",
    playersAndOutput = "播放器与输出",
    realtimeStatus = "实时状态",
    path = "路径",
    formatLabel = "格式",
    sourceSampleRate = "源采样率",
    bitDepth = "位深",
    bitRate = "码率",
    channels = "声道",
    lossless = "无损",
    outputDevice = "输出设备",
    deviceModel = "设备型号",
    outputEngine = "输出引擎",
    outputSpec = "输出规格",
    volume = "音量",
    elapsed = "已播放",
    totalDuration = "总时长",
    queuePosition = "队列位置",
    dataSource = "数据源",
    specSummary = "规格摘要",
    bitDsd = "1 bit（DSD）",
    yesValue = "是",
    noOrUnknown = "否/未知",
    outputEngineValue = "Daphile 音频引擎（Squeezelite）",
    outputSpecDsd = "原生/DoP 由 Daphile 设备设置决定",
    outputSpecAuto = "Daphile 自动匹配音频设备",
    fixedLevel = "固定电平 %d",
    networkStream = "网络流",
    localFile = "本地文件",
    pathCopied = "路径已复制",
    audioOutputNote = "以上为 LMS 返回的实时音频规格。Daphile 负责实际解码与 DAC 输出，输出采样率和 DSD 模式由 Daphile 音频设备设置决定。",
    copyAction = "复制",
    seekUnsupported = "当前音源不支持定位",
    appShortName = "达菲遥控",
    onlineWithModel = "在线 · %s",
    playerLabel = "播放器",
    remove = "移除",
    scanLibrary = "扫描音乐库",
    clearInput = "清除",
    scanFinished = "扫描完成",
    recentlyAddedSub = "按入库时间查看新专辑",
    scanDialogBody = "让 Daphile 重新检查音乐目录，更新新增或变更的曲目。",
    scanStartedUpdate = "已开始扫描",
    scanUpdate = "扫描更新（推荐）",
    rebuildStarted = "已开始重建音乐库",
    rebuildLibrary = "重建音乐库",
    rebuildHint = "重建会清空索引并重新扫描，可能耗时较长。扫描进度会显示在音乐库页面顶部。",
    noSearchResultsTitle = "没有找到结果",
    noSearchResultsBody = "未找到与「%s」相关的内容，试试其他关键词",
    playingAlbum = "正在播放 %s",
    noArtistsTitle = "没有艺术家",
    noArtistsBody = "音乐库中尚未扫描到艺术家信息",
    noGenresTitle = "没有流派",
    noGenresBody = "音乐库中尚未扫描到流派信息",
    noYearsTitle = "没有年份信息",
    noYearsBody = "音乐库曲目缺少发行年份标签",
    noPlaylistsTitle = "没有播放列表",
    noPlaylistsBody = "可在正在播放页把当前队列保存为播放列表",
    noContentTitle = "这里还没有内容",
    noContentBody = "请在 Daphile 网页端检查该插件或目录",
    musicFolders = "音乐文件夹",
    playFolder = "播放此文件夹",
    folderEmptyTitle = "文件夹为空",
    folderEmptyBody = "此目录下没有音乐文件",
    browseTitle = "浏览",
    noCurrentTrack = "暂无正在播放的曲目",
    matchingLyrics = "正在匹配歌词…",
    noLyricsHint = "暂无歌词\n点按查看歌词页",
    lyricsWithSource = "歌词 · %s",
    noSongsTitle = "没有歌曲",
    noSongsBody = "音乐库中尚未扫描到曲目",
    noTracksTitle = "没有曲目",
    noTracksBody = "这张专辑没有可播放的曲目",
    playlistEmptyTitle = "播放列表为空",
    playlistEmptyBody = "这个播放列表没有曲目",
    syncApplied = "同步设置已生效",
    syncCancelled = "已取消同步",
    powerOff = "关闭电源",
    powerOn = "开启电源",
    syncMaster = "同步主播放器",
    independentPlayer = "独立播放",
    favorites = "收藏",
    radios = "电台",
    streamingServices = "串流服务",
    noRadiosTitle = "没有可用电台",
    noRadiosBody = "请在 Daphile 插件设置中安装电台插件（如 TuneIn）",
    noStreamingTitle = "没有串流服务",
    noStreamingBody = "请在 Daphile 插件设置中安装 TIDAL、Qobuz、Spotty 等插件",
    favoritesEmptyTitle = "收藏夹为空",
    favoritesEmptyBody = "在正在播放页收藏曲目，或从 Daphile 网页端添加收藏",
    playAllItems = "全部播放",
    browseContents = "浏览内容"
)

internal val zh = UiStrings(
    navNow = "正在播放",
    navLibrary = "音乐库",
    navRadio = "电台与收藏",
    navPlayers = "播放器",
    navSettings = "设置",

    back = "返回",
    cancel = "取消",
    close = "关闭",
    confirm = "确定",
    retry = "重试",
    refresh = "刷新",
    search = "搜索",
    searchHint = "搜索曲目、专辑、艺术家…",
    loading = "加载中…",
    saved = "已保存",

    queue = "播放队列",
    lyrics = "歌词",
    power = "电源控制",
    more = "更多",
    audioOutput = "音频输出信息",
    matchCover = "在线匹配封面",
    sleepTimer = "睡眠定时",
    favoriteTrack = "收藏当前曲目",
    saveQueue = "保存队列为播放列表",
    stop = "停止播放",
    clearQueue = "清空队列",
    share = "分享",
    audioInfo = "音频输出信息",
    fixedVolume = "固定音量",
    volumeDisabledHint = "达菲已关闭音量控制，请在服务器端调整",
    mute = "静音",
    unmute = "取消静音",
    sleepRemaining = "睡眠定时剩余 %s",
    unknownTrack = "未知曲目",
    noContent = "暂无播放内容",
    selectMusic = "选择音乐开始播放",
    playerOff = "播放器已关闭",
    prevTrack = "上一首",
    nextTrack = "下一首",
    play = "播放",
    pause = "暂停",
    repeatMode = "循环模式",
    listLoop = "列表循环",
    singleLoop = "单曲循环",
    repeatOff = "循环已关闭",
    shuffleOn = "开启随机播放",
    shuffleOff = "关闭随机播放",
    favorite = "收藏",
    favorited = "已收藏",
    addedToFavorites = "已加入收藏",
    pausedToast = "已暂停",
    playingToast = "开始播放",
    noPlayersTitle = "服务器暂无可用播放器",
    noPlayersBody = "请在 Daphile 音频设置中启用播放器，或检查 USB DAC 连接",
    viewPlayers = "查看播放器",
    addServer = "添加服务器",
    noServerTitle = "还没有连接 Daphile",
    noServerBody = "添加局域网中的 Daphile 主机，即可开始遥控播放",
    choosePlayer = "选择播放器",
    queueEmptyTitle = "队列为空",
    queueEmptyBody = "去音乐库挑一张专辑开始播放",
    goLibrary = "去音乐库",
    clearQueueTitle = "清空播放队列",
    clearQueueBody = "清空后当前播放会停止，确定要继续吗？",
    queueTracks = "%d 首曲目",
    sleepMinutes = "%d 分钟",
    sleepAfter = "到点后",
    sleepStop = "停止播放",
    sleepPause = "暂停播放",
    sleepPowerOff = "关闭播放器",
    start = "开始",
    restart = "重新开始",
    cancelTimer = "取消定时",
    playlistName = "播放列表名称",
    save = "保存",
    audioOutputTitle = "音频输出信息",
    matchedCover = "已匹配在线封面",
    noOnlineCover = "未找到在线封面",
    shareTitle = "分享曲目",
    nothingToShare = "暂无可分享的曲目",
    shareOpened = "已打开分享",
    sleepStarted = "睡眠定时",

    playersTitle = "播放器",
    currentBadge = "当前",
    playingBadge = "播放中",
    openNowPlaying = "打开正在播放",
    syncTo = "同步到",
    unsync = "取消同步",
    syncedTo = "已同步到 %s",
    online = "在线",
    offline = "离线",

    libraryTitle = "音乐库",
    albums = "专辑",
    songs = "歌曲",
    artists = "艺术家",
    genres = "流派",
    years = "年份",
    folders = "文件夹",
    playlists = "播放列表",
    randomAlbum = "随机播放一张专辑",
    randomAlbumSub = "从整个音乐库随机挑选",
    albumUnit = "%d 张专辑",
    songUnit = "%d 首曲目",
    artistUnit = "%d 位艺术家",
    genreUnit = "%d 个流派",
    browseByYear = "按年代浏览",
    browseByFolder = "按音乐目录浏览",
    serverPlaylists = "服务器保存的列表",
    viewAlbums = "查看专辑",
    openPlaylist = "打开播放列表",
    openYear = "查看该年份专辑",

    albumsTitle = "专辑",
    songsTitle = "歌曲",
    playAll = "播放全部",
    addToQueue = "加入队列",
    view = "查看",
    sortBy = "排序",
    sortAlbum = "专辑名",
    sortArtist = "艺术家",
    sortYear = "年份",
    sortNew = "最近添加",
    listView = "列表视图",
    gridView = "网格视图",
    noAlbumsTitle = "没有专辑",
    noAlbumsBody = "当前分类下没有可浏览的专辑",
    tracksUnit = "%d 首曲目",
    loadMore = "加载更多（%d/%d）",
    onlineCover = "在线匹配封面",
    matchedCoverToast = "已匹配在线封面",
    noOnlineCoverToast = "未找到在线封面",

    lyricsTitle = "歌词",
    copyLyrics = "复制歌词",
    exportLrc = "导出 .lrc",
    refreshLyrics = "重新匹配歌词",
    lyricsSaved = "已保存",
    lyricsSourceLabel = "歌词源",
    sourceAuto = "自动（网易云优先）",
    sourceNetease = "网易云音乐",
    sourceQq = "QQ 音乐",
    sourceLrclib = "LRCLIB",
    noLyricsTitle = "暂无歌词",
    rematch = "重新匹配",
    noTimelineHint = "该歌词不含时间轴，仅支持滚动浏览",
    syncedLabel = "实时同步",
    lyricsCopied = "歌词已复制",
    lyricsExported = "已导出歌词",
    exportFailed = "导出失败",
    nothingToCopy = "暂无歌词可复制",
    nothingToExport = "暂无歌词可导出",
    lyricsFrom = "歌词来源：%s",

    myPlaylists = "我的歌单",
    myPlaylistsSub = "保存在本机的自定义歌单",
    dailyRecommendation = "每日推荐",
    dailyRecommendationSub = "每天从曲库随机挑选 20 首",
    regenerate = "重新生成",
    createPlaylist = "新建歌单",
    renamePlaylist = "重命名",
    deletePlaylist = "删除歌单",
    deletePlaylistTitle = "删除歌单",
    deletePlaylistBody = "确定删除「%s」？该操作不可撤销。",
    playlistCreated = "歌单已创建",
    playlistRenamed = "歌单已重命名",
    playlistDeleted = "歌单已删除",
    addToPlaylist = "加入歌单",
    newPlaylistName = "新歌单名称",
    createAndAdd = "新建并加入",
    noPlaylistsHint = "还没有自定义歌单，可在下方新建一个。",
    addedToPlaylist = "已加入歌单",
    trackCount = "%d 首",
    saveToMyPlaylists = "存为我的歌单",
    queueAddedCount = "已加入队列",
    removeFromPlaylist = "从歌单移除",
    removedFromPlaylist = "已从歌单移除",
    emptyPlaylistsTitle = "还没有歌单",
    emptyPlaylistsBody = "长按任意歌曲即可加入歌单，也可以在这里新建",
    emptyPlaylistTitle = "歌单为空",
    emptyPlaylistBody = "长按歌曲并选择「加入歌单」",
    emptyDailyTitle = "暂无推荐",
    emptyDailyBody = "点击右上角刷新即可生成今日推荐",
    addAllToQueue = "全部加入队列",
    allSongsToQueue = "所有歌曲加入队列",
    allSongsPlayAll = "播放全部歌曲",
    randomToQueue = "随机加入队列",
    randomToQueueSub = "自定义随机曲目数量（1–200）",
    randomCountTitle = "随机加入队列",
    randomCountHint = "输入 1–200 之间的数量",
    randomQueued = "已随机加入 %d 首",

    settingsTitle = "设置",
    serverSection = "服务器",
    serverManage = "服务器管理",
    daphileManage = "Daphile 管理",
    lmsWeb = "LMS 网页端",
    systemInfo = "系统信息",
    systemInfoSub = "服务器版本、播放器与音乐库统计",
    playbackSection = "播放与同步",
    pollInterval = "状态刷新间隔",
    pollIntervalSub = "推送不可用时的状态轮询频率",
    mediaNotification = "媒体通知",
    mediaNotificationSub = "在通知栏显示播放控制",
    lockScreen = "锁屏媒体控制",
    lockScreenSub = "在锁屏界面显示封面与播放控制",
    onlineMeta = "在线歌词与封面",
    onlineMetaSub = "网易云、QQ 音乐、LRCLIB 与 MusicBrainz",
    appearanceSection = "外观",
    theme = "主题",
    storageSection = "存储",
    lyricsLibrary = "歌词库",
    clearCoverCache = "清除封面缓存",
    clearCoverCacheSub = "封面图片可随时重新加载",
    systemSection = "系统",
    rescan = "重新扫描音乐库",
    rescanSub = "让 Daphile 重新索引音乐目录",
    hostPower = "主机电源控制",
    aboutSection = "关于",
    version = "版本",
    language = "语言",
    languageAuto = "跟随系统",
    languageZh = "简体中文",
    languageEn = "English",
    reboot = "重启",
    shutdown = "关机",
    suspend = "挂起",
    seconds = "%d 秒",
    clearLyricsTitle = "清空歌词库",
    clearLyricsBody = "将删除本机已保存的 %d 个 .lrc 文件，下次播放时可重新在线获取。",
    clear = "清空",
    lyricsCleared = "歌词库已清空",
    rescanTitle = "重新扫描音乐库",
    rescanBody = "触发服务器重新扫描音乐库？扫描期间音乐库可能不完整。",
    scan = "扫描",
    scanStarted = "已触发音乐库扫描",

    aboutTitle = "关于达菲遥控",
    aboutAppName = "达菲遥控 · Daphile Remote",
    aboutIntro = "面向 Daphile / Lyrion Music Server 的第三方 Android 遥控应用。界面基于 Jetpack Compose，为局域网内的达菲主机提供播放控制、音乐库浏览与音频信息展示。",
    aboutFeatures = "主要功能",
    aboutFeature1 = "播放控制、队列、睡眠定时与多播放器切换",
    aboutFeature2 = "音乐库浏览、文件夹浏览与即时搜索，全列表音质标签",
    aboutFeature3 = "在线歌词（网易云 / QQ 音乐 / LRCLIB）、导出 .lrc 与本地歌词库",
    aboutFeature4 = "音频输出信息、在线封面匹配、锁屏媒体控制与主机电源管理",
    aboutAuthor = "作者：panjiasen007-lab",
    aboutLicense = "开源许可：GNU GPL v3.0（GPL-3.0-only）。应用不收集个人数据，服务器信息与凭据仅保存在本机。",













    extra = zhExtra
)

private val enExtra = ExtraStrings(
    connecting = "Connecting",
    reconnecting = "Reconnecting",
    unreachable = "Unreachable",
    notConnected = "Not connected",
    connectingDaphile = "Connecting to Daphile…",
    reconnectingBanner = "Connection lost, reconnecting…",
    serverUnreachable = "Server unreachable",
    reconnect = "Reconnect",
    noServerAdded = "No server added yet",
    addServerHint = "Add a Daphile host to get started",
    serverSummary = "LMS %s · %d players · Admin port %d",
    lyricsFileCount = "%d .lrc files",
    cacheCleared = "Cover cache cleared",
    manageAddress = "Admin address %s",
    hostPowerHint = "Restart and suspend use Daphile's official web shortcuts; shutdown uses /cgi-bin/Info?ACTION=shutdown. Every action requires confirmation.",
    hostCommandSent = "Command sent; the server will disconnect shortly",
    copiedToClipboard = "Copied to clipboard",
    copyInfo = "Copy Info",
    deleteServerTitle = "Delete Server",
    deleteServerBody = "Delete \"%s\"? Locally saved credentials and cache will also be cleared.",
    deleted = "Deleted",
    edit = "Edit",
    delete = "Delete",
    switchedTo = "Switched to %s",
    discoverHint = "Enter the Daphile host IP address, or use LAN discovery",
    connectYourDaphile = "Connect your Daphile",
    editServer = "Edit Server",
    serverIntro = "Enter your Daphile host address to start controlling your digital audio system.",
    hostRequired = "Please enter the host address",
    lmsPortRange = "The LMS port must be between 1 and 65535",
    daphilePortRange = "The Daphile admin port must be between 1 and 65535",
    nameOptional = "Name (optional)",
    hostAddressLabel = "Host address (IP or hostname)",
    lmsHttpPort = "LMS HTTP port",
    daphilePortLabel = "Daphile admin port",
    daphilePortHint = "Used for host power and admin pages, default 80",
    usernameOptional = "Username (optional)",
    passwordOptional = "Password (optional)",
    passwordHint = "Leave empty if no login is required",
    hidePassword = "Hide password",
    showPassword = "Show password",
    scanningLan = "Scanning your LAN…",
    discoverServers = "Auto-discover LAN servers",
    connectedToLms = "Connected · LMS %s",
    connectingServer = "Connecting to server…",
    testAndSave = "Test & Save",
    serverEditNote = "Daphile uses plain HTTP on your LAN by default: LMS port 9000, admin port 80. Use a VPN such as ZeroTier for remote access and stay mindful of network security.",
    noServersFound = "No servers found",
    serversFoundCount = "Found %d servers",
    noServersFoundBody = "No servers found. Please enter the IP address manually.",
    playerWithName = "Player · %s",
    poweredOn = "Currently on",
    poweredOff = "Currently off",
    turnOnPlayer = "Turn player on",
    daphileHost = "Daphile host",
    hostActionNote = "Host actions interrupt playback and require confirmation.",
    confirmActionTitle = "Confirm %s",
    confirmActionBody = "The Daphile host (%s) is about to perform \"%s\". Playback will stop and the host may be unavailable for a few minutes.",
    confirmActionNote = "This relies on the official Daphile web interface and only runs after you confirm.",
    hostRestart = "Restart host",
    hostShutdown = "Shutdown host",
    hostSuspend = "Suspend host",
    themeSystem = "Follow system",
    themeDark = "Dark",
    themeLight = "Light",
    themeSpring = "Spring · Green Bud",
    themeSummer = "Summer · Blue Sea",
    themeAutumn = "Autumn · Maple",
    themeWinter = "Winter · Frost",
    themeTaiji = "Taiji · Yin-Yang",
    themePanda = "Panda · Black & White",
    qualityStandard = "Standard",
    systemInfoCopyTitle = "Daphile Remote System Info",
    systemInfoCopyServer = "Server: %s (%s:%d)",
    systemInfoCopyVersion = "LMS version: %s",
    systemInfoCopyState = "Status: %s",
    systemInfoCopyPlayers = "Players: %d",
    systemInfoCopyLibrary = "Library: %d albums, %d tracks, %d artists",
    systemInfoCopyNetwork = "Network: %s:%d",
    lmsVersion = "LMS version",
    serverStatus = "Server status",
    networkAddress = "Network address",
    playerCount = "Players",
    tracks = "Tracks",
    scanningLibrary = "Scanning music library…",
    noPlayers = "No players available",
    audioOutputScreenTitle = "Audio Output",
    notPlayingTitle = "Nothing playing",
    playingState = "Playing",
    stoppedState = "Stopped",
    audioSource = "Audio Source",
    playersAndOutput = "Players & Output",
    realtimeStatus = "Live Status",
    path = "Path",
    formatLabel = "Format",
    sourceSampleRate = "Source Sample Rate",
    bitDepth = "Bit Depth",
    bitRate = "Bitrate",
    channels = "Channels",
    lossless = "Lossless",
    outputDevice = "Output Device",
    deviceModel = "Device Model",
    outputEngine = "Output Engine",
    outputSpec = "Output Spec",
    volume = "Volume",
    elapsed = "Elapsed",
    totalDuration = "Total Duration",
    queuePosition = "Queue Position",
    dataSource = "Data Source",
    specSummary = "Spec Summary",
    bitDsd = "1 bit (DSD)",
    yesValue = "Yes",
    noOrUnknown = "No / Unknown",
    outputEngineValue = "Daphile audio engine (Squeezelite)",
    outputSpecDsd = "Native/DoP is determined by Daphile device settings",
    outputSpecAuto = "Daphile auto-matches the audio device",
    fixedLevel = "Fixed level %d",
    networkStream = "Network stream",
    localFile = "Local file",
    pathCopied = "Path copied",
    audioOutputNote = "These are the live audio specs reported by LMS. Daphile handles decoding and DAC output; the output sample rate and DSD mode are determined by Daphile audio device settings.",
    copyAction = "Copy",
    seekUnsupported = "Seeking is not supported for this source",
    appShortName = "Daphile Remote",
    onlineWithModel = "Online · %s",
    playerLabel = "Player",
    remove = "Remove",
    scanLibrary = "Scan Music Library",
    clearInput = "Clear",
    scanFinished = "Scan complete",
    recentlyAddedSub = "Browse new albums by date added",
    scanDialogBody = "Ask Daphile to re-check music folders and update added or changed tracks.",
    scanStartedUpdate = "Scan started",
    scanUpdate = "Scan for updates (recommended)",
    rebuildStarted = "Library rebuild started",
    rebuildLibrary = "Rebuild Library",
    rebuildHint = "Rebuilding clears the index and rescans, which may take a while. Progress is shown at the top of the library page.",
    noSearchResultsTitle = "No results found",
    noSearchResultsBody = "Nothing found for \"%s\". Try other keywords.",
    playingAlbum = "Playing %s",
    noArtistsTitle = "No artists",
    noArtistsBody = "No artist information has been scanned yet",
    noGenresTitle = "No genres",
    noGenresBody = "No genre information has been scanned yet",
    noYearsTitle = "No year information",
    noYearsBody = "Library tracks have no release year tags",
    noPlaylistsTitle = "No playlists",
    noPlaylistsBody = "Save the current queue as a playlist from the Now Playing page",
    noContentTitle = "Nothing here yet",
    noContentBody = "Check this plugin or folder in the Daphile web UI",
    musicFolders = "Music folders",
    playFolder = "Play this folder",
    folderEmptyTitle = "Folder is empty",
    folderEmptyBody = "No music files in this folder",
    browseTitle = "Browse",
    noCurrentTrack = "No track is currently playing",
    matchingLyrics = "Matching lyrics…",
    noLyricsHint = "No lyrics\nTap to open the lyrics page",
    lyricsWithSource = "Lyrics · %s",
    noSongsTitle = "No songs",
    noSongsBody = "No tracks have been scanned in the library yet",
    noTracksTitle = "No tracks",
    noTracksBody = "This album has no playable tracks",
    playlistEmptyTitle = "Playlist is empty",
    playlistEmptyBody = "This playlist has no tracks",
    syncApplied = "Sync settings applied",
    syncCancelled = "Sync cancelled",
    powerOff = "Power off",
    powerOn = "Power on",
    syncMaster = "Sync master",
    independentPlayer = "Independent",
    favorites = "Favorites",
    radios = "Radio",
    streamingServices = "Streaming Services",
    noRadiosTitle = "No radio available",
    noRadiosBody = "Install a radio plugin (e.g. TuneIn) in Daphile plugin settings",
    noStreamingTitle = "No streaming services",
    noStreamingBody = "Install TIDAL, Qobuz, Spotty or other plugins in Daphile plugin settings",
    favoritesEmptyTitle = "Favorites is empty",
    favoritesEmptyBody = "Favorite a track on Now Playing, or add favorites from the Daphile web UI",
    playAllItems = "Play All",
    browseContents = "Browse contents"
)

internal val en = UiStrings(
    navNow = "Now Playing",
    navLibrary = "Library",
    navRadio = "Radio",
    navPlayers = "Players",
    navSettings = "Settings",

    back = "Back",
    cancel = "Cancel",
    close = "Close",
    confirm = "OK",
    retry = "Retry",
    refresh = "Refresh",
    search = "Search",
    searchHint = "Search tracks, albums, artists…",
    loading = "Loading…",
    saved = "Saved",

    queue = "Play Queue",
    lyrics = "Lyrics",
    power = "Power",
    more = "More",
    audioOutput = "Audio Output Info",
    matchCover = "Match Cover Online",
    sleepTimer = "Sleep Timer",
    favoriteTrack = "Favorite Current Track",
    saveQueue = "Save Queue as Playlist",
    stop = "Stop",
    clearQueue = "Clear Queue",
    share = "Share",
    audioInfo = "Audio Output Info",
    fixedVolume = "Fixed Volume",
    volumeDisabledHint = "Volume control is disabled in Daphile; adjust on the server",
    mute = "Mute",
    unmute = "Unmute",
    sleepRemaining = "Sleep timer left %s",
    unknownTrack = "Unknown Track",
    noContent = "Nothing Playing",
    selectMusic = "Pick some music to start",
    playerOff = "Player is off",
    prevTrack = "Previous",
    nextTrack = "Next",
    play = "Play",
    pause = "Pause",
    repeatMode = "Repeat",
    listLoop = "Repeat All",
    singleLoop = "Repeat One",
    repeatOff = "Repeat Off",
    shuffleOn = "Enable Shuffle",
    shuffleOff = "Disable Shuffle",
    favorite = "Favorite",
    favorited = "Favorited",
    addedToFavorites = "Added to favorites",
    pausedToast = "Paused",
    playingToast = "Playing",
    noPlayersTitle = "No player available",
    noPlayersBody = "Enable a player in Daphile audio settings, or check the USB DAC",
    viewPlayers = "View Players",
    addServer = "Add Server",
    noServerTitle = "Not connected to Daphile",
    noServerBody = "Add a Daphile host on your LAN to start controlling playback",
    choosePlayer = "Choose Player",
    queueEmptyTitle = "Queue is empty",
    queueEmptyBody = "Pick an album from the library to start",
    goLibrary = "Go to Library",
    clearQueueTitle = "Clear Play Queue",
    clearQueueBody = "Playback will stop. Continue?",
    queueTracks = "%d tracks",
    sleepMinutes = "%d min",
    sleepAfter = "When finished",
    sleepStop = "Stop playback",
    sleepPause = "Pause playback",
    sleepPowerOff = "Power off player",
    start = "Start",
    restart = "Restart",
    cancelTimer = "Cancel Timer",
    playlistName = "Playlist name",
    save = "Save",
    audioOutputTitle = "Audio Output Info",
    matchedCover = "Online cover matched",
    noOnlineCover = "No online cover found",
    shareTitle = "Share Track",
    nothingToShare = "Nothing to share",
    shareOpened = "Share sheet opened",
    sleepStarted = "Sleep Timer",

    playersTitle = "Players",
    currentBadge = "Current",
    playingBadge = "Playing",
    openNowPlaying = "Open Now Playing",
    syncTo = "Sync To",
    unsync = "Unsync",
    syncedTo = "Synced to %s",
    online = "Online",
    offline = "Offline",

    libraryTitle = "Library",
    albums = "Albums",
    songs = "Songs",
    artists = "Artists",
    genres = "Genres",
    years = "Years",
    folders = "Folders",
    playlists = "Playlists",
    randomAlbum = "Shuffle an Album",
    randomAlbumSub = "Pick randomly from the whole library",
    albumUnit = "%d albums",
    songUnit = "%d tracks",
    artistUnit = "%d artists",
    genreUnit = "%d genres",
    browseByYear = "Browse by year",
    browseByFolder = "Browse by music folder",
    serverPlaylists = "Playlists saved on the server",
    viewAlbums = "View albums",
    openPlaylist = "Open playlist",
    openYear = "Albums from this year",

    albumsTitle = "Albums",
    songsTitle = "Songs",
    playAll = "Play All",
    addToQueue = "Add to Queue",
    view = "View",
    sortBy = "Sort",
    sortAlbum = "Album",
    sortArtist = "Artist",
    sortYear = "Year",
    sortNew = "Recently Added",
    listView = "List view",
    gridView = "Grid view",
    noAlbumsTitle = "No albums",
    noAlbumsBody = "There are no albums in this category",
    tracksUnit = "%d tracks",
    loadMore = "Load more (%d/%d)",
    onlineCover = "Match Cover Online",
    matchedCoverToast = "Online cover matched",
    noOnlineCoverToast = "No online cover found",

    lyricsTitle = "Lyrics",
    copyLyrics = "Copy Lyrics",
    exportLrc = "Export .lrc",
    refreshLyrics = "Rematch Lyrics",
    lyricsSaved = "Saved",
    lyricsSourceLabel = "Source",
    sourceAuto = "Auto (NetEase first)",
    sourceNetease = "NetEase Music",
    sourceQq = "QQ Music",
    sourceLrclib = "LRCLIB",
    noLyricsTitle = "No lyrics",
    rematch = "Rematch",
    noTimelineHint = "These lyrics have no timeline; scroll only",
    syncedLabel = "Synced",
    lyricsCopied = "Lyrics copied",
    lyricsExported = "Lyrics exported",
    exportFailed = "Export failed",
    nothingToCopy = "No lyrics to copy",
    nothingToExport = "No lyrics to export",
    lyricsFrom = "Source: %s",

    myPlaylists = "My Playlists",
    myPlaylistsSub = "Custom playlists stored on this device",
    dailyRecommendation = "Daily Mix",
    dailyRecommendationSub = "20 random tracks picked from your library every day",
    regenerate = "Regenerate",
    createPlaylist = "New Playlist",
    renamePlaylist = "Rename",
    deletePlaylist = "Delete Playlist",
    deletePlaylistTitle = "Delete Playlist",
    deletePlaylistBody = "Delete \"%s\"? This cannot be undone.",
    playlistCreated = "Playlist created",
    playlistRenamed = "Playlist renamed",
    playlistDeleted = "Playlist deleted",
    addToPlaylist = "Add to Playlist",
    newPlaylistName = "New playlist name",
    createAndAdd = "Create & Add",
    noPlaylistsHint = "No custom playlists yet. Create one below.",
    addedToPlaylist = "Added to playlist",
    trackCount = "%d tracks",
    saveToMyPlaylists = "Save to My Playlists",
    queueAddedCount = "Added to queue",
    removeFromPlaylist = "Remove from playlist",
    removedFromPlaylist = "Removed from playlist",
    emptyPlaylistsTitle = "No playlists yet",
    emptyPlaylistsBody = "Long-press any song to add it, or create one here",
    emptyPlaylistTitle = "This playlist is empty",
    emptyPlaylistBody = "Long-press a song and choose \"Add to Playlist\"",
    emptyDailyTitle = "No recommendation yet",
    emptyDailyBody = "Tap refresh in the top right to build today's mix",
    addAllToQueue = "Add All to Queue",
    allSongsToQueue = "Queue All Songs",
    allSongsPlayAll = "Play All Songs",
    randomToQueue = "Random to Queue",
    randomToQueueSub = "Choose how many random tracks (1–200)",
    randomCountTitle = "Random to Queue",
    randomCountHint = "Enter a number between 1 and 200",
    randomQueued = "Added %d random tracks",

    settingsTitle = "Settings",
    serverSection = "Server",
    serverManage = "Server Management",
    daphileManage = "Daphile Admin",
    lmsWeb = "LMS Web UI",
    systemInfo = "System Info",
    systemInfoSub = "Server version, players and library stats",
    playbackSection = "Playback & Sync",
    pollInterval = "Status Poll Interval",
    pollIntervalSub = "Polling frequency when push is unavailable",
    mediaNotification = "Media Notification",
    mediaNotificationSub = "Show playback controls in the notification shade",
    lockScreen = "Lock Screen Controls",
    lockScreenSub = "Show cover art and controls on the lock screen",
    onlineMeta = "Online Lyrics & Covers",
    onlineMetaSub = "NetEase, QQ Music, LRCLIB and MusicBrainz",
    appearanceSection = "Appearance",
    theme = "Theme",
    storageSection = "Storage",
    lyricsLibrary = "Lyrics Library",
    clearCoverCache = "Clear Cover Cache",
    clearCoverCacheSub = "Cover art can be reloaded at any time",
    systemSection = "System",
    rescan = "Rescan Music Library",
    rescanSub = "Ask Daphile to re-index your music folders",
    hostPower = "Host Power Control",
    aboutSection = "About",
    version = "Version",
    language = "Language",
    languageAuto = "Follow system",
    languageZh = "简体中文",
    languageEn = "English",
    reboot = "Restart",
    shutdown = "Shutdown",
    suspend = "Suspend",
    seconds = "%d s",
    clearLyricsTitle = "Clear Lyrics Library",
    clearLyricsBody = "Delete %d saved .lrc files? They can be fetched online again later.",
    clear = "Clear",
    lyricsCleared = "Lyrics library cleared",
    rescanTitle = "Rescan Music Library",
    rescanBody = "Trigger a library rescan? The library may be incomplete during scanning.",
    scan = "Scan",
    scanStarted = "Library scan started",

    aboutTitle = "About Daphile Remote",
    aboutAppName = "Daphile Remote",
    aboutIntro = "A third-party Android remote for Daphile / Lyrion Music Server. Built with Jetpack Compose, it brings playback control, library browsing and audio insight to your LAN-based Daphile host.",
    aboutFeatures = "Key Features",
    aboutFeature1 = "Playback control, queue, sleep timer and multi-player switching",
    aboutFeature2 = "Library and folder browsing, instant search, quality badges everywhere",
    aboutFeature3 = "Online lyrics (NetEase / QQ Music / LRCLIB), .lrc export and local lyrics library",
    aboutFeature4 = "Audio output info, online cover matching, lock screen controls and host power",
    aboutAuthor = "Author: panjiasen007-lab",
    aboutLicense = "License: GNU GPL v3.0 (GPL-3.0-only). No personal data is collected; server info and credentials stay on this device.",













    extra = enExtra
)

val LocalStrings = staticCompositionLocalOf { zh }

val strings: UiStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalStrings.current

/** 解析当前生效的界面语言：固定语言直接使用，auto 时按系统语言匹配。 */
@Composable
fun rememberUiLanguage(setting: String): AppLanguage {
    val configuration = LocalConfiguration.current
    val chosen = AppLanguage.fromKey(setting)
    if (chosen != AppLanguage.AUTO) return chosen
    val locale = configuration.locales[0]
    val code = locale.language.lowercase()
    val country = locale.country.uppercase()
    val script = locale.script
    return when {
        code == "zh" && (country == "TW" || country == "HK" || country == "MO" || script == "Hant") ->
            AppLanguage.ZH_TW
        code == "zh" -> AppLanguage.ZH
        else -> AppLanguage.entries.firstOrNull { it.key == code } ?: AppLanguage.EN
    }
}

/** 根据设置与系统语言解析文案表。 */
@Composable
fun rememberUiStrings(setting: String): UiStrings = uiStringsFor(rememberUiLanguage(setting))

internal fun uiStringsFor(language: AppLanguage): UiStrings = when (language) {
    AppLanguage.ZH -> zh
    AppLanguage.ZH_TW -> zhTW
    AppLanguage.EN -> en
    AppLanguage.JA -> ja
    AppLanguage.KO -> ko
    AppLanguage.KO_KP -> koKp
    AppLanguage.RU -> ru
    AppLanguage.DE -> de
    AppLanguage.FR -> fr
    AppLanguage.ES -> es
    AppLanguage.IT -> it
    AppLanguage.PT -> pt
    AppLanguage.NL -> nl
    AppLanguage.EL -> el
    AppLanguage.VI -> vi
    AppLanguage.TH -> th
    AppLanguage.AR -> ar
    AppLanguage.AZ -> az
    AppLanguage.FA -> fa
    AppLanguage.AUTO -> en
}

fun languageDisplayName(language: AppLanguage, strings: UiStrings): String =
    if (language == AppLanguage.AUTO) strings.languageAuto else language.label

fun lyricsSourceName(source: com.daphile.remote.data.model.LyricsSource, strings: UiStrings): String =
    when (source) {
        com.daphile.remote.data.model.LyricsSource.AUTO -> strings.sourceAuto
        com.daphile.remote.data.model.LyricsSource.NETEASE -> strings.sourceNetease
        com.daphile.remote.data.model.LyricsSource.QQ -> strings.sourceQq
        com.daphile.remote.data.model.LyricsSource.LRCLIB -> strings.sourceLrclib
    }
