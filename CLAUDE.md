# CLAUDE.md — Agent & Developer Operational Manual

Welcome to **Naughty** (`com.example.naughty`), an offline-first, context-aware note-taking and task-management Android application crafted with 100% Kotlin and Jetpack Compose.

This document serves as the single source of truth for AI agents and human engineers working on this repository. It covers environment constraints, architecture, database schemas, background services, editor internals, theming, gotchas, and verification workflows.

---

## 1. Environment & Build System

### 1.1 SDK and Runtime Setup
* **`JAVA_HOME`**: `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (JetBrains Runtime / OpenJDK 25 toolchain).
* **`ANDROID_HOME`**: `~/Library/Android/sdk`.
* **Toolchain Target**: Kotlin JVM toolchain target is set to Java 17 (`jvmToolchain(17)`).
* **Target & Compile SDK**: `36` (Android 16 / Vanilla Ice Cream / Upside Down Cake + back-compat down to Android 7.0 / API 24).
* **Kotlin**: `2.3.20` with Compose Compiler plugin and KSP `2.3.12`.
* **Android Gradle Plugin (AGP)**: `9.0.1`.

> [!IMPORTANT]
> **Always export `JAVA_HOME` before invoking `./gradlew` commands** in your shell sessions:
> ```bash
> export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
> ```

### 1.2 Common Build & Test Commands
* **Run Unit Tests**:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ./gradlew testDebugUnitTest
  ```
* **Assemble Debug APK**:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ./gradlew assembleDebug
  ```
* **Production / Release Build Script**:
  ```bash
  ./build_release.sh
  ```
  * `./build_release.sh --bump`: Increments `VERSION_CODE` and patch version in `version.properties` before running tests and compiling.
  * `./build_release.sh --install`: Compiles and automatically installs the release APK onto the connected adb device/emulator.
* **Tracked Version File**:
  `version.properties` tracks `VERSION_CODE` (e.g. `3`) and `VERSION_NAME` (e.g. `2.1`).

---

## 2. System Architecture & Component Overview

Naughty adheres strictly to modern Android architecture principles: **Unidirectional Data Flow (UDF)**, **Repository Pattern**, **Offline-First Local Storage**, and **Manual Dependency Injection**.

```
                           +----------------------------------------+
                           |          UI Layer (Compose)            |
                           |  - NoteListScreen (Cards, Swiping)     |
                           |  - NoteEditorScreen (Blocks, Markdown) |
                           |  - BindingSheet (App selector, expiry) |
                           |  - UniversalSearchOverlay (Fast lookup)|
                           +-------------------+--------------------+
                                               |
                                     StateFlow | User Events
                                               v
                           +----------------------------------------+
                           |              ViewModels                |
                           |  - NoteListViewModel                   |
                           |  - NoteEditorViewModel                 |
                           |  - BindingViewModel                    |
                           +-------------------+--------------------+
                                               |
                                               v
                           +----------------------------------------+
                           |              Repositories              |
                           |  - NoteRepository                      |
                           |  - BindingRepository                   |
                           +--------+----------------------+--------+
                                    |                      |
            Room (SQLite)           v                      v   File System
     +------------------------------+----+   +-------------+---------------+
     | NoteMetadataDao  AppBindingDao    |   | MarkdownFileManager         |
     | (Titles, tags, pins, reminders,   |   | (Physical .md files stored  |
     |  biometrics, binding rules)       |   |  in context.filesDir/notes) |
     +-----------------------------------+   +-----------------------------+
