import java.util.Properties

val localProperties = file("local.properties")
val mapboxToken: String? = if (localProperties.exists()) {
    Properties().apply { load(localProperties.inputStream()) }
        .getProperty("MAPBOX_ACCESS_TOKEN")
} else null

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { 
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = mapboxToken
            }
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        
    }
}

rootProject.name = "LocalTrail"
include(":app")
