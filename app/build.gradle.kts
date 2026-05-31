plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.github.triplet.play")
}

val natives by configurations.creating

android { namespace = "com.apptists.poplur2025"; compileSdk = 36
    defaultConfig { applicationId = "com.apptists.poplur2025"; minSdk = 26; targetSdk = 36; versionCode = 1; versionName = "1.0" }
    
    signingConfigs {
        create("release") {
            storeFile = file(project.findProperty("RELEASE_STORE_FILE") ?: "../keystore/release.jks")
            storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
            keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    packaging { resources.excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*") }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

play {
    serviceAccountCredentials.set(file("../play-services-key.json"))
    defaultToAppBundles.set(true)
    track.set("internal")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:face-detection:16.1.7")

    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    implementation("com.badlogicgames.gdx:gdx:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.1")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86_64")
}

tasks.register<Copy>("copyNatives") {
    natives.files.forEach { file ->
        val outputDir = when {
            file.name.contains("arm64-v8a") -> "arm64-v8a"
            file.name.contains("armeabi-v7a") -> "armeabi-v7a"
            file.name.contains("x86_64") -> "x86_64"
            file.name.contains("x86") -> "x86"
            else -> null
        }
        if (outputDir != null) {
            from(zipTree(file)) {
                include("*.so")
                into(outputDir)
            }
        }
    }
    into("src/main/jniLibs")
}

project.afterEvaluate {
    tasks.findByName("preBuild")?.dependsOn(tasks.getByName("copyNatives"))
}
