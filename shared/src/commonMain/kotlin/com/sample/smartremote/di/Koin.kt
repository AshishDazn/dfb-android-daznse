package com.sample.smartremote.di

import com.sample.smartremote.AudioService
import com.sample.smartremote.RemoteViewModel
import com.sample.smartremote.WebSocketService
import com.sample.smartremote.data.repository.AuthRepository
import com.sample.smartremote.data.repository.RemoteRepository
import com.sample.smartremote.logic.ActionHandler
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.module

fun initKoin(
    appDeclaration: (KoinApplication) -> Unit = {}
): KoinApplication {
    return startKoin {
        appDeclaration(this)
        modules(
            platformModule,
            commonModule
        )
    }
}

val commonModule = module {
    single {
        HttpClient {
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
    }
    single { WebSocketService() }
    single { AudioService() }
    single { ActionHandler() }
    single { AuthRepository(get(), get()) }
    single { RemoteRepository(get(), get()) }
    single { RemoteViewModel(get(), get(), get(), get()) }
}

expect val platformModule: Module
