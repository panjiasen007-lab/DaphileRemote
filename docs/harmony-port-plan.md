# DaphileRemote 原生鸿蒙迁移方案 A：ArkTS 全量重写

> 目标版本：功能对齐 Android 1.9.2（versionCode 14）
> 目标平台：HarmonyOS NEXT（Stage 模型，ArkTS/ArkUI）
> 方案定位：新建 `harmony/` 工程重写，不复用 Kotlin 代码，不做 APK 兼容层、不做 WebView/Flutter 套壳。
>
> 实施状态：已完成并发布 v1.9.2 鸿蒙版（2026-11-28），发布资产见 GitHub Releases。

---

## 0. 结论摘要

1. **可行**：本项目没有本地音频解码与播放引擎，核心是「局域网 HTTP JSON-RPC 遥控 + 声明式 UI + Canvas 自绘」，鸿蒙均有对应能力。
2. **工作量结构**：2.7 万行 Kotlin 中，约 1.25 万行（协议层 3.3k + 多语言 9.2k）可机械迁移；约 9k 行页面需按 ArkUI 重写；约 5.4k 行自绘主题需重画；系统能力 643 行全部替换。
3. **三个必须提前验证的点**：
   - 纯遥控端能否长期驻留后台（长时任务一致性校验）；
   - AVSession 在「不自带播放」的场景能否稳定出现在锁屏/控制中心；
   - ArkTS 是否具备 GBK/Big5 解码能力（乱码修复依赖）。
4. **明确降级项**（若验证不通过）：
   - 锁屏进度由 AVSession 播放状态插值，曲目变化仅在应用前台刷新；
   - 睡眠定时仅在应用存活期间可靠；
   - 桌面卡片只显示快照，不做实时进度。

---

## 1. 范围与目标

### 1.1 平台基线

| 项目 | 取值 |
| --- | --- |
| 系统 | HarmonyOS NEXT 5.0 及以上（最低 API 12） |
| 开发工具 | DevEco Studio 当前稳定版 |
| 语言 | ArkTS（严格模式），禁止 `any` 滥用 |
| 模型 | Stage 模型，单 `entry` HAP |
| 构建 | Hvigor（`hvigorw assembleHap`） |
| 包名 | `com.daphile.remote` |
| 版本 | 首发 `1.9.2`（与 Android 功能对齐），后续独立演进 |
| 设备 | 手机 / 折叠屏 / 平板 / PC 自适应布局；自动识别设备并支持设置内手动切换 |

### 1.2 功能范围（对齐 1.9.2）

- 播放控制：播放/暂停/上一首/下一首/进度/音量/静音/循环/随机、多播放器切换与同步、睡眠定时。
- 音乐库：专辑、艺术家、流派、年份、播放列表、歌曲、文件夹、最近添加、即时搜索、扫描与进度。
- 收藏、电台与插件浏览、单条入队。
- 本地自定义歌单（新建/重命名/删除/长按加入/队列存为歌单）。
- 每日推荐 20 首、随机 1–200 首入队、全部歌曲入队/播放、收藏全部播放/入队。
- 歌词：LMS 本地、本地 `.lrc`、网易云/QQ/LRCLIB、逐行高亮、复制、导出、30 天缓存。
- 在线封面：MusicBrainz + CAA、网易云、iTunes，本地缓存。
- 音频输出信息、系统信息、主机电源、Daphile/LMS 网页入口、多服务器、局域网发现、WOL。
- 通知与锁屏媒体控制、4x1 桌面卡片。
- 14 套外观主题（跟随系统、深色、浅色、春夏秋冬、太极、熊猫、中国红、风花雪月）。
- 20 种界面语言（含繁体、阿拉伯语、波斯语），保持 LTR，无英文回退。

### 1.3 非目标

- 不做本地播放、投屏接收、DLNA 渲染。
- 不兼容 HarmonyOS 4.x（该版本直接安装现有 APK）。
- 不引入 Kotlin Multiplatform、Flutter、React Native、WebView 壳。
- 不做 Android → 鸿蒙的本地数据迁移（沙箱不同，后续可做导入导出）。
- 不做多窗口/自由窗口专属交互（使用系统默认窗口管理）；平板/PC 侧边导航与内容限宽已实现。

---

## 2. 架构设计

### 2.1 工程结构

