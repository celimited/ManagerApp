package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.AttendanceCardStatus
import org.celimited.manager.model.attendance.AttendanceStatus
import org.celimited.manager.model.attendance.LeaveApprovalStatus
import org.celimited.manager.model.attendance.LocalCheckIn
import org.celimited.manager.model.attendance.TodayAttendance
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Status for the Home "Attendance" card, from the cached today-status. Switches from
 * [AttendanceCardStatus.NotGiven] to [AttendanceCardStatus.TimePassed] by itself when the
 * deadline is reached while the card is on screen.
 */
@OptIn(ExperimentalTime::class, ExperimentalCoroutinesApi::class)
class ObserveAttendanceCardUseCase(
    private val repository: AttendanceRepository,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() }
) {
    operator fun invoke(): Flow<AttendanceCardStatus> =
        repository.observeAttendanceData().transformLatest { data ->
            val today = data?.todayAttendance
            val status = decideAttendanceCard(today, data?.localCheckIn, nowMillis())
            emit(status)
            val deadline = today?.attendanceDeadlineEpochMillis
            if (status == AttendanceCardStatus.NotGiven && deadline != null) {
                delay(deadline - serverNow(today, nowMillis()))
                emit(AttendanceCardStatus.TimePassed)
            }
        }
}

internal fun decideAttendanceCard(
    today: TodayAttendance?,
    localCheckIn: LocalCheckIn?,
    nowMillis: Long
): AttendanceCardStatus {
    // A check-in made here wins even if a refreshed today-status still lags behind.
    if (localCheckIn != null) {
        return AttendanceCardStatus.CheckedIn(localCheckIn.epochMillis, localCheckIn.isLate)
    }
    if (today == null) return AttendanceCardStatus.Unknown
    return when (today.status) {
        AttendanceStatus.PRESENT -> AttendanceCardStatus.CheckedIn(epochMillis = null, isLate = null)
        AttendanceStatus.LEAVE -> when (today.leave?.status) {
            LeaveApprovalStatus.APPROVED -> AttendanceCardStatus.OnLeave
            else -> AttendanceCardStatus.LeavePending
        }
        AttendanceStatus.ABSENT -> {
            val deadline = today.attendanceDeadlineEpochMillis
            if (deadline != null && serverNow(today, nowMillis) >= deadline) {
                AttendanceCardStatus.TimePassed
            } else {
                AttendanceCardStatus.NotGiven
            }
        }
        AttendanceStatus.UNKNOWN -> AttendanceCardStatus.Unknown
    }
}

/** Device time shifted onto the server clock, so a wrong device clock doesn't move the deadline. */
private fun serverNow(today: TodayAttendance, deviceNowMillis: Long): Long =
    deviceNowMillis + (today.clockSkewMillis ?: 0L)
