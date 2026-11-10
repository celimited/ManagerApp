package org.celimited.manager.core.common

/** Which server a build talks to (dev = TEST, prod = LIVE). Set at build time. */
enum class ServerEnvironment { TEST, LIVE }

/**
 * Build-time app settings, provided by each platform's Koin module: Android from the dev/prod flavor's
 * BuildConfig, iOS from APP_ENVIRONMENT set by iosApp/Configuration/{Test,Live}.xcconfig.
 */
data class AppConfig(
    val environment: ServerEnvironment
)
