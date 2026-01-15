package com.mocca.app

import android.app.Application
import com.mocca.app.di.androidModule
import com.mocca.app.di.appModules
import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class MoccaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize logging
        Napier.base(DebugAntilog())
        
        // Initialize Koin DI
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@MoccaApp)
            modules(appModules + androidModule)
        }
    }
}
