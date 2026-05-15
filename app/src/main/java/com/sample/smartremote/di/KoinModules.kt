package com.sample.smartremote.di

import com.google.gson.Gson
import com.sample.smartremote.WebSocketService
import com.sample.smartremote.data.SecurePreferences
import com.sample.smartremote.data.repository.AuthRepository
import com.sample.smartremote.data.repository.RemoteRepository
import com.sample.smartremote.logic.ActionHandler
import com.sample.smartremote.RemoteViewModel
import com.sample.smartremote.AuthViewModel
import okhttp3.OkHttpClient
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

import java.util.concurrent.TimeUnit

val appModule = module {
    single { 
        OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }
    single { Gson() }
    single { SecurePreferences(androidContext()) }
    singleOf(::WebSocketService)
    singleOf(::ActionHandler)

    singleOf(::AuthRepository)
    single { RemoteRepository(get(), get(), get()) }

    viewModel { AuthViewModel(get()) }
    viewModel { RemoteViewModel(get(), get()) }
}
