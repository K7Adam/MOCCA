package com.mocca.app.di

import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import com.mocca.app.data.local.LocalCacheFactory
import com.mocca.app.data.security.SecureTokenStorage
import com.mocca.app.data.security.SecureTokenStorageImpl
import com.mocca.app.discovery.ServerDiscoveryManager
import com.mocca.app.domain.manager.AndroidUpdateManager
import com.mocca.app.domain.manager.PlatformUpdateManager
import com.mocca.app.domain.provider.AndroidAppVersionProvider
import com.mocca.app.domain.provider.AppVersionProvider
import com.mocca.app.util.AndroidAppLifecycleObserver
import com.mocca.app.util.AppLifecycleObserver
import com.mocca.app.util.NetworkObserver
import com.mocca.app.util.NetworkObserverImpl
import okio.Path.Companion.toPath
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Android-specific Koin module.
 */
val androidModule = module {
    single { LocalCacheFactory(androidContext()) }
    single<NetworkObserver> { NetworkObserverImpl(androidContext()) }
    single<PlatformUpdateManager> { AndroidUpdateManager(androidContext()) }
    single<AppVersionProvider> { AndroidAppVersionProvider(androidContext()) }

    // App lifecycle observer for background/foreground detection
    single<AppLifecycleObserver> { 
        AndroidAppLifecycleObserver(androidContext().applicationContext as android.app.Application) 
    }
    
    // Override with Android Keystore implementation
    single<SecureTokenStorage> { SecureTokenStorageImpl(androidContext()) }
    
    // Server discovery for auto-discovery of OpenCode servers
    single<com.mocca.app.discovery.ServerDiscovery> { ServerDiscoveryManager(androidContext()) }
    
    // OPTIMIZED: Coil ImageLoader with memory and disk cache configuration
    single {
        ImageLoader.Builder(androidContext())
            .memoryCache {
                MemoryCache.Builder()
                    // Use 15% of app memory for image cache
                    .maxSizePercent(androidContext(), 0.15)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(androidContext().cacheDir.resolve("image_cache").absolutePath.toPath())
                    .maxSizeBytes(50 * 1024 * 1024) // 50MB disk cache
                    .build()
            }
            .build()
    }
}
