# TurboOverlay

**TurboOverlay** is a lightweight, high-performance, real-time system monitoring tool and game optimizer for Android devices. Powered by **Shizuku**, it displays critical hardware metrics in an intuitive floating overlay panel and applies system-level performance tweaks without requiring root access.

Its primary goal is to deliver a convenient, Game Turbo-style monitoring experience while keeping resource consumption as low as reasonably possible, especially during demanding mobile games.

---

## Why TurboOverlay?

TurboOverlay is designed for gamers who want access to important hardware information without covering their screen with unnecessary monitoring elements or adding avoidable background overhead.

The development philosophy is simple:

**Monitor what matters. Keep the game running.**

Whether you are playing a demanding title such as BGMI or another resource-intensive Android game, TurboOverlay aims to provide useful real-time information through a lightweight floating interface.

The application is designed with lower-end and older Android devices in mind as well, including Android 10 devices. Actual performance and compatibility may vary depending on device hardware, Android implementation, game requirements, and background conditions.

### Inspired by the Game Turbo Experience

TurboOverlay attempts to provide the convenience of a gaming-assistant overlay commonly found in smartphone Game Turbo utilities.

It brings together:
- Real-time CPU monitoring.
- Battery temperature and power analytics.
- Internet speed and ping information.
- Quick performance controls.
- A compact floating dashboard.

All of this is designed to remain accessible while gaming, without turning the monitoring interface into another resource-heavy application.

---

## Key Features

### 1. Automatic Shizuku Detection & Direct Permission Popup

- **Instant Service Detection:** Automatically detects the active Shizuku background service on app launch or resume.
- **Direct Permission Dialog:** Automatically prompts the native Shizuku permission dialog inside the app, without requiring users to switch to the Shizuku application manually.
- **Rootless Elevated Privileges:** Uses the AIDL binder interface for shell execution through Shizuku.

### 2. Real-Time CPU Monitoring & Per-Core Frequency Analyzer

- **CPU Load Graph:** Real-time total CPU utilization percentage (0–100%) with custom Canvas graph rendering and history buffering.
- **Per-Core Clock Frequencies:** Displays live frequencies for CPU cores in MHz and GHz.
- **Universal Core Engine:** Multi-path sysfs fallback using `scaling_cur_freq`, `cpuinfo_cur_freq`, and batch `cat` execution, with automatic core online/offline state detection.

### 3. Battery & Power Consumption Analytics

- **Live Temperature Graph:** Real-time battery temperature monitoring in °C.
- **Power Draw Readout:** Displays voltage (V), current (mA), and power consumption in watts (W).
- **Charging State Intelligence:** Adjusts current direction and power calculations according to the charging state.

### 4. Network Speed & Ping Latency Tracker

- **Real-Time Bandwidth:** Live download and upload rates in KB/s, MB/s, and Mbps.
- **Ping Latency (ms):** Network latency monitoring for online gaming.
- **Signal Strength:** Displays available cellular/Wi-Fi signal strength information in dBm.

### 5. Protect Your Game Experience (Anti-Overheating Guard)

- **Continuous Load Protection:** Accessible through Settings. Automatically stops the overlay service if CPU load remains above 90% continuously for 20 seconds.
- **Automatic Counter Reset:** Resets the 20-second monitoring counter if CPU load drops below 90% during the monitoring period.
- **Material Switch:** A green-indicating toggle that is OFF by default on first installation.

This feature is intended to reduce additional monitoring overhead when the device is under sustained high CPU load. It is not a substitute for Android's thermal management system.

### 6. Built-in Game Launcher

- **Quick Access:** Add your favourite games and applications using the (+) dialog.
- **Auto Boost & Launch:** Launching a selected game automatically starts the overlay monitoring service and applies the configured performance boost.
- **Easy Management:** Remove games and applications by unchecking them inside the (+) dialog.

### 7. Interactive Floating Overlay Window

