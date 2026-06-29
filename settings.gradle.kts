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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MOCCA"
include(":composeApp")
include(":androidApp")
// :benchmark module — Baseline Profile generator.
// Uncomment after upgrading to AGP 9.0.1 stable (com.android.test plugin
// has a classpath conflict with AGP 9.0.0-rc03).
// include(":benchmark")
