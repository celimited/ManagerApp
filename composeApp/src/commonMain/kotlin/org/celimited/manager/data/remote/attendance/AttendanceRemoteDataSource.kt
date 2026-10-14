package org.celimited.manager.data.remote.attendance

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
import org.celimited.manager.data.remote.dto.attendance.ApplyLeaveRequestDto
import org.celimited.manager.data.remote.dto.attendance.DistributorDto
import org.celimited.manager.data.remote.dto.attendance.LeaveReasonDto
import org.celimited.manager.data.remote.dto.attendance.GeofenceConfigDto
import org.celimited.manager.data.remote.dto.attendance.LateReasonDto
import org.celimited.manager.data.remote.dto.attendance.MonthlyAttendanceDto
import org.celimited.manager.data.remote.dto.attendance.SaveAttendanceRequestDto
import org.celimited.manager.data.remote.dto.attendance.SubordinateHierarchyDto
import org.celimited.manager.data.remote.dto.attendance.TeamSummaryDto
import org.celimited.manager.data.remote.dto.attendance.TodayAttendanceStatusDto

class AttendanceRemoteDataSource(
    private val httpClient: HttpClient,
    private val tokenStorage: TokenStorage
) {
    suspend fun getTodayStatus(userId: String): ApiEnvelope<TodayAttendanceStatusDto> {
        val token = accessToken()
        return httpClient.get("attendance/today-status") {
            authorize(token)
            parameter("userId", userId)
        }.body()
    }

    suspend fun getMonthlySummary(userId: String, month: Int, year: Int): ApiEnvelope<MonthlyAttendanceDto> {
        val token = accessToken()
        return httpClient.get("attendance/monthly-summary") {
            authorize(token)
            parameter("userId", userId)
            parameter("month", month)
            parameter("year", year)
        }.body()
    }

    suspend fun getLeaveReasons(): ApiEnvelope<List<LeaveReasonDto>> {
        val token = accessToken()
        return httpClient.get("attendance/leave-reasons") {
            authorize(token)
        }.body()
    }

    suspend fun applyLeave(request: ApplyLeaveRequestDto): ApiEnvelope<JsonElement> {
        val token = accessToken()
        return httpClient.post("attendance/apply-leave") {
            authorize(token)
            setBody(request)
        }.body()
    }

    suspend fun getLateReasons():ApiEnvelope<List<LateReasonDto>> {
        val token = accessToken()
        return httpClient.get("attendance/late-reasons") {
            authorize(token)
        }.body()
    }

    suspend fun getGeofenceConfig(userId: String): ApiEnvelope<List<GeofenceConfigDto>> {
        val token = accessToken()
        return httpClient.get("user/geofence-config") {
            authorize(token)
            parameter("userId", userId)
        }.body()
    }

    suspend fun getSubordinateHierarchyLevels(): ApiEnvelope<SubordinateHierarchyDto> {
        val token = accessToken()
        return httpClient.get("user/subordinate-hierarchy-levels") {
            authorize(token)
        }.body()
    }

    suspend fun getTeamSummary(levelKey: String): ApiEnvelope<TeamSummaryDto> {
        val token = accessToken()
        return httpClient.get("attendance/team-summary") {
            authorize(token)
            parameter("levelKey", levelKey)
        }.body()
    }

    suspend fun getDistributors(): ApiEnvelope<List<DistributorDto>> {
        val token = accessToken()
        return httpClient.get("user/distributors") {
            authorize(token)
        }.body()
    }

    suspend fun saveAttendance(request: SaveAttendanceRequestDto): ApiEnvelope<JsonElement> {
        val token = accessToken()
        return httpClient.post("attendance/save-attendance") {
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
