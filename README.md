# HotspotEnhancer

An LSPosed module to enhance the built-in WiFi hotspot on Android 14–16 (AOSP/LineageOS).

## Features
- **Real-time connected devices**: See who's connected (MAC, IP, hostname).
- **Max client limit**: Set the maximum number of devices that can connect.
- **Quick Edit**: Change SSID and Password without restarting the hotspot (where supported).
- **Blocklist**: Block specific MAC addresses.
- **Notifications**: Get notified when a new device joins.
- **Auto-off Timer**: Automatically turn off the hotspot after a period of inactivity.
- **UI Integration**: Integrates into the system Hotspot settings.

## Compatibility
- Android 14 (API 34)
- Android 15 (API 35)
- Android 16 (API 36)
- Tested on AOSP / LineageOS

## Requirements
- Rooted with KernelSU, Magisk, or APatch
- LSPosed Framework installed

## How to Build
1. Open in Android Studio.
2. Build the `app` module.
3. Install the generated APK.
4. Enable the module in LSPosed manager.
5. Select recommended scopes (System Framework, Settings, System UI).
6. Reboot (or restart hooked processes).

## License
Apache License 2.0
