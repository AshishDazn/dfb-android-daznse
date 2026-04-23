package com.sample.smartremote.data

import kotlinx.serialization.Serializable

@Serializable
data class RemoteDeviceResponse(
    val id: String,
    val nickName: String?,
)

@Serializable
data class RemoteDevice(
    val id: String,
    val name: String
)