- **Floating Handle:** Draggable, edge-snapping handle that can remain accessible over other applications.
- **Compact Dashboard Panel:** Quick tabs for CPU Graph, Core Frequencies, Battery Metrics, Network Metrics, and Performance Controls.
- **Gesture Controls:** Swipe and tap interactions for hiding or expanding the monitoring panel.
- **Haptic Feedback:** Touch animations and tactile feedback on supported interactions.

---

## Why TurboOverlay Does Not Include an FPS Monitor

**TurboOverlay intentionally does not include an FPS monitoring feature.**

The objective is to keep the application lightweight and avoid unnecessary monitoring operations while a game is running.

An additional FPS measurement mechanism can introduce its own processing overhead, depending on the implementation and device. For this reason, the developer has chosen not to add an FPS counter as a planned feature.

### How can you identify lag without an FPS counter?

You can observe CPU utilization and its sudden changes through the real-time CPU graph.

For example:
- Sudden CPU utilization spikes may indicate increased processing demand.
- Repeated CPU spikes can help identify periods of unstable workload.
- CPU frequency changes alongside temperature increases may indicate performance degradation.

These readings can help users investigate possible performance issues, but CPU spikes alone do not prove that a game is lagging. GPU limitations, memory pressure, thermal throttling, network latency, and game-engine behaviour can also contribute to lag.

**The philosophy is to monitor the device without making the monitoring itself another source of unnecessary workload.**

---

## Designed for Gaming, Not Game Interference

TurboOverlay was developed with one primary objective:

**To provide useful hardware monitoring and performance controls during demanding games without unnecessarily interfering with the gaming experience.**

The developer has personally used earlier versions of TurboOverlay for approximately six months while testing and using the application.

During this personal testing period, the developer has not observed an account ban attributable to TurboOverlay.

However, no third-party application can guarantee that a game will never issue an account restriction or ban. Anti-cheat systems, game updates, and publisher policies are outside the developer's control. Users should review the relevant game's rules and use the application at their own discretion.

### Game File Integrity

TurboOverlay is designed to monitor device-level system information and perform its documented system-level operations.

**It is not designed to modify, replace, delete, or patch game installation files or game data.**

It does not require users to modify game files to use its monitoring overlay.

---

## Google Play Protect & APK Installation

When installing the APK downloaded from GitHub, Android may display a Google Play Protect scanning recommendation or request a security scan.

This can happen with applications installed outside Google Play.

### What should users do?

1. Allow Google Play Protect to scan the APK when prompted.
2. Wait for the scan to finish.
3. Review the scan result before proceeding with installation.

The developer encourages users to perform the scan rather than dismissing the recommendation.

Earlier published builds have been tested by the developer, but scan results may differ between versions and devices. No security scan should be treated as a permanent guarantee that an application is risk-free.

**If Google Play Protect displays an actual harmful-app warning, do not ignore or bypass it.**

---

## 8. Lightweight Performance Philosophy

TurboOverlay is designed to keep its monitoring workload controlled.

Its development priorities include:

- Avoiding unnecessary background operations.
- Using optimized monitoring threads.
- Keeping the floating interface compact.
- Avoiding an FPS measurement subsystem.
- Reducing monitoring activity when sustained high CPU load is detected.

Actual resource consumption depends on device hardware, monitoring frequency, enabled features, and Android background-process behaviour.

---

## 9. 100% Private, Secure & Offline

- **No Internet Permission:** The application is designed to operate without internet connectivity.
- **No Remote Telemetry:** No remote analytics or usage tracking is intended.
- **Local Monitoring:** System metrics are processed locally on the device.
- **Lightweight Footprint:** Monitoring operations are optimized to reduce unnecessary CPU and battery usage.

---

## Screenshots

Explore the TurboOverlay interface and its monitoring features.

### Main Dashboard

<img width="720" height="1600" alt="Dashboardd" src="https://github.com/user-attachments/assets/72277e10-da21-4c66-b0cf-1f8b28f1c2dd" />


### Floating Gaming Overlay

