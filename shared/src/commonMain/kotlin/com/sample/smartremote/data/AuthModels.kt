package com.sample.smartremote.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class LoginRequest(
    @SerialName("email") val email: String,
    @SerialName("password") val password: String,
    @SerialName("platform") val platform: String = "androidtv",
    @SerialName("deviceId") val deviceId: String
)

@Serializable
data class LoginResponse(
    @SerialName("token") val token: String? = null,
    @SerialName("error") val error: String? = null
)
