# MOCCA ProGuard Rules

# Suppress JDK 9+ warnings
-dontwarn java.lang.invoke.StringConcatFactory

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.mocca.app.**$$serializer { *; }
-keepclassmembers class com.mocca.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.mocca.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.atomicfu.**
-dontwarn io.netty.**
-dontwarn com.typesafe.**
-dontwarn org.slf4j.**

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}

# Voyager
-keep class cafe.adriel.voyager.** { *; }

# SQLDelight
-keep class app.cash.sqldelight.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# Keep model classes
-keep class com.mocca.app.domain.model.** { *; }
-keep class com.mocca.app.api.** { *; }
-keep class com.mocca.app.db.** { *; }
