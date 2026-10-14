package org.celimited.manager.model.attendance

import kotlinx.datetime.LocalDate

data class LeaveReason(
    val id: Int,
    val name: String
)

data class ApplyLeaveParams(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val leaveReasonId: Int,
    val remarks: String?,
    /** Device location at the time of applying. */
    val latitude: Double,
    val longitude: Double
)
