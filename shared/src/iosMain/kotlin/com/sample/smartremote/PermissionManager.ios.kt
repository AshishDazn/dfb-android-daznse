package com.sample.smartremote

import androidx.compose.runtime.Composable

actual class PermissionManager actual constructor() {
    @Composable
    actual fun withAudioPermission(content: @Composable (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit) {
        // iOS handles audio permission automatically when AudioQueue or AVAudioSession starts
        // For simplicity, we assume permission is granted or handled by the system dialog
        content(true) {
            // No action needed on iOS for simple use cases
        }
    }
}
