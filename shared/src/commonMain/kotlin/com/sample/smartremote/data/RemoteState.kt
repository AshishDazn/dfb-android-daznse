package com.sample.smartremote.data

sealed class RemoteState {
    data object IDLE : RemoteState()
    data object LISTENING : RemoteState()
    data object PROCESSING : RemoteState()
    data class RESULT(val transcript: String) : RemoteState()
    data class ERROR(val message: String? = null) : RemoteState()
}
