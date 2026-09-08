package org.celimited.manager.core.di

import io.ktor.client.HttpClient
import kotlinx.serialization.json.Json
import org.celimited.manager.core.network.createHttpClient
import org.koin.dsl.module

val networkModule = module {
    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
    }
    single<HttpClient> { createHttpClient(engine = get(), json = get()) }
}
