package org.celimited.manager.data.remote.home

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.celimited.manager.core.common.token.TokenStorage
import org.celimited.manager.core.network.ApiEnvelope
import org.celimited.manager.data.remote.dto.home.DashboardInfoDataDto

class HomeRemoteDataSource(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) {
    /** Today's Order + MTD Performance blocks for Home. [date] is ISO yyyy-MM-dd. */
    suspend fun getDashboardInfo(userId: String, userLevel: Int, date: String): ApiEnvelope<DashboardInfoDataDto> {
        val token = accessToken()
        return httpClient.get("home/dashboard-info") {
            authorize(token)
            parameter("userId", userId)
            parameter("userLevel", userLevel)
            parameter("date", date)
        }.body()
    }

    private suspend fun accessToken(): String? =
        tokenStorage.getTokens()?.accessToken?.takeIf { it.isNotBlank() }

    private fun HttpRequestBuilder.authorize(token: String?) {
        if (token != null) bearerAuth(token)
    }
}
