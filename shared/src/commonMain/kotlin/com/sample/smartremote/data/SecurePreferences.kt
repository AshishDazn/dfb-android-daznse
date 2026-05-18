package com.sample.smartremote.data

interface SecurePreferences {
    fun saveAuthToken(token: String)
    fun getAuthToken(): String?
    fun clearAuthToken()
    fun isAuthorized(): Boolean
}
