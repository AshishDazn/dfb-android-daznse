package com.sample.smartremote.data.repository

import com.sample.smartremote.data.LoginRequest
import com.sample.smartremote.data.LoginResponse
import com.sample.smartremote.data.SecurePreferences
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository(
    private val securePreferences: SecurePreferences,
    private val httpClient: HttpClient
) {
    suspend fun signIn(email: String, password: String): Result<String> = withContext(Dispatchers.Default) {
        val loginRequest = LoginRequest(
            email = email,
            password = password,
            platform = "dazn-se",
            deviceId = "TestDalsiEndpointDevice1"
        )

        try {
            val response = httpClient.post("https://cdn.stag.business.dazn.com/authentication/euc1/v1/signin") {
                contentType(ContentType.Application.Json)
                setBody(loginRequest)
            }

            if (response.status == HttpStatusCode.OK) {
                val loginResponse: LoginResponse = response.body()
                val token = loginResponse.token
                if (token != null) {
                    securePreferences.saveAuthToken(token)
                    Result.success(token)
                } else {
                    Result.failure(Exception("Token not found in response"))
                }
            } else {
                Result.failure(Exception("Login failed: ${response.status}"))
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
