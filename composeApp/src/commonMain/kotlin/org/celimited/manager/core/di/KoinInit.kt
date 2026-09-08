package org.celimited.manager.core.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module

fun initKoin(platformModules: List<Module>) {
    startKoin {
        modules(platformModules + commonModule + networkModule + loginModule)
    }
}
