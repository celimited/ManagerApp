package org.celimited.manager.core.network

import org.celimited.manager.core.common.AppConfig
import org.celimited.manager.core.common.ServerEnvironment

/**
 * The single place the API base URL is decided. Read on every request (HttpClientFactory.kt), so the
 * URL can change at runtime without rebuilding the HttpClient.
 *
 * When client routing is decided (company code or API gateway), swap the implementation bound in
 * NetworkModule. Feature code never sees the URL.
 */
interface BaseUrlProvider {
    fun baseUrl(): String
}

/** Today: one base URL per environment, picked by the build (Android dev/prod flavor, iOS Test/Live config). */
class EnvironmentBaseUrlProvider(private val config: AppConfig) : BaseUrlProvider {
    override fun baseUrl(): String = when (config.environment) {
        ServerEnvironment.TEST -> DEV_BASE_URL
        ServerEnvironment.LIVE -> PROD_BASE_URL
    }

    private companion object {
        const val DEV_BASE_URL = "http://210.4.65.222/SupervisorApp/api/v1/"
        const val PROD_BASE_URL = "https://syngentawebapp.azurewebsites.net/SupervisorApp/api/v1/"
    }
}
