package com.sample.smartremote

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

actual class PermissionManager actual constructor() {
    @Composable
    actual fun withAudioPermission(content: @Composable (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit) {
        val context = LocalContext.current
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            hasPermission = isGranted
        }

        content(hasPermission) {
            launcher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
