# MOCCA ProGuard Rules - Optimized for Production
# See: https://developer.android.com/build/shrink-code

# ═══════════════════════════════════════════════════════════════════════════════
# GENERAL
# ═══════════════════════════════════════════════════════════════════════════════

# Suppress JDK 9+ warnings
-dontwarn java.lang.invoke.StringConcatFactory

# Keep attributes for debugging
-keepattributes *Annotation*, InnerClasses, Signature, Exception, LineNumberTable

# ═══════════════════════════════════════════════════════════════════════════════
# KOTLINX SERIALIZATION - Targeted rules
# ═══════════════════════════════════════════════════════════════════════════════

-dontnote kotlinx.serialization.AnnotationsKt

# Keep serializers for specific model classes
-keepclassmembers class com.mocca.app.domain.model.** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class com.mocca.app.api.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keepclassmembers class com.mocca.app.db.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep serializer classes
-keep,includedescriptorclasses class com.mocca.app.**$$serializer { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# KTOR - Minimal required rules
# ═══════════════════════════════════════════════════════════════════════════════

# Keep Ktor client engines
-keep class io.ktor.client.engine.** { *; }

# Keep Ktor serialization
-keep class io.ktor.serialization.** { *; }

# Keep auth providers
-keepclassmembers class io.ktor.client.plugins.auth.providers.** { *; }

-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# ═══════════════════════════════════════════════════════════════════════════════
# KOIN - Minimal required rules
# ═══════════════════════════════════════════════════════════════════════════════

# Keep Koin core
-keep class org.koin.core.** { *; }

# Keep Koin annotations
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
    @org.koin.core.annotation.* <fields>;
}

# ═══════════════════════════════════════════════════════════════════════════════
# VOYAGER - Minimal required rules
# ═══════════════════════════════════════════════════════════════════════════════

-keep class cafe.adriel.voyager.core.** { *; }
-keepclassmembers class * extends cafe.adriel.voyager.core.screen.Screen {
    public <init>(...);
}

# ═══════════════════════════════════════════════════════════════════════════════
# SQLDELIGHT - Minimal required rules
# ═══════════════════════════════════════════════════════════════════════════════

-keep class app.cash.sqldelight.driver.** { *; }
-keep class com.mocca.app.db.** { *; }

# ═══════════════════════════════════════════════════════════════════════════════
# COMPOSE - Minimal required rules
# ═══════════════════════════════════════════════════════════════════════════════

-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ═══════════════════════════════════════════════════════════════════════════════
# DOMAIN MODELS - Keep only what's needed
# ═══════════════════════════════════════════════════════════════════════════════

# Keep domain model classes (for serialization)
-keep class com.mocca.app.domain.model.Session { *; }
-keep class com.mocca.app.domain.model.Message { *; }
-keep class com.mocca.app.domain.model.MessagePart { *; }
-keep class com.mocca.app.domain.model.ServerConfig { *; }
-keep class com.mocca.app.domain.model.FileInfo { *; }
-keep class com.mocca.app.domain.model.GitStatusResponse { *; }
-keep class com.mocca.app.domain.model.GitDiff { *; }
-keep class com.mocca.app.domain.model.PermissionRequest { *; }
-keep class com.mocca.app.domain.model.QuestionRequest { *; }

# Keep API response classes
-keep class com.mocca.app.api.*Response { *; }
-keep class com.mocca.app.api.*Request { *; }
-keep class com.mocca.app.api.GitServer* { *; }
