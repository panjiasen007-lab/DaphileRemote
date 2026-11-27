# DaphileRemote 1.9.2 — HarmonyOS NEXT

原生鸿蒙 ArkTS/ArkUI 版本，功能对齐 Android 1.9.2。

## 下载

- 资产：`DaphileRemote-1.9.2-harmony.hap`
- 大小：1,380,208 字节（约 1.32 MB）
- SHA-256：`9BB36D8E61A1F238B21D24B0F4ECB5DCC32FB096E83D09F585470EEDD8041B17`
- 包名：`com.daphile.remote`
- 版本：1.9.2（versionCode 14）
- 系统要求：HarmonyOS NEXT 5.0 及以上（API 12）

## 安装 / 升级 / 卸载

```powershell
# 全新安装
hdc install DaphileRemote-1.9.2-harmony.hap

# 覆盖升级（保留数据）
hdc install -r DaphileRemote-1.9.2-harmony.hap

# 卸载
hdc uninstall com.daphile.remote
```

## 本版内容

### 鸿蒙原生复刻
- ArkTS/ArkUI 全量重写，覆盖正在播放、音乐库、电台、播放器、设置、歌词、队列、搜索、文件夹、每日推荐、自定义歌单、音频输出、系统信息等页面。
- LMS JSON-RPC 遥控、多服务器、WOL、主机电源、Daphile/LMS 网页入口。
- 20 种界面语言，文案由 Kotlin 源自动生成并全量覆盖。
- 14 套外观主题。

### 本次新增 / 优化
- 新增手机、折叠屏、平板、PC 自适应布局：
  - 手机：底部五标签导航
  - 折叠屏：图标侧栏
  - 平板：侧边导航 + 内容限宽
  - PC：更宽侧栏 + 内容限宽
- 启动自动识别设备类型与窗口宽度，PC 窗口缩放即时切换布局；设置 → 外观 → 界面布局可手动选择自动/手机/折叠屏/平板/电脑，并持久保存。
- 补全「自动发现局域网服务器」端口扫描：并发探测 9000 端口，约 3.4 秒扫描 508 个地址，识别 LMS 服务并可一键保存。
- 新增应用图标：深色背景 + 青绿色音符，分层图标与启动图标。
- 平板/PC 歌词体验：歌词居中、当前行放大到 24sp、封面放大、队列弹层限宽、歌词页限宽居中；同步歌词优先，未同步歌词不再伪造当前行，过滤“歌曲名 - 歌手A/歌手B”制作信息行。
- 专辑网格按设备自适应列数（手机 3 / 折叠屏 4 / 平板 5 / PC 6），专辑详情、电台、播放器、服务器管理/编辑等页面在宽屏下限宽居中。

## 验证结果

在华为模拟器上完成：

- 安装：Mate 90 Pro（手机）、MatePad Pro 13（平板）、SeqAudioPC（2in1）均安装并启动。
- 升级：平板先安装 1.9.1（versionCode 13），覆盖安装 1.9.2（14）成功，`bm dump` 显示 14 / 1.9.2。
- 卸载：卸载后 `bm dump` 查询失败，随后重新安装成功。
- 布局：自动识别手机/平板/PC；PC 窗口缩窄后自动切换手机布局；手动布局切换与持久化正常。
- 歌词：同步歌词正确高亮，宽屏居中放大；未同步歌词不再错误高亮。
- 局域网发现：约 3.4 秒发现 `192.168.124.88`。

## 签名说明

本 HAP 使用 OpenHarmony 测试证书与 release profile 签名，适合模拟器与开发调试。安装到零售 HarmonyOS 设备时，请在 DevEco Studio 中使用自己的证书与 Profile 重新签名后再安装。

## 已知差异

- 媒体通知开关目前仅保存偏好，鸿蒙端尚未接入 AVSession 媒体通知。
- 桌面卡片在模拟器启动器显示为黑底，属于启动器渲染限制。
- Android 1.5 的火柴人对决动画未在鸿蒙端复刻。

## 相关文档

- [README](https://github.com/panjiasen007-lab/DaphileRemote/blob/main/README.md)
- [鸿蒙迁移方案与实施状态](https://github.com/panjiasen007-lab/DaphileRemote/blob/main/docs/harmony-port-plan.md)

---

# English summary

Native HarmonyOS NEXT port (ArkTS/ArkUI) aligned with Android 1.9.2. Download `DaphileRemote-1.9.2-harmony.hap` and install with `hdc install -r DaphileRemote-1.9.2-harmony.hap` (HarmonyOS NEXT 5.0 / API 12+).

Highlights: responsive phone/foldable/tablet/PC layouts with auto-detection and a manual override in Settings, LAN server discovery via port scanning, a new layered app icon, and centred wide-screen lyrics with synced-lyrics preference.

The HAP is signed with the OpenHarmony test certificate for emulators and development. Re-sign with your own DevEco certificate and profile for retail devices.
