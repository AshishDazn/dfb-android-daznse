package com.sample.smartremote.data

data class WebSocketResponse(
    val event: String?,
    val type: String?,
    val transcript: String?,
    val customerId: String?,
    val devices: List<RemoteDeviceResponse>?,
)