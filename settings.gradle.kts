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
        // libadb-android (in-app wireless ADB) is published on JitPack.
        maven { url = uri("https://jitpack.io") }
        // Shizuku is published on Maven Central; GlyphMatrix SDK ships as a local .aar in :matrix/libs.
    }
}

rootProject.name = "Dot"

include(":app")
include(":core")
include(":device")
include(":matrix")
include(":designsystem")
include(":feature-editor")
include(":feature-key")
include(":feature-game")
