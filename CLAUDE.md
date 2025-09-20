# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Kotlin Multiplatform project for an Android TV remote control application that uses WebRTC for video streaming and RPC for control commands. The project targets Android, iOS, Web (WebAssembly), and includes a Ktor server backend.

## Architecture

The project is structured into four main modules:

- **composeApp**: The main client application using Compose Multiplatform, supports Android, iOS, and Web platforms
- **server**: Ktor server that handles WebRTC signaling, ADB device management, and video streaming
- **shared**: Common code shared between all targets, contains RPC service interfaces and data models
- **iosApp**: iOS-specific entry point (referenced in README but not present in current structure)

### Key Components

- **RPC Communication**: Uses kotlinx-rpc for client-server communication via the `AtvControlService` interface
- **WebRTC Integration**: Handles video streaming from Android TV devices to client applications
- **ADB Management**: Server-side ADB device discovery and command execution for Android TV control
- **Multi-platform UI**: Compose Multiplatform UI with platform-specific implementations

## Development Commands

### Build Commands

**Android Application:**
```bash
./gradlew :composeApp:assembleDebug
```

**Server:**
```bash
./gradlew :server:run
# Or use the custom task:
./gradlew :server:runApp
```

**Web Application:**
```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun
```

### Test Commands
```bash
./gradlew test
./gradlew :composeApp:testDebugUnitTest  # Android tests
./gradlew :shared:commonTest              # Shared module tests
```

### Build All Targets
```bash
./gradlew build
```

## Package Structure

Main package: `tokyo.isseikuzumaki.atvremote`

- Client code in `composeApp/src/{platform}Main/kotlin/`
- Server code in `server/src/main/kotlin/`
- Shared code in `shared/src/{platform}Main/kotlin/`
- Platform-specific implementations follow KMP conventions with `commonMain`, `androidMain`, `iosMain`, `wasmJsMain`, etc.

## Key Files

- `shared/src/commonMain/kotlin/.../AtvControlService.kt`: Main RPC service interface
- `server/src/main/kotlin/.../Application.kt`: Server entry point
- `composeApp/src/commonMain/kotlin/.../App.kt`: Main UI entry point
- `server/src/main/kotlin/.../service/AdbManager.kt`: ADB device management
- `server/src/main/kotlin/.../service/WebRTCSignalingManager.kt`: WebRTC signaling logic

## Development Notes

- The project uses Gradle version catalogs (libs.versions.toml) for dependency management
- Compose Multiplatform is used for cross-platform UI
- WebRTC-KMP library provides WebRTC functionality across platforms
- The server includes custom Gradle tasks (`buildApp`, `runApp`) for convenience
- Platform-specific source sets allow for native functionality where needed