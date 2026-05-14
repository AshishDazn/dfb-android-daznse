package com.sample.smartremote.data

class AndroidSettings : Settings {
    private val map = mutableMapOf<String, Any>()

    override fun putString(key: String, value: String) {
        map[key] = value
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return map[key] as? String ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        map[key] = value
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return map[key] as? Boolean ?: defaultValue
    }
}

actual fun createSettings(): Settings = AndroidSettings()
