package com.sample.smartremote.data

import kotlinx.serialization.Serializable

@Serializable
data class WebSocketResponse(
    val event: String? = null,
    val type: String? = null,
    val transcript: String? = null,
    val customerId: String? = null,
    val devices: List<RemoteDeviceResponse>? = null
)
