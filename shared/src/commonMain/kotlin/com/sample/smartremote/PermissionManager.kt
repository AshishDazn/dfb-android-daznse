package com.sample.smartremote

import androidx.compose.runtime.Composable

expect class PermissionManager() {
    @Composable
    fun withAudioPermission(content: @Composable (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit)
}
