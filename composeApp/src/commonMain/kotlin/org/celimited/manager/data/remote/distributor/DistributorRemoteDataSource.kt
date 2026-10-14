package org.celimited.manager.data.remote.distributor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import kotlinx.serialization.json.JsonElement
import org.celimited.manager.core.common.token.TokenStorage
import org.celimited.manager.core.network.ApiEnvelope
import org.celimited.manager.data.remote.dto.distributor.DistributorOverviewDto
import org.celimited.manager.data.remote.dto.distributor.SaveIssueRequestDto
import org.celimited.manager.data.remote.dto.distributor.StockSummaryDto
import org.celimited.manager.data.remote.dto.distributor.TeamMemberDto

class DistributorRemoteDataSource(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) {
    suspend fun getTeamMembers(distributorId: Int): ApiEnvelope<List<TeamMemberDto>> {
        val token = accessToken()
        return httpClient.get("user/team-members") {
            authorize(token)
            parameter("distributorId", distributorId)
        }.body()
    }

    suspend fun getStockSummary(distributorId: Int): ApiEnvelope<StockSummaryDto> {
        val token = accessToken()
        // Path spelling ("distibutor") and the capitalised param match the backend exactly.
        return httpClient.get("distibutor/stock-summary") {
            authorize(token)
            parameter("DistributorId", distributorId)
        }.body()
    }

    suspend fun getOverview(distributorId: Int): ApiEnvelope<DistributorOverviewDto> {
        val token = accessToken()
        return httpClient.get("distibutor/overview") {
            authorize(token)
            parameter("DistributorId", distributorId)
        }.body()
    }

    suspend fun saveIssue(request: SaveIssueRequestDto): ApiEnvelope<JsonElement> {
        val token = accessToken()
        return httpClient.post("distributor/save-issue") {
            authorize(token)
            setBody(request)
        }.body()
    }

    private suspend fun accessToken(): String? =
        tokenStorage.getTokens()?.accessToken?.takeIf { it.isNotBlank() }

    private fun HttpRequestBuilder.authorize(token: String?) {
        if (token != null) bearerAuth(token)
    }
}
