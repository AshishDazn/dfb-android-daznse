package com.sample.smartremote

expect class AudioService() {
    fun startRecording(onData: (ByteArray) -> Unit)
    fun stopRecording()
}
