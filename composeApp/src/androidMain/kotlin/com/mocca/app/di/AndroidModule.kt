package com.mocca.app.di

import com.mocca.app.data.local.LocalCacheFactory
import com.mocca.app.domain.manager.AndroidUpdateManager
import com.mocca.app.domain.manager.PlatformUpdateManager
import com.mocca.app.domain.provider.AndroidAppVersionProvider
import com.mocca.app.domain.provider.AppVersionProvider
import com.mocca.app.util.NetworkObserver
import com.mocca.app.util.NetworkObserverImpl
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
}
