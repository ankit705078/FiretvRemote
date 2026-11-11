plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.firecontrol"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.firecontrol"
        minSdk = 28 // Android 9+ required for BluetoothHidDevice API
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    // Pure-Kotlin embedded ADB client (zero PC or Python needed!)
    implementation("dev.mobile:dadb:1.2.7")
    // Coroutines for background socket / Bluetooth dispatch
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