```text
harmony/
├── AppScope/
│   ├── app.json5                  # bundleName / 版本 / 图标
│   └── resources/
├── entry/
│   ├── src/main/
│   │   ├── module.json5           # 权限、Ability、FormExtensionAbility、network 配置
│   │   ├── resources/
│   │   │   ├── base/element/      # app_name 等系统级字符串
│   │   │   ├── base/media/        # 应用图标、通知小图标
│   │   │   └── rawfile/           # 内置字体/占位图（如有）
│   │   └── ets/
│   │       ├── entryability/EntryAbility.ets
│   │       ├── entryformability/PlayerFormAbility.ets
│   │       ├── pages/Index.ets    # 根页面：Tabs + Navigation
│   │       ├── app/AppContext.ets # 等价 AppGraph，手工单例
│   │       ├── data/
│   │       │   ├── model/Models.ets
│   │       │   ├── remote/LmsClient.ets
│   │       │   ├── remote/LmsParser.ets
│   │       │   ├── remote/WolClient.ets
│   │       │   ├── local/SettingsStore.ets
│   │       │   ├── local/CryptoStore.ets
│   │       │   ├── online/OnlineMetadataService.ets
│   │       │   └── repo/DaphileRepository.ets
│   │       ├── media/AvSessionController.ets
│   │       ├── media/PlaybackPolicy.ets
│   │       ├── ui/screens/        # 20 个页面
│   │       ├── ui/components/     # 列表、曲目行、徽标、封面
│   │       ├── ui/art/            # 背景、场景、封面框、滑块、黑胶、火柴人
│   │       ├── ui/theme/          # 14 套配色与 ThemeArt
│   │       ├── i18n/              # UiStrings + 20 语言（生成物）
│   │       ├── widget/pages/      # 卡片 ArkTS 页面
│   │       └── util/              # 日志、格式化、防抖、哈希
│   ├── src/ohosTest/              # 单元测试
│   └── src/test/                  # 本地测试（可选）
├── tools/i18n-gen/                # 文案生成脚本（仓库内，产物入库）
├── hvigorfile.ts
├── build-profile.json5
└── oh-package.json5
```

### 2.2 分层与依赖方向

```text
UI (pages/components/art)
        ↓ 只读快照 + 命令调用
DaphileRepository（轮询、状态机、业务编排）
        ↓
LmsClient / LmsParser / WolClient      SettingsStore / CryptoStore      OnlineMetadataService
        ↓                                     ↓                              ↓
@kit.NetworkKit http/socket          @kit.ArkData preferences + HUKS    http + fs + ImageKit
```

- **禁止 UI 直接发 HTTP**：所有 LMS 命令必须经 Repository，保证状态刷新一致。
- **AppContext**：`EntryAbility.onCreate` 创建并挂到全局单例，替代 `AppGraph`/`DaphileApp`。
- **状态管理统一使用 V2**（`@ObservedV2` / `@Trace`），禁止 V1/V2 混用；页面局部状态用 `@Local`，跨页面状态读单例。
- **ArkTS 无线程安全问题**：原 Kotlin 的 `Mutex`/`Semaphore` 改为「在途标记 + Promise 队列」；不要照搬协程调度器。
- **轮询生命周期**：`EntryAbility.onForeground` 启动，`onBackground` 停止（或进入仅 AVSession 模式，见 5.1）。

### 2.3 Android → 鸿蒙 依赖映射

| Android 依赖 | 鸿蒙替代 | 说明 |
| --- | --- | --- |
| Jetpack Compose | ArkUI 声明式组件 | 布局/状态概念相近，语法全换 |
| Navigation Compose | `Navigation` + `NavPathStack` | 路由对象化，不再用字符串 URL |
| ViewModel + StateFlow | `@ObservedV2/@Trace` 单例 + 监听回调 | 无 Flow，用快照 + 订阅 |
| OkHttp | `@kit.NetworkKit` 的 `http` | POST JSON-RPC、Basic Auth、超时 |
| `org.json` | 原生 `JSON.parse/stringify` | ArkTS 严格类型，需显式接口 + 守卫 |
| DataStore Preferences | `@kit.ArkData` 的 `preferences` | 键值 + JSON 字符串 |
| Android Keystore AES/GCM | HUKS（`@kit.UniversalKeystoreKit`） | 密钥不可导出，密文存 preferences |
| Coil | `Image` + 自建内存/磁盘缓存 | 封面 URL 来自 LMS |
| MediaSessionCompat / NotificationCompat | AVSession Kit + Notification Kit | 需验证遥控端场景 |
| 前台服务 + WakeLock | 长时任务（受业务类型校验） | **不能伪造类型**，见 5.1 |
| AppWidgetProvider/RemoteViews | Form Kit（FormExtensionAbility + ArkTS 卡片） | 刷新有配额 |
| Kotlin 协程 | `async/await` + `setTimeout`/`setInterval` | 无结构化并发，需自行管理句柄 |
| `MessageDigest` | `@kit.CryptoArchitectureKit` | 缓存键哈希 |
| `java.io.File` | `@kit.CoreFileKit` 的 `fs` | 沙箱目录 |
| `URLEncoder` | `encodeURIComponent` | 路径/查询编码 |
| `SimpleDateFormat` | 手写格式化或 `Intl` | 避免时区差异 |
| `UUID` | `util.generateRandomUUID()` | 歌单 ID |

---

## 3. 数据与协议层设计

### 3.1 文件对应关系

