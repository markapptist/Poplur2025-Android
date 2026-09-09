import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.gms.google-services")
    id("com.github.triplet.play")
    id("io.github.takahirom.roborazzi")
}

val natives by configurations.creating

// Mapillary is used for Android street-level imagery (never on iOS, which uses Apple Look
// Around instead). Reads MAPILLARY_ACCESS_TOKEN from the gitignored local.properties file —
// see local.properties.example — so the real token is never committed. Do not log this value.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}
val mapillaryAccessToken: String = localProperties.getProperty("MAPILLARY_ACCESS_TOKEN") ?: ""
fun productionAdUnitId(propertyName: String, fallback: String, required: Boolean): String {
    val value = localProperties.getProperty(propertyName)?.trim().orEmpty().ifEmpty { fallback }
    require(!value.contains("3940256099942544")) {
        "$propertyName must not use Google's demo publisher account"
    }
    require(value.isEmpty() || Regex("ca-app-pub-[0-9]+/[0-9]+").matches(value)) {
        "$propertyName must be a valid AdMob ad-unit ID"
    }
    require(!required || value.isNotEmpty()) { "$propertyName is required" }
    return value
}

val admobInterstitialUnitId = productionAdUnitId(
    propertyName = "ADMOB_INTERSTITIAL_UNIT_ID",
    fallback = "ca-app-pub-2086359936541081/6696033455",
    required = true,
)
val admobFeedNativeUnitId = productionAdUnitId(
    propertyName = "ADMOB_FEED_NATIVE_UNIT_ID",
    fallback = "",
    required = false,
)

