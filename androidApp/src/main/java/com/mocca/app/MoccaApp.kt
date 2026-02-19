package com.mocca.app

import android.app.Application
import com.mocca.app.di.androidModule
import com.mocca.app.di.appModules
import com.mocca.app.util.AppLifecycleObserver
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module
import com.mocca.app.domain.manager.NotificationTracker
import com.mocca.app.manager.AndroidNotificationTracker

val appAndroidModule = module {
    single<NotificationTracker> { AndroidNotificationTracker(androidContext()) }
}

class MoccaApp : Application() {
    
    private val appLifecycleObserver: AppLifecycleObserver by inject()
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        Napier.base(DebugAntilog())
        
        // Initialize Koin DI
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MoccaApp)
            modules(appModules + androidModule + appAndroidModule)
        }
        
        // Start observing app lifecycle for background/foreground detection
        appLifecycleObserver.startObserving()
        Napier.i("[MoccaApp] App lifecycle observer started")
    }
}
