package org.celimited.manager.domain.usecase.attendance

import kotlinx.datetime.LocalDate
import org.celimited.manager.model.attendance.AttendanceCardStatus
import org.celimited.manager.model.attendance.AttendanceStatus
import org.celimited.manager.model.attendance.LeaveApprovalStatus
import org.celimited.manager.model.attendance.LeaveInfo
import org.celimited.manager.model.attendance.LocalCheckIn
import org.celimited.manager.model.attendance.TodayAttendance
import kotlin.test.Test
import kotlin.test.assertEquals

class AttendanceCardDecisionTest {

    private val deadline = 1_000_000_000L
    private val minute = 60_000L

    private fun today(
        status: AttendanceStatus = AttendanceStatus.ABSENT,
        deadline: Long? = this.deadline,
        skew: Long? = 0L,
        leave: LeaveInfo? = null
    ) = TodayAttendance(status, deadline, skew, leave)

    private fun leave(status: LeaveApprovalStatus) =
        LeaveInfo(LocalDate(2026, 2, 15), LocalDate(2026, 2, 18), "Casual Leave", status)

    @Test
    fun absentBeforeDeadlineIsNotGiven() {
        assertEquals(AttendanceCardStatus.NotGiven, decideAttendanceCard(today(), null, deadline - minute))
    }

    @Test
    fun absentAfterDeadlineIsTimePassed() {
        assertEquals(AttendanceCardStatus.TimePassed, decideAttendanceCard(today(), null, deadline + minute))
        assertEquals(AttendanceCardStatus.TimePassed, decideAttendanceCard(today(), null, deadline))
    }

    @Test
    fun absentWithoutDeadlineIsNotGiven() {
        assertEquals(
            AttendanceCardStatus.NotGiven,
            decideAttendanceCard(today(deadline = null), null, deadline + minute)
        )
    }

    @Test
    fun deadlineUsesServerClock() {
        // Device is 5 minutes behind the server, so the server has already passed the deadline.
        val skewed = today(skew = 5 * minute)
        assertEquals(AttendanceCardStatus.TimePassed, decideAttendanceCard(skewed, null, deadline - minute))
    }

    @Test
    fun localCheckInShowsTimeAndLateFlag() {
        val onTime = LocalCheckIn(epochMillis = 42L, isLate = false)
        assertEquals(
            AttendanceCardStatus.CheckedIn(42L, false),
            decideAttendanceCard(today(AttendanceStatus.PRESENT), onTime, deadline)
        )
        val late = LocalCheckIn(epochMillis = 43L, isLate = true)
        assertEquals(
            AttendanceCardStatus.CheckedIn(43L, true),
            decideAttendanceCard(today(AttendanceStatus.PRESENT), late, deadline)
        )
    }

    @Test
    fun localCheckInWinsOverLaggingAbsentStatus() {
        val checkIn = LocalCheckIn(epochMillis = 42L, isLate = false)
        assertEquals(
            AttendanceCardStatus.CheckedIn(42L, false),
            decideAttendanceCard(today(), checkIn, deadline + minute)
        )
    }

    @Test
    fun presentWithoutLocalRecordHasNoTime() {
        assertEquals(
            AttendanceCardStatus.CheckedIn(null, null),
            decideAttendanceCard(today(AttendanceStatus.PRESENT), null, deadline)
        )
    }

    @Test
    fun leaveMapsToPendingOrOnLeave() {
        val approved = today(AttendanceStatus.LEAVE, leave = leave(LeaveApprovalStatus.APPROVED))
        assertEquals(AttendanceCardStatus.OnLeave, decideAttendanceCard(approved, null, deadline))
        val pending = today(AttendanceStatus.LEAVE, leave = leave(LeaveApprovalStatus.PENDING))
        assertEquals(AttendanceCardStatus.LeavePending, decideAttendanceCard(pending, null, deadline))
    }

    @Test
    fun missingOrUnknownIsUnknown() {
        assertEquals(AttendanceCardStatus.Unknown, decideAttendanceCard(null, null, deadline))
        assertEquals(
            AttendanceCardStatus.Unknown,
            decideAttendanceCard(today(AttendanceStatus.UNKNOWN), null, deadline)
        )
    }
}
