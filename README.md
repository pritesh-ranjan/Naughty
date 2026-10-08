<div align="center">

# 📓 NAUGHTY

### *The Context-Aware, Local-First Markdown Workspace & Timeline Engine for Android.*

[![CI Build](https://img.shields.io/github/actions/workflow/status/pritesh-ranjan/Naughty/build.yml?branch=main&style=flat-square&logo=githubactions&logoColor=white&label=CI%20Build)](https://github.com/pritesh-ranjan/Naughty/actions)
[![Latest Release](https://img.shields.io/github/v/release/pritesh-ranjan/Naughty?style=flat-square&logo=github&color=3B82F6&label=Release)](https://github.com/pritesh-ranjan/Naughty/releases/latest)
[![Total Downloads](https://img.shields.io/github/downloads/pritesh-ranjan/Naughty/total?style=flat-square&logo=android&color=10B981&label=Downloads)](https://github.com/pritesh-ranjan/Naughty/releases)
[![100% Offline](https://img.shields.io/badge/Privacy-100%25%20Offline%20(No%20Internet)-059669?style=flat-square)](PRIVACY.md)
[![Hardware Biometrics](https://img.shields.io/badge/Security-Hardware%20Biometrics-D97706?style=flat-square)](SECURITY.md)
[![License: MIT](https://img.shields.io/badge/License-MIT-F59E0B.svg?style=flat-square)](LICENSE)

<br/>

> **Most note apps trap your thoughts inside a silo.**  
> **Naughty surfaces them dynamically the exact moment and place you need them.**

<br/>

[Why Naughty?](#-why-naughty) •
[Features](#-features) •
[Themes](#-handcrafted-themes) •
[Privacy & Security](#-privacy--security) •
[Download](#-download--install) •
[License](#-license)

---

</div>

<br/>

## 💡 Why Naughty?

Have you ever made a shopping list, only to forget checking it when you arrived at the store and opened your grocery app? Or jotted down meeting notes, only to scramble looking for them when opening Zoom or Slack?

**Naughty connects your notes to your actions.**

Instead of passively storing notes in a silo, Naughty uses lightweight on-device detection to surface relevant notes and checklists as heads-up cards the instant you open designated apps. 

Every note lives as a standard, physical `.md` (Markdown) file on your phone. No cloud accounts, no subscriptions, no tracking—just pure utility and total privacy.

---

## ⚡ Features

### 🧠 Context-Aware App Binding
* **Automatic Surfacing**: Bind any note to one or more apps (e.g. show your grocery checklist inside your delivery app, or meeting agenda inside Zoom).
* **Flexible Triggers**: Set bindings to appear **every time**, **just once** (auto-dismissing after first use), or until a **custom expiry date**.
* **Non-Intrusive**: Surfaces as a clean heads-up card that automatically dismisses when you leave the app.

### 🗺️ Timeline Tasks & Workflows
* **Multi-Step Tracking**: Track high-stakes, multi-week efforts (visa applications, home purchases, product launches) through structured milestones.
* **Smart Progress**: Automatic progress calculation (`done / (total - cancelled)`) that accurately excludes cancelled steps.
* **Audit History**: Built-in activity log tracks state changes and progress notes over time.
* **Milestone Deadlines**: Schedule exact alarms tied to milestone due dates.

### 🎙️ AI Voice Notes & Waveform Player
* **Neural Noise Suppression**: On-device AI filters background noise, wind, and hum for studio-clear recordings.
* **Live AI Denoise Toggle**: Switch noise reduction on/off mid-recording with a single tap.
* **WhatsApp-Style Playback**: Smooth, interactive waveform scrubber with tap/drag seeking and 1x / 1.5x / 2x speed controls.
* **Offline Dictation**: Transcribe voice notes to text 100% on-device and insert them into your notes in one click.

### 📝 Hybrid Markdown Editor
* **Dual Editing Modes**: Seamlessly switch between tactile block editing (interactive checkboxes, headers, text) and raw Markdown with live preview.
* **Smart Checklists**: Pressing Enter on an empty checklist item automatically converts back to standard text.
* **Time Travel**: Instant snapshot-based undo and redo up to 30 states.
* **Inline Reminders**: Embed date and time reminders directly into task markdown (`[🔔 Label](reminder:...)`).

### 📥 Universal Quick Capture
* **System Share Ingestion**: Share text, links, or images to Naughty directly from any app.
* **"Add to Naughty" Context Menu**: Highlight text anywhere on your device and save it in one tap.
* **Interactive Image Capture**: Choose to attach shared images, extract text using on-device OCR, or do both.
* **Instant Clipboard OCR**: Dedicated share shortcut to extract text from images straight to your clipboard.
* **Home Screen Shortcuts**: Long-press the app icon for quick actions to create a note or start a timeline task.

### 🔒 Hardware-Backed Biometric Vault
* **Biometric Lock**: Secure sensitive notes with fingerprint, face unlock, or device PIN.
* **Zero Preview Leakage**: Locked notes are completely masked across cards, search results, and notifications until authenticated.
* **Session Security**: Notes remain unlocked during your active session and auto-lock upon exiting.

### ⏰ Reliable Alarms & Calendar Sync
* **Exact Alarms**: Accurate alerts scheduled with Android's exact alarm system that fire on time even during deep battery sleep.
* **Reboot Resilient**: Alarms automatically reschedule when your phone restarts.
* **Calendar Integration**: Export note reminders directly to Google Calendar or your default calendar app.

### 🔍 Fast Search & Workspace Organization
* **Sub-Millisecond Search**: Instantly query titles, note content, and bound app names.
* **Quick-Jot Dock**: Capture fleeting thoughts in seconds from the persistent bottom dock.
* **Swipe Actions**: Swipe left to Archive, swipe right to send to Trash.
* **Trash Recovery**: Restore mistakenly deleted notes or wipe them permanently from the recovery drawer.

### 📂 Zero Data Lock-In
* **Standard Markdown**: Every note body is saved as a real `.md` file in local storage.
* **Universal Portability**: View, edit, or copy your files in Obsidian, Logseq, VS Code, or any text editor anytime.

---

## 🎨 Handcrafted Themes

Choose from 14 curated color palettes with canvas-rendered notebook paper textures (Blueprint Grid, Dot Grid, Ruled Lines, and Clean Canvas):

| Theme | Style | Palette Vibe |
| :--- | :--- | :--- |
| **Mononoke** | Dark | Emerald Monospace & Charcoal |
| **Gundam** | Light | Blueprint Grid & Royal Cobalt |
| **Tokyo Drift** | Dark | Cyberpunk Neon Cyan & Magenta |
| **Vendetta** | Dark | Typewriter Noir & Crimson |
| **Piccolo** | Light | Mint Paper & Royal Violet |
| **A24** | Dark | Indie Cinema Slate & Amber Warmth |
| **Brave** | Light | Editorial Serif & Burnt Terracotta |
| **Agrabah** | Dark | Arabian Night Violet & Gold |
| **Dracula** | Dark | Classic Dracula Slate, Pink & Mint |
| **Nord** | Dark | Arctic Frost Steel & Glacier Cyan |
| **Gruvbox** | Dark | Retro Warm Groove & Ochre |
| **Solarized** | Light | Terminal Parchment & Cyan |
| **Dot Matrix** | Dark | 80s CRT Phosphor Green |
| **Flexoki** | Light | Minimal Inky Paper & Forest Pine |

*Supports Light, Dark, and System Auto mode.*

---

## 🛡️ Privacy & Security

Naughty is built on an uncompromising privacy foundation:

* **100% Offline**: Zero internet permissions declared in the app manifest. Naughty physically cannot connect to any remote server or cloud.
* **Zero Surveillance**: No analytics, telemetry, user tracking, or advertising SDKs.
* **On-Device Intelligence**: Voice denoising, speech-to-text, and image OCR run exclusively on your phone's processor.
* **No Cloud Leakage**: Automated cloud backups are disabled to prevent unencrypted cloud syncing of private notes.
* **Transparent Storage**: Your notes are stored as plain Markdown (`.md`) files on your device that you own completely.

*For full details, see our [Privacy Policy](PRIVACY.md) and [Security Policy](SECURITY.md).*

---

## 📦 Download & Install

### Latest Release
Download the latest signed release APK from [**GitHub Releases**](https://github.com/pritesh-ranjan/Naughty/releases/latest):
1. Download **`Naughty-v2.1.10-release.apk`**.
2. Tap the downloaded file to install on your Android device (enable "Install Unknown Apps" if prompted).
3. Open Naughty and enjoy your distraction-free workspace!

### Install via ADB
```bash
adb install -r Naughty-v2.1.10-release.apk
```

---

## 🤝 Contributing

Contributions are welcomed! Whether you want to add new color themes, refine Markdown rendering, or improve features:
1. Fork the repository.
2. Create your feature branch (`git checkout -b feature/AmazingFeature`).
3. Commit your changes (`git commit -m 'feat: Add AmazingFeature'`).
4. Push to the branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE) for details.

<br/>

<div align="center">

Crafted with care for thinkers, builders, and privacy purists.  
**Naughty** — *Notes that know when to show up.*

</div>
