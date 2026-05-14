package com.sample.smartremote.data

interface Settings {
    fun putString(key: String, value: String)
    fun getString(key: String, defaultValue: String? = null): String?
    fun putBoolean(key: String, value: Boolean)
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean
}

expect fun createSettings(): Settings
