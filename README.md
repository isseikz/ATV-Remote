# Signaling Library & ATV-Remote Demo

This is a Kotlin Multiplatform project providing a generic WebRTC signaling library with an ATV-Remote demo application targeting Android, iOS, Web, and Server.

## Project Structure

The project is organized into two main parts:

### 📚 Generic Signaling Library (`signalinglib`)

Reusable WebRTC signaling components for any WebRTC application:

* **[/shared](./shared/src)** - Common WebRTC signaling interfaces and models
  - [commonMain](./shared/src/commonMain/kotlin) contains RPC service interfaces (`ISignalingService`, `ISessionService`) and data models
  - Platform-specific implementations for Android, iOS, JVM, and WebAssembly

* **[/server](./server/src)** - WebRTC signaling server library
  - [main/kotlin](./server/src/main/kotlin) contains signaling server implementation (`SignalingServiceImpl`, `SessionManager`)
  - Can be used as a dependency in other server applications

* **[/client](./client/src)** - WebRTC signaling client library
  - [commonMain](./client/src/commonMain/kotlin) contains client-side WebRTC wrapper and signaling client
  - Multiplatform support for Android, iOS, and Web

### 🎮 Demo Application (`demo`)

ATV-Remote demo showcasing the signaling library usage:

* **[/demo/shared](./demo/shared/src)** - ATV-Remote specific shared code
  - Contains ADB control interfaces and Android TV specific data models

* **[/demo/server](./demo/server/src)** - ATV-Remote signaling server
  - [main/kotlin](./demo/server/src/main/kotlin) contains the server application with ADB device management
  - Uses the generic signaling library with ATV-specific functionality

* **[/demo/composeApp](./demo/composeApp/src)** - ATV-Remote client application
  - [commonMain](./demo/composeApp/src/commonMain/kotlin) contains the UI and client logic
  - Platform-specific implementations for Android, iOS, and Web
  - Demonstrates D-pad controls, video streaming, and device management

* **[/iosApp](./iosApp)** - iOS application entry point
  - Contains iOS application configuration and SwiftUI integration

## Architecture

```
signalinglib (Root project)
├── shared/           # Generic WebRTC signaling interfaces & models
├── server/           # Generic signaling server library
├── client/           # Generic signaling client library
└── demo/            # ATV-Remote demo application
    ├── shared/       # ATV-specific shared code
    ├── server/       # ATV-Remote server (uses signaling library)
    └── composeApp/   # ATV-Remote UI (uses signaling library)
```

## Build and Run

### Build Generic Library

To build the reusable signaling library components:

```shell
# Build all library components
./gradlew :shared:build :server:build :client:build

# Or build individually
./gradlew :shared:build      # Core interfaces and models
./gradlew :server:build      # Server library
./gradlew :client:build      # Client library
```

### Build and Run Demo Application

**Server (ATV-Remote signaling server):**
```shell
./gradlew :demo:server:runApp
```

**Android Application:**
```shell
./gradlew :demo:composeApp:assembleDebug
```

**Web Application:**
```shell
./gradlew :demo:composeApp:wasmJsBrowserDevelopmentRun
```

**iOS Application:**
Open the [/iosApp](./iosApp) directory in Xcode and run it from there, or use the IDE's run configuration.

### Development Commands

**Test all components:**
```shell
./gradlew test
./gradlew :demo:composeApp:testDebugUnitTest    # Android tests
./gradlew :shared:commonTest                    # Shared module tests
```

**Build everything:**
```shell
./gradlew build
```

## Using the Signaling Library

The generic signaling library can be used in other WebRTC projects:

1. **Add dependencies** to your `build.gradle.kts`:
   ```kotlin
   dependencies {
       implementation(project(":shared"))      // Core interfaces
       implementation(project(":server"))      // For server applications
       implementation(project(":client"))      // For client applications
   }
   ```

2. **Implement signaling services** using `ISignalingService` and `ISessionService`

3. **Use WebRTC client** with `SignalingClient` and `WebRTCWrapper`

See the [demo application](./demo) for complete implementation examples.

## Technology Stack

- **Kotlin Multiplatform** - Cross-platform development
- **Compose Multiplatform** - Declarative UI framework
- **Ktor** - Server framework with WebSocket support
- **kotlinx-rpc** - Type-safe RPC communication
- **WebRTC-KMP** - Multiplatform WebRTC implementation
- **WebRTC-SDK** (iOS) - Native iOS WebRTC support

## Documentation

- [Design Specification](./docs/design.md) - Detailed architecture and design decisions
- [API Specification](./docs/server-client-api-specification.md) - RPC service interfaces and communication protocols

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web).
If you face any issues, please report them on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).