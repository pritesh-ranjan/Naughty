# AGENTS.md — Agent & Developer Operational Manual

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
* **Primary Emulator AVD**: `Pixel_10a` located at `~/Library/Android/sdk/emulator/emulator -avd Pixel_10a`.

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

### 1.3 Android Emulator & ADB Operations
* **Configured AVD**: `Pixel_10a`.
* Launch emulator command:
  ```bash
  export ANDROID_HOME="$HOME/Library/Android/sdk"
  "$ANDROID_HOME/emulator/emulator" -avd Pixel_10a
  ```
* Wait for boot completion:
  ```bash
  export ANDROID_HOME="$HOME/Library/Android/sdk"
  "$ANDROID_HOME/platform-tools/adb" wait-for-device shell 'while [[ -z $(getprop sys.boot_completed) ]]; do sleep 1; done;'
  ```
* **Autonomous ADB & Emulator Operations (No Permission Required)**:
  * Taking screenshots inside `adb` (`adb shell screencap -p /sdcard/screen.png` and pulling) does **not** require user permission.
  * Interacting with Naughty or the Android OS via `adb` in the emulator (`adb shell input tap/swipe/text/keyevent`, `adb shell am start`, `adb shell uiautomator dump`, `adb install -r`, etc.) does **not** require user permission.
  * Execute all emulator and adb verification/interaction commands autonomously without prompting or asking the user for permission.

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
* **6 -> 7**: Added `timeline_tracker`, `timeline_stage`, `timeline_milestone`, and `timeline_update` tables for multi-task timeline tracking.
* **7 -> 8**: Added `graph_node`, `graph_edge`, `graph_evidence`, and `recent_notification_log` tables for self-building offline Knowledge Graph.
* **8 -> 9**: Merged archived notes into Deleted (`isDeleted = 1, isArchived = 0`), purged orphaned app bindings for soft-deleted notes, removed Archive feature across UI, and transitioned to full-screen Settings with home feature tiles.
* **9 -> 10**: Dropped `graph_node`, `graph_edge`, `graph_evidence`, and `recent_notification_log` tables, removing offline Knowledge Graph feature and dependencies.

---

## 4. Key Functional Subsystems

### 4.1 Context-Aware App Binding Engine
App Binding surfaces notes as notifications when a user launches specific third-party applications (e.g., viewing a grocery list inside Instacart, or meeting notes inside Zoom).

* **Detection Components**: `BindingDetector.kt`, `CircleToSearchDetector.kt`, and `DetectorForegroundService.kt`.
* **Mechanism & Process Management**:
  * Runs a 1-second polling loop over `UsageStatsManager.queryEvents` with a 2-second sliding lookback window to prevent asynchronous IPC dropped events.
  * Backed by `DetectorForegroundService` (`foregroundServiceType="specialUse"` with `IMPORTANCE_MIN` ongoing silent notification), preventing the Linux cgroup v2 cached-app freezer from halting detection 10s after backgrounding.
  * Integrated with `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` in `PermissionGate` and Settings to circumvent aggressive OEM task killers (Samsung One UI Sleeping Apps, Xiaomi HyperOS/MIUI).
  * Automatically pauses polling when the screen turns off (`PowerManager.isInteractive == false`) and resets `currentForegroundPackage = null` so waking or unlocking back into a bound app reliably re-triggers.
* **Circle to Search Detection**:
  * Tracks Google App surfaces (`omnient`, `lens`, `robin`, `search.assistant`).
  * Features a 20-second watchdog auto-expiry timer (`CTS_MAX_DURATION_MS`) to automatically recover if an exit event is dropped or dismissed without an activity stop.
