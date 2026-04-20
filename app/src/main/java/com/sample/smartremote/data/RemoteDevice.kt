package com.sample.smartremote.data

data class RemoteDeviceResponse(
    val id: String,
    val nickName: String?,
)

data class RemoteDevice(
    val id: String,
    val name: String
)