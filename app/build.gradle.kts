plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Firebase switches on automatically once you add app/google-services.json (see README).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.example.alarmapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.alarmapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.3.0"
        // 10.0.2.2 reaches your computer from the emulator. On a real phone, change it
        // in the app (Connected apps screen) to your computer's Wi-Fi IP.
        buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8000\"")
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.09.03"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-messaging")
}
