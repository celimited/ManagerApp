package org.celimited.manager.model.attendance

import kotlinx.datetime.LocalDate

enum class AttendanceStatus { PRESENT, ABSENT, LEAVE, UNKNOWN }

enum class LeaveApprovalStatus { APPROVED, PENDING, UNKNOWN }

data class LeaveInfo(
    val fromDate: LocalDate?,
    val toDate: LocalDate?,
    val leaveType: String,
    val status: LeaveApprovalStatus
)

data class TodayAttendance(
    val status: AttendanceStatus,
    val attendanceDeadlineEpochMillis: Long?,
    /** serverTime minus the device clock at the moment the response arrived; null if unparseable. */
    val clockSkewMillis: Long?,
    val leave: LeaveInfo?
)

/**
 * The check-in this device made today. today-status has no check-in time or late flag, so the
 * Home card reads them from here.
 */
data class LocalCheckIn(
    val epochMillis: Long,
    val isLate: Boolean
)
