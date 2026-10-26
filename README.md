# DaphileRemote

Daphile / Lyrion Music Server（LMS 9.x）的 Android 遥控器，界面基于 Jetpack Compose。
An Android remote for Daphile / Lyrion Music Server (LMS 9.x), built with Jetpack Compose.

[中文](#中文) | [English](#english)

---

## 中文

面向本地局域网的 Daphile 主机，提供播放控制、音乐库浏览、歌词、音频输出信息、封面匹配、锁屏媒体控制与主机电源管理。

- 应用显示名：达菲遥控
- 包名：`com.daphile.remote`
- 当前版本：1.6.1（versionCode 8）
- 最低系统：Android 8.0（API 26），目标 API 35

### 功能

#### 播放控制
- 正在播放页：左侧旋转黑胶封面、右侧同步歌词卡，顶栏直接进入队列与歌词；曲名旁显示当前曲目音质徽标
- 火柴人对决动画：位于原频谱位置，两个火柴人随播放进度从左侧打到右侧，轮换直拳、高踢、扫堂腿、腾空回旋踢四种招式，配色与背景随主题变化
- 播放 / 暂停 / 上一首 / 下一首、拖动进度、音量调节、静音；静音按钮位于循环模式按钮右侧
- 音量严格跟随 Daphile 的「音量控制」开关：服务器关闭音量控制时显示「固定音量」并禁用滑块与静音按钮，开启后才可调节
- 随机、列表循环、单曲循环切换
- 一键收藏当前曲目、分享曲目、查看播放队列
- 播放队列：查看、跳播、删除、清空、保存为播放列表
- 睡眠定时
- 多播放器切换、各播放器独立音量与“同步到”其他播放器；播放器卡片内可直接播放/暂停并跳转正在播放

#### 音乐库
- 专辑、艺术家、流派、年份、播放列表、歌曲、文件夹分类浏览
- 我的歌单：本地自定义歌单，可新建、重命名、删除，长按任意歌曲即可加入歌单（支持新建并加入）
- 每日推荐：每天从整个曲库随机生成 20 首，可一键重新生成、全部播放、全部加入队列或存为自定义歌单
- 随机入队：填写 1–200 的数量，确认后从曲库随机抽取并加入播放队列
- 全部歌曲：歌曲页顶部可一键将所有曲目加入队列或全部播放
- 专辑长按快捷操作（播放全部 / 加入队列 / 查看）与“随机播放一张专辑”
- 音质标签：所有曲目列表（歌曲、专辑详情、播放列表、队列、搜索、文件夹）均显示 DSD64/128/256/512、Hi-Res、SQ、HQ、标准 标签，专辑详情汇总展示最佳音质；标签由服务器返回的格式/采样率/位深/码率实时推导，新扫描入库的曲目自动获得
- 按文件夹层级浏览 Daphile 音乐目录（`musicfolder`）
- 最近添加
- 即时搜索（曲目 / 专辑 / 艺术家）
- 扫描更新 / 重建音乐库，并实时显示扫描进度

#### 收藏
- 收藏页顶部支持「全部播放」与「全部加入队列」

#### 歌词
- 优先读取 LMS 本地歌词（`songinfo`）与本地已保存的 `.lrc`
- 歌词源可选：自动（网易云优先）、网易云音乐、QQ 音乐、LRCLIB；选择后持久保存
- 指定歌词源未命中时自动回退显示本地歌词，不会清空页面
- LRC 解析、逐行高亮、自动滚动、点击歌词跳转播放位置
- 复制歌词、导出 `.lrc`（系统文件选择器）
- 歌词按 `艺术家/专辑/曲名.lrc` 保存到应用私有歌词库，可在设置页查看路径、数量并清空
- 本地缓存 30 天

#### 在线封面
- MusicBrainz + Cover Art Archive
- 网易云专辑封面（中文曲库）
- iTunes Search API 兜底
- 按专辑名与艺人加权匹配，结果本地缓存

#### 音频输出
- 音源信息：路径、格式、采样率、位深、码率、声道、无损标识
- 播放器与输出：输出设备、设备型号、输出引擎、输出规格、音量、LMS 版本
- 实时状态：已播放、总时长、队列位置
- 规格标签可点击进入详情，DSD 自动识别为 DSD64/128/256/512

#### 主机与系统
- 电源中心：关闭主机、重启主机、挂起主机（均需二次确认）
- Daphile 管理页（默认 80 端口）、LMS 网页端、系统信息
- 多服务器管理、局域网自动发现
- 凭据使用 Android Keystore AES/GCM 加密存储
- 关于卡片：点击设置 → 版本，弹出应用简介、功能概览、版本号与开源许可说明

#### 多语言
- 界面语言可选：跟随系统 / 简体中文 / 繁體中文 / English / 日本語 / 한국어 / 조선어 / Русский / Deutsch / Français / Español / Italiano / Português / Nederlands / Ελληνικά / Tiếng Việt / ไทย / العربية / Azərbaycan / فارسی，共 20 项，选择即时生效并持久保存
- 阿拉伯语、波斯语为完整本地化文案，界面保持左到右排版，避免整页镜像导致布局倒序
- 每种语言均为全量文案覆盖（约 420 条，含音频输出、系统信息、服务器配置、电源控制、文件夹浏览等全部页面），不依赖英文回退

#### 通知与桌面
- 媒体通知（封面、标题、上一首/暂停/下一首）
- 锁屏媒体卡片（可开关）；播放期间由 `mediaPlayback` 前台服务承载通知并持有唤醒锁，锁屏后进度、曲目与通知持续同步
- 4x1 桌面小部件
- 九种外观主题：跟随系统、深色、浅色、春·青芽、夏·碧海、秋·枫叶、冬·霜雪、太极·阴阳、熊猫·黑白

### 技术栈

- Kotlin 2.0.21、Jetpack Compose（Material 3）、Navigation Compose
- ViewModel + StateFlow
- OkHttp + `org.json`（LMS JSON-RPC：`POST /jsonrpc.js`，`slim.request`）
- DataStore Preferences、Android Keystore
- Coil（封面加载与缓存）
- MediaSessionCompat + 媒体通知 + `mediaPlayback` 前台服务（含部分唤醒锁）

### 构建

要求：JDK 17、Android SDK 35。

```powershell
# Windows
.\gradlew.bat assembleDebug
```

产物：`app/build/outputs/apk/debug/app-debug.apk`

安装：

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 配置

1. 在设置 → 服务器管理中添加服务器：
   - 主机：Daphile 主机 IP，例如 `192.168.124.5`
   - LMS 端口：`9000`（默认）
   - Daphile 管理端口：`80`（电源与 Daphile 管理页使用）
2. 选择播放器。
3. Android 13 及以上首次启动会请求通知权限；如需锁屏/通知栏媒体卡片，请在设置中开启“媒体通知”和“锁屏媒体控制”。

### 权限

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 访问 LMS、在线歌词/封面服务 |
| `ACCESS_NETWORK_STATE` | 连接状态检测 |
| `ACCESS_WIFI_STATE` | 读取本机网段用于局域网服务器扫描 |
| `POST_NOTIFICATIONS` | 媒体通知与锁屏卡片 |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | 播放期间以前台服务承载媒体通知，保证锁屏后仍能同步进度 |
| `WAKE_LOCK` | 锁屏后保持轮询与媒体状态更新 |

### 在线服务与隐私

- 仅将当前曲目的标题、艺人、专辑、时长用于歌词/封面检索。
- 音质标签完全由本机根据服务器返回的元数据推导，不请求第三方服务。
- 不收集账号、不上传音乐文件、不接入统计或广告 SDK。
- 歌词/封面结果仅缓存在本机应用私有目录。
- 使用的第三方服务：网易云音乐公开接口、QQ 音乐公开接口、LRCLIB、MusicBrainz、Cover Art Archive、iTunes Search API。

### 目录结构

```
app/src/main/java/com/daphile/remote/
├── data/
│   ├── local/        # DataStore、Keystore 凭据
│   ├── model/        # 数据模型
│   ├── online/       # 在线歌词与封面
│   ├── remote/       # LMS 客户端与解析
│   └── repo/         # 仓库层（轮询、重连、状态）
├── media/            # MediaSession、媒体通知、前台服务
├── ui/
│   ├── common/       # 通用组件与格式化
│   ├── navigation/   # 路由
│   ├── screens/      # 各页面
│   └── theme/        # 主题
└── widget/           # 桌面小部件
```

### 已知限制

- 歌词与封面依赖第三方公开接口，曲目信息不规范时可能匹配失败。
- 主机电源操作要求手机能直接访问 Daphile 的 80 端口；跨网段时需自行做端口转发。
- 部分服务器不返回码率/无损字段时，音频信息会显示为“—”或“未知”。
- 扫描进度取自 `rescanprogress`，不同 LMS 版本的字段可能略有差异。

### 版本

- 1.6.1：修复锁屏后歌曲进度不更新（新增 `mediaPlayback` 前台服务承载媒体通知并持有唤醒锁）；取消阿拉伯语、波斯语的整页 RTL 镜像，界面保持左到右排版。
- 1.6.0：新增本地自定义歌单（新建 / 重命名 / 删除 / 长按歌曲加入 / 队列存为歌单）；每日推荐（每天随机 20 首，可重新生成、存为歌单）；随机 1–200 首加入队列；歌曲页全部入队 / 全部播放；收藏页全部播放 / 全部入队；界面语言扩展至 20 种。
- 1.5.0：正在播放页原频谱位置改为火柴人对决动画（随播放进度左右推进、四种招式、随主题变色）；静音按钮移至循环按钮右侧；音量严格跟随达菲「音量控制」开关，关闭时禁用调节与静音；设置页新增「关于」卡片与语言选择。
- 1.4.0：全量音质标签体系（DSD64/128/256/512、Hi-Res、SQ、HQ、标准），覆盖歌曲/专辑/播放列表/队列/搜索/文件夹等全部曲目列表与正在播放页、专辑详情页；搜索结果补充完整音频元数据。
- 1.3.0：歌词源可选（自动网易云优先 / 网易云 / QQ 音乐 / LRCLIB）并持久保存，未命中自动回退本地；正在播放页移除频谱与底部歌词/队列按钮，歌词入口移到顶栏队列与电源之间，放大封面与歌词卡；歌词页新增复制与导出 `.lrc`；设置页新增歌词库路径/数量/清空；专辑长按操作与随机播放、播放器卡片播放控制。
- 1.2.0：正在播放页重构（旋转黑胶、同步歌词卡、实时频谱），控制按钮文字提示与收藏按钮，新增春/夏/秋/冬/太极/熊猫主题。
- 1.1.0：新增歌词、音频输出信息、文件夹浏览、扫描、在线封面、锁屏媒体卡片、主机电源中心；修复队列跳转导航与通知权限问题。
- 1.0.0：首个可用版本（播放控制、音乐库、多服务器、小部件）。

### 许可证

本项目基于 [GNU General Public License v3.0](LICENSE) 开源，SPDX 标识：`GPL-3.0-only`。

Copyright (C) 2026 panjiasen007-lab

你可以自由使用、修改和分发本软件，但分发修改后的版本时必须以相同许可证公开源代码。详见 [LICENSE](LICENSE)。

---

## English

An Android remote for a Daphile host on your local network: playback control, library browsing, lyrics, audio output details, cover matching, lock-screen media controls and host power management.

- App label: 达菲遥控 (Daphile Remote)
- Package: `com.daphile.remote`
- Current version: 1.6.1 (versionCode 8)
- Minimum: Android 8.0 (API 26), target API 35

### Features

#### Playback
- Now Playing: spinning vinyl cover on the left, synced lyrics card on the right, with queue and lyrics entries in the top bar; the quality badge sits next to the track title
- Stick-figure duel animation in the former spectrum area: two fighters advance from left to right with playback, cycling four moves (jab, high kick, sweep, spinning kick) and taking on the current theme colors
- Play / pause / previous / next, seek, volume and mute; the mute button sits to the right of the repeat button
- Volume strictly follows Daphile's volume-control switch: when disabled, "Fixed volume" is shown and the slider and mute button are locked
- Shuffle, list repeat and single repeat
- Favorite the current track, share it, open the queue
- Queue: browse, jump, remove, clear, save as a playlist
- Sleep timer
- Multi-player switching, per-player volume and "sync to" another player; player cards can play/pause and jump to Now Playing

#### Library
- Browse albums, artists, genres, years, playlists, songs and folders
- My Playlists: local custom playlists with create, rename and delete; long-press any song to add it to a playlist (including creating a new one on the spot)
- Daily Recommendations: 20 random tracks from the whole library each day, with regenerate, play all, add all to queue and save as a playlist
- Random queue: enter 1-200 tracks and add a random selection to the queue
- All songs: add every track to the queue or play them all from the top of the Songs screen
- Long-press album shortcuts (play all / add to queue / view) and "play a random album"
- Quality badges: every track list (songs, album detail, playlists, queue, search, folders) shows DSD64/128/256/512, Hi-Res, SQ, HQ or Standard; album detail summarizes the best quality. Badges are derived locally from the server's format, sample rate, bit depth and bitrate, so newly scanned tracks get them automatically
- Browse the Daphile music folder hierarchy (`musicfolder`)
- Recently added
- Instant search (tracks / albums / artists)
- Scan for updates or rebuild the library, with live progress

#### Favorites
- Play all or add all favorites to the queue from the top of the Favorites screen

#### Lyrics
- Reads LMS lyrics (`songinfo`) and locally saved `.lrc` files first
- Selectable source: Auto (NetEase first), NetEase Music, QQ Music, LRCLIB, persisted across launches
- Falls back to local lyrics when the chosen source has no match, without clearing the page
- LRC parsing, line highlighting, auto-scroll, tap a line to seek
- Copy lyrics, export `.lrc` via the system file picker
- Lyrics are stored in the app's private library as `artist/album/title.lrc`; Settings shows the path, file count and a clear action
- 30-day local cache

#### Online covers
- MusicBrainz + Cover Art Archive
- NetEase album covers (Chinese catalog)
- iTunes Search API fallback
- Weighted album/artist matching with local caching

#### Audio output
- Source: path, format, sample rate, bit depth, bitrate, channels, lossless flag
- Player and output: output device, model, engine, output spec, volume, LMS version
- Live state: elapsed, total duration, queue position
- Spec badges open details; DSD is detected as DSD64/128/256/512

#### Host and system
- Power center: shut down, restart or suspend the host (each requires confirmation)
- Daphile admin page (port 80 by default), LMS web UI, system info
- Multi-server management and LAN auto-discovery
- Credentials encrypted with Android Keystore AES/GCM
- About card: Settings → Version opens an app introduction, feature overview, version and license notes

#### Languages
- UI language: follow system / Simplified Chinese / Traditional Chinese / English / Japanese / Korean / Korean (DPRK) / Russian / German / French / Spanish / Italian / Portuguese / Dutch / Greek / Vietnamese / Thai / Arabic / Azerbaijani / Persian - 20 options, applied instantly and persisted
- Arabic and Persian are fully localized while the layout stays left-to-right, avoiding the reversed layout caused by whole-page mirroring
- Every language covers the full string set (about 420 strings, including audio output, system info, server setup, power control and folder browsing) with no English fallback

#### Notifications and home screen
- Media notification (cover, title, previous / pause / next)
- Lock-screen media card (toggleable); while playing, a `mediaPlayback` foreground service hosts the notification and holds a wake lock, so progress, track and notification keep updating after the screen locks
- 4x1 home-screen widget
- Nine themes: follow system, dark, light, Spring, Summer, Autumn, Winter, Taiji and Panda

### Tech stack

- Kotlin 2.0.21, Jetpack Compose (Material 3), Navigation Compose
- ViewModel + StateFlow
- OkHttp + `org.json` (LMS JSON-RPC: `POST /jsonrpc.js`, `slim.request`)
- DataStore Preferences, Android Keystore
- Coil (cover loading and caching)
- MediaSessionCompat + media notification + `mediaPlayback` foreground service (with a partial wake lock)

### Build

Requires JDK 17 and Android SDK 35.

```powershell
# Windows
.\gradlew.bat assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`

Install:

```powershell
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Setup

1. Add a server under Settings → Servers:
   - Host: Daphile IP, e.g. `192.168.124.5`
   - LMS port: `9000` (default)
   - Daphile admin port: `80` (used for power actions and the admin page)
2. Pick a player.
3. On Android 13+ the app asks for notification permission on first launch; enable "Media notification" and "Lock-screen controls" in Settings to get the media card.

### Permissions

| Permission | Purpose |
| --- | --- |
| `INTERNET` | Access LMS and the online lyrics/cover services |
| `ACCESS_NETWORK_STATE` | Connection state detection |
| `ACCESS_WIFI_STATE` | Read the local subnet for LAN discovery |
| `POST_NOTIFICATIONS` | Media notification and lock-screen card |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Host the media notification in a foreground service so progress keeps updating while locked |
| `WAKE_LOCK` | Keep polling and media state updates running after the screen locks |

### Online services and privacy

- Only the current track's title, artist, album and duration are used for lyrics/cover lookups.
- Quality badges are derived locally from server metadata; no third-party service is called.
- No accounts, no music uploads, no analytics or ad SDKs.
- Lyrics and cover results are cached only in the app's private directory.
- Third-party services used: NetEase Music public API, QQ Music public API, LRCLIB, MusicBrainz, Cover Art Archive, iTunes Search API.

### Project layout

```
app/src/main/java/com/daphile/remote/
├── data/
│   ├── local/        # DataStore, Keystore credentials
│   ├── model/        # Data models
│   ├── online/       # Online lyrics and covers
│   ├── remote/       # LMS client and parsing
│   └── repo/         # Repository (polling, reconnect, state)
├── media/            # MediaSession, media notification, foreground service
├── ui/
│   ├── common/       # Shared components and formatting
│   ├── navigation/   # Routes
│   ├── screens/      # Screens
│   └── theme/        # Themes
└── widget/           # Home-screen widget
```

### Known limitations

- Lyrics and covers rely on third-party public APIs; unusual track metadata may fail to match.
- Power actions require direct access to port 80 on the Daphile host; use port forwarding across subnets.
- When a server omits bitrate or lossless fields, audio details show "—" or "unknown".
- Scan progress comes from `rescanprogress`; field names vary slightly between LMS versions.

### Versions

- 1.6.1: Fixed playback progress freezing after the screen locks (added a `mediaPlayback` foreground service hosting the media notification with a wake lock); removed whole-UI RTL mirroring for Arabic and Persian so the layout stays left-to-right.
- 1.6.0: Local custom playlists (create / rename / delete / long-press to add / save queue as playlist); Daily Recommendations (20 random tracks per day, regenerate, save as playlist); random 1-200 tracks to the queue; add all songs to queue or play all; play all or add all favorites; UI languages expanded to 20.
- 1.5.0: The former spectrum area on Now Playing became a stick-figure duel animation (advances with playback, four moves, theme colors); mute moved to the right of repeat; volume strictly follows Daphile's volume-control switch; added the About card and language selection in Settings.
- 1.4.0: Full audio-quality badge system (DSD64/128/256/512, Hi-Res, SQ, HQ, Standard) across every track list, Now Playing and album detail; search results include complete audio metadata.
- 1.3.0: Selectable lyrics source (Auto with NetEase first / NetEase / QQ Music / LRCLIB) with persistence and local fallback; removed the spectrum and bottom lyrics/queue buttons, moved the lyrics entry between queue and power with a larger cover and lyrics card; copy and `.lrc` export; lyrics library path, count and clear action in Settings; album long-press actions, shuffle play and player-card controls.
- 1.2.0: Rebuilt Now Playing (spinning vinyl, synced lyrics card, real-time spectrum), labeled controls and a favorite button, plus Spring / Summer / Autumn / Winter / Taiji / Panda themes.
- 1.1.0: Added lyrics, audio output info, folder browsing, scanning, online covers, lock-screen media card and host power center; fixed queue-jump navigation and notification permission issues.
- 1.0.0: First usable release (playback control, library, multi-server, widget).

### License

This project is open source under the [GNU General Public License v3.0](LICENSE), SPDX identifier `GPL-3.0-only`.

Copyright (C) 2026 panjiasen007-lab

You may use, modify and distribute this software freely, but modified versions must be released under the same license with source code available. See [LICENSE](LICENSE) for details.
