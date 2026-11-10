package org.celimited.manager.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.celimited.manager.core.common.AppConfig
import org.celimited.manager.core.common.AppEnvironment

fun createHttpClient(engine: HttpClientEngine, json: Json, config: AppConfig): HttpClient =
    HttpClient(engine) {
        install(ContentNegotiation) {
            json(json)
        }
        install(Logging) {
            // Don't log API payloads in store builds.
            level = if (config.environment == AppEnvironment.LIVE) LogLevel.NONE else LogLevel.INFO
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 90_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 90_000
        }
        defaultRequest {
            url(config.baseUrl)
            contentType(ContentType.Application.Json)
        }
    }
