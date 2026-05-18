import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.sample.smartremote"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sample.smartremote"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    flavorDimensions += "environment"
    productFlavors {
        create("stage") {
            dimension = "environment"
            applicationIdSuffix = ".stage"
            versionNameSuffix = "-stage"
            buildConfigField("String", "BASE_URL", "\"https://cdn.stag.business.dazn.com/\"")
            buildConfigField("String", "WS_URL", "\"ws://63.178.32.34:3000/\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://cdn.business.dazn.com/\"")
            buildConfigField("String", "WS_URL", "\"ws://63.178.32.34:3000/\"")
        }
        create("qa") {
            dimension = "environment"
            applicationIdSuffix = ".test"
            versionNameSuffix = "-test"
            buildConfigField("String", "BASE_URL", "\"https://cdn.test.business.dazn.com/\"")
            buildConfigField("String", "WS_URL", "\"ws://63.178.32.34:3000/\"")
        }
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    implementation(libs.androidx.lifecycle.runtime.ktx)
}
