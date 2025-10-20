
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false // Add apply false here
    id("com.google.gms.google-services") version "4.4.3" apply false
    alias(libs.plugins.kotlin.android) apply false // Also good practice for this plugin
    alias(libs.plugins.kotlin.parcelize) apply false // And this one if
}

allprojects {
    repositories {
        google()  // Required for Firebase dependencies
        mavenCentral()  // Required for other dependencies
    }
}