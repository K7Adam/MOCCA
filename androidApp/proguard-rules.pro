# AndroidApp ProGuard Rules

# Suppress JDK 9+ warnings
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Keep all classes from composeApp module
-keep class com.mocca.app.** { *; }

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}
