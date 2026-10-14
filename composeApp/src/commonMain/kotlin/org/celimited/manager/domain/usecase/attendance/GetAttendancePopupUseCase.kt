package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.flow.first
import org.celimited.manager.model.attendance.AttendancePopupContent
import org.celimited.manager.model.attendance.AttendancePopupDecision
import org.celimited.manager.model.attendance.AttendanceStatus
import org.celimited.manager.model.attendance.LeaveApprovalStatus
import org.celimited.manager.model.attendance.TodayAttendance
import org.celimited.manager.domain.repository.AttendanceRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Decides which attendance popup to show from the cached session data. Only fetches pieces that
 * failed to load at login; otherwise no API calls are made.
 */
@OptIn(ExperimentalTime::class)
class GetAttendancePopupUseCase(
    private val repository: AttendanceRepository,
    private val loadAttendanceDataUseCase: LoadAttendanceDataUseCase,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    suspend operator fun invoke(): AttendancePopupContent {
        if (repository.observeAttendanceData().first()?.isComplete != true) {
            loadAttendanceDataUseCase()
        }
        val data = repository.observeAttendanceData().first()
        return AttendancePopupContent(
            decision = decideAttendancePopup(data?.todayAttendance, nowMillis()),
            distributors = data?.distributors.orEmpty(),
            lateReasons = data?.lateReasons.orEmpty()
        )
    }
}

internal const val MAX_CLOCK_SKEW_MILLIS = 2 * 60 * 1000L

internal fun decideAttendancePopup(today: TodayAttendance?, nowMillis: Long): AttendancePopupDecision {
    if (today == null) return AttendancePopupDecision.None
    return when (today.status) {
        AttendanceStatus.PRESENT, AttendanceStatus.UNKNOWN -> AttendancePopupDecision.None
        AttendanceStatus.LEAVE -> {
            val leave = today.leave ?: return AttendancePopupDecision.None
            if (leave.status == LeaveApprovalStatus.APPROVED) {
                AttendancePopupDecision.OnLeave(leave)
            } else {
                AttendancePopupDecision.LeavePending(leave)
            }
        }
        AttendanceStatus.ABSENT -> {
            val skew = today.clockSkewMillis
            if (skew != null && (skew > MAX_CLOCK_SKEW_MILLIS || skew < -MAX_CLOCK_SKEW_MILLIS)) {
                return AttendancePopupDecision.WrongDeviceTime
            }
            val deadline = today.attendanceDeadlineEpochMillis
                ?: return AttendancePopupDecision.MarkAttendance(minutesRemaining = null)
            val remainingMillis = deadline - nowMillis
            if (remainingMillis < 0) {
                AttendancePopupDecision.Late
            } else {
                // Round up so "0 minutes remaining" only shows at the exact deadline.
                AttendancePopupDecision.MarkAttendance(minutesRemaining = (remainingMillis + 59_999) / 60_000)
            }
        }
    }
}
