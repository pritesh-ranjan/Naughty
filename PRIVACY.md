# Privacy Policy & Architecture

**Last Updated**: October 2026  
**Application**: Naughty (`com.example.naughty`)  
**License**: MIT License  

---

## 1. Executive Summary & Privacy Pledge

**Naughty is an offline-first, local-only note-taking and contextual task management application.**

Our fundamental design philosophy is zero data lock-in and zero surveillance:
* **Zero Internet Access**: Naughty explicitly strips and does not declare `android.permission.INTERNET` in its manifest. The application cannot communicate with any remote servers, cloud infrastructure, or third parties.
* **Zero Telemetry or Analytics**: There are no analytics libraries, event trackers, user identifiers, advertising SDKs, or remote crash reporting tools.
* **Local Machine Learning & Audio**: OCR text extraction (Google ML Kit), neural audio noise reduction (Audx/RNNoise), and speech transcription (Android SpeechRecognizer) run exclusively on your device's local CPU/NPU.
* **Hardware-Backed Biometrics**: Biometric protection is handled strictly through the official Android BiometricPrompt and hardware keystore.
* **Open File Formats**: All note contents are stored on your device as physical UTF-8 `.md` (Markdown) files under the app-specific storage directory.

---

## 2. Manifest Permissions & Justifications

Every permission requested by Naughty serves a direct, on-device user functional purpose and has been reviewed for least privilege:

| Permission | Category | Why Naughty Needs It | Offline Safeguard |
| :--- | :--- | :--- | :--- |
| `RECORD_AUDIO` | Runtime | Captures microphone audio for voice notes and on-device transcription. | Raw audio is processed locally with Audx/Opus and stored internally; never transmitted over any network. |
| `PACKAGE_USAGE_STATS` | Special App Access | Required by the Context-Aware App Binding engine to detect when you launch an app bound to a note (e.g. showing meeting notes when launching Zoom). | Queried strictly on device in memory via `UsageStatsManager`. Event data is never written to disk or transmitted anywhere. |
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Delivers heads-up contextual cards when a bound app is in foreground, and alarms for user-scheduled note reminders. | Notifications are created locally via `NotificationManager` and auto-dismiss upon leaving the target app. |
| `SCHEDULE_EXACT_ALARM` | Special / Alarm | Allows reliable exact alerts for user-scheduled note reminders and milestone due dates. | Scheduled via Android's local `AlarmManager` with `RTC_WAKEUP`. |
| `USE_EXACT_ALARM` | Normal (API 33+) | Standard permission for alarm and reminder apps to ensure task alerts fire accurately during Doze mode. | Operates 100% on-device. |
| `RECEIVE_BOOT_COMPLETED` | Broadcast | Automatically reschedules active note alarms when your phone restarts. | Handled locally by `ReminderReceiver`. |
| `USE_BIOMETRIC` / `USE_FINGERPRINT` | Hardware Security | Authenticates the user via fingerprint, facial unlock, or device PIN before unlocking sensitive notes. | Cryptographic keys stay inside the hardware-backed Android Keystore / Secure Enclave. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE` | Foreground Service | Allows context monitoring when foreground service execution is necessary. Subtype: `app_context_monitoring`. | Zero background network transfer. |

### Notice on Network & Package Visibility Permissions
* **`android.permission.INTERNET`**: **STRIPPED / ABSENT**. Naughty explicitly blocks this permission via `tools:node="remove"`.
* **`android.permission.QUERY_ALL_PACKAGES`**: **STRIPPED / ABSENT**. Naughty does NOT query all packages. It adheres to Android 11+ policy by specifying `<queries>` for launcher intent activities and speech recognition only, protecting user application privacy while allowing note binding to user-facing apps.

---

## 3. Data Storage, Backup & Extraction Policy

### 3.1 Local Storage
* **Metadata**: Note titles, tags, pins, reminder timestamps, and app binding associations are stored in a local SQLite Room database (`naughty_db`) in private app storage.
* **Note Bodies**: Stored as standard Markdown (`.md`) files in the app's internal files directory (`context.filesDir/notes/`).
* **Audio Notes**: Encoded directly into `.opus` files with `.wf` waveform amplitude data in private internal storage (`context.filesDir/note_audio/`).

### 3.2 Cloud Backup Exclusion
To prevent sensitive notes or biometrically locked data from being uploaded to third-party cloud services without user knowledge, Naughty configures strict data extraction rules:
* **Cloud Backup (`<cloud-backup>`)**: **Disabled**. All database files and note markdown files are excluded from automated Google Drive / Android Cloud backups.
* **Device Transfer (`<device-transfer>`)**: **Enabled**. Direct phone-to-phone migration via cable or Wi-Fi Direct during device upgrades is permitted so users keep their notes.

---

## 4. Cryptographic Code Signing & Integrity

Every production release of Naughty is signed using a cryptographic release key with Android APK Signature Schemes **v2** and **v3**.

* **Signer Owner**: `CN=Android Debug, O=Android, C=US`
* **Algorithm**: RSA 2048-bit
* **SHA-256 Certificate Fingerprint**:  
  `D9:0C:E1:4A:43:37:48:1A:80:CF:5E:33:D5:DF:4F:E8:B3:E8:8D:32:3B:15:2F:63:E4:A0:90:60:5E:63:60:69`
* **SHA-1 Certificate Fingerprint**:  
  `FC:E7:F7:CC:36:7D:B9:B5:62:6B:5D:B1:9D:E6:F8:E4:D0:14:AA:F2`

You can verify the signature of any distributed APK using the official Android SDK tool:
```bash
apksigner verify --verbose --print-certs Naughty-v*-release.apk
```

---

## 5. Contact & Audits

Naughty is Free and Open Source Software. You are invited and encouraged to inspect, build, and audit the source code directly:
* **Source Repository**: [https://github.com/pritesh-ranjan/Naughty](https://github.com/pritesh-ranjan/Naughty)
* **License**: MIT License
