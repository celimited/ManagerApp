package org.celimited.manager.core.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.celimited.manager.core.common.device.IosDeviceInfoProvider
import org.koin.core.module.Module
import org.koin.dsl.module

fun iosPlatformModule(): Module = module {
    single<HttpClientEngine> { Darwin.create() }
    single<DeviceInfoProvider> { IosDeviceInfoProvider() }
}
