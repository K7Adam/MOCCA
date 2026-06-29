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

# Koin — keep annotation-annotated methods and core reflection entry points.
# Avoid broad -keep class org.koin.** { *; } which defeats shrinking/optimization.
-keep @org.koin.core.annotation.Module class * { *; }
-keep @org.koin.core.annotation.Single class * { *; }
-keep @org.koin.core.annotation.Factory class * { *; }
-keepclassmembers class * {
    @org.koin.core.annotation.* <methods>;
}
# Koin core uses reflection for module/class loading
-keep class org.koin.core.module.ModuleFactory { *; }
-keep class org.koin.core.definition.Definitions { *; }
-keep class org.koin.core.instance.InstanceFactory { *; }
-keep class org.koin.core.instance.SingleInstanceFactory { *; }
-keep class org.koin.core.instance.FactoryInstanceFactory { *; }
-keep class org.koin.android.ext.koin.AndroidModuleExt { *; }
-keep class org.koin.android.scope.AndroidScopeComponent { *; }
-keepclassmembers class org.koin.core.** {
    public *;
}
