@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeMultiplatform)
    //alias(libs.plugins.kotlinCompose)
    kotlin("plugin.compose")
    alias(libs.plugins.sqldelight)
}

kotlin {
    //compose {}
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.ui)
            //implementation(compose.compiler.auto)
            //implementation(compose.components.uiToolingPreview)
            //implementation(libs.compose.uiToolingPreview)
            implementation(libs.compose.animation)
            implementation(libs.compose.animation.graphics)
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.materialIconsExtended)
            //implementation(compose.uiTooling)

            // Ktor
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            // Serialization
            implementation(libs.kotlinx.serialization.json)

            // Moko MVVM
            implementation(libs.moko.mvvm.core)
            implementation(libs.moko.mvvm.compose)

            // Napier
            implementation(libs.napier)

            // SQLDelight
            implementation(libs.sqldelight.runtime)
        }

        androidMain.dependencies {
            /* ----- AndroidX Core ----- */
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)

            implementation(libs.androidx.material.icons.extended)

            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.ktor.client.okhttp)

            implementation(libs.sqldelight.android.driver)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native.driver)
            //implementation(libs.kotlinx.coroutinesSwing)
        }
    }
}

android {
    namespace = "com.sample.smartremote.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.sample.smartremote.shared.cache")
        }
    }
}
