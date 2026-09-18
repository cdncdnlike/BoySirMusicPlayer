# BoySirMusicPlayer

A local music player built with JavaFX and MaterialFX. It supports basic playback, playlists, volume control, and system notifications. The packaged build includes a trimmed JRE, so users do not need to install Java separately.

## ✨ Features

- 🎵 Play local music files (MP3, WAV, AAC, and other formats supported by JavaFX Media)
- 📂 Playlist management
- ⏯️ Play / Pause / Previous / Next
- 🔊 Volume control
- 🎨 Material Design UI powered by MaterialFX
- 🔔 Native system notifications when switching tracks
- 🌐 Update checker (reads the latest version from your server)
- 📦 Windows installer support (`.exe` / `.msi`) with bundled JRE

## 🛠️ Tech Stack

| Technology | Version |
|---|---|
| Java | 17 |
| JavaFX | 17.0.14 |
| Gradle | 8.13 (Kotlin DSL) |
| MaterialFX | 11.17.0 |
| Gson | 2.10.1 |
| Packaging | jlink + jpackage + WiX 3.14 |

## 🚀 Getting Started

### Requirements

- JDK 17 or later
- Gradle (the project includes `gradlew`, so no separate installation is needed)
- Windows 10/11
- WiX Toolset 3.14 (only required for building the installer)

### Run the Project

```bash
# Windows
gradlew.bat run

# macOS / Linux
./gradlew run
