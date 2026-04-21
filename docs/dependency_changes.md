# Compose Multiplatform Migration: Dependency Changes

This document outlines the required changes to the project's dependencies for the migration to Compose Multiplatform. The dependencies will be managed in the `shared/build.gradle.kts` file of the new `shared` module.

### Core Plugins

| Current Plugin (in `app`) | New Plugin (in `shared`) | Purpose |
| :--- | :--- | :--- |
| `com.android.application` | `org.jetbrains.kotlin.multiplatform` | Enables Kotlin Multiplatform builds. |
| `org.jetbrains.kotlin.android` | `org.jetbrains.compose` | Configures Jetpack and Compose Multiplatform. |
| - | `com.android.library` | Applied in the `android` block for the `shared` module. |

---

### Dependency Mapping

The following table maps the current Android-specific dependencies to their proposed multiplatform replacements.

| Current Dependency (`libs.*`) | Proposed Multiplatform Dependency | Target Source Set | Notes |
| :--- | :--- | :--- | :--- |
| `androidx.core.ktx` | (Varies) | `androidMain` | Often replaced by Kotlin stdlib or other multiplatform libraries. Android-specific parts remain in `androidMain`. |
| `androidx.lifecycle.runtime.ktx` | (Removed) | - | Lifecycle management will be handled by a multiplatform ViewModel/navigation library. |
| `androidx.lifecycle.viewmodel.compose` | `org.moko.mvvm:viewmodel-compose` | `commonMain` | **Example.** Other options like Voyager or MVIKotlin can also be used for multiplatform ViewModels. |
| `androidx.activity.compose` | (Removed) | - | This is an Android-specific integration, handled by the `androidApp` module's setup. |
| `androidx.compose.bom` | `org.jetbrains.compose:compose-bom` | `commonMain` | Use the Compose Multiplatform Bill of Materials. |
| `androidx.compose.ui` | `org.jetbrains.compose.ui:ui` | `commonMain` | |
| `androidx.compose.material3` | `org.jetbrains.compose.material3:material3` | `commonMain` | |
| `androidx.material.icons.extended`| `org.jetbrains.compose.material:material-icons-extended` | `commonMain` | |
| `okhttp` | `io.ktor:ktor-client-core` | `commonMain` | Ktor is a more idiomatic KMM choice for networking. |
| `okhttp` | `io.ktor:ktor-client-okhttp` | `androidMain` | Ktor engine for Android. |
| - | `io.ktor:ktor-client-darwin` | `iosMain` | Ktor engine for iOS. |
| `gson` | `org.jetbrains.kotlinx:kotlinx-serialization-json` | `commonMain` | The official Kotlin multiplatform serialization library. |
| `androidx.core.splashscreen`| (Platform-specific) | `androidMain` | Android splash screen remains in `androidApp`. A separate implementation is needed for iOS. |

---

### New Dependencies

These are new dependencies that will be added to support the multiplatform architecture.

| New Dependency | Purpose | Target Source Set |
| :--- | :--- | :--- |
| `io.ktor:ktor-client-content-negotiation` | Ktor plugin for JSON serialization. | `commonMain` |
| `io.ktor:ktor-serialization-kotlinx-json` | Integrates `kotlinx.serialization` with Ktor. | `commonMain` |
| `dev.icerock.moko:mvvm-core` | Multiplatform ViewModel library. | `commonMain` |
| `io.github.aakira:napier` | A multiplatform logging library. | `commonMain` |
| `app.cash.sqldelight:runtime` | SQLDelight for multiplatform database (if needed).| `commonMain` |
| `app.cash.sqldelight:android-driver`| SQLDelight Android driver. | `androidMain` |
| `app.cash.sqldelight:native-driver` | SQLDelight iOS driver. | `iosMain` |