<img width="1600" height="720" alt="Screenshot_20261128-124504" src="https://github.com/user-attachments/assets/228ca1ae-67a4-4276-8f3c-b53b70781b8b" />


### CPU Monitoring

<img width="1600" height="720" alt="CPU" src="https://github.com/user-attachments/assets/251df590-aabb-4f60-9a0e-2d5e05b1440c" />


### Battery & Power Monitoring

<img width="1600" height="720" alt="Battery" src="https://github.com/user-attachments/assets/d73953e6-0e8d-4af3-af88-edb2006879a4" />
<img width="1600" height="720" alt="Screenshot_20261128-131242" src="https://github.com/user-attachments/assets/1f64b485-8209-4bcc-a5b1-cc00f3b54c7f" />



### Network Monitoring

<img width="1600" height="720" alt="Net" src="https://github.com/user-attachments/assets/21196cf4-f3bb-476f-98d8-4b5a0894b33e" />


---

## Prerequisites

1. **Android Version:** Android 8.0 (Oreo / API level 26) or higher.
2. **Shizuku Service:** Installed and activated on your device.
   - Download Shizuku from [Google Play](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or [GitHub Releases](https://github.com/RikkaApps/Shizuku/releases).
   - Start Shizuku through Wireless Debugging or ADB:
     ```bash
     adb shell sh /sdcard/Android/data/moe.shizuku.privileged.api/start.sh
     ```
3. **Display Over Other Apps Permission:** Required to render the floating window over games and other applications.

---

## How to Use the App

### 1. Setup Permissions

- Open Shizuku and ensure its service is running.
- Open TurboOverlay.
- Grant Shizuku permission through the automatic permission dialog.
- Allow Display over other apps permission when prompted by Android.

### 2. Launch the Overlay

- Tap **START OVERLAY**.
- A small transparent handle will appear near the top-left edge of the screen.

### 3. Navigate the Floating Overlay

- Tap or swipe the edge handle to open the monitoring dashboard.
- **CPU Tab:** View the live CPU load graph and individual core frequencies.
- **Battery Tab:** View battery temperature, voltage, current, and power draw.
- **Network Tab:** View download/upload speed, ping latency, and signal strength.
- **Performance Tab:**
  - Tap **Boost** for instant performance optimization.
  - Enable **PM Mode (Performance Per Minute)** to reapply the configured ADB performance tweaks every 60 seconds while gaming.
- **Hide Panel:** Tap the minimize icon or swipe towards the screen edge to collapse the dashboard.

### 4. Stop the Service

Tap the red **STOP APP** button on the main screen to stop the floating overlay service completely. This works with or without Shizuku.

---

## How to Build & Run

### 1. Clone the Repository

```bash
git clone https://github.com/Priy​​anshu75799/TurboOverlay.git
cd TurboOverlay
```

### 2. Build Debug or Release APK

**Debug Build:**
```bash
./gradlew assembleDebug
```

**Release Build:**
```bash
./gradlew assembleRelease
```

The generated APK will be available under:

```text
app/build/outputs/apk/
```

---

## Technical Architecture

- **Language:** Kotlin 2.1.0
- **Build System:** Android Gradle Plugin (AGP) 8.13.2 with Gradle Wrapper 9.4.1 and Kotlin DSL (`build.gradle.kts`)
- **Minimum SDK:** 29 (Android 10)
- **Target SDK:** 36 (Android 16)
- **Privilege & IPC:** Shizuku API 13.1.5 with custom AIDL service (`IPrivilegedShellService.aidl`)
- **UI & Graphics:** Material Design 3, View Binding, and custom Canvas `GraphView`

---

## License

This project is open-source under the **Apache License 2.0**.

See [LICENSE](LICENSE) for details.

---

## Author & Maintainer

Developed and maintained by **[Priyanshu75799](https://github.com/Priyanshu75799)**.

If you find TurboOverlay useful, consider giving the repository a ⭐ on GitHub!

---

**TurboOverlay: Know Your Device. Monitor Every Match.**
