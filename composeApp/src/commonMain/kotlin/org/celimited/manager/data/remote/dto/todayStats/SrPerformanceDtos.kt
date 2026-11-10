package org.celimited.manager.data.remote.dto.todayStats

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * today-stats/getSrPerformance?srId=: everything on the SR Details page. Only the fields the app uses
 * are declared; the rest (incl. orderDetails, whose item shape isn't known yet) are ignored.
 */
@Serializable
data class SrPerformanceDataDto(
    val srData: SrPerformanceInfoDto? = null,
    val srOutletStatus: SrOutletStatusDto? = null,
    val srProductivity: SrProductivityDto? = null,
    val targetAchievement: SrTargetAchievementDto? = null
)

@Serializable
data class SrPerformanceInfoDto(
    val srid: Int? = null,
    val srName: String? = null,
    val srCode: String? = null,
    val srDesignation: String? = null,
    val contactNo: String? = null,
    val phoneNo: String? = null,
    val territoryName: String? = null,
    val attendanceTime: String? = null,
    val firstVisitTime: String? = null,
    val firstOrderTime: String? = null,
    val lastOrderTime: String? = null,
    val lastVisitTime: String? = null,
    val firstOrderOutlet: String? = null,
    val lastOrderOutlet: String? = null,
    val spentHours: String? = null,
    @SerialName("whatsAppURL") val whatsAppUrl: String? = null,
    val lpsc: Double? = null,
    val visited: Double? = null
)

@Serializable
data class SrOutletStatusDto(
    val activeOutlet: Int? = null,
    val zeroSalesOutlet: Int? = null,
    val activeSku: Int? = null,
    val zeroSalesSku: Int? = null
)

@Serializable
data class SrProductivityDto(
    val scheduled: Double? = null,
    val visited: Double? = null,
    val productive: Double? = null,
    val remaining: Double? = null,
    val strikeRate: Double? = null,
    val productivity: Double? = null
)

@Serializable
data class SrTargetAchievementDto(
    val todaysTarget: Double? = null,
    val todaysOrder: Double? = null,
    val todaysAchievement: Double? = null,
    val tillDateTarget: Double? = null,
    val tillDateOrder: Double? = null,
    val tillDateAchievement: Double? = null
)
