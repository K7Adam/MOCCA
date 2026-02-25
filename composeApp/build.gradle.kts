plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.sqldelight)
}

// Compose Compiler configuration for stability and metrics
composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose-stability.conf"))
    
    // Strong Skipping Mode is enabled by default in Kotlin 2.0+
    // No need to specify featureFlags explicitly
    
    // Enable Compose metrics for performance analysis
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
    reportsDestination = layout.buildDirectory.dir("compose_reports")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidLibrary {
        namespace = "com.mocca.app"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}
        
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.auth)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.serialization.kotlinx.json)
            // Ktor Encoding (Compression)
            implementation(libs.ktor.client.encoding)

            // Koin DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)

            // Voyager Navigation
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.screenmodel)
            implementation(libs.voyager.koin)
            implementation(libs.voyager.transitions)

            // KotlinX
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.collections.immutable)

            // Logging
            implementation(libs.napier)

            // Material3 Adaptive
            implementation(libs.compose.material3.adaptive)
            implementation(libs.compose.material3.adaptive.layout)
            implementation(libs.compose.material3.adaptive.navigation)

            // Coil Image Loading
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Markdown
            implementation(libs.markdown.renderer)
            implementation(libs.markdown.renderer.m3)
            implementation(libs.highlights)

            // SLF4J for Ktor logging
            implementation(libs.slf4j.simple)
            
            // FileKit for cross-platform file picking
            implementation(libs.filekit.core)
            implementation(libs.filekit.compose)

            // Paging 3
            implementation(libs.paging.common)
            implementation(libs.paging.compose)

            // Haze - Blur effects for liquid glass UI
            implementation(libs.haze)
            
            // Liquid - True liquid glass with lens refraction
            implementation(libs.liquid)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.sqldelight.android.driver)
            implementation(libs.koin.android)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.lifecycle.runtime)
            implementation(libs.androidx.lifecycle.process)
            implementation(compose.preview)
            implementation(libs.kotlinx.serialization.json)
            
            // Backdrop - Kyant0's Liquid Glass library (Android-only, requires API 31+)
            implementation(libs.backdrop)
            
            // ML Kit for QR code scanning
            implementation("com.google.mlkit:barcode-scanning:17.2.0")
            
            // CameraX for camera preview
            implementation("androidx.camera:camera-camera2:1.3.0")
            implementation("androidx.camera:camera-lifecycle:1.3.0")
            implementation("androidx.camera:camera-view:1.3.0")
        }
    }
}

sqldelight {
    databases {
        create("AppDatabase") {
            packageName.set("com.mocca.app.db")
            version = 2
            // Schema now in commonMain/sqldelight, generated for all targets
        }
    }
}