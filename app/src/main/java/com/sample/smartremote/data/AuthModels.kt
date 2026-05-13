package com.sample.smartremote.data

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("platform") val platform: String = "androidtv",
    @SerializedName("deviceId") val deviceId: String
)

data class LoginResponse(
    @SerializedName("token") val token: String? = null,
    @SerializedName("error") val error: String? = null
)
