package com.sample.smartremote

expect class SpeechToTextService {
    fun startListening(onResult: (String, Boolean) -> Unit, onError: (String) -> Unit)
    fun stopListening()
}
