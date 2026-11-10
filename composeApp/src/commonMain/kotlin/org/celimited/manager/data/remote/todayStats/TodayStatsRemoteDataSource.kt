package org.celimited.manager.data.remote.todayStats

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.celimited.manager.core.common.token.TokenStorage
import org.celimited.manager.core.network.ApiEnvelope
import org.celimited.manager.data.remote.dto.todayStats.SrListDataDto
import org.celimited.manager.data.remote.dto.todayStats.SrPerformanceDataDto
import org.celimited.manager.data.remote.dto.todayStats.TeamSummaryDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodayAttendanceDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodayProductivityDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodaySummaryDataDto

class TodayStatsRemoteDataSource(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) {
    /** Every My Summary card in one call. [date] is ISO yyyy-MM-dd. */
    suspend fun getOrderSummary(userId: String, userLevel: Int, date: String): ApiEnvelope<TodaySummaryDataDto> {
        val token = accessToken()
        return httpClient.get("today-stats/order-summary/") {
            authorize(token)
            parameter("date", date)
            parameter("userId", userId)
            parameter("userLevel", userLevel)
        }.body()
    }

    /** [date] is ISO yyyy-MM-dd. */
    suspend fun getAttendance(levelId: Int, unitId: Int, date: String): ApiEnvelope<TodayAttendanceDataDto> {
        val token = accessToken()
        return httpClient.get("today-stats/attendance") {
            authorize(token)
            parameter("levelId", levelId)
            parameter("unitId", unitId)
            parameter("date", date)
        }.body()
    }

    /** [date] is ISO yyyy-MM-dd. */
    suspend fun getProductivity(levelId: Int, unitId: Int, date: String): ApiEnvelope<TodayProductivityDataDto> {
        val token = accessToken()
        return httpClient.get("today-stats/getOrderCallProductivity/") {
            authorize(token)
            parameter("levelId", levelId)
            parameter("unitId", unitId)
            parameter("date", date)
        }.body()
    }

    /** Every SR under [userId] in one response (the API doesn't paginate or search). */
    suspend fun getSrList(userId: String): ApiEnvelope<SrListDataDto> {
        val token = accessToken()
        return httpClient.get("today-stats/srs") {
            authorize(token)
            parameter("userId", userId)
        }.body()
    }

    /** My Team cards: one row per unit at [userLevel] under [userId]. */
    suspend fun getTeamSummary(userId: String, userLevel: Int): ApiEnvelope<TeamSummaryDataDto> {
        val token = accessToken()
        return httpClient.get("today-stats/order-summary-by-hierarchy") {
            authorize(token)
            parameter("userLevel", userLevel)
            parameter("userId", userId)
        }.body()
    }

    /** Everything on the SR Details page for one SR. */
    suspend fun getSrPerformance(srId: Int): ApiEnvelope<SrPerformanceDataDto> {
        val token = accessToken()
        return httpClient.get("today-stats/getSrPerformance") {
            authorize(token)
            parameter("srId", srId)
        }.body()
    }

    private suspend fun accessToken(): String? =
        tokenStorage.getTokens()?.accessToken?.takeIf { it.isNotBlank() }

    private fun HttpRequestBuilder.authorize(token: String?) {
        if (token != null) bearerAuth(token)
    }
}
