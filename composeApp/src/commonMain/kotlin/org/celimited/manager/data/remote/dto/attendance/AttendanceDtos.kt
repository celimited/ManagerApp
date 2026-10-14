package org.celimited.manager.data.remote.dto.attendance

import kotlinx.serialization.Serializable
import org.celimited.manager.data.remote.dto.login.DeviceInfoDto

@Serializable
data class TodayAttendanceStatusDto(
    val serverTime: String? = null,
    val attendanceDeadline: String? = null,
    val status: String? = null,
    val leave: LeaveInfoDto? = null
)

@Serializable
data class LeaveInfoDto(
    val fromDate: String? = null,
    val toDate: String? = null,
    val leaveType: String? = null,
    val status: String? = null
)

@Serializable
data class LateReasonDto(
    val id: Int? = null,
    val name: String? = null
)

@Serializable
data class GeofenceConfigDto(
    val salespointId: Int? = null,
    val checkInOption: Int? = null,
    val defaultRadiusMeters: Int? = null
)

@Serializable
data class DistributorDto(
    val id: Int? = null,
    val name: String? = null,
    val latitude: String? = null,
    val longitude: String? = null,
    val code: String? = null,
    val address: String? = null
)

/** Optional fields default to null and are left out of the request body. */
@Serializable
data class SaveAttendanceRequestDto(
    val salespointId: Int,
    val latitude: String,
    val longitude: String,
    val image: String? = null,
    val reason: String? = null,
    val lateReasonId: Int? = null,
    val remarks: String? = null,
    val deviceInfo: DeviceInfoDto
)

@Serializable
data class MonthlyAttendanceDto(
    val summary: MonthlySummaryDto? = null,
    val calendar: List<CalendarDayDto>? = null
)

@Serializable
data class MonthlySummaryDto(
    // Double in case the server ever sends a fractional percentage.
    val attendancePercentage: Double? = null,
    val presentDays: Int? = null,
    val lateDays: Int? = null,
    val leaveDays: Int? = null,
    val absentDays: Int? = null
)

@Serializable
data class CalendarDayDto(
    val date: String? = null,
    val status: String? = null,
    val statusId: Int? = null
)

@Serializable
data class SubordinateHierarchyDto(
    val currentUserRole: String? = null,
    val levels: List<HierarchyLevelDto>? = null
)

@Serializable
data class HierarchyLevelDto(
    val levelKey: String? = null,
    val levelName: String? = null,
    val presentCount: Int? = null,
    val totalCount: Int? = null
)

@Serializable
data class TeamSummaryDto(
    val levelKey: String? = null,
    val summary: TeamSummaryCountsDto? = null,
    val managerList: List<TeamManagerDto>? = null
)

@Serializable
data class TeamSummaryCountsDto(
    val total: Int? = null,
    val present: Int? = null,
    val late: Int? = null,
    val leave: Int? = null,
    val absent: Int? = null
)

/** `checkInTime` arrives already formatted ("09:05 AM"), null unless present or late. */
@Serializable
data class TeamManagerDto(
    val userId: Int? = null,
    val fullName: String? = null,
    val designation: String? = null,
    val profilePictureUrl: String? = null,
    val locationPath: String? = null,
    val status: String? = null,
    val checkInTime: String? = null
)

@Serializable
data class LeaveReasonDto(
    val id: Int? = null,
    val name: String? = null
)

/** Dates are ISO "yyyy-MM-dd". Null remarks are left out of the request body. */
@Serializable
data class ApplyLeaveRequestDto(
    val fromDate: String,
    val toDate: String,
    val leaveReasonId: Int,
    val latitude: String,
    val longitude: String,
    val remarks: String? = null,
    val deviceInfo: DeviceInfoDto
)
