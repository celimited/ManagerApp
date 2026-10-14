package org.celimited.manager.model.todayStats

import kotlinx.datetime.LocalDateTime

/** One page of the SR tab; [totalCount] is across all pages. [page] is 1-based. */
data class SrPage(
    val items: List<SrSummary>,
    val page: Int,
    val totalCount: Int
)

/** One row of the SR tab. Percents are 0–100. */
data class SrSummary(
    val srId: Int,
    val name: String,
    val photoUrl: String?,
    val locationName: String,
    val territoryName: String,
    val isOnline: Boolean,
    val checkInAt: LocalDateTime?,
    val firstOrderAt: LocalDateTime?,
    val lastOrderAt: LocalDateTime?,
    val strikeRate: Double,
    val productiveRate: Double,
    val lpc: Double,
    val orderAchievementPercent: Double
)

data class SrDetail(
    val profile: SrProfile,
    val timeline: SrTimeline,
    val work: SrWorkCounts,
    val target: SrTarget
)

data class SrProfile(
    val srId: Int,
    val name: String,
    val designation: String,
    val photoUrl: String?,
    val phoneNo: String,
    val whatsAppUrl: String?,
    val locationName: String,
    val territoryName: String,
    val isOnline: Boolean
)

enum class SrWorkMode { ON_FIELD, OFFICE, OTHER }

data class SrTimeline(
    val loggedInAt: LocalDateTime?,
    val workMode: SrWorkMode,
    val spentMinutes: Int,
    val hasVisited: Boolean,
    val firstOrderAt: LocalDateTime?,
    val firstOrderOutlet: String?,
    val lastOrderAt: LocalDateTime?,
    val lastOrderOutlet: String?
)

data class SrWorkCounts(
    val scheduled: Int,
    val visited: Int,
    val productive: Int,
    val lpc: Double
)

data class SrTarget(
    val todayTarget: Double,
    val orderValue: Double,
    val achievementPercent: Double
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