| 现有文件 | 行数 | ArkTS 目标 | 处理方式 |
| --- | ---: | --- | --- |
| `data/model/Models.kt` | 266 | `data/model/Models.ets` | 逐类型改成 `interface`/`class`；`LmsException` 改为 `LmsError` + `Error` 子类 |
| `data/model/TrackDisplay.kt` | 127 | `data/model/TrackDisplay.ets` | 评分算法照搬；字符集解码能力待验证 |
| `data/remote/LmsClient.kt` | 335 | `data/remote/LmsClient.ets` | `http.createHttp()`；命令列表逐条搬 |
| `data/remote/LmsParser.kt` | 425 | `data/remote/LmsParser.ets` | 解析函数 1:1；`optJSONObject` 改为属性守卫 |
| `data/remote/WolClient.kt` | 67 | `data/remote/WolClient.ets` | UDP 广播魔术包 |
| `data/local/SettingsStore.kt` | 325 | `data/local/SettingsStore.ets` | Preferences；JSON 字段名保持一致 |
| `data/local/CryptoStore.kt` | 67 | `data/local/CryptoStore.ets` | HUKS AES/GCM + Base64 |
| `data/online/OnlineMetadataService.kt` | 585 | `data/online/OnlineMetadataService.ets` | 歌词/封面请求、LRC 解析、缓存目录 |
| `data/repo/DaphileRepository.kt` | 1067 | `data/repo/DaphileRepository.ets` | 状态机与业务方法逐段搬；Flow 改快照通知 |
| `AppGraph.kt` / `DaphileApp.kt` | 36 | `app/AppContext.ets` | 手工单例 |
| `ui/navigation/Routes.ets` | 44 | `ui/navigation/Routes.ets` | 路由名常量 + 参数类型 |
| `media/*` | 413 | `media/AvSessionController.ets` + `media/PlaybackPolicy.ets` | 完全重写 |
| `widget/*` | 230 | `entryformability/` + `widget/pages/PlayerCard.ets` | 完全重写 |

### 3.2 LMS 客户端

- 请求体保持完全一致：
  ```json
  {"id":1,"method":"slim.request","params":["<playerId>",["<cmd>","<arg>"]]}
  ```
- `POST {baseUrl}/jsonrpc.js`，`Content-Type: application/json; charset=utf-8`。
- Basic Auth：鸿蒙无 `Credentials.basic`，用 `btoa(username:password)`（注意 UTF-8 用户名需先编码）。
- 超时：连接 5s / 读取 12s / 总 18s；鸿蒙 `http` 提供 `connectTimeout`、`readTimeout`，总超时用 `Promise.race` 包装。
- 错误映射保持：HTTP 401/403 → 认证失败；响应以 `<` 开头 → 需要登录 Daphile；非 JSON → 解析失败；`result._error` → 业务错误。
- 明文 HTTP：在 `module.json5` 配置网络安全策略；不同 API 版本默认策略不同，必须显式声明并在真机验证（见 5.6）。

### 3.3 解析与分页

- 保留 `TRACK_TAGS = "cdelgoaIirstTyquQ"`，音质字段来源不变。
- 保留 `Paged<T>` 语义：`count` 是本次返回条数，翻页依据 `hasMore`，不能把 `count` 当总数。
- 所有列表解析保留去重与重复 key 防护（1.9 修复点），ArkUI `ForEach` 必须使用稳定业务 key，禁止索引 key。
- `search` 仍需二次用 `titles + search:` 拉完整音质字段。

### 3.4 仓库状态机

`DaphileRepository` 对外暴露一个不可变快照 `AppSnapshot`：

| 字段 | 来源 | 说明 |
| --- | --- | --- |
| `connection` | 轮询 | Idle/Connecting/Connected/Reconnecting/Lost |
| `server` / `servers` | 设置 | 当前服务器 |
| `players` / `currentPlayerId` | `players` + `status` | 30 秒刷新一次 |
| `nowPlaying` | `status` + 队列 | 曲目、进度、音量、循环、随机 |
| `serverInfo` / `scanStatus` / `wakeState` | 各接口 | 系统信息与扫描 |
| `lastError` | 异常 | 供连接横幅显示 |

轮询规则原样保留：

1. 服务器信息为空或失败过 → 先拉 `serverstatus`。
2. 播放器列表为空或超过 30 秒 → 刷新播放器。
3. `quickStatus` 每 `pollSeconds`（2–30 秒）一次。
4. 满足任一条件才拉全队列：无缓存、队列为空、`playlist_tracks` 变化、`playlist_timestamp` 变化、当前曲目 id 变化。
5. 失败指数退避 1s→60s；断连 5 分钟判 Lost；自动唤醒 10 秒后触发、60 秒冷却。

### 3.5 随机算法

保留「客户端多起点采样」实现，绕过 LMS `sort:random` 同会话固定顺序：

- 先取曲库总数 `titles 0 0`。
- 总数 ≤ 目标数 → 全量取回后洗牌。
- 否则按 12 段采样、`LinkedHashMap` 去重、最多 48 次尝试，最后洗牌取前 N。
- 随机专辑同理：读总数后随机起点取 1 张。

### 3.6 本地数据

- Preferences 键名与 Android 保持一致：`servers`、`current_server_id`、`theme`、`poll_seconds`、`lyrics_source`、`app_language`、`custom_playlists`、`daily_recommendation`、`player_<serverId>` 等。
- 自定义歌单仍本地存储（不走 LMS playlists edit），结构与 Android 完全一致（id/name/tracks/createdAt/updatedAt）。
- 密码字段写入前用 HUKS 加密；服务器 JSON 中仅存密文。
- 每日推荐按 `yyyy-MM-dd` 复用，支持强制重新生成。

### 3.7 歌词与封面

