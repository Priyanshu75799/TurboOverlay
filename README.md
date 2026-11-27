# TurboOverlay

**TurboOverlay** is a lightweight, high-performance, real-time system monitoring tool and game optimizer for Android devices. Powered by **Shizuku**, it displays critical hardware metrics in an intuitive floating overlay panel and applies universal performance ADB tweaks without requiring root access.

---

## Key Features

### 1. Automatic Shizuku Detection & Direct Permission Popup
- **Instant Service Detection**: Automatically detects active Shizuku background service on app launch or resume.
- **Direct Permission Dialog**: Automatically prompts the native Shizuku permission dialog inside the app—no need to switch to the Shizuku app manually.
- **Rootless Elevated Privileges**: Uses AIDL binder interface for secure, low-overhead shell execution.

### 2. Real-Time CPU Monitoring & Per-Core Frequency Analyzer
- **CPU Load Graph**: Real-time total CPU utilization percentage (0–100%) with custom canvas graph rendering and history buffering.
- **Per-Core Clock Frequencies**: Displays live frequencies for Cores 0 through 7 in **MHz** and **GHz**.
- **Universal Core Engine**: Multi-path sysfs fallback (`scaling_cur_freq`, `cpuinfo_cur_freq`, batch `cat` execution) with automatic core online/offline state detection.

### 3. Battery & Power Consumption Analytics
- **Live Temperature Graph**: Real-time battery thermal monitoring (°C).
- **Power Draw Readout**: Real-time Voltage (V), Current (mA), and Power consumption in Watts (W).
- **Charging State Intelligence**: Automatically adjusts current direction and power draw calculations based on charging status.

### 4. Network Speed & Ping Latency Tracker
- **Real-Time Bandwidth**: Live Download and Upload rates in KB/s, MB/s, and Mbps.
- **Ping Latency (ms)**: Real-time network latency ping indicator designed for lag-free online gaming (BGMI, Free Fire, PUBG, Call of Duty, Mobile Legends).
- **Signal Strength**: Real-time cellular/Wi-Fi signal strength in dBm.

### 5. Protect Your Game Experience (Anti-Overheating Guard)
- **Continuous Load Protection**: Accessible via Settings. Automatically turns off the overlay service if CPU load stays above 90% continuously for 20 seconds.
- **Loophole-Free Monitoring**: Automatically resets the 20-second counter if CPU load drops below 90% at any point during monitoring.
- **Vibrant Green Switch**: Features a sleek Material toggle switch that illuminates green when enabled (OFF by default on first install).

### 6. Built-in Game Launcher
- **Quick Access**: Add your favorite games and apps to the launcher using the (+) dialog.
- **Auto Boost & Launch**: Launching a game automatically starts the Overlay Monitoring Service and applies an instant performance boost.
- **Easy Management**: Remove any game or app from the launcher anytime by unchecking it inside the (+) dialog.

### 7. Interactive Floating Overlay Window
- **Floating Handle**: Edge-snapping draggable handle overlayable over any game or app.
- **Compact Dashboard Panel**: Quick tabs for CPU Graph, Core Frequencies, Battery Metrics, Network Metrics, and Performance Controls.
- **Gesture Controls**: Smooth swipe & tap gestures for hiding or expanding the monitor.
- **Haptic Feedback**: Touch animations and tactile feedback on button interactions.

### 8. 100% Private, Secure & Offline
- **Zero Internet Access**: Complete privacy with zero network calls, zero telemetry, and zero remote data collection.
- **Lightweight Footprint**: Optimized background thread handlers for minimal CPU/battery usage while gaming.

---

## Prerequisites

1. **Android Version**: Android 8.0 (Oreo / API level 26) or higher.
2. **Shizuku Service**: Installed and activated on your device.
   - Download Shizuku from [Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub Releases](https://github.com/RikkaApps/Shizuku/releases).
   - Start Shizuku via **Wireless Debugging** or **ADB command**:
     ```bash
     adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
     ```
3. **Display Over Other Apps Permission**: Required to render the floating window over games.

---

## How to Use the App (User Manual)

1. **Setup Permissions**:
   - Open **Shizuku** and ensure the service is running.
   - Open **TurboOverlay**. When prompted, grant **Shizuku permission** on the auto-popup dialog.
   - Allow **Display over other apps** (Overlay) permission when prompted by Android settings.

2. **Launch the Overlay**:
   - Tap **START OVERLAY**. A small transparent handle will appear near the top-left edge of your screen.

3. **Navigate the Floating Overlay**:
   - Tap or swipe the edge handle to open the compact monitoring dashboard over any game or app.
   - **CPU Tab (Chip Icon)**: Click once to view the live CPU load graph. Click again to view individual **Core Frequencies (Cores 0–7)**.
   - **Battery Tab (Battery Icon)**: View live battery temperature graph, Voltage (V), Current (mA), and Power draw (W).
   - **Network Tab (Speed Icon)**: View live Download/Upload speed, Ping Latency (ms), and Signal Strength (dBm).
   - **Performance Tab (Bolt Icon)**: 
     - Tap **Boost** for instant one-tap performance optimization.
     - Toggle **PM Mode (Performance Per Minute)** to automatically re-apply ADB performance tweaks every 60 seconds while gaming.
   - **Hide Panel**: Tap the minimize icon or swipe towards the screen edge to collapse the dashboard back into the handle.

4. **Stop the Service**:
   - Tap the red **STOP APP** button in the main screen anytime to stop the floating overlay service completely (works with or without Shizuku).

---

## How to Build & Run

### 1. Clone the repository
```bash
git clone https://github.com/Priyanshu75799/TurboOverlays.git
cd TurboOverlays
```

### 2. Build Debug or Release APK
- **Debug Build**:
  ```bash
  ./gradlew assembleDebug
  ```
- **Release Build**:
  ```bash
  ./gradlew assembleRelease
  ```

Output APK will be generated under `app/build/outputs/apk/`.

---

## ️ Technical Architecture

- **Language**: Kotlin 2.1.0
- **Build System**: Gradle 8.13.2 with Kotlin DSL (`build.gradle.kts`)
- **Min SDK**: 26 | **Target SDK**: 35 (Android 15)
- **Privilege & IPC**: [Shizuku API 13.1.5](https://github.com/RikkaApps/Shizuku) + Custom AIDL Service (`IPrivilegedShellService.aidl`)
- **UI & Graphics**: Material Design 3, View Binding, Custom Canvas `GraphView`

---

## License

This project is open-source under the **MIT License**. See [LICENSE](LICENSE) for details.

---

## 👨‍💻 Author & Maintainer

Developed by **[Priyanshu75799](https://github.com/Priyanshu75799)**.
If you like this project, please give it a ⭐ on GitHub!
