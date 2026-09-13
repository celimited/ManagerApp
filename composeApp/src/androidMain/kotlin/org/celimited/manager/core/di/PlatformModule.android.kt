package org.celimited.manager.core.di

import android.content.Context
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.celimited.manager.core.common.device.AndroidDeviceInfoProvider
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

fun androidPlatformModule(context: Context): Module = module {
    single<HttpClientEngine> {
        OkHttp.create {
            // OkHttp's own default (10s) timeout is shorter than Ktor's HttpTimeout plugin
            // deadline (HttpClientFactory.kt) and would otherwise fire first.
            config {
                connectTimeout(15, TimeUnit.SECONDS)
                readTimeout(45, TimeUnit.SECONDS)
                writeTimeout(45, TimeUnit.SECONDS)
            }
        }
    }
    single<DeviceInfoProvider> { AndroidDeviceInfoProvider(context.applicationContext) }
}
