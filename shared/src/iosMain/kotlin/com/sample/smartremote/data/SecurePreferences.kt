package com.sample.smartremote.data

import platform.Foundation.NSUserDefaults

class IosSecurePreferences : SecurePreferences {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override fun saveAuthToken(token: String) {
        userDefaults.setObject(token, KEY_AUTH_TOKEN)
    }

    override fun getAuthToken(): String? {
        return userDefaults.stringForKey(KEY_AUTH_TOKEN)
    }

    override fun clearAuthToken() {
        userDefaults.removeObjectForKey(KEY_AUTH_TOKEN)
    }

    override fun isAuthorized(): Boolean {
        return getAuthToken() != null
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
    }
}