- 目录：`filesDir/lyrics`（用户可见副本，按 `艺术家/专辑/曲名.lrc`）、`filesDir/lyrics_cache`（30 天缓存）、`filesDir/covers`（封面缓存）。
- 歌词顺序：本地 `.lrc` → LMS `songinfo.lyrics` → 在线源（自动：网易云 → QQ → LRCLIB）。
- 封面顺序：MusicBrainz/CAA → 网易云 → iTunes；缓存键为 `艺术家|专辑` 哈希。
- 导出 `.lrc` 使用 `@ohos.file.picker` 的 DocumentViewPicker。
- 清除歌词库：递归删除并重建目录，返回统计值。

---

## 4. UI、主题与多语言

### 4.1 页面清单与文件对应

| Android 页面 | ArkTS 页面 | 路由名 | 关键点 |
| --- | --- | --- | --- |
| `NowPlayingScreen` | `ui/screens/NowPlayingPage.ets` | `now` | 黑胶、歌词卡、场景动画、控制、音量、队列弹层、睡眠/保存队列对话框 |
| `LibraryScreen` | `LibraryPage.ets` | `library` | 搜索、扫描进度、分类入口、随机入队 |
| `RadioScreen` | `RadioPage.ets` | `radio` | 电台/应用/插件浏览 |
| `PlayersScreen` | `PlayersPage.ets` | `players` | 多播放器、独立音量、同步到 |
| `SettingsScreen` | `SettingsPage.ets` | `settings` | 服务器、主题、语言、歌词源、通知、缓存、关于 |
| `ServerListScreen` | `ServerListPage.ets` | `servers` | 多服务器、局域网发现 |
| `ServerEditScreen` | `ServerEditPage.ets` | `serverEdit` | 测试连接、仅保存、MAC |
| `AlbumsScreen` | `AlbumsPage.ets` | `albums` | 专辑/艺人/流派/年份筛选、排序、长按菜单 |
| `AlbumDetailScreen` | `AlbumDetailPage.ets` | `album` | 最佳音质汇总、播放全部/入队 |
| `PlaylistDetailScreen` | `PlaylistDetailPage.ets` | `playlist` | 服务器歌单详情 |
| `BrowseScreen` | `BrowsePage.ets` | `browse` | 分类列表容器 |
| `BrowserScreen` | `BrowserPage.ets` | `browser` | 电台/插件条目 |
| `SongsScreen` | `SongsPage.ets` | `songs` | 分页、全部入队/播放 |
| `FolderBrowserScreen` | `FolderBrowserPage.ets` | `folders` | 层级浏览、重复 key 防护 |
| `LyricsScreen` | `LyricsPage.ets` | `lyrics` | 逐行高亮、点击定位、复制/导出 |
| `AudioOutputScreen` | `AudioOutputPage.ets` | `audioOutput` | 音源、输出设备、规格 |
| `MyPlaylistsScreen` | `MyPlaylistsPage.ets` | `myPlaylists` | 自定义歌单增删改 |
| `MyPlaylistsScreen.CustomPlaylistScreen` | `CustomPlaylistPage.ets` | `myPlaylist` | 歌单详情、移除曲目 |
| `MyPlaylistsScreen.DailyRecommendationScreen` | `DailyRecommendationPage.ets` | `daily` | 20 首、重新生成、播放/入队/存歌单 |
| `SystemInfoScreen` | `SystemInfoPage.ets` | `systemInfo` | 服务器统计、扫描/重建 |

公共组件：`Components.kt`（曲目行、封面、空态、加载态、音质徽标、区块卡片）→ `ui/components/`；`SpinningVinyl`、`StickFight`、`ThemeScenes`、`ThemeBackdrops`、`ThemedControls` → `ui/art/`。

### 4.2 导航

- 根页面 `Index.ets`：`Tabs` 五个 Tab（正在播放/音乐库/电台/播放器/设置）+ 内层 `Navigation`。
- 二级页面压入 `NavPathStack`，参数用对象，不再拼接 URL；专辑/歌单等 ID 做 `encodeURIComponent` 的场景消失。
- 返回栈行为与 Android 一致：Tab 切换保留各自栈，`launchSingleTop` 的语义用 `pushPath` 前查重实现。
- 全局根节点强制 `direction: Direction.Ltr`（阿拉伯语/波斯语也不镜像）。

### 4.3 主题体系

保留双层设计：

1. **配色层**：14 套主题（`System/Dark/Light/Spring/Summer/Autumn/Winter/Taiji/Panda/ChinaRed/Wind/Flower/Snow/Moon`），每套有明暗两套 `ColorScheme` 等价物（ArkUI 用 `@Provide` 主题令牌或全局 `ThemeTokens`）。
2. **元素层**：`ThemeArt` 四件套：
   - 背景 `ThemeBackdropKind`：Pulse、Lattice、WindField、Blossom、Snowfall、MoonSky、Starfield、PaperGrid、WillowBreeze、OceanWaves、MapleFall、FrostBloom、YinYang、BambooShade。
   - 场景 `ThemeSceneKind`：StickFight、ShadowPuppet、Kites、Butterflies、SnowChase、MoonSword、SoundWave、PaperPlane、Swallows、Sailboat、Geese、Skater、PushHands、PandaBamboo。
   - 封面框 `ThemeCoverFrame`：None、LatticeRing、Windmill、BlossomWindow、Frost、MoonPhase、HaloRing、ThinFrame、BambooRing、WaveCrest、MapleWreath、IceCrystal、Bagua、BambooLeaf。
   - 滑块 `ThemeSliderStyle`：Default、RuyiBar、SeedThumb、BudThumb、Icicle、SilverLine、GlowDot、HairLine、LeafThumb、ShellThumb、MapleThumb、FrostNeedle、YinYangThumb、PawThumb。

