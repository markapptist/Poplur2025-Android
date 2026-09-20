import org.gradle.authentication.http.BasicAuthentication

pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
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
include(":performance")
include(":claymap_assets")

// Use the default build directory. Avoid using /private/tmp for build outputs on macOS
// as it can have limited space or be purged by the system, leading to build errors.
// The project is located in ~/Developer, which is safe from iCloud sync issues.
