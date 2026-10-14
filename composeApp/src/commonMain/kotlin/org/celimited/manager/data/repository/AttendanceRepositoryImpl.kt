package org.celimited.manager.data.repository

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.core.network.safeApiCall
import org.celimited.manager.core.network.safeApiCallNoData
import org.celimited.manager.data.local.attendance.AttendanceLocalDataSource
import org.celimited.manager.data.mapper.attendance.toDistributors
import org.celimited.manager.data.mapper.attendance.toDomain
import org.celimited.manager.data.mapper.attendance.toGeofenceConfigs
import org.celimited.manager.data.mapper.attendance.toLateReasons
import org.celimited.manager.data.mapper.login.toDto
import org.celimited.manager.data.remote.attendance.AttendanceRemoteDataSource
import org.celimited.manager.data.remote.dto.attendance.SaveAttendanceRequestDto
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.data.mapper.attendance.toLeaveReasons
import org.celimited.manager.data.remote.dto.attendance.ApplyLeaveRequestDto
import org.celimited.manager.model.attendance.ApplyLeaveParams
import org.celimited.manager.model.attendance.AttendanceSessionData
import org.celimited.manager.model.attendance.LeaveReason
import org.celimited.manager.model.attendance.LocalCheckIn
import org.celimited.manager.model.attendance.MonthlyAttendance
import org.celimited.manager.model.attendance.SaveAttendanceParams
import org.celimited.manager.model.attendance.SubordinateHierarchy
import org.celimited.manager.model.attendance.TeamAttendanceSummary
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AttendanceRepositoryImpl(
    private val remoteDataSource: AttendanceRemoteDataSource,
    private val localDataSource: AttendanceLocalDataSource,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) : AttendanceRepository {

    override suspend fun loadAttendanceData(userId: String, forceRefresh: Boolean): DataResult<Unit> =
        coroutineScope {
            if (forceRefresh) localDataSource.clear()
            val cached = localDataSource.data.value ?: AttendanceSessionData()

            val today = async {
                if (cached.todayAttendance != null) return@async null
                safeApiCall { remoteDataSource.getTodayStatus(userId) }
                    .map { it.toDomain(deviceNowMillis = nowMillis()) }
            }
            val lateReasons = async {
                if (cached.lateReasons != null) return@async null
                safeApiCall { remoteDataSource.getLateReasons() }.map { it.toLateReasons() }
            }
            val geofence = async {
                if (cached.geofenceConfigs != null) return@async null
                safeApiCall { remoteDataSource.getGeofenceConfig(userId) }.map { it.toGeofenceConfigs() }
            }
            val distributors = async {
                if (cached.distributors != null) return@async null
                safeApiCall { remoteDataSource.getDistributors() }.map { it.toDistributors() }
            }

            val todayResult = today.await()
            val lateReasonsResult = lateReasons.await()
            val geofenceResult = geofence.await()
            val distributorsResult = distributors.await()

            localDataSource.update { current ->
                current.copy(
                    todayAttendance = current.todayAttendance ?: todayResult.successOrNull(),
                    lateReasons = current.lateReasons ?: lateReasonsResult.successOrNull(),
                    geofenceConfigs = current.geofenceConfigs ?: geofenceResult.successOrNull(),
                    distributors = current.distributors ?: distributorsResult.successOrNull()
                )
            }

            listOf(todayResult, lateReasonsResult, geofenceResult, distributorsResult)
                .filterIsInstance<DataResult.Error>()
                .firstOrNull()
                ?: DataResult.Success(Unit)
        }

    override fun observeAttendanceData(): Flow<AttendanceSessionData?> = localDataSource.data

    override suspend fun saveAttendance(params: SaveAttendanceParams, deviceInfo: DeviceInfo): DataResult<String> {
        val request = SaveAttendanceRequestDto(
            salespointId = params.salespointId,
            latitude = params.latitude.toString(),
            longitude = params.longitude.toString(),
            reason = params.lateReason?.name,
            lateReasonId = params.lateReason?.id,
            remarks = params.remarks?.trim()?.takeIf { it.isNotEmpty() },
            deviceInfo = deviceInfo.toDto()
        )
        val result = safeApiCallNoData { remoteDataSource.saveAttendance(request) }
        if (result is DataResult.Success) {
            // Server-corrected so the shown time matches the server clock, like the deadline.
            val skew = localDataSource.data.value?.todayAttendance?.clockSkewMillis ?: 0L
            localDataSource.recordCheckIn(
                LocalCheckIn(epochMillis = nowMillis() + skew, isLate = params.lateReason != null)
            )
        }
        return result
    }

    override suspend fun refreshTodayStatus(userId: String): DataResult<Unit> {
        val result = safeApiCall { remoteDataSource.getTodayStatus(userId) }
            .map { it.toDomain(deviceNowMillis = nowMillis()) }
        if (result is DataResult.Success) {
            localDataSource.update { it.copy(todayAttendance = result.data) }
        }
        return result.map { }
    }

    override suspend fun refreshDistributors(): DataResult<Unit> {
        val result = safeApiCall { remoteDataSource.getDistributors() }.map { it.toDistributors() }
        if (result is DataResult.Success) {
            localDataSource.update { it.copy(distributors = result.data) }
        }
        return result.map { }
    }

    override suspend fun getMonthlyAttendance(userId: String, year: Int, month: Int): DataResult<MonthlyAttendance> =
        safeApiCall { remoteDataSource.getMonthlySummary(userId, month = month, year = year) }
            .map { it.toDomain() }

    override suspend fun getLeaveReasons(): DataResult<List<LeaveReason>> =
        safeApiCall { remoteDataSource.getLeaveReasons() }.map { it.toLeaveReasons() }

    override suspend fun getSubordinateHierarchyLevels(): DataResult<SubordinateHierarchy> =
        safeApiCall { remoteDataSource.getSubordinateHierarchyLevels() }.map { it.toDomain() }

    override suspend fun getTeamAttendanceSummary(levelKey: String): DataResult<TeamAttendanceSummary> =
        safeApiCall { remoteDataSource.getTeamSummary(levelKey) }.map { it.toDomain() }

    override suspend fun applyLeave(params: ApplyLeaveParams, deviceInfo: DeviceInfo): DataResult<String> {
        val request = ApplyLeaveRequestDto(
            fromDate = params.fromDate.toString(),
            toDate = params.toDate.toString(),
            leaveReasonId = params.leaveReasonId,
            latitude = params.latitude.toString(),
            longitude = params.longitude.toString(),
            remarks = params.remarks?.trim()?.takeIf { it.isNotEmpty() },
            deviceInfo = deviceInfo.toDto()
        )
        return safeApiCallNoData { remoteDataSource.applyLeave(request) }
    }

    override fun consumeAutoPopup(): Boolean = localDataSource.consumeAutoPopup()

    private inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
        is DataResult.Success -> DataResult.Success(transform(data))
        is DataResult.Error -> this
        is DataResult.Loading -> this
    }

    private fun <T> DataResult<T>?.successOrNull(): T? = (this as? DataResult.Success)?.data
}
