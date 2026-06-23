import org.gradle.authentication.http.BasicAuthentication

pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = providers.gradleProperty("MAPBOX_DOWNLOADS_TOKEN")
                    .orElse(providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN"))
                    .orElse("")
                    .get()
            }
        }
    }
}
rootProject.name = "Poplur2025Android"
include(":app")
include(":wear")

// Move build output outside iCloud-synced ~/Documents to prevent macOS
// conflict-copy duplication (the "intermediates 2" / "compileDebugKotlin 2" problem).
gradle.allprojects {
    layout.buildDirectory.set(File("/tmp/poplur-android-build/${project.name}"))
}
