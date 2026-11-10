package org.celimited.manager.core.common

enum class AppEnvironment { TEST, LIVE }

/**
 * Build-time app settings, provided by each platform's Koin module: Android from the flavor's
 * BuildConfig, iOS from Info.plist values set by iosApp/Configuration/{Test,Live}.xcconfig.
 *
 * This is the one place the API base URL is decided. When per-client selection (AKG, Coke, Arla, ...)
 * is added, only the code that builds this changes.
 */
data class AppConfig(
    val baseUrl: String,
    val environment: AppEnvironment
)