实现约定：

- 每套主题的几何/动画公式从 Kotlin 源码移植，绘制 API 换成 ArkUI `Canvas` + `CanvasRenderingContext2D`。
- 统一 `Ticker`：页面可见且播放中才启动，30fps 上限（可配置 60fps），退后台立即停止。
- 进度驱动量（火柴人位置、风筝线、雪花位移）以播放进度为主时钟，避免与轮询时间漂移。
- 所有自绘组件必须支持「静态模式」：电量低/不可见/主题预览时只画一帧。

### 4.4 多语言

- 结构：`i18n/UiStrings.ets` 定义 interface（约 420 个字段），每种语言一个 `const` 对象；`zh` 为基准，`AUTO` 回退 `en`，其余 19 种全量覆盖。
- **不采用 resources/string.json**：420 × 20 ≈ 8400 条资源，动态切换语言需改系统配置，且容易出现英文回退；继续沿用「内存文案表 + 设置即时生效」。
- **代码生成**：`tools/i18n-gen` 解析 `Strings.kt` / `StringsIntl.kt` / `StringsIntl2.kt` 的 `UiStrings(...)` 实例，生成 `i18n/locales/*.ets`。生成物入库，禁止手改。
- 校验脚本：字段数量一致、无空值、无缺失、无多余；生成失败即阻断构建。
- 语言解析：固定语言直接使用；`auto` 时按系统语言，`zh-TW/HK/MO/Hant` → 繁体，其余匹配语言码，未匹配 → 英文。
- 设置页语言对话框：20 项可滚动列表，选择后立即生效并持久化。

### 4.5 曲名显示与乱码修复

- 保留开关：`useFileNameAsTitle`（从 LMS url 提取文件名）、`fixGarbledText`（乱码修复）。
- 评分/严格往返编码逻辑照搬。
- **风险**：ArkTS `util.TextDecoder` 的字符集支持有限（UTF-8/UTF-16 确定可用，GBK/Big5/Shift_JIS 待验证）。若不可用：
  - 首版先支持 ISO-8859-1/Windows-1252 ↔ UTF-8 的修复（覆盖大多数 UTF-8 误读）；
  - GBK/Big5 通过 NAPI 引入轻量 iconv，或暂不修复并在设置页说明；
  - 该能力列入 Phase 0 验证项（S5）。

---

## 5. 系统能力设计

### 5.1 媒体会话、通知与后台（最高风险）

现状：Android 用 `mediaPlayback` 前台服务 + 唤醒锁，保证锁屏后继续轮询。鸿蒙的长时任务要求「任务类型与真实业务一致」并做一致性校验，纯遥控端不能合法申请 `AUDIO_PLAYBACK`。

设计：

1. **AVSession**：创建 `audio` 类型会话，写入 metadata（标题/艺人/专辑/时长/封面）、playbackState（状态/进度/倍速/可用动作）、queueItems；注册 play/pause/stop/next/previous/seek 回调，转发给 Repository。
2. **前台**：正常轮询（2–30 秒），状态变化即时更新 AVSession。
3. **退后台**：
   - 停止轮询；
   - AVSession 保持 `PLAYING` 状态并带 `speed=1`，系统 UI 自行插值进度；
   - 曲目切换/跳播在回到前台时立即全量刷新；
   - 不伪造长时任务类型。
4. **通知**：优先依赖 AVSession 的系统媒体卡片；如仍需普通通知，用 Notification Kit + WantAgent，按钮点击拉起应用并携带动作参数（不保证后台直接执行）。
5. **睡眠定时**：仅应用存活时可靠；退后台后如系统挂起进程，定时动作不执行。设置页需明确提示，或提供「仅在应用前台生效」文案。
6. **验证项 S2**：纯遥控端 AVSession 是否被媒体中心展示、锁屏是否可见、后台存活时长、通知按钮可用性。

### 5.2 桌面卡片

- `FormExtensionAbility` + ArkTS 卡片，等效 4x1（鸿蒙栅格按 4×2 / 2×4 实机确认）。
- 展示：封面、曲名、艺人、播放/暂停、上一首/下一首。
- 数据：`WidgetBridge` 等价物把最新快照写入 Preferences；封面写入沙箱文件。
- 更新：
  - 应用前台时状态变化调用 `formProvider.requestFormUpdate`；
  - 卡片按钮通过 `postCardAction` 触发 `onFormEvent`，在 FormExtensionAbility 内读取服务器配置并直接发一条 LMS 命令（快速路径），随后请求刷新；
  - 定时刷新受配额限制（每张卡片每天 50 次），**不做进度动画**。
- 验证项 S3：卡片能否读取沙箱封面文件、按钮回调时延、长按/尺寸适配。

### 5.3 局域网发现与 WOL

