plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.poplur2025"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.poplur2025"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        // Maps API key is injected via manifest placeholder.
        manifestPlaceholders["MAPS_API_KEY"] = "AIzaSyDgMYA1YisgbpEGyl0Nj8VGoorbqmfAkG0"
    }

    buildFeatures {
        compose = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val bom = platform("com.google.firebase:firebase-bom:33.5.1")
    implementation(bom)
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    implementation("androidx.compose.ui:ui:1.7.8")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.8")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended-android:1.7.0-alpha01")
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.8")

    implementation("androidx.navigation:navigation-compose:2.8.6")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Images
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Maps Compose
    implementation("com.google.maps.android:maps-compose:4.4.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // CameraX
    val camerax = "1.4.1"
    implementation("androidx.camera:camera-core:$camerax")
    implementation("androidx.camera:camera-camera2:$camerax")
    implementation("androidx.camera:camera-lifecycle:$camerax")
    implementation("androidx.camera:camera-video:$camerax")
    implementation("androidx.camera:camera-view:$camerax")

    // Networking (YouTube)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("com.google.guava:guava:33.0.0-android")
}
