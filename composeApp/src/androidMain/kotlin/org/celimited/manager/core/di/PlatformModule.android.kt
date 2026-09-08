package org.celimited.manager.core.di

import android.content.Context
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import org.celimited.manager.core.common.device.AndroidDeviceInfoProvider
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.koin.core.module.Module
import org.koin.dsl.module

fun androidPlatformModule(context: Context): Module = module {
    single<HttpClientEngine> { OkHttp.create() }
    single<DeviceInfoProvider> { AndroidDeviceInfoProvider(context.applicationContext) }
}
