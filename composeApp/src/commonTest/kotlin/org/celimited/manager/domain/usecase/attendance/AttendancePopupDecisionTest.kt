package org.celimited.manager.domain.usecase.attendance

import kotlinx.datetime.LocalDate
import org.celimited.manager.model.attendance.AttendancePopupDecision
import org.celimited.manager.model.attendance.AttendanceStatus
import org.celimited.manager.model.attendance.LeaveApprovalStatus
import org.celimited.manager.model.attendance.LeaveInfo
import org.celimited.manager.model.attendance.TodayAttendance
import kotlin.test.Test
import kotlin.test.assertEquals

class AttendancePopupDecisionTest {

    private val deadline = 1_000_000_000L
    private val minute = 60_000L

    private fun absent(skew: Long? = 0L, deadline: Long? = this.deadline) = TodayAttendance(
        status = AttendanceStatus.ABSENT,
        attendanceDeadlineEpochMillis = deadline,
        clockSkewMillis = skew,
        leave = null
    )

    private fun leave(status: LeaveApprovalStatus) = LeaveInfo(
        fromDate = LocalDate(2026, 2, 15),
        toDate = LocalDate(2026, 2, 18),
        leaveType = "Casual Leave",
        status = status
    )

    @Test
    fun presentShowsNothing() {
        val today = absent().copy(status = AttendanceStatus.PRESENT)
        assertEquals(AttendancePopupDecision.None, decideAttendancePopup(today, deadline - minute))
    }

    @Test
    fun unknownStatusOrMissingDataShowsNothing() {
        assertEquals(AttendancePopupDecision.None, decideAttendancePopup(null, deadline))
        val unknown = absent().copy(status = AttendanceStatus.UNKNOWN)
        assertEquals(AttendancePopupDecision.None, decideAttendancePopup(unknown, deadline))
    }

    @Test
    fun approvedLeaveShowsOnLeave() {
        val info = leave(LeaveApprovalStatus.APPROVED)
        val today = absent().copy(status = AttendanceStatus.LEAVE, leave = info)
        assertEquals(AttendancePopupDecision.OnLeave(info), decideAttendancePopup(today, deadline))
    }

    @Test
    fun nonApprovedLeaveShowsPending() {
        listOf(LeaveApprovalStatus.PENDING, LeaveApprovalStatus.UNKNOWN).forEach { status ->
            val info = leave(status)
            val today = absent().copy(status = AttendanceStatus.LEAVE, leave = info)
            assertEquals(AttendancePopupDecision.LeavePending(info), decideAttendancePopup(today, deadline))
        }
    }

    @Test
    fun leaveWithoutDetailsShowsNothing() {
        val today = absent().copy(status = AttendanceStatus.LEAVE, leave = null)
        assertEquals(AttendancePopupDecision.None, decideAttendancePopup(today, deadline))
    }

    @Test
    fun clockSkewBeyondTwoMinutesIsWrongDeviceTime() {
        val over = 2 * minute + 1
        assertEquals(AttendancePopupDecision.WrongDeviceTime, decideAttendancePopup(absent(skew = over), 0))
        assertEquals(AttendancePopupDecision.WrongDeviceTime, decideAttendancePopup(absent(skew = -over), 0))
    }

    @Test
    fun clockSkewWithinTwoMinutesIsAccepted() {
        val result = decideAttendancePopup(absent(skew = 2 * minute), deadline - 20 * minute)
        assertEquals(AttendancePopupDecision.MarkAttendance(minutesRemaining = 20), result)
    }

    @Test
    fun beforeDeadlineShowsMarkAttendanceWithMinutesRoundedUp() {
        val result = decideAttendancePopup(absent(), deadline - 19 * minute - 1)
        assertEquals(AttendancePopupDecision.MarkAttendance(minutesRemaining = 20), result)
    }

    @Test
    fun atDeadlineIsStillOnTime() {
        assertEquals(AttendancePopupDecision.MarkAttendance(minutesRemaining = 0), decideAttendancePopup(absent(), deadline))
    }

    @Test
    fun afterDeadlineShowsLate() {
        assertEquals(AttendancePopupDecision.Late, decideAttendancePopup(absent(), deadline + 1))
    }

    @Test
    fun missingDeadlineShowsMarkAttendanceWithoutCountdown() {
        val result = decideAttendancePopup(absent(deadline = null), deadline)
        assertEquals(AttendancePopupDecision.MarkAttendance(minutesRemaining = null), result)
    }
}
