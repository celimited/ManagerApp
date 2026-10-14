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
import org.celimited.manager.data.remote.dto.todayStats.TodayAttendanceDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodayProductivityDataDto

class TodayStatsRemoteDataSource(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) {
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

    /** [date] is ISO yyyy-MM-dd; [page] is 1-based. */
    suspend fun getSrList(
        levelId: Int,
        unitId: Int,
        date: String,
        search: String?,
        page: Int,
        pageSize: Int
    ): ApiEnvelope<SrListDataDto> {
        val token = accessToken()
        return httpClient.get("today-stats/srs") {
            authorize(token)
            parameter("levelId", levelId)
            parameter("unitId", unitId)
            parameter("date", date)
            if (search != null) parameter("search", search)
            parameter("page", page)
            parameter("pageSize", pageSize)
        }.body()
    }

    private suspend fun accessToken(): String? =
        tokenStorage.getTokens()?.accessToken?.takeIf { it.isNotBlank() }

    private fun HttpRequestBuilder.authorize(token: String?) {
        if (token != null) bearerAuth(token)
    }
}
