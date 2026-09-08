package org.celimited.manager

import android.app.Application
import org.celimited.manager.core.di.androidPlatformModule
import org.celimited.manager.core.di.initKoin

class ManagerApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin(listOf(androidPlatformModule(this)))
    }
}
