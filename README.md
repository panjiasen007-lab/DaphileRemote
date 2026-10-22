# DaphileRemote

Daphile / Lyrion Music Server（LMS 9.x）的 Android 遥控器。界面基于 Jetpack Compose，面向本地局域网的 Daphile 主机，提供播放控制、音乐库浏览、歌词、音频输出信息、封面匹配、锁屏媒体控制与主机电源管理。

- 应用显示名：达菲遥控
- 包名：`com.daphile.remote`
- 当前版本：1.1.0（versionCode 2）
- 最低系统：Android 8.0（API 26），目标 API 35

## 功能

### 播放控制
- 播放 / 暂停 / 上一首 / 下一首、拖动进度、音量调节、静音
- 随机、循环模式切换
- 播放队列：查看、跳播、删除、清空、保存为播放列表
- 睡眠定时
- 多播放器切换、各播放器独立音量与“同步到”其他播放器

### 音乐库
- 专辑、艺术家、流派、年份、播放列表、歌曲、文件夹分类浏览
- 按文件夹层级浏览 Daphile 音乐目录（`musicfolder`）
- 最近添加
- 即时搜索（曲目 / 专辑 / 艺术家）
- 扫描更新 / 重建音乐库，并实时显示扫描进度

### 歌词
- 优先读取 LMS 本地歌词（`songinfo`）
- 在线匹配：LRCLIB，中文曲库回退网易云音乐
- LRC 解析、逐行高亮、自动滚动、点击歌词跳转播放位置
- 本地缓存 30 天

### 在线封面
- MusicBrainz + Cover Art Archive
- 网易云专辑封面（中文曲库）
- iTunes Search API 兜底
- 按专辑名与艺人加权匹配，结果本地缓存

### 音频输出
- 音源信息：路径、格式、采样率、位深、码率、声道、无损标识
- 播放器与输出：输出设备、设备型号、输出引擎、输出规格、音量、LMS 版本
- 实时状态：已播放、总时长、队列位置
- 规格标签可点击进入详情，DSD 自动识别为 DSD64/128/256/512

### 主机与系统
- 电源中心：关闭主机、重启主机、挂起主机（均需二次确认）
- Daphile 管理页（默认 80 端口）、LMS 网页端、系统信息
- 多服务器管理、局域网自动发现
- 凭据使用 Android Keystore AES/GCM 加密存储

### 通知与桌面
- 媒体通知（封面、标题、上一首/暂停/下一首）
- 锁屏媒体卡片（可开关）
- 4x1 桌面小部件
- 深色 / 浅色 / 跟随系统主题

## 技术栈

- Kotlin 2.0.21、Jetpack Compose（Material 3）、Navigation Compose
- ViewModel + StateFlow
- OkHttp + `org.json`（LMS JSON-RPC：`POST /jsonrpc.js`，`slim.request`）
- DataStore Preferences、Android Keystore
- Coil（封面加载与缓存）
- MediaSessionCompat + 媒体通知

## 构建

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

## 配置

1. 在设置 → 服务器管理中添加服务器：
   - 主机：Daphile 主机 IP，例如 `192.168.124.5`
   - LMS 端口：`9000`（默认）
   - Daphile 管理端口：`80`（电源与 Daphile 管理页使用）
2. 选择播放器。
3. Android 13 及以上首次启动会请求通知权限；如需锁屏/通知栏媒体卡片，请在设置中开启“媒体通知”和“锁屏媒体控制”。

## 权限

| 权限 | 用途 |
| --- | --- |
| `INTERNET` | 访问 LMS、在线歌词/封面服务 |
| `ACCESS_NETWORK_STATE` | 连接状态检测 |
| `ACCESS_WIFI_STATE` | 读取本机网段用于局域网服务器扫描 |
| `POST_NOTIFICATIONS` | 媒体通知与锁屏卡片 |

## 在线服务与隐私

- 仅将当前曲目的标题、艺人、专辑、时长用于歌词/封面检索。
- 不收集账号、不上传音乐文件、不接入统计或广告 SDK。
- 歌词/封面结果仅缓存在本机应用私有目录。
- 使用的第三方服务：LRCLIB、网易云音乐公开接口、MusicBrainz、Cover Art Archive、iTunes Search API。

## 目录结构

```
app/src/main/java/com/daphile/remote/
├── data/
│   ├── local/        # DataStore、Keystore 凭据
│   ├── model/        # 数据模型
│   ├── online/       # 在线歌词与封面
│   ├── remote/       # LMS 客户端与解析
│   └── repo/         # 仓库层（轮询、重连、状态）
├── media/            # MediaSession、通知
├── ui/
│   ├── common/       # 通用组件与格式化
│   ├── navigation/   # 路由
│   ├── screens/      # 各页面
│   └── theme/        # 主题
└── widget/           # 桌面小部件
```

## 已知限制

- 歌词与封面依赖第三方公开接口，曲目信息不规范时可能匹配失败。
- 主机电源操作要求手机能直接访问 Daphile 的 80 端口；跨网段时需自行做端口转发。
- 部分服务器不返回码率/无损字段时，音频信息会显示为“—”或“未知”。
- 扫描进度取自 `rescanprogress`，不同 LMS 版本的字段可能略有差异。

## 版本

- 1.1.0：新增歌词、音频输出信息、文件夹浏览、扫描、在线封面、锁屏媒体卡片、主机电源中心；修复队列跳转导航与通知权限问题。
- 1.0.0：首个可用版本（播放控制、音乐库、多服务器、小部件）。
