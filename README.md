# 🏍️ Mary Spares — Store Stock Manager

> **Modern, offline-first inventory & spare parts management system built exclusively for Mary Two Wheelers Spares store.**

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20%26%20Material%203-4285F4.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Firebase](https://img.shields.io/badge/Backend-Firebase%20Cloud%20Firestore-FFA611.svg?style=flat&logo=firebase)](https://firebase.google.com)
[![Room](https://img.shields.io/badge/Local%20DB-Room%20SQLite-2C3E50.svg?style=flat&logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Android](https://img.shields.io/badge/Platform-Android%207.0%2B%20%28API%2024--37%29-3DDC84.svg?style=flat&logo=android)](https://developer.android.com)

---

## 📥 Download & Install APK

Download the latest production release directly from GitHub:

- 🏷️ **Latest Release**: [**Download MarySpares-v1.0.4.apk (v1.0.4)**](https://github.com/jinsu-2005/MarySpares-Stock-Manager/releases/tag/v1.0.4)
- 📦 **All Releases**: [GitHub Releases Page](https://github.com/jinsu-2005/MarySpares-Stock-Manager/releases)

### How to Install on Android:
1. Download **`MarySpares-v1.0.4.apk`** to your phone.
2. Tap the downloaded file in your notification bar or file manager.
3. If prompted, allow *"Install from unknown sources"* or *"Allow from this source"*.
4. Tap **Install** and open the app!

---

## ✨ Key Features

| Feature | Description |
| :--- | :--- |
| 📦 **Smart Inventory Tracking** | Auto-assigned continuous serials (`#1`, `#2`...), custom part numbers, shelf/rack locations, and Rupee pricing (Selling Price & MRP). |
| ⚡ **Offline-First Architecture** | High-speed local Room SQLite caching. Full app functionality available offline with zero latency. |
| 🔄 **Firebase Cloud Sync** | Seamless two-way background sync with Cloud Firestore. Queues offline actions and uploads automatically when online. |
| 🔔 **Stock Alerts & Review Badge** | Instant alerts for low stock ($\le 5$ units) and out-of-stock ($0$ units) with filter tabs and quick one-tap restock actions. |
| 📜 **Audit History & Logs** | Complete transaction logging for all inward additions, stock adjustments, and sales with customizable retention pruning. |
| 👥 **Role-Based Access Control** | Secure Google & Email authentication supporting 5 user roles (*Admin*, *Owner*, *Staff*, *Relative*, *Friend*) with invite whitelisting. |
| 💾 **SAF Backups & CSV Export** | Export store inventory directly to CSV or create complete 4-collection database `.zip` backups to any user-chosen folder (Downloads, Drive, SD card). |
| 🎨 **Adaptive Modern UI** | Polished Glassmorphic Floating Dock, custom rounded snackbars, and a high-contrast Dark & Light mode. |

---

## 🛠️ Architecture & Tech Stack

- **Language**: Kotlin 100%
- **UI Toolkit**: Jetpack Compose + Material 3
- **Design Pattern**: MVVM (Model-View-ViewModel) + Repository Pattern + Kotlin Coroutines & Flow
- **Local Storage**: Android Jetpack Room (SQLite)
- **Cloud Backend**: Firebase Authentication & Cloud Firestore
- **File & Storage Handling**: Android Storage Access Framework (SAF)

---

## 🚀 Building from Source

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **JDK**: Java 17 or Java 21
- **Android SDK**: API 34+ (compileSdk 37)

### Steps:
1. **Clone the Repository**:
   ```bash
   git clone https://github.com/jinsu-2005/MarySpares-Stock-Manager.git
   cd MarySpares-Stock-Manager
   ```

2. **Add Firebase Configuration**:
   - Place your `google-services.json` file inside the `app/` folder:
     ```
     MarySpares/
     └── app/
         └── google-services.json
     ```

3. **Build the Debug APK**:
   ```bash
   # On Windows PowerShell
   .\gradlew assembleDebug

   # On macOS / Linux
   ./gradlew assembleDebug
   ```

4. **Locate the Output APK**:
   - Output directory: `app/build/outputs/apk/debug/app-debug.apk`

---

## 👤 Developer & Maintainer

- **Developer & Maintainer**: **Jinsu J**
- **Store**: **Mary Two Wheelers Spares**
- **Repository**: Private / Store Inventory Management System
