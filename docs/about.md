# About the SmartRemote Project

This document provides a high-level overview of the SmartRemote project's purpose, architecture, and key dependencies.

## 1. Project Purpose

The SmartRemote project is a sophisticated remote control application designed for home cinema systems. The core goal is to provide a premium, intuitive, and visually rich user experience that moves beyond traditional grid-based remote layouts.

Based on the `DESIGN.md` document, the creative vision is "The Obsidian Conductor," which emphasizes a **Tactile Ethereal** aesthetic. This is achieved through a dark, layered UI with a focus on typography, tonal depth, and subtle lighting effects to create a feeling of a premium physical remote.

Key features appear to include:
*   Standard remote control functionalities.
*   Voice command capabilities, powered by the `AudioService`.
*   Real-time communication with the controlled device via WebSockets.

## 2. Architecture

The project follows a modern Android architecture based on the **Model-View-ViewModel (MVVM)** pattern, leveraging Jetpack Compose for the UI layer.

### Architectural Layers:

*   **UI Layer (View):**
    *   **Framework:** Built entirely with **Jetpack Compose**.
    *   **Entry Point:** `MainActivity.kt` serves as the single activity, hosting all composable screens.
    *   **Theming:** The `ui/theme` package contains the application's design system, including colors, typography, and shapes, to enforce the "Obsidian Conductor" aesthetic.

*   **ViewModel Layer:**
    *   **`RemoteViewModel.kt`**: This is the core of the presentation layer's logic. It is responsible for managing the state of the remote control UI (`RemoteState.kt`), processing user input, and interacting with the service layer.

*   **Service Layer:**
    *   This layer encapsulates specific functionalities and external interactions.
    *   **`WebSocketService.kt`**: Manages the persistent, real-time, two-way communication with the target device using the WebSocket protocol.
    *   **`AudioService.kt`**: Handles audio recording and processing. It is configured to use the `VOICE_RECOGNITION` audio source and includes effects like noise suppression and echo cancellation, indicating its use for voice commands.

*   **Data Layer (Model):**
    *   The `data/` package contains the data models for the application.
    *   **`RemoteDevice.kt`**: Represents the device being controlled.
    *   **`RemoteState.kt`**: Defines the state of the remote control UI.
    *   **`WebSocketResponse.kt`**: Models the data received from the WebSocket server.

## 3. Dependency Graph (Key Libraries)

The project utilizes a set of modern, standard libraries for Android development:

*   **UI Toolkit:**
    *   `androidx.compose.*`: The project is built on **Jetpack Compose**, Google's modern declarative UI toolkit for Android.
    *   `androidx.activity:activity-compose`: Provides the integration between Jetpack Compose and the Android `Activity`.

*   **Architecture Components:**
    *   `androidx.lifecycle:lifecycle-viewmodel-compose`: Provides the `viewModel()` composable function to easily integrate ViewModels into the Compose UI.
    *   `androidx.lifecycle:lifecycle-runtime-ktx`: Provides lifecycle-aware coroutine scopes.

*   **Networking:**
    *   `com.squareup.okhttp3:okhttp`: A powerful and widely-used HTTP client for Android and Java, which also provides WebSocket support.
    *   `com.google.code.gson:gson`: A library for converting Kotlin objects into their JSON representation and vice-versa.

*   **Core & Utilities:**
    *   `org.jetbrains.kotlin:kotlin-stdlib`: The Kotlin standard library.
    *   `androidx.core:core-ktx`: Provides Kotlin extensions for Android framework APIs.
    *   `androidx.core:core-splashscreen`: For implementing the Android 12+ splash screen.