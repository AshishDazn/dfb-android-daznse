package com.sample.smartremote

import android.app.Application
import com.sample.smartremote.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class SmartRemoteApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@SmartRemoteApp)
            modules(appModule)
        }
    }
}
