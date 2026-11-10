package org.celimited.manager.core.di

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.celimited.manager.core.network.BaseUrlProvider
import org.celimited.manager.core.network.EnvironmentBaseUrlProvider
import org.celimited.manager.core.network.createHttpClient
import org.koin.dsl.module

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    // Swap this binding for client routing (company code or API gateway). See BaseUrlProvider.kt.
    single<BaseUrlProvider> { EnvironmentBaseUrlProvider(config = get()) }
    single<HttpClient> {
        createHttpClient(engine = get(), json = get(), config = get(), baseUrlProvider = get())
    }
}
