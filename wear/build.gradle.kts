plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.apptists.poplur2025.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.apptists.poplur2025.wear"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

configurations {
    create("natives")
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.05.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("com.google.android.gms:play-services-wearable:19.0.0")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // LibGDX
    implementation("com.badlogicgames.gdx:gdx:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-backend-android:1.13.1")
    implementation("com.badlogicgames.gdx:gdx-bullet:1.13.1")
    "natives"("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-armeabi-v7a")
    "natives"("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-arm64-v8a")
    "natives"("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86")
    "natives"("com.badlogicgames.gdx:gdx-platform:1.13.1:natives-x86_64")
}

tasks.register<Copy>("copyNatives") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
    
    val nativesConfig = configurations.getByName("natives")
    nativesConfig.resolvedConfiguration.resolvedArtifacts.forEach { artifact ->
        val classifier = artifact.classifier ?: ""
        val abi = when {
            classifier.endsWith("arm64-v8a") -> "arm64-v8a"
            classifier.endsWith("armeabi-v7a") -> "armeabi-v7a"
            classifier.endsWith("x86_64") -> "x86_64"
            classifier.endsWith("x86") -> "x86"
            else -> null
        }
        
        if (abi != null) {
            from(zipTree(artifact.file)) {
                include("*.so")
                into(abi)
            }
        }
    }
    
    into(file("src/main/jniLibs"))
}

tasks.named("preBuild") {
    dependsOn("copyNatives")
}
