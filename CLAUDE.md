# Naughty - Android Note-Taking App

## Environment Setup

### Java / Android SDK
- **JAVA_HOME**: `/Applications/Android Studio.app/Contents/jbr/Contents/Home` (JetBrains Runtime, OpenJDK 25)
- **ANDROID_HOME**: `~/Library/Android/sdk`
- These are set permanently in `~/.zprofile`.
- When running Gradle commands, always export `JAVA_HOME` first:
  ```bash
  export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
  ```

### Build Commands
- Automated release build (tests + release build + root APK): `./build_release.sh`
- Bump version and build: `./build_release.sh --bump`
- Build and install on connected device: `./build_release.sh --install`
- Build debug APK: `./gradlew assembleDebug`
- Install on device/emulator: `./gradlew installDebug`
- Available emulator: use `emulator -list-avds` to find AVDs, then `emulator -avd <name>` to launch.
- Version is tracked in `version.properties` (`VERSION_CODE` and `VERSION_NAME`).

## Project Structure
- `app/src/main/java/com/example/naughty/` — Main source root
  - `ui/editor/` — Note editor screen (NoteEditorScreen.kt, NoteEditorViewModel)
  - `ui/binding/` — App binding sheet (BindingSheet.kt, BindingViewModel.kt)
  - `ui/notelist/` — Note list and card components (NoteCard.kt with AttachedImageThumbnail)
  - `ui/components/` — Shared components (PaperBackground, SettingsDialog, etc.)
  - `ui/theme/` — Theming system (NoteTheme, NoteThemeRegistry)
  - `notification/` — Notification managers (ContextNotificationManager, ReminderManager)
  - `binding/` — App binding detection (BindingDetector, BindingDetectorService)
  - `data/` — Data layer (repositories, local database)

## Key Patterns
- Uses Jetpack Compose for all UI
- ViewModel + StateFlow for state management
- Notes support: text blocks, checklist blocks, inline reminders, attached images, color themes
- App binding system: binds notes to apps with persistent/one-shot types and expiration options
- Notifications: context-based (triggered by app binding detection) and reminder-based (scheduled alarms)
