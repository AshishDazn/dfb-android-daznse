package com.sample.smartremote.data.repository

import com.google.gson.Gson
import com.sample.smartremote.BuildConfig
import com.sample.smartremote.data.LoginRequest
import com.sample.smartremote.data.LoginResponse
import com.sample.smartremote.data.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class AuthRepository(
    private val securePreferences: SecurePreferences,
    private val httpClient: OkHttpClient,
    private val gson: Gson
) {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        val loginRequest = LoginRequest(
            email = email,
            password = password,
            platform = "dazn-se",
            deviceId = "TestDalsiEndpointDevice1"
        )

        val requestBody = gson.toJson(loginRequest).toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url("${BuildConfig.BASE_URL}authentication/euc1/v1/signin")
            .post(requestBody)
            .build()

        try {
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val loginResponse = gson.fromJson(responseBody, LoginResponse::class.java)
                    val token = loginResponse.token
                    if (token != null) {
                        securePreferences.saveAuthToken(token)
                        Result.success(token)
                    } else {
                        Result.failure(Exception("Token not found in response"))
                    }
                } else {
                    Result.failure(IOException("Login failed: ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isAuthorized(): Boolean = securePreferences.isAuthorized()

    fun getAuthToken(): String? = securePreferences.getAuthToken()

    fun logout() {
        securePreferences.clearAuthToken()
    }
}
