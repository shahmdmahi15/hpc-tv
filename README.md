# HPC TV — Native Android TV Waiting Room Display Application

**Health & Pain Care Center (HPC) • Android TV Display Client**  
*Built with 100% Native Kotlin & Jetpack Compose for Android TV (Min SDK 6.0 / API 23)*

---

## 🏥 Application Overview

**HPC TV** is the official dedicated Android TV application for **Health & Pain Care Center (Jashore, Bangladesh)**. Designed specifically for waiting room television screens, it delivers real-time patient queue announcements, doctor consultation chambers tracking, physical therapy room session countdowns, and clinic bulletin notices.

### Key Capabilities
* **100% Native Jetpack Compose**: Built with modern Compose TV foundations, avoiding slow webviews or browser wrappers.
* **Pixel-Perfect HPC Clinical Design**: High-contrast clinical dark theme (`#09090B`) utilizing the official HPC Emerald (`#10B981`), Amber (`#F59E0B`), and Zinc palettes to protect TV screens from OLED burn-in while remaining readable from 10–20 feet away.
* **Zero-Configuration LAN Auto-Discovery**: Automatically discovers the HPC clinic server on the local Wi-Fi network via UDP broadcast on port `8888` (`HPC_DISCOVER_SERVER`).
* **High-Speed Real-Time Sync (SSE)**: Connects to `/api/v1/tv/waiting-room/stream` for instant queue changes, status updates, and emergency broadcasts.
* **Dual-Language Voice Announcements (Bangla & English)**:
  * Plays a resonant dual-harmonic hospital bell chime (*Ding-Dong* at 523Hz & 392Hz) generated via native PCM audio synthesis.
  * Speaks clear spoken voice announcements: Bengali first, followed seamlessly by English (`"দয়া করে মনোযোগ দিন। টোকেন ১২..."` -> `"Attention please. Token 12..."`).
* **Live Therapy Modality Countdown Timers**: Decrements remaining seconds (`MM:SS`) smoothly every second for active therapy modalities (Short Wave Diathermy, Ultrasound, Traction, etc.).
* **Continuous Bulletin Marquee**: Live scrolling ticker cycling through admin bulletin notices and urgent clinic announcements.
* **Admin Fleet Telemetry**: Automatically reports live round-trip ping latency and device health back to `/api/network/heartbeat`, appearing on the clinic administrator's hardware fleet monitor (`/admin/network`).

---

## 🛠 Project Structure

```
HPCTV/
├── app/
│   ├── src/main/
│   │   ├── java/com/shahmdmahi/hpctv/
│   │   │   ├── audio/
│   │   │   │   ├── HospitalChimePlayer.kt      # Native PCM synthesis of hospital bell chime
│   │   │   │   └── ClinicTtsManager.kt         # Bilingual Text-To-Speech sequencer (BN + EN)
│   │   │   ├── data/
│   │   │   │   ├── discovery/
│   │   │   │   │   └── ServerDiscoveryManager.kt # UDP broadcast (port 8888) auto-discovery
│   │   │   │   ├── model/
│   │   │   │   │   └── WaitingRoomModels.kt    # Kotlinx Serialization data classes
│   │   │   │   └── network/
│   │   │   │       ├── HpcNetworkClient.kt     # OkHttpClient with bundled Root CA trust
│   │   │   │       ├── WaitingRoomApiService.kt# Snapshot REST client
│   │   │   │       ├── WaitingRoomSseManager.kt# Long-lived SSE real-time stream listener
│   │   │   │       └── TelemetryHeartbeatManager.kt # Periodic ping reporter (every 12s)
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   │   ├── HeaderSection.kt        # Brand logo, live clock, status pills
│   │   │   │   │   ├── DoctorChambersSection.kt# Doctor consultation chambers & serials
│   │   │   │   │   ├── TherapyRoomsSection.kt  # 15 Therapy rooms & live countdown timers
│   │   │   │   │   ├── DoctorCallAnnouncementDialog.kt # Fullscreen caller modal with pulse
│   │   │   │   │   ├── EmergencyAlertBanner.kt # Emergency sysadmin alert banner
│   │   │   │   │   ├── BulletinMarqueeBar.kt   # Continuous basicMarquee ticker
│   │   │   │   │   └── ServerConnectionDialog.kt# Radar discovery overlay & manual IP setup
│   │   │   │   ├── theme/
│   │   │   │   │   ├── Color.kt                # HPC Emerald & Zinc color palette
│   │   │   │   │   ├── Theme.kt                # TV Material3 high-contrast dark theme
│   │   │   │   │   └── Type.kt                 # Typographic scale for TV viewing
│   │   │   │   └── viewmodel/
│   │   │   │       └── WaitingRoomViewModel.kt # StateFlows, countdown tickers & audio triggers
│   │   │   └── MainActivity.kt                 # TV Immersive mode & KeepScreenOn flag
│   │   ├── res/
│   │   │   ├── drawable/                       # HPC clinic branding logo and icon
│   │   │   ├── raw/root_ca.crt                 # Bundled clinic SSL root certificate
│   │   │   └── xml/network_security_config.xml # Local LAN SSL trust configuration
│   │   └── AndroidManifest.xml                 # TV Leanback & network permissions
│   └── build.gradle.kts                        # App dependencies (OkHttp, Compose TV, etc.)
└── gradle/
    └── libs.versions.toml                      # Version catalog
```

