# AndroidApp ProGuard Rules

# Suppress JDK 9+ warnings
-dontwarn java.lang.invoke.StringConcatFactory
-dontwarn java.lang.management.ManagementFactory
-dontwarn java.lang.management.RuntimeMXBean

# Keep Android bootstrap classes (Application, Activity, Service, Receiver)
-keep class com.mocca.app.MoccaApp { *; }
-keep class com.mocca.app.MainActivity { *; }
-keep class com.mocca.app.service.ActiveSessionService { *; }
-keep class com.mocca.app.service.PermissionActionReceiver { *; }
-keep class com.mocca.app.domain.manager.ApkDownloadReceiver { *; }
-keep class com.mocca.app.manager.AndroidNotificationTracker { *; }
-keep class com.mocca.app.data.repository.SessionActivityManager { *; }

# Koin
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}
