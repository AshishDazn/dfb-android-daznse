package com.sample.smartremote

import androidx.compose.runtime.*
import platform.AVFAudio.*
import kotlinx.cinterop.*

actual class PermissionManager actual constructor() {
    @OptIn(ExperimentalForeignApi::class)
    @Composable
    actual fun withAudioPermission(content: @Composable (hasPermission: Boolean, requestPermission: () -> Unit) -> Unit) {
        val audioSession = remember { AVAudioSession.sharedInstance() }
        var hasPermission by remember {
            mutableStateOf(audioSession.recordPermission() == AVAudioSessionRecordPermissionGranted)
        }

        val requestPermission: () -> Unit = {
            audioSession.requestRecordPermission { granted ->
                hasPermission = granted
            }
        }

        content(hasPermission, requestPermission)
    }
}
