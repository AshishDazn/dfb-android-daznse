package com.sample.smartremote

actual class AudioService actual constructor() {
    actual fun startRecording(onData: (ByteArray) -> Unit) {
        // TODO: Implement iOS audio recording
    }
    
    actual fun stopRecording() {
        // TODO: Implement iOS audio recording stop
    }
}