* **Required Permissions**: `android.permission.PACKAGE_USAGE_STATS`, `android.permission.FOREGROUND_SERVICE`, `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`, `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Handled gracefully in UI through `PermissionGate.kt` and `SettingsComponents.kt`.
* **Binding Types**:
  * `PERSISTENT`: Triggers notification whenever target app comes to foreground.
  * `ONE_SHOT`: Deactivates automatically after first trigger.
  * `expiresAt`: Optional timestamp after which binding becomes inactive.
* **Notification Dispatcher & Channels**: `ContextNotificationManager.kt`.
  * Channels:
    * `naughty_context_v2`: "Context Notes" (`IMPORTANCE_HIGH`, `CATEGORY_REMINDER`), auto-dismisses when switching apps, 30s auto-timeout.
    * `naughty_cts_v1`: "Circle to Search" (`IMPORTANCE_HIGH`, `CATEGORY_RECOMMENDATION`), 15s auto-timeout, prompts snippet capture after exiting CTS.
    * `naughty_reminders_v1`: "Note Reminders" (`IMPORTANCE_HIGH`, `CATEGORY_REMINDER`).
    * `naughty_detector_service_v1`: "Context Engine Service" (`IMPORTANCE_MIN`, `CATEGORY_SERVICE`).
  * Capture toggle: User-facing switch in Settings ("Capture notes notification") persisted in DataStore (`CTS_NOTIFICATION_ENABLED_KEY`) allows enabling/disabling CTS exit prompt notifications.
  * Masks content if `note.metadata.isLocked == true`.

### 4.2 Hybrid Block & Raw Markdown Editor
The note editor supports two concurrent views:
1. **Block Mode**: Deconstructs markdown into structured interactive UI blocks (`EditorBlock.Text` and `EditorBlock.Checklist`).
   * Tapping Enter on an empty checklist item exits to a normal text line.
   * Tapping the cross icon (`X`) on a todo/checklist item shows an immediate Toast ("Task removed") and smoothly fades out and collapses (`AnimatedVisibility` shrink & fade spec) before removing from persistence.
   * Checklists support toggling, reordering, inline reminders, and image attachments.
2. **Raw Markdown Mode**: Direct full-text markdown editing for rapid note editing and full markdown control.

**Inline Reminders**:
* Syntax: `[🔔 Custom Label](reminder:<timestampMillis>)`.
* Parsed and extracted by `InlineReminderParser.kt`.
* Cleaned on export or plain-text preview.

**Image Attachments & Viewer**:
* Displayed with `AttachedImageThumbnail`. Tapping an image opens `FullscreenImageViewer` with zoom/pan, dismiss cross, and direct system share action via `FileProvider`.

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
  * `Intent.ACTION_SEND` (`audio/*`) & `Intent.ACTION_SEND_MULTIPLE` (`audio/*`): Audio voice notes and recordings.
  * `Intent.ACTION_PROCESS_TEXT` (`text/plain`): Directly accessible from the OS floating text selection context menu ("Add to Naughty").
* **Offline-First Content Ingestion**:
  * Incoming image streams are immediately copied to `context.filesDir/note_images/` (`img_<timestamp>_<uuid>.<ext>`).
  * Incoming audio streams are copied to `context.filesDir/note_audio/` (`audio_<timestamp>_<uuid>.<ext>`).
* **Intelligent Title & Card Formatting**:
  * `ShareIntentHandler.kt` cleans leading markdown headers, bullets, and checkboxes from raw text for clean card titles.
  * URLs without explicit subjects are smartly parsed to domain and page slugs (e.g. `github.com • linux`).
  * Checklists (`- [ ]`) are automatically flagged as `cardType = "checklist"`.
* **Interactive Image Share Options (`ShareImageOptionSheet.kt`)**:
  * Sharing image(s) surfaces a dedicated modal bottom sheet with thumbnail previews and 3 actions:
    1. **Attach Image**: Creates note with image markdown attachments.
    2. **Extract Text (OCR)**: Scans image on-device with ML Kit (`ImageTextExtractor`) to transcribe recognized text directly into a text note.
    3. **Both (Image & OCR Text)**: Transcribes recognized text and appends the attached image markdown below it.
* **Interactive Audio Share Options (`ShareAudioOptionSheet.kt`)**:
  * Sharing audio surfaces a dedicated modal bottom sheet with 3 actions:
    1. **Attach Voice Note**: Attaches shared audio directly with playable waveform player.
    2. **Transcribe Audio**: Transcribes speech offline on-device to text note without storing audio.
    3. **Both (Audio & Transcript)**: Attaches playable voice note and appends transcription text.
* **Instant Routing**:
  * `MainActivity` (`launchMode="singleTask"`) ingests content asynchronously on `Dispatchers.IO`, posts visual Toast confirmation, and pushes `NoteEditorKey(newNoteId)` onto Navigation 3's back stack with automatic `PermissionGate` bypass.

### 4.7 Voice Notes, Audx Neural Noise Suppression & Direct Opus Encoding Subsystem
High-fidelity on-device voice recording with neural noise reduction and WhatsApp-style interactive playback:
* **Audio Capture Engine & Decoupled Architecture**:
  * Uses Android `AudioRecord` capturing raw 16-bit PCM at 48,000 Hz. Buffer allocated to `>= 1 second` headroom (`maxOf(minBufferSize * 8, SAMPLE_RATE * 2)`) to prevent kernel overflow.
  * Capture thread (`AudxAudioRecordThread`, `THREAD_PRIORITY_URGENT_AUDIO`) runs independently of compression, enqueuing raw/denoised PCM into a thread-safe `LinkedBlockingQueue<ShortArray>`.
  * Dedicated encoding worker thread (`OpusEncoderWorkerThread`, `THREAD_PRIORITY_AUDIO`) consumes frames and feeds `OpusMediaCodecEncoder` with non-blocking polling, completely eliminating audio crackling and dropped frames.
  * Hardware platform `NoiseSuppressor` is explicitly disabled (`NoiseSuppressor.setEnabled(false)`) to prevent double-processing artifacts.
* **Audx Neural Noise Suppression**:
  * Integrates `audx-android` (RNNoise neural network model) operating on continuous 10-ms frames (480 samples/channel) with reusable buffers on the capture thread.
  * **Real-Time Toggle**: Mid-recording toggle pill (`[✨ AI Denoise: ON/OFF]`) switches seamlessly between neural denoising and raw microphone audio on a per-frame basis without restarting the stream.
* **Direct Opus Streaming & Zero WAV Overhead**:
  * Raw cleaned PCM chunks grouped into standard 20-ms Opus frames (960 samples) and continuously encoded by Android's native `MediaCodec` Opus encoder (`audio/opus`, 48 kHz, 32 kbps).
  * Direct multiplexing into an Ogg container via `MediaMuxer` (`MUXER_OUTPUT_OGG` on API 29+; RFC 7845 packet writer fallback on older APIs), saving directly as `.opus` in `context.filesDir/note_audio/` with zero intermediate WAV files.
* **Prominent Waveform Visualization & Playback**:
  * Dynamic root-scaled amplitude vectors (`sqrt(rms / 2500f)`) saved alongside as `.wf` metadata for high-contrast waveform rendering.
  * WhatsApp-style waveform player (`VoiceNotePlayer.kt`) supporting interactive tap/drag scrubbing, 1x/1.5x/2x playback speed cycling, deletion, external sharing via `FileProvider`, and note attachment.
  * Mini waveform badge (`VoiceNoteCardBadge`) with duration and 18-bar soundwave on note cards.
* **Offline Voice-to-Text Dictation & File Transcription**:
  * Simultaneous live transcription during recording automatically saved alongside audio as `.txt`.
  * Audio file transcription using `MediaExtractor` and PCM decoding stream via `ParcelFileDescriptor` pipe on Android 13+.
  * Watchdog timer and error handling prevent infinite loading states.
* **App Launcher Shortcuts**:
  * `res/xml/shortcuts.xml` defines launcher quick actions for New Note (`shortcut_new_note`), Todo List (`shortcut_todo_note`), and Timeline Tasks (`shortcut_timeline_tasks`).

### 4.8 Todo-Based Notes & Miniature Popup Window Subsystem
First-class support for dedicated Todo-based notes (`cardType = "todo"`) and isolated floating miniature popup window:
* **Official Todo Note Resolution**:
  * Automatically detects and adopts any active note titled `"Todo"` (case-insensitive) as the official Todo list note (`NoteRepository.adoptExistingTodoNoteIfPresent()`).
  * If none exists when triggered via shortcut or user request, creates the official `"Todo"` note with `cardType = "todo"` and `isPinned = true`.
  * Cards display a `"TODO"` pill badge with real-time completion counter (e.g. `2/5`) and task filter integration.
* **Miniature Floating Popup Window (`QuickNotePopupActivity.kt`)**:
  * Runs in its own isolated task (`taskAffinity=""`, `excludeFromRecents="true"`, theme `Theme.Naughty.QuickPopup`).
  * Floats directly over the home screen or third-party app without bringing `MainActivity` to the foreground.
  * Provides editable title, full block & todo checklist support, and the bottom notes bar (block menu, raw markdown toggle, checkboxes, bullets, image attachment, on-device OCR, voice note recording, inline reminders, undo/redo).
  * Seamless "Open in App" expand action transitions into `MainActivity`'s full editor, and blank drafts are automatically cleaned up if dismissed untouched.

### 4.9 First-Launch & Post-Update Feature Showcase Subsystem
Interactive onboarding and feature highlight bottom sheet (`FeatureShowcaseSheet.kt`) automatically surfaced on fresh installations and after every application upgrade:
* **Lifecycle & Version Tracking**:
  * Persists `LAST_SEEN_VERSION_CODE_KEY` via `themeDataStore`.
  * Deterministic display logic via `shouldShowFeatureShowcase(lastSeen, currentCode)`: triggers if `lastSeen == null` (first-ever install) or `lastSeen < currentCode` (app upgrade).
  * Suppressed automatically during direct external share intent handling (`ShareIntentHandler`) to avoid interrupting user flows, displaying as soon as the user returns to the primary experience.
  * Tapping dismiss or acknowledgment buttons saves `currentVersionCode` into DataStore.
* **Content Presentation**:
  * Crisp, non-technical, easy-to-understand copy highlighting:
    1. **App-Linked Notes (App Binding)**: Automatic contextual note surfacing on app launch.
    2. **Circle to Search Capture**: Instant note creation prompt after exiting Circle to Search / Lens.
    3. **Timeline Tasks**: Multi-step goals with custom stages, ordered milestones, and visual progress bars.
    4. **Photo & Voice Notes**: Studio-clarity voice memos with neural noise reduction and interactive soundwaves.
    5. **Offline Text Scanner (OCR)**: On-device text extraction in drafts or via the system share sheet.
    6. **Offline Speech-to-Text**: Real-time voice-to-text dictation and file transcription without internet.
    7. **@ Note Mentions**: Quick linking and referencing between notes and contacts.
    8. **100% Offline & Private**: Zero cloud servers, zero telemetry, zero accounts, open Markdown storage.
    9. **Biometric Privacy Lock & Fast Search**: Fingerprint/Face security with in-memory session unlock and universal search.
* **On-Demand Accessibility**:
  * Re-accessible anytime in Settings under "Feature Showcase & Key Highlights" for exploration.

### 4.10 Samsung One UI 9 Handcrafted Design System & UI Revamp
The entire user interface conforms strictly to **Samsung One UI 9** design philosophy, emphasizing one-handed reachability, visual hierarchy, squircle island cards, and OLED-pure contrast:
* **One UI Foundations (`ui/oneui/theme/`)**:
  * `OneUiColors`: AMOLED pure pitch black (`0xFF000000`) in dark mode, elevated dark cards (`0xFF1C1C1E`), elevated sheets/dialogs (`0xFF252528`), inset hairline dividers (`0xFF2C2C30`), vivid Samsung blue accent (`0xFF3E91FF`), and full One UI badge color palette (`blue`, `purple`, `orange`, `green`, `teal`, `red`, `gray`).
  * `OneUiShapes`: 24dp squircle island cards (`card`), 16dp nested inner elements (`inner`), pill buttons and capsule dock (`pill`), 28dp top-rounded bottom sheets (`sheet`), 26dp dialog squircle (`dialog`), and 18dp popup menus (`menu`).
  * `OneUiTypography`: Large reachability title (`32sp`, semi-bold), collapsible bar title (`19sp`), section title (`20sp`), group labels (`13sp`), body text, subhead, and caption badge.
* **Reusable One UI Component Kit (`ui/oneui/components/`)**:
  * `OneUiScaffold`: Collapsible header architecture featuring large reachability titles that transition into compact top bars on scroll.
  * `OneUiCard` & `OneUiCardGroup`: Elevated squircle island groups with inset hairline dividers (respecting leading icon indent) and rounded top/bottom bounds.
  * `OneUiBottomDock`: Floating capsule dock `[ 🔍 Search notes  │  ＋ ]` anchored above navigation bar.
  * `OneUiBottomSheet` & `OneUiDialog` (`OneUiConfirmDialog`): Consistent modal dialogs and bottom sheets with smooth enter/exit animations and One UI button tokens.
  * `OneUiSwitch`: Custom Samsung-spec pill switch with sliding circular thumb.
  * `OneUiProgress` (`OneUiDonut`, `OneUiProgressBar`): Multi-segment analytics donut charts and rounded progress indicators.
  * `OneUiChip`, `OneUiSegmentedControl`, `OneUiTextField`, `OneUiEmptyState`, `OneUiIconBadge`, `OneUiFilledButton`, `OneUiTonalButton`.
* **Preservation of Note Canvas & Handcrafted Themes**:
  * **Strict Canvas Invariant**: Note background (`canvasBackground`), paper patterns (`PaperBackground`), custom font families, and text colors are 100% driven by `NoteTheme`.
  * All surrounding chrome, editor toolbars, attachment chips (`VoiceNotePlayer`, `AttachedImageThumbnail`), sheets, and menus adopt One UI 9.
* **Single-Column Home Screen & Read-Only Cards**:
  * Home notes screen uses a clean single-column layout grouped chronologically via `NoteSections.kt` (`Pinned`, `Today`, `Yesterday`, `This week`, `Month Year`).
  * Cards are strictly read-only on the home feed (tap to open editor; long-press opens One UI action bottom sheet with Pin, Lock, Binding, Reminder, and Delete options).

---

## 5. Directory Structure Reference

```
app/src/main/java/com/example/naughty/
├── AppContainer.kt                # Dependency Injection root & Room migrations
├── MainActivity.kt                # Single-activity Compose entrypoint
├── NaughtyApp.kt                  # Application class; launches background observers
├── QuickNotePopupActivity.kt      # Floating miniature popup for launcher shortcuts
├── ShareCopyTextActivity.kt       # Floating context menu "Add to Naughty"
├── binding/                       # App binding detection engine
│   ├── BindingDetector.kt         # UsageStats polling loop in application scope
│   ├── BindingEvent.kt            # Foreground change event definitions
│   ├── CircleToSearchDetector.kt  # Google App / Lens surface detector
│   └── DetectorForegroundService.kt # Silent foreground service preventing app freeze
├── data/
│   ├── file/
│   │   └── MarkdownFileManager.kt # Disk markdown read/write operations
│   ├── local/                     # Room Entities & DAOs
│   │   ├── AppBinding.kt          # Binding entity (one-shot, persistent, expires)
│   │   ├── AppBindingDao.kt
│   │   ├── AppDatabase.kt         # Room DB definition
│   │   ├── Converters.kt
│   │   ├── NoteMetadata.kt        # Note entity (pins, trash, lock, theme, cardType)
│   │   ├── NoteMetadataDao.kt
│   │   └── timeline/              # Room entities & DAO for Timeline
│   │       ├── TimelineDao.kt
│   │       └── TimelineEntities.kt
│   ├── mention/                   # Note and contact mention search
│   │   ├── MentionItem.kt
│   │   └── MentionRepository.kt
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
│   │   ├── ClipSaveSheet.kt       # Quick CTS clip save bottom sheet
│   │   ├── FeatureShowcaseSheet.kt# Onboarding & update feature highlight sheet
│   │   ├── FullscreenImageViewer.kt
│   │   ├── PaperBackground.kt     # Grid, dot, and ruled canvas paper renderer
│   │   ├── PermissionGate.kt      # Usage stats permission request UI
│   │   ├── ShareAudioOptionSheet.kt # Audio share modal sheet (Attach, Transcribe, Both)
│   │   ├── ShareImageOptionSheet.kt # Image share modal sheet (Image, OCR, Both)
│   │   ├── UniversalSearchOverlay.kt # One UI 9 full-screen search
│   │   ├── VoiceNotePlayer.kt     # WhatsApp-style scrubber, speed toggle & transcript
│   │   └── VoiceRecordingSheet.kt # Live AudioRecord + Audx toggle recording bottom sheet
│   ├── editor/                    # Note editing suite
│   │   ├── BulletTextHelper.kt    # Automatic bullet and checkbox list indentation
│   │   ├── EditorBlock.kt         # Block parser & markdown serializer
│   │   ├── InlineReminderParser.kt# [🔔 ...](reminder:...) regex parser
│   │   ├── MentionSuggestionSheet.kt # In-editor @ mention sheet
│   │   ├── NoteEditorScreen.kt    # Main editor Compose layout
│   │   ├── NoteEditorViewModel.kt # State management, undo/redo, auto-save
│   │   ├── ReminderBottomSheet.kt
│   │   └── ThemeSelectionSheet.kt # Palette & paper theme picker
│   ├── navigation/
│   │   └── NavGraph.kt            # Navigation 3 route definitions
│   ├── notelist/                  # Main notes list
│   │   ├── NoteListScreen.kt      # One UI 9 collapsing header, filter tabs, bottom dock
│   │   ├── NoteListViewModel.kt
│   │   ├── NotePreviewCard.kt     # Read-only One UI squircle card with badge & attachments
│   │   └── NoteSections.kt        # Chronological date grouping engine
│   ├── oneui/                     # Samsung One UI 9 Handcrafted Suite
│   │   ├── components/            # Shared reusable One UI components
│   │   │   ├── OneUiBottomSheet.kt
│   │   │   ├── OneUiBottomDock.kt
│   │   │   ├── OneUiButton.kt
│   │   │   ├── OneUiCard.kt
│   │   │   ├── OneUiCardGroup.kt
│   │   │   ├── OneUiChip.kt
│   │   │   ├── OneUiDialog.kt
│   │   │   ├── OneUiEmptyState.kt
│   │   │   ├── OneUiIconBadge.kt
│   │   │   ├── OneUiProgress.kt
│   │   │   ├── OneUiScaffold.kt
│   │   │   ├── OneUiScrollToTop.kt
│   │   │   ├── OneUiSearchField.kt
│   │   │   ├── OneUiSectionHeader.kt
│   │   │   ├── OneUiSegmentedControl.kt
│   │   │   ├── OneUiSwitch.kt
│   │   │   └── OneUiTextField.kt
│   │   └── theme/                 # Design tokens & theme provider
│   │       ├── OneUiColors.kt
│   │       ├── OneUiShapes.kt
│   │       ├── OneUiTheme.kt
│   │       └── OneUiTypography.kt
│   ├── settings/                  # Full-screen Settings & configuration
│   │   ├── SettingsComponents.kt  # Theme preview cards & about dialog
│   │   ├── SettingsScreen.kt      # General, appearance, and maintenance settings
│   │   └── SettingsViewModel.kt
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
    ├── ImageOptimizer.kt          # Background downsampling & JPEG compression for attachments
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
8. **Autonomous Emulator & ADB Operations**:
   Taking screenshots via `adb` and interacting with the app via `adb` in the emulator (screen capture, tap/swipe/key input, activity starts, UI inspection, and app verification) does **not** require permission from the user. Run these operations autonomously without asking for approval.
