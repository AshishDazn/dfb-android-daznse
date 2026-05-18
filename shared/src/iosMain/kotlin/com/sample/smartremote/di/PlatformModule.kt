package com.sample.smartremote.di

import com.sample.smartremote.data.IosSecurePreferences
import com.sample.smartremote.data.SecurePreferences
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<SecurePreferences> { IosSecurePreferences() }
}
