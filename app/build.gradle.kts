plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.github.triplet.play")
    id("io.github.takahirom.roborazzi")
}

val natives by configurations.creating

android { namespace = "com.apptists.poplur2025"
    compileSdk = 36;
    defaultConfig { applicationId = "com.apptists.poplur2025"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    
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

    androidResources {
        noCompress += listOf(
            "tflite", "pb", "emd", "tfl",
            "models_bundled/**",
            "mlkit_odt_localizer/**",
            "mlkit_odt_default_classifier/**"
        )
    }

    packaging {
        resources {
            excludes += setOf("META-INF/DEPENDENCIES", "META-INF/LICENSE*", "META-INF/NOTICE*")
            pickFirsts.add("assets/models_bundled/**")
            pickFirsts.add("assets/mlkit_odt_localizer/**")
            pickFirsts.add("assets/mlkit_odt_default_classifier/**")
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

play {
    serviceAccountCredentials.set(file("../play-services-key.json"))
    defaultToAppBundles.set(true)
    track.set("internal")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.activity:activity-compose")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")

    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    implementation("com.mapbox.maps:android:11.19.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.camera:camera-video:1.4.1")
    implementation("com.google.mlkit:object-detection:17.0.2")
    implementation("com.google.mlkit:face-detection:16.1.7")

    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("io.agora.rtc:full-rtc-basic:4.6.0")

    implementation("com.badlogicgames.gdx:gdx:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-bullet:1.13.1")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86_64")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.63.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.63.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.63.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
}

tasks.register("copyNatives") {
    inputs.files(natives)
    outputs.dir("src/main/jniLibs")
    doLast {
        natives.files.forEach { file ->
            val outputDir = when {
                file.name.contains("arm64-v8a") -> "arm64-v8a"
                file.name.contains("armeabi-v7a") -> "armeabi-v7a"
                file.name.contains("x86_64") -> "x86_64"
                file.name.contains("x86") -> "x86"
                else -> null
            }
            if (outputDir != null) {
                copy {
                    from(zipTree(file))
                    include("*.so")
                    into("src/main/jniLibs/$outputDir")
                }
            }
        }
    }
}

project.afterEvaluate {
    tasks.findByName("preBuild")?.dependsOn(tasks.getByName("copyNatives"))
}
