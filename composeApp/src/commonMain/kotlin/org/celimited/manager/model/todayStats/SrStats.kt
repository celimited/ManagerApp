package org.celimited.manager.model.todayStats

import kotlinx.datetime.LocalDateTime

/** One row of the SR tab. Percents are 0–100; money is taka. Times are null until they happen. */
data class SrSummary(
    val srId: Int,
    val srCode: String,
    val name: String,
    val designation: String,
    val contactNo: String,
    val levelName: String,
    val attendanceAt: LocalDateTime?,
    val firstOrderAt: LocalDateTime?,
    val lastOrderAt: LocalDateTime?,
    val scheduledCalls: Double,
    val placement: Double,
    val successfulMemo: Double,
    val strikeRate: Double,
    val productiveRate: Double,
    val achievementPercent: Double,
    val lpc: Double,
    val targetValue: Double,
    val orderValue: Double
)

/** Everything on the SR Details page, from today-stats/getSrPerformance. */
data class SrDetail(
    val profile: SrProfile,
    val timeline: SrTimeline,
    val work: SrWorkCounts,
    val target: SrTarget,
    val outlets: SrOutletStatus
)

data class SrProfile(
    val srId: Int,
    val srCode: String,
    val name: String,
    val designation: String,
    val phoneNo: String,
    val whatsAppUrl: String?,
    val territoryName: String,
    val isOnline: Boolean
)

/** Times are null until they happen. [spentHours] is the server's text as-is (blank when not started). */
data class SrTimeline(
    val attendanceAt: LocalDateTime?,
    val firstVisitAt: LocalDateTime?,
    val firstOrderAt: LocalDateTime?,
    val firstOrderOutlet: String?,
    val lastOrderAt: LocalDateTime?,
    val lastOrderOutlet: String?,
    val spentHours: String
)

/** Percents are 0–100. */
data class SrWorkCounts(
    val scheduled: Double,
    val visited: Double,
    val productive: Double,
    val remaining: Double,
    val strikeRate: Double,
    val productiveRate: Double,
    val lpc: Double
)

/** Money is taka; achievements are 0–100. */
data class SrTarget(
    val todayTarget: Double,
    val todayOrder: Double,
    val todayAchievement: Double,
    val tillDateTarget: Double,
    val tillDateOrder: Double,
    val tillDateAchievement: Double
)

data class SrOutletStatus(
    val activeOutlets: Int,
    val zeroSalesOutlets: Int,
    val activeSkus: Int,
    val zeroSalesSkus: Int
)

data class SrOrderItem(
    val id: Int,
    val name: String,
    val subName: String,
    val qty: Double,
    val memoCount: Int,
    val specialDiscount: Double,
    val promoDiscount: Double,
    val totalDiscount: Double,
    val freeValue: Double,
    val orderQtyUom: Double,
    val freeQty: Double
)
