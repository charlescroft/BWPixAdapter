# BWPixAdapter
> **中文说明见 [README.md](README.md).**



A modern, open-source Android client for the **BW Pix / BWLED smart bulb camera**.
It replaces the dated 10-year-old original app with a clean Material 3 / Jetpack
Compose interface while reusing the original, battle-tested **PPPP / PPCS** native
engine so the camera keeps working exactly as before.

> **What it is:** An *adapter* — a new UI shell over the proven legacy engine that
> talks to the camera over its local AP Wi-Fi hotspot (no cloud involved).
>
> **License:** GPL v3. See [LICENSE](LICENSE).

---

## Features

- **Live view** with hardware-decoded smooth rendering (OpenGL ES 2.0 / YUV), 16:9
  aspect-ratio-locked player and immersive fullscreen.
- **Local recording** — record the live H.264 stream to your phone as an AVI file.
- **Alarm auto-record** — optionally start/stop local recording automatically when
  the camera reports a motion alarm.
- **Audio listen & intercom** — hear the camera's microphone and talk back through
  the bulb speaker.
- **Light & colour-temperature control** — cold-white / warm-yellow sliders with
  per-device memory.
- **TF card explorer** — browse `/sd/` directly over the camera's HTTP server:
  - Date-grouped **recordings** (AVI) with in-app local playback, progress seek,
    audio, and download to `Movies/BWPixAdapter`.
  - **Snapshots** (JPEG) grid with preview and save-to-gallery.
  - Card status (正常/异常), capacity bar, and format.
- **Event journal** — motion/photo events with long-press multi-select batch delete,
  per-event photo download to a `.nomedia` private cache, save-to-gallery, and
  retention-day auto-cleanup.
- **Device settings** — Wi-Fi join (scan + password), time sync, TF card
  formatting, and reboot.
- **Dark mode** — System / Light / Dark with a home-screen toggle.

---

## Requirements

- Android phone with a Wi-Fi hotspot mode (the camera only speaks over its own AP).
- The **BW Pix / BWLED** smart bulb camera (AP-mode IP `192.168.234.1`, UID on the
  label, e.g. `BWLED-009433-...`).
- Android 7.0+ (minSdk 21). Built and tested on **Android 16 / API 36**.

> ⚠️ The camera's 32-bit native libraries contain **text relocations**, so the app
> must run with `targetSdk = 22` and `useLegacyPackaging = true`. Do not raise
> `targetSdk` above 22 or Android's linker will refuse to load them.

---

## Build

```bash
git clone https://github.com/<your-account>/BWPixAdapter.git
cd BWPixAdapter
export ANDROID_HOME=$HOME/Library/Android/sdk   # or wherever your SDK lives
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

The project ships with the Gradle wrapper; no global Gradle install is needed.

---

## Getting Started

1. **Power the bulb** and wait ~30 seconds for its Wi-Fi module to boot.
2. From your phone, **join the bulb's Wi-Fi hotspot** (its SSID; AP IP
   `192.168.234.1`).
3. Open **BWPixAdapter** and tap the **+** (add device). Enter the device name, the
   **UID** printed on the bulb label (e.g. `BWLED-009433-LFPFT`), and the on-label
   credentials (default login `admin`).
4. Tap **查看实况** to watch the live stream.

### Live view controls

| Control | Action |
|---|---|
| ▶ **播放** (control bar) | Start local AVI playback of a downloaded recording |
| **录像 / 停止** | Start / stop recording the live stream to the phone |
| **抓拍** | Grab a JPEG snapshot via `snap.cgi` |
| **监听** | Hear the camera microphone |
| **喊话** | Two-way intercom through the bulb speaker |
| Resolution badge | Switch 720P / VGA / QVGA |
| Fullscreen (⛶) | Immersive landscape player |

### TF card recordings

1. Tap **TF卡录像** on the device card.
2. Pick a recording; if it's already downloaded a **✓** is shown — tap **回放** to
   play the local file (with seek + audio), or tap the download icon to fetch it.
3. Snapshots are grouped by date in the **抓拍照片** tab; tap to preview, **保存**
   to export to the gallery.

### Events

1. Open **事件抓拍** from the device card.
2. Long-press to multi-select, use the top-right **删除** to batch-delete (also
   cleans the matching TF card event files where the firmware honours the command).
3. Use the **保留天数** menu to auto-expire old events.

---

## Project structure

```
app/src/main/java/
  com/bwpixadapter/app/       # Modern Compose UI (device list, live, events, TF, settings)
  com/bwpixadapter/app/media/ # Local AVI demuxer, H264 decoder view, live recorder, audio
  com/easyview/…              # Ported legacy PPPP/PPCS engine (BridgeService, cameras, tables)
  object/p2pwificam/…         # Legacy engine support (native caller, utils, tables)
  object/easyview/bwpix/R     # Resource-id bridge for the legacy engine
app/src/main/jniLibs/…        # 32-bit armeabi native libraries (PPPP/PPCS SDK, ffmpeg, etc.)
```

The PPPP/PPCS native SDK and the ported engine are compiled from the original app
under the GPL v3 for interoperability with this device.

---

## Troubleshooting

See the official Bell & Wyson device manual in
[docs/OFFICIAL_MANUAL.md](docs/OFFICIAL_MANUAL.md) for hardware-level issues
(hotspot not starting, SD card limits, resets).

Common app-side checks:

- **No video / "摄像头未在线"** — make sure the phone is on the bulb's hotspot, the
  UID/credentials are correct, and the bulb has been powered ≥30 s.
- **TF card not detected** — only **2–32 GB** cards are supported; re-seat the card
  with the bulb **powered off**.
- **Downloaded video plays at the wrong speed / no audio** — recordings are H.264 +
  PCM in AVI; the built-in player demuxes and decodes them locally. If a file is
  corrupt, re-download it.
- **Deleting TF card files only ACKs** — some firmware builds acknowledge the
  delete command but don't physically remove files; use **卡管理 → 格式化 TF 卡** to
  fully wipe.

---

## License

This project is licensed under the **GNU General Public License v3.0** — see
[LICENSE](LICENSE) for the full text.

BWPixAdapter is an independent project and is **not affiliated with or endorsed by
Bell & Wyson**. "BW Pix" and "BW-Pix+" are trademarks of their respective owners and
are referenced only to describe the compatible hardware.
