package org.celimited.manager.core.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.celimited.manager.core.common.AppConfig
import org.celimited.manager.core.common.AppEnvironment
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.celimited.manager.core.common.device.IosDeviceInfoProvider
import org.celimited.manager.core.common.location.IosLocationProvider
import org.celimited.manager.core.common.location.LocationProvider
import org.celimited.manager.core.common.preferences.IosLoginPreferences
import org.celimited.manager.core.common.preferences.LoginPreferences
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSBundle

fun iosPlatformModule(): Module = module {
    // API_BASE_URL / APP_ENVIRONMENT are set per configuration in iosApp/Configuration/{Test,Live}.xcconfig.
    single {
        val info = NSBundle.mainBundle.infoDictionary
        AppConfig(
            baseUrl = info?.get("API_BASE_URL") as? String
                ?: error("API_BASE_URL missing from Info.plist; check the Xcode configuration's xcconfig"),
            environment = if (info["APP_ENVIRONMENT"] == "TEST") AppEnvironment.TEST else AppEnvironment.LIVE
        )
    }
    single<HttpClientEngine> {
        Darwin.create {
            // Darwin's own default session timeout is shorter than Ktor's HttpTimeout plugin
            // deadline (HttpClientFactory.kt) and would otherwise fire first.
            configureSession {
                timeoutIntervalForRequest = 45.0
                timeoutIntervalForResource = 45.0
            }
        }
    }
    single<DeviceInfoProvider> { IosDeviceInfoProvider() }
    single<LoginPreferences> { IosLoginPreferences() }
    single<LocationProvider> { IosLocationProvider() }
}
