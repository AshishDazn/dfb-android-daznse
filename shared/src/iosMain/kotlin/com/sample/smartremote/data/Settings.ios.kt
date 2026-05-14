package com.sample.smartremote.data

import platform.Foundation.NSUserDefaults

class IosSettings : Settings {
    private val delegate = NSUserDefaults.standardUserDefaults

    override fun putString(key: String, value: String) {
        delegate.setObject(value, key)
    }

    override fun getString(key: String, defaultValue: String?): String? {
        return delegate.stringForKey(key) ?: defaultValue
    }

    override fun putBoolean(key: String, value: Boolean) {
        delegate.setBool(value, key)
    }

    override fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (delegate.objectForKey(key) != null) {
            delegate.boolForKey(key)
        } else {
            defaultValue
        }
    }
}

actual fun createSettings(): Settings = IosSettings()