- 读取本机 IPv4 地址与网段：`@kit.NetworkKit` 的 connection/网络接口信息。
- 端口扫描：TCP connect 9000 端口，64 并发 + 800ms 硬性超时（到时强制关闭 socket），先扫当前服务器所在 /24，再扫本机网段，最多 3 个网段各 1–254。
- 识别：端口开放后用 LMS `serverstatus` 校验并读取版本，过滤非 LMS 服务。
- WOL：UDP 开启 broadcast，向 `255.255.255.255:9` 与子网广播地址发送 6×0xFF + 16×MAC 的魔术包。
- 仅前台触发，扫描期间按钮显示加载态；实测 508 个地址约 3.4 秒完成。

### 5.4 主机电源与网页入口

- 电源：`GET http://host:80/cgi-bin/...`，带 Basic Auth；成功/失败文案与 Android 一致。
- Daphile 管理页 / LMS 网页：`startAbility` + `ohos.want.action.viewData` 打开系统浏览器，或应用内 Web 组件（首版用系统浏览器，避免混合内容与证书问题）。

### 5.5 加密存储

- HUKS 生成 AES-256-GCM 密钥（别名固定），IV 随机，密文 Base64 后写入 Preferences。
- 解密失败（换机/清密钥）时视为空密码，提示重新输入，不崩溃。
- 不实现明文降级。

### 5.6 权限与网络配置

| Android 权限 | 鸿蒙权限/配置 | 用途 |
| --- | --- | --- |
| `INTERNET` | `ohos.permission.INTERNET` | LMS、歌词、封面、电源 |
| `ACCESS_NETWORK_STATE` | `ohos.permission.GET_NETWORK_INFO` | 网络状态与网段 |
| `ACCESS_WIFI_STATE` | 同上（优先用 connection 模块） | 局域网扫描 |
| `POST_NOTIFICATIONS` | `notificationManager.requestEnableNotification` | 通知授权 |
| `FOREGROUND_SERVICE(_MEDIA_PLAYBACK)` | `ohos.permission.KEEP_BACKGROUND_RUNNING` + `backgroundModes` | 仅在有合法业务类型时申请 |
| `WAKE_LOCK` | 不申请；无后台任务即无需运行锁 | 避免无意义保活 |

- `module.json5` 增加网络安全配置，显式允许局域网明文 HTTP；若目标 API 默认禁止，未配置会直接报 `Cleartext traffic not permitted`。
- 所有权限在设置页/隐私说明中给出用途，局域网扫描必须由用户点击触发。

---

## 6. 分阶段实施计划

> 阶段按依赖排序，不设时间估算；每阶段结束需通过验收再进入下一阶段。

### Phase 0：技术验证（先做，决定架构）

| ID | 验证内容 | 通过标准 | 失败后的替代 |
| --- | --- | --- | --- |
| S1 | 鸿蒙 HTTP 访问真实 Daphile：明文、Basic Auth、JSON-RPC、超时 | 能拉到 `serverstatus`、`players`、当前曲目 | 检查 network 配置/端口；必要时应用内提示仅支持 HTTPS |
| S2 | AVSession 遥控端：锁屏/控制中心展示、按钮回调、退后台存活 | 系统媒体卡可见、按钮能控制 LMS、退后台 5 分钟内状态不被清空 | 只保留前台控制；锁屏用普通通知；明确降级 |
| S3 | Form Kit 卡片：封面、按钮、刷新 | 卡片能显示快照并完成播放/暂停 | 卡片退化为「打开应用」入口 |
| S4 | Canvas 性能：14 套背景/场景在中端机 | 页面滑动稳定 30fps+，无持续掉帧 | 降帧、静态化、减少粒子 |
| S5 | 文本解码：GBK/Big5/Shift_JIS | 能修复常见中文乱码 | 先支持 UTF-8/Latin-1；GBK 走 NAPI 或延后 |

产出：`docs/harmony-spikes.md`，记录结论、设备型号、系统版本、日志。

### Phase 1：工程骨架与数据层

- 创建 DevEco 工程、`module.json5`、权限、图标、启动页。
- `AppContext`、`Models`、`LmsClient`、`LmsParser`、`SettingsStore`、`CryptoStore`。
- i18n 生成器 + 20 语言产物 + 语言选择对话框。
- 服务器列表/编辑页、主题选择骨架、连接横幅。
- 验收：
  - 新增服务器 → 测试连接 → 保存 → 重启应用仍存在；
  - 错误 IP/密码有正确错误提示；
  - 20 种语言切换即时生效，无缺字段。

### Phase 2：播放主链路

- 轮询状态机、当前播放器选择、`NowPlaying` 快照。
- 正在播放页（先默认深色，不含主题自绘）：封面、曲名、进度、播放/暂停/上下首、音量/静音、循环/随机、收藏、分享、队列弹层、睡眠定时、保存队列。
- 多播放器页：播放/暂停、独立音量、同步到。
- 验收：
  - 真实设备控制播放/暂停/切歌/拖动/音量；
  - 队列 1000 首仍可打开并定位当前曲目；
  - 断网后显示重连横幅，恢复后自动连上。

### Phase 3：音乐库

- 音乐库首页、专辑/艺人/流派/年份/播放列表、专辑详情、歌单详情、歌曲（分页）、搜索、文件夹、收藏、电台/应用/插件、扫描。
- 音质徽标体系与专辑最佳音质。
- 验收：
  - 专辑/歌曲/歌单超过 100 条可继续加载，无重复项、无重复 key；
  - 搜索返回完整音质字段；
  - NAS 文件夹深层浏览不崩溃；
  - 扫描进度实时更新。