android { namespace = "com.apptists.poplur2025"
    compileSdk = 35;
    testBuildType = providers.gradleProperty("poplurInstrumentRelease").map { if (it == "true") "release" else "debug" }.getOrElse("debug")
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationId = "com.apptists.poplur2025"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "1.0.1"
        buildConfigField("String", "MAPILLARY_ACCESS_TOKEN", "\"$mapillaryAccessToken\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_UNIT_ID", "\"$admobInterstitialUnitId\"")
        buildConfigField("String", "ADMOB_FEED_NATIVE_UNIT_ID", "\"$admobFeedNativeUnitId\"")
        // App Check attests with Play Integrity, which needs the app distributed
        // through Play. A sideloaded QA handset can never satisfy that, so the qa
        // build type below swaps in the debug provider. OFF here so release can
        // never carry the bypass: a debug token in a shipped APK would let anyone
        // forge attestation against verifyCardPurchase, which grants paid credits.
        buildConfigField("boolean", "APPCHECK_DEBUG_PROVIDER", "false")
    }

    buildFeatures {
        buildConfig = true
    }

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
            // Preserve the source PNG bytes and skip AAPT2's redundant lossless
            // recompression. Several large nodpi images otherwise exceed AAPT2's
            // fixed two-minute per-file compile timeout on release builds.
            isCrunchPngs = false
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            buildConfigField("boolean", "APPCHECK_DEBUG_PROVIDER", "true")
        }
        // QA: release in every respect that matters — same applicationId, same
        // release signing, same minify — so it installs IN PLACE over the handset's
        // existing release build without the uninstall that would wipe its state.
        // The single difference is the App Check provider.
        create("qa") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "APPCHECK_DEBUG_PROVIDER", "true")
        }
    }

    androidResources {
        noCompress += listOf(
            "tflite", "pb", "emd", "tfl",
            "glb", "gltf",
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
    implementation(project(":performance"))
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
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("androidx.webkit:webkit:1.12.1")

    implementation(platform("com.google.firebase:firebase-bom:33.9.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-appcheck-debug")
    implementation("com.google.firebase:firebase-appcheck-playintegrity")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    // Firebase Analytics. The web page is the source of screen_view and every
    // in-app action event (relayed through analytics/AnalyticsRelay.kt); native
    // logs only login / sign_up. Automatic screen reporting and Advertising ID
    // collection are switched off in AndroidManifest.xml.
    implementation("com.google.firebase:firebase-analytics")
    // Engagement push notifications. Version comes from the BOM above; the
    // credentials come from google-services.json, so Android needs no equivalent
    // of the APNs key that gates iOS delivery.
    implementation("com.google.firebase:firebase-messaging-ktx")

    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")

    implementation("com.mapbox.maps:android-ndk27:11.30.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.android.gms:play-services-ads:25.4.0")
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
    implementation("androidx.media3:media3-transformer:1.5.1")
    implementation("androidx.media3:media3-effect:1.5.1")

    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.agora.rtc:full-rtc-basic:4.6.0")

    implementation("com.badlogicgames.gdx:gdx:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-bullet:1.13.1")
    implementation("com.github.mgsx-dev.gdx-gltf:gltf:2.3.0")
    // SceneView (Google Filament) renders the profile 3D Mark head in its own GL context,
    // avoiding libGDX's process-global Gdx.* singletons owned by the persistent cube overlay.
    implementation("io.github.sceneview:sceneview:2.2.1")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86_64")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.05.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.16.1")
    testImplementation("io.github.takahirom.roborazzi:roborazzi:1.63.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-compose:1.63.0")
    testImplementation("io.github.takahirom.roborazzi:roborazzi-junit-rule:1.63.0")
    testImplementation("androidx.compose.ui:ui-test-junit4")
}

// Config-cache-compatible native extraction: uses injected FileSystemOperations /
// ArchiveOperations instead of the script-scoped copy {} / zipTree(), which cannot be
// serialized into the configuration cache (org.gradle.configuration-cache=true).
abstract class CopyNativesTask : DefaultTask() {
    @get:org.gradle.api.tasks.InputFiles
    abstract val nativeJars: org.gradle.api.file.ConfigurableFileCollection

    @get:org.gradle.api.tasks.OutputDirectory
    abstract val jniLibsDir: org.gradle.api.file.DirectoryProperty

    @get:javax.inject.Inject
    abstract val fs: org.gradle.api.file.FileSystemOperations

    @get:javax.inject.Inject
    abstract val archives: org.gradle.api.file.ArchiveOperations

    @org.gradle.api.tasks.TaskAction
    fun run() {
        nativeJars.files.forEach { file ->
            val arch = when {
                file.name.contains("arm64-v8a") -> "arm64-v8a"
                file.name.contains("armeabi-v7a") -> "armeabi-v7a"
                file.name.contains("x86_64") -> "x86_64"
                file.name.contains("x86") -> "x86"
                else -> null
            } ?: return@forEach
            fs.copy {
                from(archives.zipTree(file))
                include("*.so")
                into(jniLibsDir.dir(arch))
            }
        }
    }
}

tasks.register<CopyNativesTask>("copyNatives") {
    nativeJars.from(natives)
    jniLibsDir.set(layout.projectDirectory.dir("src/main/jniLibs"))
}

project.afterEvaluate {
    tasks.findByName("preBuild")?.dependsOn(tasks.getByName("copyNatives"))
}

// Frame-profile the freshly installed build on an attached low-tier phone (the
// Galaxy A03s is the reference device). Hooked to installDebug rather than
// assembleDebug: until the APK is actually on the device there is nothing to
// measure. Runs detached so it never adds minutes to a build — results land in
// .codex/a03-perf-history.jsonl and a regression is written to
// .codex/a03-last-regression.txt. Opt out with -PskipA03Profile.
tasks.matching { it.name == "installDebug" }.configureEach {
    doLast {
        if (project.hasProperty("skipA03Profile")) return@doLast
        val script = File("/Users/matrix/Developer/.codex/profile-a03.sh")
        if (!script.exists()) return@doLast
        ProcessBuilder("bash", "-c",
            "'${script.path}' --quiet > /Users/matrix/Developer/.codex/a03-last-regression.txt 2>&1 || true")
            .start()
        logger.lifecycle("A03 frame profile started in background — see .codex/a03-last-regression.txt")
    }
}
