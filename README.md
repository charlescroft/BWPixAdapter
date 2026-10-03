# BWPixAdapter
> **English README: [README.en.md](README.en.md).**


一个面向 **BW Pix / BWLED 智能灯泡摄像头** 的现代化开源安卓客户端。
它用全新的 Material 3 / Jetpack Compose 界面替换掉十余年前的原版应用，同时复用原版久经考验的
**PPPP / PPCS 原生引擎**，让摄像头完全按原有方式工作。

> **定位：** 一个「适配器」—— 在久经验证的旧引擎之上套一层全新的 UI，通过摄像头自带的
> AP 模式 Wi-Fi 热点直连（不依赖任何云服务）。
>
> **许可证：** GPL v3，详见 [LICENSE](LICENSE)。

---

## 功能特性

- **实况观看**：硬件解码 + OpenGL ES 2.0 流畅渲染，锁定 16:9 比例、支持沉浸式全屏。
- **本地录像**：把实时 H.264 流录制为 AVI 文件保存到手机。
- **报警自动录像**：可选开关，收到移动报警时自动开始/结束本地录像。
- **声音监听与对讲喊话**：收听摄像头麦克风，并通过灯泡喇叭双向通话。
- **灯光与色温控制**：冷白光 / 暖黄光滑块调节，按设备记忆上次设置。
- **TF 卡文件浏览器**：直接通过摄像头 HTTP 服务读取 `/sd/` 目录：
  - 按日期分组的**录像**（AVI），App 内本地播放（支持进度拖动、声音），并可下载到
    `Movies/BWPixAdapter`。
  - **抓拍照片**（JPEG）九宫格预览，可保存到系统相册。
  - 卡状态（正常/异常）、容量进度条、格式化。
- **事件记录**：移动/抓拍事件，长按多选批量删除、单张按需下载到 `.nomedia` 私有缓存、
  保存到相册、按保留天数自动清理。
- **设备设置**：Wi-Fi 联网（扫描 + 输密码）、时间校准、TF 卡格式化、重启设备。
- **暗黑模式**：跟随系统 / 浅色 / 深色，首页一键切换。

---

## 运行要求

- 支持热点模式的安卓手机（摄像头只在其自带 AP 下通信）。
- **BW Pix / BWLED** 智能灯泡摄像头（AP 模式 IP `192.168.234.1`，UID 见机身标签，如
  `BWLED-009433-...`）。
- Android 7.0+（minSdk 21）。已在 **Android 16 / API 36** 上编译测试。

> ⚠️ 摄像头的 32 位原生库包含 **text relocations**，因此必须保持 `targetSdk = 22` 且
> `useLegacyPackaging = true`。请勿将 targetSdk 提升到 22 以上，否则 Android linker
> 会拒绝加载这些库。

---

## 编译构建

```bash
git clone https://github.com/charlescroft/BWPixAdapter.git
cd BWPixAdapter
export ANDROID_HOME=$HOME/Library/Android/sdk   # 或你的 SDK 路径
./gradlew :app:assembleDebug
# 产物：app/build/outputs/apk/debug/app-debug.apk
```

项目自带 Gradle Wrapper，无需全局安装 Gradle。发布版可安装 APK 请到
[GitHub Releases](https://github.com/charlescroft/BWPixAdapter/releases) 下载。

---

## 快速上手

1. **给灯泡通电**，等待约 30 秒让 Wi-Fi 模块启动。
2. 手机**连接灯泡的 Wi-Fi 热点**（AP IP `192.168.234.1`）。
3. 打开 **BWPixAdapter**，点右下角 **+** 添加设备：填写设备名、机身标签上的 **UID**
   （如 `BWLED-009433-LFPFT`）与标签凭证（默认账号 `admin`）。
4. 点 **查看实况** 即可观看实时画面。

### 实况页控制

| 控件 | 作用 |
|---|---|
| **录像 / 停止** | 开始 / 停止把实时流录制到手机 |
| **抓拍** | 通过 `snap.cgi` 抓取一张 JPEG 快照 |
| **监听** | 收听摄像头麦克风 |
| **喊话** | 通过灯泡喇叭双向对讲 |
| 清晰度徽标 | 切换 720P / VGA / QVGA |
| 全屏（⛶） | 沉浸式横屏播放 |

### TF 卡录像

1. 在设备卡片点 **TF卡录像**。
2. 选择录像：已下载的文件显示 **✓**，点 **回放** 直接播放本地文件（支持拖动进度与声音），
   或点下载图标拉取到手机。
3. **抓拍照片** 标签按日期分组；点开预览，点 **保存** 导出到系统相册。

### 事件

1. 从设备卡片进入 **事件抓拍**。
2. 长按进入多选，用右上角 **删除** 批量删除（在固件支持时会同步清理 TF 卡对应事件文件）。
3. 用 **保留天数** 菜单自动过期清理旧事件。

---

## 项目结构

```
app/src/main/java/
  com/bwpixadapter/app/       # 现代化 Compose UI（设备列表、实况、事件、TF、设置）
  com/bwpixadapter/app/media/ # 本地 AVI 解复用器、H264 解码视图、录像、音频
  com/easyview/…              # 移植的旧版 PPPP/PPCS 引擎（BridgeService、cameras、tables）
  object/p2pwificam/…         # 旧引擎支撑（native caller、工具、数据表）
  object/easyview/bwpix/R     # 旧引擎资源 ID 桥接
app/src/main/jniLibs/…        # 32 位 armeabi 原生库（PPPP/PPCS SDK、ffmpeg 等）
```

PPPP/PPCS 原生 SDK 与移植引擎按 GPL v3 从原版应用编译，仅用于与本设备互操作。

---

## 故障排查

硬件层面的问题（热点不启动、SD 卡限制、复位等）请查阅官方说明书：
[docs/OFFICIAL_MANUAL.md](docs/OFFICIAL_MANUAL.md)（Bell & Wyson BW-Pix+ 官方手册摘录）。

常见应用侧排查：

- **无画面 / 「摄像头未在线」** — 确认手机已连上灯泡热点、UID/凭证正确、灯泡通电 ≥30 秒。
- **TF 卡识别不到** — 仅支持 **2–32 GB** 卡；插拔卡时务必让灯泡**断电**。
- **下载的视频速度异常 / 无声** — 录像为 AVI 内的 H.264 + PCM，内置播放器本地解复用并解码；
  若文件损坏请重新下载。
- **删除 TF 卡文件仅回 ACK** — 部分固件版本对删除指令只回执不实际删除文件；可在
  **卡管理 → 格式化 TF 卡** 彻底清理。

---

## 许可证

本项目基于 **GNU General Public License v3.0** 发布 —— 完整文本见 [LICENSE](LICENSE)。

BWPixAdapter 是独立项目，**与 Bell & Wyson 无任何关联或背书**。「BW Pix」「BW-Pix+」
为其各自所有者的商标，此处仅用于描述兼容硬件。
