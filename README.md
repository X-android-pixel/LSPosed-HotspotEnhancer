# 🚀 LSPosed-HotspotEnhancer

[![Build APK](https://github.com/github-jules/hotspot-enhancer/actions/workflows/build.yml/badge.svg)](https://github.com/github-jules/hotspot-enhancer/actions/workflows/build.yml)
[![Release](https://img.shields.io/github/v/release/github-jules/hotspot-enhancer)](https://github.com/github-jules/hotspot-enhancer/releases)
[![License](https://img.shields.io/github/license/github-jules/hotspot-enhancer)](LICENSE)

An advanced LSPosed module to unlock MIUI-style features for the built-in WiFi hotspot on AOSP-based Android 14–16.

---

## 📸 Screenshots

| Dashboard | Client List | Settings |
| :---: | :---: | :---: |
| ![Dashboard Placeholder](https://via.placeholder.com/200x400?text=Dashboard) | ![Client List Placeholder](https://via.placeholder.com/200x400?text=Client+List) | ![Settings Placeholder](https://via.placeholder.com/200x400?text=Settings) |

---

## ✨ Features

- 📱 **Real-time Connected Devices**: View MAC, IP, and hostname of all connected clients.
- 🔢 **Custom Client Limits**: Set `wifi_softap_max_num_clients` beyond default system limits.
- ⚡ **Quick SSID/Password Edit**: Update hotspot credentials without a full restart.
- 🛡️ **MAC Blacklist**: Block specific devices from connecting with a persistent blacklist.
- 🔔 **Connection Notifications**: Get notified instantly when new devices join.
- ⏲️ **Auto-off Timer**: Automatically turn off the hotspot after a period of inactivity to save battery.
- 🎨 **System UI Integration**: Seamlessly adds an entry point into the system Hotspot settings.

---

## 🛠️ Installation

### Requirements
- **Root**: KernelSU, Magisk (v24+), or APatch.
- **Framework**: LSPosed (Zygisk version) or Vector.

### Steps
1. Download the latest APK from the [Releases](https://github.com/github-jules/hotspot-enhancer/releases) page.
2. Install the APK on your device.
3. Open the **LSPosed Manager** and enable the **HotspotEnhancer** module.
4. Ensure the following scopes are selected:
   - `System Framework` (android)
   - `Settings` (com.android.settings)
   - `System UI` (com.android.systemui)
5. Reboot your device (or restart the hooked processes).

---

## 📱 Compatibility

- **OS**: AOSP, LineageOS 21 (Android 14), LineageOS 22 (Android 15), LineageOS 23 (Android 16).
- **Devices**: Pixel, Xiaomi (POCO/Redmi running AOSP-based ROMs), Motorola, and more.
- **APIs**: 34, 35, 36.

---

## 🏗️ Building from Source

```bash
git clone https://github.com/github-jules/hotspot-enhancer.git
cd hotspot-enhancer
./gradlew assembleRelease
```
The APK will be located in `app/build/outputs/apk/release/`.

---

## 🤝 Credits & Disclaimer

### Credits
- [LSPosed Framework](https://github.com/LSPosed/LSPosed)
- [Android Open Source Project](https://source.android.com/)

### Disclaimer
**Use at your own risk.** This module modifies system-level behavior. The developers are not responsible for any data loss, battery drain, or hardware damage.

---

## 📄 License
This project is licensed under the [Apache License 2.0](LICENSE).