```

### 2.1 Dependency Injection (`AppContainer.kt`)
* **No Dagger / Hilt**: The application deliberately uses a lightweight, manual dependency injection container via `AppContainer`. This keeps compilation times ultra-fast (<1 second incremental).
* Access the container anywhere via `(context.applicationContext as NaughtyApp).container`.
* Container singletons include:
  * `AppDatabase` (Room)
  * `MarkdownFileManager` (Physical disk storage)
  * `NoteRepository`
  * `BindingRepository`
  * `BindingDetector`
  * `ContextNotificationManager`
  * `ReminderManager`
  * ViewModel factory methods (`noteListViewModel()`, `noteEditorViewModel()`, `bindingViewModel()`).

---

## 3. Storage Layer & The Dual-Storage Pattern

Naughty uses a **hybrid dual-storage strategy** designed for zero data lock-in and high-speed queries:

1. **Room Database (`AppDatabase`, `naughty_db`)**:
   * Stores fast-queryable metadata: `id`, `title`, `createdAt`, `modifiedAt`, `isPinned`, `isArchived`, `isDeleted`, `reminderTime`, `isLocked`, `colorTheme`, `cardType`, `fileName`, and `tag`.
   * Houses app binding rules (`app_binding` table).
2. **Physical Markdown Files (`MarkdownFileManager`)**:
   * The ground truth for note bodies lives as `.md` files under `context.filesDir/notes/`.
   * Files can be shared or exported as pure markdown (`.md`) without database dependency.

### 3.1 Room Migrations Protocol
When altering `NoteMetadata` or `AppBinding` entities:
1. Increment the database version in `AppDatabase.kt`.
2. Write an explicit `Migration(from, to)` in `AppContainer.kt`.
3. Register the migration in `Room.databaseBuilder(...).addMigrations(...)`.
4. Run `./gradlew testDebugUnitTest` to ensure existing DAOs and queries function correctly.

Existing Migrations Record:
* **2 -> 3**: Added `expiresAt INTEGER DEFAULT NULL` to `app_binding`.
* **3 -> 4**: Added `isDeleted INTEGER NOT NULL DEFAULT 0` to `note_metadata` (Trash / soft-delete).
* **4 -> 5**: Added `reminderTime INTEGER DEFAULT NULL` to `note_metadata` (Alarms).
* **5 -> 6**: Added `isLocked INTEGER NOT NULL DEFAULT 0` to `note_metadata` (Biometric security).

---

## 4. Key Functional Subsystems

### 4.1 Context-Aware App Binding Engine
App Binding surfaces notes as notifications when a user launches specific third-party applications (e.g., viewing a grocery list inside Instacart, or meeting notes inside Zoom).

* **Detection Component**: `BindingDetector.kt`.
* **Mechanism**: Uses `UsageStatsManager.queryEvents` polled on `Dispatchers.IO` once every second. Screen state is verified via `PowerManager.isInteractive` to halt polling when device is asleep.
* **Required Permission**: `android.permission.PACKAGE_USAGE_STATS`. Handled gracefully in UI through `PermissionGate.kt`.
* **Binding Types**:
  * `PERSISTENT`: Triggers notification whenever target app comes to foreground.
  * `ONE_SHOT`: Deactivates automatically after first trigger.
  * `expiresAt`: Optional timestamp after which binding becomes inactive.
* **Notification Dispatcher**: `ContextNotificationManager.kt`.
  * Channel: `naughty_context_v2`.
  * Auto-dismisses when switching away from the bound app.
  * Auto-timeout: 30 seconds.
  * Masks content if `note.metadata.isLocked == true`.

### 4.2 Hybrid Block & Raw Markdown Editor
The note editor supports two concurrent views:
1. **Block Mode**: Deconstructs markdown into structured interactive UI blocks (`EditorBlock.Text` and `EditorBlock.Checklist`).
   * Tapping Enter on an empty checklist item exits to a normal text line.
   * Checklists support toggling, reordering, inline reminders, and image attachments.
2. **Raw Markdown Mode**: Direct full-text markdown editing with live Markwon preview toggle.

**Inline Reminders**:
* Syntax: `[🔔 Custom Label](reminder:<timestampMillis>)`.
* Parsed and extracted by `InlineReminderParser.kt`.
* Cleaned on export or plain-text preview.

**OCR Text Extraction**:
* `ImageTextExtractor.kt` executes **on-device** Latin text recognition via Google ML Kit (`com.google.mlkit:text-recognition`).
* Extracts text directly from images attached to notes and appends/inserts them seamlessly into the active editor block.

**Biometric Security**:
* `BiometricAuthHelper.kt` integrates `androidx.biometric:biometric`.
* Supports `BIOMETRIC_STRONG` with automatic fallback to device PIN/Pattern/Password (`DEVICE_CREDENTIAL`).
* In-memory session unlocking: `NoteLockSession` retains unlocked state during active app lifecycle.
* Locked notes hide body text across note cards, search overlays, and notifications until authenticated.

### 4.3 Scheduled Reminders & Alarms
* `ReminderManager.kt` schedules exact alarms using `AlarmManager.setExactAndAllowWhileIdle`.
* Catches Android 12+ `SCHEDULE_EXACT_ALARM` requirements with fallback.
* Dispatches through `ReminderReceiver.kt` (registered for `BOOT_COMPLETED` for reboot persistence).
* Can optionally export reminders into the native Google Calendar / Android Calendar Provider via `ReminderManager.createCalendarIntent()`.

### 4.4 Handcrafted Theming System
* Defined in `NoteThemes.kt` (`NoteThemeRegistry`).
* 14 distinct typographic themes:
  * Dark: **Mononoke**, **Tokyo Drift**, **Vendetta**, **A24**, **Agrabah**, **Dracula**, **Nord**, **Gruvbox**, **Dot Matrix** (CRT phosphor).
  * Light: **Gundam**, **Piccolo**, **Brave**, **Solarized**, **Flexoki**.
* Paper patterns: `GRID`, `DOTS`, `LINES`, and `NONE`, rendered on Compose Canvas via `PaperBackground.kt`.
* App theme mode: `LIGHT`, `DARK`, or `AUTO` (system), persisted via Jetpack DataStore Preferences (`THEME_MODE_KEY`).

### 4.5 Timeline Tasks Subsystem
Independent workflow and progress engine for multi-step, high-stakes efforts (e.g. loan approvals, passport renewals, product launches):
* **Architecture**: Trackers contain ordered Milestones, customizable Stages, and an immutable-style Activity/Update log.
* **Status Engine**: 5 built-in categories (`NOT_STARTED`, `IN_PROGRESS`, `BLOCKED`, `DONE`, `CANCELLED`) plus tracker-specific custom stages. Deletion of a custom stage safely falls back assigned milestones to the built-in stage of the same category with activity logging.
* **Progress Math**: Excludes cancelled milestones (`done / (total - cancelled)`). Tracker-level status is deterministically derived from milestone states.
* **Alarms & Reminders**: Leverages `ReminderManager` with milestone due dates and `EXTRA_TRACKER_ID` routing.
* **Search Integration**: Trackers and milestones are queryable via `UniversalSearchOverlay`.

### 4.6 System Share & Content Ingestion Subsystem
Direct creation of notes from any third-party app via the Android system share sheet and text selection menu:
* **Intent Actions Handled**:
  * `Intent.ACTION_SEND` (`text/plain`, `text/*`): Text snippets, paragraphs, web links, URLs.
  * `Intent.ACTION_SEND` (`image/*`): Single image sharing from Gallery, Photos, Browser, or Files.
  * `Intent.ACTION_SEND_MULTIPLE` (`image/*`): Batch image sharing.
  * `Intent.ACTION_PROCESS_TEXT` (`text/plain`): Directly accessible from the OS floating text selection context menu ("Add to Naughty").
* **Offline-First Image Ingestion**:
  * Incoming streams are immediately copied to `context.filesDir/note_images/` (`img_<timestamp>_<uuid>.<ext>`) to prevent temporary content URI expiration.
  * Formatted as native markdown attachments (`![Image](file://...)`), instantly rendering across `AttachedImageThumbnail`, full-screen viewer, and note cards.
* **Interactive Image Share Options (`ShareImageOptionSheet.kt`)**:
  * Surfaces a dedicated modal bottom sheet with thumbnail previews and 3 actions: Attach Image, Extract Text (OCR), and Both.
* **Instant Routing**:
  * `MainActivity` (`launchMode="singleTask"`) ingests content asynchronously on `Dispatchers.IO`, posts visual Toast confirmation, and pushes `NoteEditorKey(newNoteId)` onto Navigation 3's back stack with automatic `PermissionGate` bypass.

### 4.7 Voice Notes, Audx Neural Noise Suppression & Direct Opus Encoding Subsystem
High-fidelity on-device voice recording with neural noise reduction and WhatsApp-style interactive playback:
* **Audio Capture Engine**:
  * Uses Android `AudioRecord` capturing raw 16-bit PCM at 48,000 Hz. Mono preferred; stereo automatically downmixed `(L + R) / 2` to mono.
  * Audio source dynamically selects `AudioSource.UNPROCESSED` if supported by device hardware, falling back to `AudioSource.VOICE_RECOGNITION`.
  * Hardware platform `NoiseSuppressor` is explicitly disabled (`NoiseSuppressor.setEnabled(false)`) to prevent double-processing artifacts.
* **Audx Neural Noise Suppression**:
  * Integrates `audx-android` (RNNoise neural network model) operating on continuous 10-ms frames (480 samples/channel) with reusable buffers on a dedicated `THREAD_PRIORITY_URGENT_AUDIO` worker thread.
  * **Real-Time Toggle**: Mid-recording toggle pill (`[✨ AI Denoise: ON/OFF]`) switches seamlessly between neural denoising and raw microphone audio on a per-frame basis without restarting the stream.
* **Direct Opus Streaming & Zero WAV Overhead**:
  * Raw cleaned PCM chunks are continuously encoded by Android's native `MediaCodec` Opus encoder (`audio/opus`, 48 kHz, 32 kbps).
  * Direct multiplexing into an Ogg container via `MediaMuxer` (`MUXER_OUTPUT_OGG` on API 29+; RFC 7845 packet writer fallback on older APIs), saving directly as `.opus` in `context.filesDir/note_audio/` with zero intermediate WAV files.
* **Waveform Visualization & Playback**:
  * Normalized RMS amplitude vectors saved alongside as `.wf` metadata for instant waveform rendering without audio pre-decoding.
  * WhatsApp-style waveform player (`VoiceNotePlayer.kt`) supporting interactive tap/drag scrubbing, 1x/1.5x/2x playback speed cycling, deletion, and note attachment.
* **Offline Voice-to-Text Dictation**:
  * Transcribes speech 100% on-device using Android `SpeechRecognizer` (`SpeechTranscriptionHelper.kt`), expandable under the player card with one-tap copy and markdown note insertion.
* **App Launcher Shortcuts**:
  * `res/xml/shortcuts.xml` defines launcher quick actions for Instant Note Creation (`action_new_note`) and Timeline Tasks (`action_timeline`).

---

## 5. Directory Structure Reference

```
app/src/main/java/com/example/naughty/
├── AppContainer.kt                # Dependency Injection root & Room migrations
├── MainActivity.kt                # Single-activity Compose entrypoint
├── NaughtyApp.kt                  # Application class; launches background observers
├── ShareCopyTextActivity.kt       # Floating context menu "Add to Naughty"
├── binding/                       # App binding detection engine
│   ├── BindingDetector.kt         # UsageStats polling loop
│   ├── BindingDetectorService.kt  # Legacy background service compatibility
│   └── BindingEvent.kt            # Foreground change event definitions
├── data/
│   ├── file/
│   │   └── MarkdownFileManager.kt # Disk markdown read/write operations
│   ├── local/                     # Room Entities & DAOs
│   │   ├── AppBinding.kt          # Binding entity (one-shot, persistent, expires)
│   │   ├── AppBindingDao.kt
│   │   ├── AppDatabase.kt         # Room DB definition
│   │   ├── Converters.kt
│   │   ├── NoteMetadata.kt        # Note entity (pins, archive, trash, lock)
│   │   ├── NoteMetadataDao.kt
│   │   └── timeline/              # Room entities & DAO for Timeline
│   │       ├── TimelineDao.kt
│   │       └── TimelineEntities.kt
│   └── repository/
│       ├── BindingRepository.kt
│       ├── NoteRepository.kt      # Coordinates DB metadata & .md files
│       └── TimelineRepository.kt  # Repository for timeline trackers & stages
├── notification/
│   ├── ContextNotificationManager.kt # App-context notification manager
│   ├── ReminderManager.kt            # Scheduled exact alarms & calendar intents
│   └── ReminderReceiver.kt           # Broadcast receiver for alarms & reboot
├── ui/
│   ├── binding/                   # App binding sheet UI & ViewModel
│   ├── components/                # Reusable Compose components
│   │   ├── FullscreenImageViewer.kt
│   │   ├── PaperBackground.kt     # Grid, dot, and ruled canvas paper renderer
│   │   ├── PermissionGate.kt      # Usage stats permission request UI
│   │   ├── SettingsDialog.kt      # Drawer with trash, archive, theme picker, specs & permissions
│   │   ├── ShareImageOptionSheet.kt # Image share modal sheet (Image, OCR, Both)
│   │   ├── SwipeableNoteItem.kt   # AnchoredDraggable swipe actions
│   │   ├── ThemeToggle.kt
│   │   ├── UniversalSearchOverlay.kt # Quick search with biometric gating
│   │   ├── VoiceNotePlayer.kt     # WhatsApp-style scrubber, speed toggle & transcript
│   │   └── VoiceRecordingSheet.kt # Live AudioRecord + Audx toggle recording bottom sheet
│   ├── editor/                    # Note editing suite
│   │   ├── EditorBlock.kt         # Block parser & markdown serializer
│   │   ├── InlineReminderParser.kt# [🔔 ...](reminder:...) regex parser
│   │   ├── MarkdownPreview.kt     # Markwon preview integration
│   │   ├── NoteEditorScreen.kt    # Main editor Compose layout
│   │   ├── NoteEditorViewModel.kt # State management, undo/redo, auto-save
│   │   ├── ReminderBottomSheet.kt
│   │   └── ThemeSelectionSheet.kt # Palette & paper theme picker
│   ├── navigation/
│   │   └── NavGraph.kt            # Navigation 3 route definitions
│   ├── notelist/                  # Main notes grid / list
│   │   ├── NoteCard.kt            # Themed note preview card with thumbnail & voice mic icon
│   │   ├── NoteListScreen.kt      # Floating dock, filter tabs, quick jot
│   │   └── NoteListViewModel.kt
│   ├── theme/                     # Color palettes, typography, theme registry
│   └── timeline/                  # Timeline task management suite
│       ├── TimelineColors.kt
│       ├── TimelineDetailScreen.kt
│       ├── TimelineDetailViewModel.kt
│       ├── TimelineListScreen.kt
│       ├── TimelineListViewModel.kt
│       ├── TimelineProgress.kt
│       └── components/            # Stepper & interactive sheets
│           ├── TimelineSheets.kt
│           └── TimelineStepper.kt
└── util/
    ├── AudioRecorderHelper.kt     # AudioRecord + Audx real-time denoise coordinator
    ├── BiometricAuthHelper.kt     # BiometricPrompt & NoteLockSession
    ├── ImageTextExtractor.kt      # Google ML Kit offline OCR
    ├── OpusMediaCodecEncoder.kt   # Native MediaCodec Opus streaming encoder & Ogg muxer
    ├── ShareIntentHandler.kt      # System share & selection text ingestion
    └── SpeechTranscriptionHelper.kt # Offline Android SpeechRecognizer transcription
```

---

## 6. Critical Invariants & Rules for Agents

1. **Markdown Fidelity**:
   Never strip, corrupt, or alter standard markdown syntax or Naughty's inline tags (`[🔔 ...](reminder:...)` and `![Image](uri)`). Physical `.md` files must remain valid outside the app.
2. **Biometric Privacy Safeguard**:
   Never render or preview body text for notes where `isLocked == true` until `NoteLockSession.isUnlocked(noteId)` returns true. Display `"Locked note • Tap to unlock"` instead.
3. **Database Migrations are Mandatory**:
   Never rely solely on `fallbackToDestructiveMigration()` in production. Always write explicit Room migrations in `AppContainer.kt` for any schema changes to prevent data loss.
4. **Offline & Privacy Integrity**:
   Naughty has **zero external network requests**, zero analytics, and zero telemetry. All text recognition (ML Kit) and search queries operate 100% on-device. `android.permission.INTERNET` is explicitly stripped via `tools:node="remove"`. Do not introduce network calls.
5. **Clean Unit Tests**:
   Keep unit tests fast (<1 sec) by testing domain logic in ViewModels, parsers, and utilities using standard JUnit4 and coroutine test dispatchers (`runTest`). Always verify tests pass before concluding changes.
6. **MIT License & FOSS Compliance**:
   Naughty is open-source under the **MIT License**. Never include proprietary SDKs or tracking modules.
7. **Production Signing & Package Visibility**:
   Release APKs/AABs are signed using APK Signature Schemes v2 and v3 (`release.keystore`, SHA-256 `d90ce14a4337481a80cf5e33d5df4fe8b3e88d323b152f63e4a090605e636069`, alias `androiddebugkey`). This signing key is pinned to match the user's installed package signature, ensuring seamless in-place updates (`adb install -r`) with zero data/database loss. Never switch or regenerate this keystore. App visibility is handled strictly via `<queries>` intent filters for launcher activities—never reintroduce `QUERY_ALL_PACKAGES`. Cloud backup is explicitly disabled in `data_extraction_rules.xml`.
