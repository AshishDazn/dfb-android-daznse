# Compose Multiplatform Migration: Development Plan

This document outlines the step-by-step plan for migrating the SmartRemote project to Compose Multiplatform.

## Phase 1: Project Restructuring and Setup

The goal of this phase is to create the new Kotlin Multiplatform Mobile (KMM) project structure and configure the Gradle build system.

1.  **Create a `shared` Module:**
    *   Create a new Gradle module named `shared`. This will be a Kotlin Multiplatform module.
    *   Configure the `shared/build.gradle.kts` file with the Kotlin Multiplatform and Compose Multiplatform plugins.
2.  **Define Source Sets:**
    *   In the `shared` module, set up the standard KMM source sets:
        *   `commonMain`: For code shared between Android and iOS.
        *   `androidMain`: For Android-specific implementations.
        *   `iosMain`: For iOS-specific implementations.
3.  **Update Project-Level Gradle:**
    *   Modify the root `build.gradle.kts` to include the new `shared` module.
4.  **Create `androidApp` and `iosApp` Modules:**
    *   The existing `:app` module can be repurposed as the `androidApp`. Its role will be simplified to a thin container for the shared UI.
    *   Create a new Xcode project for the `iosApp` that will consume the `shared` module as a framework.

## Phase 2: Dependency Migration

This phase focuses on updating the dependencies to be multiplatform-compatible.

1.  **Update `shared/build.gradle.kts`:**
    *   Add the multiplatform dependencies as outlined in `dependency_changes.md`.
    *   This includes `org.jetbrains.compose.*` for UI, `kotlinx.serialization` for JSON, a multiplatform ViewModel library, and a multiplatform networking client like Ktor.
2.  **Remove Old Dependencies:**
    *   Remove the now-redundant Android-specific dependencies from the `androidApp`'s `build.gradle.kts`. The `androidApp` should only depend on the `shared` module and any necessary Android-specific integration libraries.

## Phase 3: Code Migration and Refactoring

This is the core phase where the existing code is moved and adapted for a multiplatform context.

1.  **Move Data and Domain Layers:**
    *   Move all data classes from `com.sample.smartremote.data` to `shared/src/commonMain`.
    *   Replace `Gson` annotations with `@Serializable` from `kotlinx.serialization`.
2.  **Move Network Layer:**
    *   Move the `WebSocketService` to `shared/src/commonMain`.
    *   If switching to Ktor, refactor the networking logic to use the Ktor client API.
3.  **Refactor the ViewModel:**
    *   Move `RemoteViewModel` to `shared/src/commonMain`.
    *   Remove the dependency on `androidx.lifecycle.ViewModel`.
    *   Replace it with a multiplatform ViewModel solution. The core logic remains the same, but the class will no longer inherit from the Android ViewModel.
4.  **Move UI Layer:**
    *   Move all Composable functions and themes from `com.sample.smartremote.ui` to `shared/src/commonMain`.
    *   Replace any imports from `androidx.compose` with `org.jetbrains.compose`.
    *   Remove any dependencies on `LocalContext` or other Android-specific context providers. These will need to be passed in from the platform-specific entry points if required.
5.  **Implement Platform-Specific Services (`AudioService`):**
    *   In `shared/src/commonMain`, define an `expect class AudioService`. This class will declare the common functions required for audio recording (e.g., `startRecording()`, `stopRecording()`).
    *   In `shared/src/androidMain`, create an `actual class AudioService` that provides the implementation using Android's `AudioRecord` APIs.
    *   In `shared/src/iosMain`, create an `actual class AudioService` that provides the implementation using Apple's `AVFoundation` framework.

## Phase 4: Application Entry Points and Wiring

This final phase connects the shared code to the platform-specific application entry points.

1.  **Android (`androidApp`):**
    *   Update `MainActivity.kt` to host the shared Composable UI from the `shared` module.
    *   Provide the `actual` implementation of `AudioService` and other platform services to the shared ViewModel.
2.  **iOS (`iosApp`):**
    *   Create a Swift UI view that hosts the Compose Multiplatform UI.
    *   In Swift, create an instance of the `AudioService` (the `actual` iOS implementation) and pass it to the shared ViewModel.
    *   Configure the Xcode project to build and link the `shared` module framework.

## Phase 5: Testing

1.  **Shared Module Tests:** Write unit tests for the shared ViewModels and services in `shared/src/commonTest`.
2.  **End-to-End Testing:** Perform manual or automated UI testing on both Android and iOS devices to ensure consistent behavior and appearance.