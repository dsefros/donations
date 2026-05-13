# Orthodox Charity App — Jetpack Compose

Fullscreen 480×480 Orthodox charity app built with Jetpack Compose.
Animated starfield, glowing Orthodox cross, 3 donation buttons in rubles.

---

## Prerequisites (Mac)

### 1. Install JDK 17
```bash
brew install --cask temurin@17
```

### 2. Install Android command-line tools (no Android Studio needed)
```bash
brew install --cask android-commandlinetools
```
This installs `sdkmanager` to `/opt/homebrew/share/android-commandlinetools/`.

### 3. Accept licences & install SDK components
```bash
export ANDROID_HOME="$HOME/Library/Android/sdk"
mkdir -p "$ANDROID_HOME"

sdkmanager --sdk_root="$ANDROID_HOME" --install \
  "platform-tools" \
  "platforms;android-34" \
  "build-tools;34.0.0"

sdkmanager --sdk_root="$ANDROID_HOME" --licenses
```
*(Accept all licences by typing `y` repeatedly)*

---

## Build the APK

```bash
cd OrthodoxCharityCompose

# Make gradlew executable
chmod +x gradlew

# Build debug APK  (fastest — no signing needed, installs on any device)
./gradlew assembleDebug

# APK is at:
#   app/build/outputs/apk/debug/app-debug.apk
```

### Build release APK (unsigned)
```bash
./gradlew assembleRelease
# APK: app/build/outputs/apk/release/app-release-unsigned.apk
```

### Sign the release APK
```bash
# Generate a keystore (one-time)
keytool -genkey -v \
  -keystore my-release-key.jks \
  -alias orthodox \
  -keyalg RSA -keysize 2048 \
  -validity 10000

# Sign
$ANDROID_HOME/build-tools/34.0.0/apksigner sign \
  --ks my-release-key.jks \
  --out app-release-signed.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

---

## Install on device / emulator

```bash
# List connected devices
adb devices

# Install
adb install app/build/outputs/apk/debug/app-debug.apk

# Launch
adb shell am start -n com.orthodox.charity/.MainActivity
```

---

## Project structure

```
OrthodoxCharityCompose/
├── gradlew                          ← run this to build
├── build.gradle                     ← root: AGP + Kotlin plugins
├── settings.gradle                  ← module includes
├── gradle.properties
├── gradle/wrapper/
│   └── gradle-wrapper.properties    ← Gradle 8.6
└── app/
    ├── build.gradle                 ← Compose BOM, compileSdk 34
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml      ← fullscreen, portrait, no title bar
        ├── java/com/orthodox/charity/
        │   └── MainActivity.kt      ← entire Compose UI (single file)
        └── res/
            ├── values/strings.xml
            └── mipmap-*/ic_launcher.png
```

---

## What the app does

- **Fullscreen immersive** — hides status bar, navigation bar, title bar
  using `FLAG_FULLSCREEN` + `SYSTEM_UI_FLAG_IMMERSIVE_STICKY`
- **Back button disabled**
- **Portrait locked**
- **Animated starfield** canvas (100 twinkling stars)
- **Orthodox 8-pointed cross** — titlo (top bar) + main crossbar +
  suppedaneum (angled footrest) — drawn with gold gradients + glow animation
- **3 donation buttons:**
  | Cause           | Amount   |
  |-----------------|----------|
  | Помощь храму    | 500 ₽    |
  | Детский приют   | 1 000 ₽  |
  | Помощь бедным   | 250 ₽    |
- Tap any button → **confirmation dialog** with gold "АМИНЬ" dismiss button
- Optimised for **480×480 dp** screens (square / smartwatch-style displays)