---

## 🚀 How to Run in Android Studio (Windows)

1. **Open the Project**:
   * Launch **Android Studio** on your Windows PC.
   * Click **Open** and select:
     `C:\Users\shahmdmahi\AndroidStudioProjects\HPCTV`

2. **Sync Gradle**:
   * Android Studio will automatically sync the Gradle configuration using your local Android SDK.

3. **Run on Android TV**:
   * **Physical Android TV / TV Box**:
     1. Enable **Developer Options** and **USB Debugging** (or **Network ADB Debugging**) on your Android TV device.
     2. Connect to the TV:
        ```cmd
        adb connect <TV_IP_ADDRESS>:5555
        ```
     3. Select the TV in Android Studio's target device dropdown and click **Run (Shift + F10)**.
   * **Android TV Emulator**:
     1. In Android Studio's Device Manager, create an **Android TV (1080p)** virtual device.
     2. Click **Run**.

---

## 🌐 Network Discovery & Communication

### 1. UDP Auto-Discovery (Zero Setup)
On boot, the app broadcasts `HPC_DISCOVER_SERVER` to UDP port `8888`. The clinic server (`hpc-web`) replies with:
```json
{
  "service": "hpc-clinic-server",
  "ip": "192.168.1.100",
  "port": 3000,
  "ssl": true,
  "name": "Health Peak Center"
}
```
The app caches this IP address so subsequent launches connect instantly without scanning.

### 2. Manual IP Override
If Wi-Fi AP Isolation prevents UDP broadcasts:
1. Use the TV Remote to click the **Settings** gear icon in the top header (or click **Manual IP** on the discovery screen).
2. Enter the server's local LAN IP (e.g. `192.168.1.100`) and click **Connect**.

---

## 🎨 Theme & Color Reference

| Name | Hex Value | Role |
| :--- | :--- | :--- |
| `HpcEmerald500` | `#10B981` | Primary Brand Color / Active Callers |
| `HpcEmerald600` | `#059669` | Primary Accents / Buttons |
| `HpcEmeraldDark` | `#064E3B` | Brand Containers & Badges |
| `HpcTvBackground`| `#09090B` | Deep TV Background (Anti Burn-in) |
| `HpcCardBackground`| `#141417`| Main Section Surfaces |
| `HpcBorderZinc800`| `#27272A`| Clean Dividers & Outlines |
| `HpcAmber500` | `#F59E0B` | Waiting Serials & Warnings |
| `HpcRed600` | `#DC2626` | Emergency Alerts & Urgent Notices |
| `HpcBlue400` | `#38BDF8` | Physical Therapy Modality Names |

---

**Health & Pain Care Center (HPC)** • Jashore, Bangladesh • 2026
