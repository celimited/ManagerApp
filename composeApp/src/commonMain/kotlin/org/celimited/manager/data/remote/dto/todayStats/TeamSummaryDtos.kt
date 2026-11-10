package org.celimited.manager.data.remote.dto.todayStats

import kotlinx.serialization.Serializable

/**
 * today-stats/order-summary-by-hierarchy?userLevel=&userId=: one row per unit at the requested level, per
 * My Team card. Rows are keyed by mhId / mhNodeId. Only the fields the app uses are declared.
 */
@Serializable
data class TeamSummaryDataDto(
    val attendance: List<TeamAttendanceRowDto>? = null,
    val callProductivity: List<TeamProductivityRowDto>? = null,
    val outletStatus: List<TeamOutletRowDto>? = null,
    val targetInventory: List<TeamTargetRowDto>? = null
)

@Serializable
data class TeamAttendanceRowDto(
    val mhNodeId: Int? = null,
    val srid: Int? = null,
    val mhName: String? = null,
    val name: String? = null,
    val totalSrCount: Int? = null,
    val attendanceGivenSR: Int? = null,
    val leaveSR: Int? = null,
    val visitSRCount: Int? = null
)

@Serializable
data class TeamProductivityRowDto(
    val mhId: Int? = null,
    val mhName: String? = null,
    val scheduled: Double? = null,
    val visited: Double? = null,
    val productive: Double? = null,
    val lpc: Double? = null,
    val strikeRate: Double? = null,
    val productivity: Double? = null
)

/** Shape assumed from SR Details' srOutletStatus; the sample response had an empty array. */
@Serializable
data class TeamOutletRowDto(
    val mhId: Int? = null,
    val mhName: String? = null,
    val activeOutlet: Int? = null,
    val zeroSalesOutlet: Int? = null,
    val activeSku: Int? = null,
    val zeroSalesSku: Int? = null
)

@Serializable
data class TeamTargetRowDto(
    val mhId: Int? = null,
    val mhName: String? = null,
    val todaysTarget: Double? = null,
    val todaysOrder: Double? = null,
    val todaysAchievement: Double? = null,
    val tillDateTarget: Double? = null,
    val tillDateOrder: Double? = null,
    val tillDateAchievement: Double? = null
)
