package com.sample.smartremote

expect class SpeechToTextService {
    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopListening()
}