### Phase 4：本地增强与主机能力

- 自定义歌单 CRUD、长按加入、队列存为歌单。
- 每日推荐、随机 1–200 入队、全部歌曲入队/播放、收藏全部。
- 睡眠定时、WOL、自动唤醒、主机电源二次确认。
- 多服务器切换、局域网发现。
- 验收：
  - 歌单重启后仍存在，重复曲目自动跳过；
  - 随机每次结果不同且不集中在一张专辑；
  - WOL 能在主机休眠时唤醒并自动重连；
  - 电源操作有二次确认。

### Phase 5：歌词、封面与信息页

- 歌词三源、LRC 解析、逐行高亮、点击定位、复制、导出、本地歌词库统计/清空。
- 在线封面三源、缓存、封面占位。
- 音频输出页、系统信息页、关于卡片。
- 验收：
  - 指定源未命中时回退本地且不清空页面；
  - 30 天缓存生效；
  - DSD64/128/256/512 与 Hi-Res/SQ/HQ/标准显示正确；
  - 导出 `.lrc` 到系统文件选择器。

### Phase 6：主题与自绘

- 14 套配色 + 14 类背景、14 类场景、14 类封面框、14 类滑块。
- 黑胶旋转、火柴人对决、歌词/进度联动。
- 主题选择页预览与即时切换。
- 验收：
  - 每套主题有独立背景/场景/封面框/滑块（不能只换色）；
  - 切换主题后系统栏、页面、弹层同步；
  - 中端机 30fps+，退后台停止绘制；
  - 14 套主题截图归档。

### Phase 7：系统集成

- AVSession、媒体通知、锁屏控制、通知权限。
- 后台策略落地（前台轮询 / 后台插值 / 回前台刷新）。
- Form Kit 卡片、卡片按钮、卡片刷新。
- 验收：
  - 播放中锁屏可见媒体卡，按钮能控制；
  - 桌面卡片显示当前曲目并可播放/暂停；
  - 退后台 10 分钟再回前台，状态在 2 秒内追平；
  - 不申请不合法长时任务。

### Phase 8：质量与发布

- 设备矩阵：Mate/Pura/nova 各一台 + 模拟器；API 12 与当前最新稳定版各一轮。
- 单元测试：解析器、分页、随机、音质、乱码、LRC。
- 性能：冷启动、内存、滑动帧率、长队列、1 万首曲库分页。
- 合规：隐私说明、权限用途、GPL-3.0 源码提供方式、第三方服务披露。
- 发布：签名、AppGallery Connect 建档、上架材料、版本号与更新说明。
- 验收：无 P0/P1 缺陷；上架检查表全部通过。

---

## 7. 关键技术决策（ADR）

| 编号 | 决策 | 理由 | 影响 |
| --- | --- | --- | --- |
| ADR-1 | 状态管理只用 V2 | 避免 V1/V2 混用导致刷新异常 | 全部页面按 `@ObservedV2/@Trace` 设计 |
| ADR-2 | 文案继续用内存表，不做 string.json | 20 语言动态切换、无英文回退、与 Android 行为一致 | 需要生成器与校验脚本 |
| ADR-3 | 导航用 `Navigation` + `NavPathStack` | 官方推荐，参数类型化 | 不再维护 URL 路由 |
| ADR-4 | 不伪造长时任务 | 系统做业务一致性校验，伪造会被拒绝/下架 | 后台能力降级，需产品确认 |
| ADR-5 | 卡片只做快照 | 定时刷新 50 次/天配额 | 不做实时进度 |
| ADR-6 | 主题绘制共用 Ticker | 统一帧率与生命周期 | 退后台必须停止 |
| ADR-7 | 不引入 KMP/Flutter/WebView | 保持原生与可维护性 | 协议层重写而非共享 |
| ADR-8 | 数据结构字段名与 Android 对齐 | 便于对照调试与未来导入导出 | 不保证跨端数据直读 |

---

## 8. 风险登记

| ID | 风险 | 概率/影响 | 验证 | 缓解 / 回退 |
| --- | --- | --- | --- | --- |
| R1 | 纯遥控端无法长期后台运行 | 高/高 | S2 | 前台轮询 + AVSession 插值 + 回前台追平；睡眠定时标注限制 |
| R2 | AVSession 不被媒体中心展示 | 中/高 | S2 | 普通通知兜底；仅前台控制 |
| R3 | 卡片无法读沙箱封面/按钮受限 | 中/中 | S3 | 卡片退化为信息+打开应用 |
| R4 | ArkTS 无 GBK/Big5 解码 | 中/中 | S5 | 先 UTF-8/Latin-1；NAPI 或延后 |
| R5 | 明文 HTTP 被默认拦截 | 中/高 | S1 | 显式网络配置；HTTPS 选项 |
| R6 | Canvas 主题掉帧 | 中/中 | S4 | 降帧/静态化/简化粒子 |
| R7 | QQ/网易云接口变更 | 中/低 | 联调 | LRCLIB 兜底；接口独立封装 |
| R8 | 上架审核（GPL、局域网扫描、权限） | 中/中 | 提交前 | 隐私说明、用户触发扫描、权限最小化 |
| R9 | ArkTS 严格模式导致解析代码膨胀 | 高/低 | Phase 1 | 统一 `JsonGuard` 工具与类型守卫 |
| R10 | API 版本差异（12 vs 最新） | 中/中 | Phase 8 | 特性检测 + 最低基线 API 12 |

