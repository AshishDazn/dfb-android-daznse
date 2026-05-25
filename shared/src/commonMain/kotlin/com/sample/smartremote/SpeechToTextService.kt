package com.sample.smartremote

expect class SpeechToTextService {
    fun isAvailable(): Boolean
    fun startListening(onResult: (String, Boolean) -> Unit, onError: (String) -> Unit)
    fun stopListening()
}
