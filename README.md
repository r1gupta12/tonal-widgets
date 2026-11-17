# Tonal Widgets

A small Material You widget pack for Android 12+ that picks up your wallpaper colours and follows light/dark mode.

| Widget | Size | Tap action |
|---|---|---|
| Big clock | 4×2 | Opens alarms |
| Pill clock | 4×1 | Time opens alarms; day bubble opens calendar |
| Date tile | 2×2 | Opens calendar |
| Battery ring | 2×2 | Opens battery usage (refreshes every 15 min) |
| Quote of the day | 4×2 | Shows the next quote |
| Search bar | 4×1 | Google/web search; mic starts voice assistant |
| Quick actions | 4×1 | Dialer, camera, maps, flashlight toggle |

All widgets can be resized. The app itself is a gallery with live previews and an **Add to home screen** button for each widget.

## Build & install

**Android Studio (easiest)**
1. Unzip, then *File → Open* the `TonalWidgets` folder. Let Gradle sync (it downloads the SDK pieces it needs).
2. Turn on USB debugging on your phone (*Settings → About phone → tap Build number 7× → Developer options → USB debugging*), plug it in, and press **Run ▶**.

**Command line** (needs Android SDK + JDK 17)
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release.apk
adb install app/build/outputs/apk/release/app-release.apk
```
You can also copy the APK to your phone and open it there (allow "Install unknown apps" for your file manager).

The release build is signed with your local debug key so you can sideload it right away. Set up a proper keystore before you put it on Google Play.

## Customising
- **Colours**: `res/values/colors.xml` (light) and `res/values-night/colors.xml` (dark) map each role to Android's `system_accent1/2/3` and `system_neutral1` tonal palettes.
- **Shapes**: `res/drawable/shape_*.xml` (circle, squircle, pill). Swap any widget's `android:background`.
- **Quotes**: `Quotes.kt`.
- **New widget**: add a layout, an `xml/info_*.xml`, a `BaseWidget` subclass, a `<receiver>` in the manifest, and an entry in `MainActivity.entries`.

## Notes
- The flashlight toggle remembers its own on/off state. If you turn the torch off from Quick Settings, the button stays lit until you tap it again. Android can also turn the torch off if it kills the app in the background.
- The minimum is Android 12 (API 31) because dynamic colours start there.
