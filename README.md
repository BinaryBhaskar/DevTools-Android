# DevTools

**One-tap Developer options, USB debugging and wireless debugging for Android.**
Quick Settings tiles and home-screen widgets, no root. Made by **Amurot**.

DevTools gives you single-tap switches for the developer settings you flip all day:

| Tile / widget | What it does |
| --- | --- |
| **Dev + ADB + Wireless** | Turns Developer options, USB debugging and wireless debugging on together, or all off |
| **Developer options** | Shows or hides the Developer options menu |
| **USB debugging** | ADB over a cable. Also turns on Developer options |
| **Wireless debugging** | ADB over Wi-Fi (Android 11+). Also turns on Developer options |
| **Stay awake** | The screen stays on while charging |
| **Show taps** | Shows a dot for each touch, for demos and screen recordings |
| **Animations** | Cycles all three animation scales: 1x › 0.5x › Off |

Widgets: a 4×1 **panel** (All 3 · Dev opts · ADB · Wi-Fi ADB) and single 1×1 buttons for each of the four
main switches. Tiles and widgets stay in sync when you change a setting anywhere else.

## Setup (one time)

Android only lets an app change these settings after you grant it `WRITE_SECURE_SETTINGS` from a
computer. The app walks you through this, and the steps are:

1. **Turn on Developer options:** Settings › About phone › tap *Build number* 7 times.
2. **Turn on USB debugging** in Developer options. On Xiaomi/HyperOS, also turn on *USB debugging (Security settings)*.
3. Install [Android platform-tools](https://developer.android.com/tools/releases/platform-tools), connect the phone, accept the prompt, then run:

   ```sh
   adb shell pm grant com.amurot.devtools android.permission.WRITE_SECURE_SETTINGS
   ```

4. Open DevTools. It detects the grant automatically.

The grant lasts until you uninstall the app. Already use wireless debugging? `adb pair` + `adb connect` work
instead of a cable.

### Behaviour notes

- Turning on USB or wireless debugging also turns on Developer options, as Settings does.
- Turning off **Developer options** on its own leaves ADB alone, so a tap never cuts the connection you are
  working over. **All 3** turns everything off.
- Wireless debugging needs Wi-Fi. The first time you use it on a new network, Android asks you to allow it.
- Some manufacturers (Samsung, Xiaomi) handle these settings a little differently. If a switch has no
  effect on your device, please [open an issue](../../issues).

## Install

Download the APK from [Releases](../../releases). An F-Droid build is planned.

## Build

```sh
./gradlew assembleDebug        # debug build: com.amurot.devtools.dev
./gradlew testDebugUnitTest    # unit tests for the toggle rules
./gradlew assembleRelease      # signed when keystore.properties or DEVTOOLS_KEYSTORE_* env vars exist
```

Requirements: JDK 17+, Android SDK 37. minSdk 31 (Android 12).

## Privacy

No internet permission, no analytics, no ads, no accounts. DevTools only reads and writes the system
settings listed above.

## License

Copyright 2026 Amurot. Licensed under the [Apache License 2.0](LICENSE).
