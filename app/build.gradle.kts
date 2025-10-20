
plugins {
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    alias(libs.plugins.android.application) // Apply the plugin here
    alias(libs.plugins.kotlin.parcelize)
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" // this version matches your Kotlin version

}

android {
    namespace = "com.example.poplur2025"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.poplur2025"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "2.0.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildToolsVersion = "35.0.0"
}

dependencies {
    implementation("com.google.android.gms:play-services-wearable:18.1.0")
// Check for the latest version
    implementation("androidx.fragment:fragment-ktx:1.8.9")
    implementation("androidx.glance:glance-appwidget:1.1.0") // Or the latest version
    implementation("androidx.glance:glance-material3:1.1.0") // Or the latest version, if using Material 3 components
    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation(platform("com.google.firebase:firebase-bom:34.0.0")) // Or latest BOM
    implementation(libs.firebase.auth)

    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
// For TabLayout
    implementation("com.google.android.gms:play-services-maps:17.0.0")
    implementation(libs.firebase.storage)
// For Maps

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4") // Or the latest version
    implementation("com.google.android.gms:play-services-maps:19.2.0")
    implementation(libs.play.services.maps.v1700)
    implementation(libs.firebase.core.v2100)
    implementation(platform(libs.firebase.bom))
    // Other dependencies
    implementation(libs.androidx.appcompat.v141)
    implementation(libs.androidx.lifecycle.runtime.ktx.v231)
    implementation(libs.picasso)

    implementation(libs.play.services.maps)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Add this to ensure Firebase libraries match in versions (use Firebase BOM)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.core)
    implementation(platform(libs.firebase.bom)) // Make sure 'libs.firebase.bom' correctly points to your BOM definition in libs.versions.toml
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