---

## 9. 验收标准

### 9.1 功能验收

以 Android 1.9.2 为基准做功能矩阵，逐项标记「通过/降级/不支持」，降级项必须写入 README 与设置页说明。重点项：

- 播放控制全链路、队列 1000 首、进度拖动。
- 曲库分页 > 100、搜索、文件夹、收藏、电台。
- 自定义歌单、每日推荐、随机、全部入队。
- 歌词三源与本地回退、封面三源、音频信息。
- 14 套主题、20 种语言。
- 通知/锁屏/卡片、WOL、电源、扫描。
- 响应式布局：手机/折叠屏/平板/PC 自动识别，设置内手动切换，窗口缩放即时响应。
- Release HAP：安装、覆盖升级、卸载、重装均通过。

### 9.2 非功能验收

- 冷启动 ≤ 2 秒（中端机，含服务器恢复）。
- 页面滑动 30fps+；主题场景 30fps+，退后台停止绘制。
- 轮询单次请求失败不影响 UI 操作；错误提示可读。
- 长标题、空元数据、异常封面 URL 不崩溃。
- 权限拒绝（通知）后功能可用，仅提示。

---

## 10. 交付物清单

1. `harmony/` 完整工程（可构建 HAP）。
2. `docs/harmony-port-plan.md`（本文）。
3. `harmony/tools/i18n-gen/`（文案生成器）与 `harmony/tools/icon-gen/`（图标生成器）。
4. `docs/release-notes-v1.9.2-harmony.md`（发布说明）。
5. GitHub Releases：`DaphileRemote-1.9.2-harmony.hap`。
6. 上架材料：隐私说明、权限说明、GPL 源码说明、版本说明。

---

## 11. 待确认事项

1. **后台与锁屏**：是否接受「退后台后曲目变化不实时、进度由系统插值」？若不接受，需要评估其他保活方案（可能不符合上架规范）。
2. **最低系统**：最低 API 12（HarmonyOS NEXT 5.0）是否足够，还是要覆盖更早的 NEXT 版本？
3. **首发范围**：14 套主题、桌面卡片、20 语言是否全部首发，还是先发「默认深色 + 全语言 + 核心播放/曲库」，主题与卡片放第二个版本？

---

## 12. 参考资料

- 长时任务（Background Tasks Kit）：https://developer.huawei.com/consumer/cn/doc/HarmonyOS-Guides/continuous-task
- AVSession 后台播放：https://developer.huawei.com/consumer/cn/doc/harmonyOS-guides/avsession-background-scene
- 使用 HTTP 访问网络（含明文配置）：https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/http-request
- ArkTS 卡片定时刷新配额：https://developer.huawei.com/consumer/cn/doc/HarmonyOS-Guides/arkts-ui-widget-passive-refresh
- module.json5 配置文件：https://developer.huawei.com/consumer/cn/doc/harmonyos-guides/module-configuration-file-V5

---

## 13. 实施与发布状态（2026-11-28）

### 13.1 发布信息

| 项目 | 内容 |
| --- | --- |
| 版本 | 1.9.2（versionCode 14），与 Android 1.9.2 对齐 |
| 发布标签 | `v1.9.2-harmony` |
| 资产 | `DaphileRemote-1.9.2-harmony.hap`（1,380,208 字节） |
| SHA-256 | `9BB36D8E61A1F238B21D24B0F4ECB5DCC32FB096E83D09F585470EEDD8041B17` |
| 签名 | OpenHarmony 测试证书 + release profile，可安装到鸿蒙模拟器；零售设备需用自有 DevEco 证书重签 |
| 安装 | `hdc install -r DaphileRemote-1.9.2-harmony.hap` |

### 13.2 已验证项

- 安装：Mate 90 Pro（手机）、MatePad Pro 13（平板）、SeqAudioPC（2in1）均安装并启动。
- 升级：平板先安装 1.9.1（versionCode 13），覆盖安装 1.9.2（14）成功，`bm dump` 显示 14 / 1.9.2。
- 卸载：卸载后 `bm dump` 查询失败；重新安装成功。
- 布局：自动识别手机/平板/PC；PC 窗口缩窄后自动切回手机布局；设置内手动切换布局并持久化。
- 局域网发现：扫描 508 个地址约 3.4 秒，发现 `192.168.124.88`。
- 歌词：同步歌词优先、未同步歌词不再伪造当前行、制作信息行过滤、宽屏歌词居中放大。
- 图标：分层图标与启动图标已接入，启动画面与桌面图标显示正常。

### 13.3 已知差异

- 媒体通知开关目前仅保存偏好，鸿蒙端尚未接入 AVSession 媒体通知。
- 桌面卡片在模拟器启动器显示为黑底，属于启动器渲染限制。
- Android 1.5 的火柴人对决动画未在鸿蒙端复刻。
