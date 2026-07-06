package com.sample.smartremote

import android.app.Application
import com.sample.smartremote.di.initKoin
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger

class SmartRemoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Napier.base(DebugAntilog())
        initKoin { koinApp ->
            koinApp.androidContext(this@SmartRemoteApp)
            koinApp.androidLogger()
        }
    }
}
