plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.sqldelight) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.detekt) apply false
}

val isDetektBuild = gradle.startParameter.taskNames.any { it.contains("detekt", ignoreCase = true) || it.contains("check", ignoreCase = true) }

if (isDetektBuild) {
    subprojects {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension>("detekt") {
           config.setFrom(rootProject.layout.projectDirectory.file("detekt.yml"))
           buildUponDefaultConfig = true
           baseline = rootProject.layout.projectDirectory.file("detekt-baseline.xml").asFile
        }
    }
}
