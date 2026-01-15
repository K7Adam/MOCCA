package com.mocca.app.di

import com.mocca.app.data.local.LocalCacheFactory
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
}
