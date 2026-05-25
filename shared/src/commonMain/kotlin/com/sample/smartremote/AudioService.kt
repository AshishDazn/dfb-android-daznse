package com.sample.smartremote

expect class AudioService() {
    fun isAvailable(): Boolean
    fun startRecording(onData: (ByteArray) -> Unit)
    fun stopRecording()
}
