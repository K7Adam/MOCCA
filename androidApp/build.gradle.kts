import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.multiplatform)
}

android {
    namespace = "com.mocca.app.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.mocca.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        
        val buildNumber = System.getenv("BUILD_NUMBER")?.toIntOrNull() ?: 1
        val versionProps = Properties()
        try {
            versionProps.load(FileInputStream(rootProject.file("gradle.properties")))
        } catch (e: Exception) {
            // Fallback if file missing
        }
        
        val baseVersion = versionProps.getProperty("VERSION_NAME", "1.0.0")
        
        versionCode = buildNumber
        versionName = if (System.getenv("CI") == "true") {
            // CI Version: 1.0.0-build.123
            "$baseVersion-build.$buildNumber"
        } else {
            // Local Version: 1.0.0
            baseVersion
        }
    }

    signingConfigs {
        create("sharedDebug") {
            storeFile = file("keystores/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
        debug {
            signingConfig = signingConfigs.getByName("sharedDebug")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs.keepDebugSymbols.add("**/libandroidx.graphics.path.so")
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":composeApp"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.android)
    implementation(libs.napier)

    implementation(libs.compose.foundation)
    implementation(libs.compose.material3.expressive)
    implementation(libs.compose.ui.graphics)

    // Glance home screen widget
    implementation(libs.androidx.glance.widget)
    implementation(libs.androidx.glance.material3)
}