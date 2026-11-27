# DevTools: app brief

| | |
| --- | --- |
| Folder | `~/Coding/Projects/AndroidApps/DevTool` |
| Package | `com.amurot.devtools` (debug: `com.amurot.devtools.dev`) |
| minSdk / target | 31 / 37 |
| Emulator allowed | Yes (dev variant); real-device checks on the owner's phone |
| GitHub | Public, open source |
| License | Apache 2.0 |
| Distribution | GitHub Releases APK now; F-Droid later (no Google/proprietary deps, `dependenciesInfo` off) |
| Foundations copied | None; a small standalone theme in the same style as Linkerly (shapes, tonal surfaces) |

## Product
- Primary user: Amurot (for daily use), then other Android developers.
- Core job: single-tap Developer options, USB debugging, wireless debugging, and all three together, from Quick Settings and home-screen widgets.
- Mechanism: `WRITE_SECURE_SETTINGS`, granted once over adb (instructions in the app), or by DevTools itself through Shizuku or root ("No computer?" path, added at the owner's request for users who won't use adb).
- Show taps (`Settings.System.show_touches`) is not a public System setting: apps can't write it even with the grant (verified on a Samsung S25 Edge, Android 16). DevTools writes it with `settings put` through Shizuku or root; without either, the switch is disabled with an explanation.

## v1 scope
- Tiles: Developer options, USB debugging, Wireless debugging, All 3, Stay awake, Show taps, Animations (cycles 1x › 0.5x › Off).
- Widgets: a 4×1 panel, plus 1×1 for All 3, Dev options, USB and Wireless.
- In-app: setup guide with copyable command, live switches, add-tile/add-widget buttons, About.
- Picked from the interview: Stay awake + Show taps, Animation scale.
- Not in v1: wireless ADB IP:port card, layout bounds/GPU overdraw (system properties, unreliable).

## Brand
- Name "DevTools", terminal-green accent (#39D98A dark / #1F9D63 light), dark-first, toggle + `</>` icon. Made by Amurot.

## Monetization
- None. Free and open source, no ads, no tracking, no internet permission.

## Rules
- Turning on USB/wireless debugging also turns on Developer options.
- Turning off Developer options alone leaves ADB on (never cut the live connection); All 3 turns everything off.
- All 3 counts as on when Dev + USB are on and wireless is on (or impossible: no Wi-Fi).
- Widgets sync through a content-trigger JobScheduler job (re-armed on boot); tiles observe settings while listening.

## Rejected
- Play Store first: WRITE_SECURE_SETTINGS apps face policy friction; F-Droid fits open source better.
- Glance widgets: RemoteViews are lighter and simpler for one-tap buttons.
- Hilt/Room: no data to store, one screen.
- Shizuku UserService/AIDL: one-off `sh -c` commands via Shizuku's (hidden) newProcess are enough; kept by an R8 rule.
