package org.celimited.manager.domain.repository

import kotlinx.coroutines.flow.Flow
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.model.attendance.ApplyLeaveParams
import org.celimited.manager.model.attendance.AttendanceSessionData
import org.celimited.manager.model.attendance.LeaveReason
import org.celimited.manager.model.attendance.MonthlyAttendance
import org.celimited.manager.model.attendance.SaveAttendanceParams
import org.celimited.manager.model.attendance.SubordinateHierarchy
import org.celimited.manager.model.attendance.TeamAttendanceSummary

interface AttendanceRepository {
    /**
     * Fetches today's status, late reasons, geofence config and distributors in parallel and
     * caches them. Pieces already cached are not fetched again. With [forceRefresh] the cache is
     * cleared first (used on a fresh login).
     */
    suspend fun loadAttendanceData(userId: String, forceRefresh: Boolean): DataResult<Unit>

    fun observeAttendanceData(): Flow<AttendanceSessionData?>

    /**
     * Returns the API's success message. On success, today's cached status becomes PRESENT and the
     * check-in time/late flag are recorded locally.
     */
    suspend fun saveAttendance(params: SaveAttendanceParams, deviceInfo: DeviceInfo): DataResult<String>

    /** Re-fetches today-status only; the cached value is kept if the call fails. */
    suspend fun refreshTodayStatus(userId: String): DataResult<Unit>

    /** Re-fetches distributors only; the cached list is kept if the call fails. */
    suspend fun refreshDistributors(): DataResult<Unit>

    /** Summary and per-day calendar for [month] (1–12) of [year]. Not cached. */
    suspend fun getMonthlyAttendance(userId: String, year: Int, month: Int): DataResult<MonthlyAttendance>

    suspend fun getLeaveReasons(): DataResult<List<LeaveReason>>

    /** Levels below the logged-in user with today's present/total counts. Not cached. */
    suspend fun getSubordinateHierarchyLevels(): DataResult<SubordinateHierarchy>

    /** Today's counts and manager list for one level ([levelKey] from the hierarchy levels). Not cached. */
    suspend fun getTeamAttendanceSummary(levelKey: String): DataResult<TeamAttendanceSummary>

    /** Returns the API's success message. */
    suspend fun applyLeave(params: ApplyLeaveParams, deviceInfo: DeviceInfo): DataResult<String>

    /** True only once per login — whether Home should auto-show the popup. */
    fun consumeAutoPopup(): Boolean
}
