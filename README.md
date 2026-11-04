# Paninda

Minimal native Android starter for the University Shop POS and stock monitoring app.
The `highly-experimental` branch is the starting point for implementing Figma screens.

## Open and run

1. Open this repository in Android Studio (Otter 2025.2.2 or newer).
2. Use JDK 17 or newer for Gradle and install Android SDK Platform 36.
3. Let Gradle sync, then run the `app` configuration on an emulator or device
   running Android 7.0 (API 24) or newer.

Android Studio configures the SDK path in the ignored `local.properties` file.
For command-line builds, set `ANDROID_HOME` to your SDK directory instead.

```sh
./gradlew assembleDebug
./gradlew lintDebug
```

On Windows, use `gradlew.bat` instead of `./gradlew`.
The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## UI starting points

- `MainActivity.kt`: activity and edge-to-edge window setup.
- `ui/PanindaApp.kt`: placeholder screen and light/dark Compose previews.
- `ui/theme/Theme.kt`: shared Material 3 theme; add Figma colors and typography here.
- `res/values/strings.xml`: user-facing text.
- `gradle/libs.versions.toml`: pinned plugin and dependency versions.

The starter uses Kotlin and Jetpack Compose with a single app module. Navigation,
authentication, workbook access, local storage, and sync will be added as their
screens and behavior are implemented.
