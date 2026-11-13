# AndroidIsland – LiquidIsland (`Dynamic Motion 1.2 For Android`)

Complete Android 14–16 (`compileSdk = 36`, `targetSdk = 36`, `minSdk = 28`) implementation of **LiquidIsland** alongside the preserved **DroidBubble** mode, engineered to match the reference video **`96942.mp4` (`Dynamic Motion 1.2 For Android`)** and **`liquid.zip`**.

## Key Architecture & Features

1. **Dual-Mode Architecture (`DroidBubble` + `LiquidIsland`)**:
   - `com.xiao.androidisland.droidbubble.*`: Preserves the original `DroidBubble` pill engine intact.
   - `com.xiao.androidisland.liquidisland.*`: Full iOS Dynamic Island, Liquid Glass renderer, Dynamic Status Bar, Edge-Based Liquid Absorption, Directional Motion Blur, iOS Control Center, Home Screen Widgets, and Settings UI.

2. **Hybrid C++17 NDK OpenGL ES 3.0 + AGSL Liquid Glass Engine**:
   - **Native C++17 / OpenGL ES 3.0 (`app/src/main/cpp/`)**: `LiquidGlassGlRenderer.cpp` and `liquid_island_jni.cpp` compile `#version 300 es` shaders providing real-time polynomial smooth-minimum (`smin`) metaball union, boundary normal reconstruction, Snell's law chromatic lens refraction, specular rim highlights, and 9-tap directional velocity motion blur.
   - **AGSL RuntimeShader & Compose Modifiers (`liquidisland/renderer/`)**: Adapted directly from `liquid.zip` (`Lens.kt`, `InnerShadow.kt`, `Vibrancy.kt`, `CombinedBackdrop.kt`, `LiquidGlassNavigationBar.kt`).

3. **All 10 Reference Video States (`96942.mp4`)**:
   - `00:00`: **Idle Compact Pill** (`120 × 32 dp`) + Dual-Month Calendar Widget (`OCT` / `NOV`) + `Spotify`, `Threads`, and live Analog Clock widget (`Tue, 13 Oct`).
   - `00:01` & `00:04`: **Compact & Expanded Notification Island** (Telegram download pill & `347 × 178 dp` expanded card with `Reply` / `Mark as read`, plus `Metrolist 13.7.0` update banner at `00:10`).
   - `00:07`, `00:09`, `00:13`, `01:08`: **Music Island & Split Dual Activity** (`150 × 32 dp` compact pill, detached `132 dp + 32.5 dp` metaball split island, and `347 × 180 dp` expanded music player card with scrubbable timeline, animated 6-bar waveform, transport controls, AirPlay output button, and toggleable `Album Gradient` mode).
   - `00:22` & `00:28`: **Timer / Stopwatch Island** (`142 × 32.5 dp` compact pill and `347 × 91 dp` expanded card with `Lap` & `X` buttons).
   - `00:39` & `00:44`: **Navigation Island** (`148 × 32.5 dp` compact turn pill and `340 × 136 dp` expanded maneuver card with `3.6 km`, `Arrive 7:21 PM`, and red `End Trip` pill).
   - `00:49`–`01:06`: **iOS Control Center** + **Ringer/Silent/Vibrate Island** (`295 × 36 dp`) + **Charging `80%` Island** (`295 × 36 dp`).
   - `01:19`–`01:25`: **Settings Screen** (`LOCK SCREEN AUTH` with `Face ID` / `Fingerprint` lock-to-checkmark morphing animation and `MEDIA STYLE` with `Album Gradient`).

4. **Dynamic iOS Status Bar with Edge-Based Liquid Absorption & Directional Motion Blur**:
   - Per-frame signed edge distance evaluation (`Phase A: Approach`, `Phase B: Contact + 9-Tap Horizontal Velocity Motion Blur`, `Phase C: Absorption`, and `Reverse Release` when the Island contracts).

## Building APK on GitHub Actions

Simply push this repository (or upload `AndroidIsland-LiquidIsland.zip` directly) to GitHub. The included `.github/workflows/android-ci.yml` automatically unpacks the archive if uploaded as a `.zip`, installs NDK 27.2.12479018 & CMake 3.22.1, runs `./gradlew testDebugUnitTest assembleDebug assembleRelease --stacktrace`, and uploads the compiled APK artifacts.
