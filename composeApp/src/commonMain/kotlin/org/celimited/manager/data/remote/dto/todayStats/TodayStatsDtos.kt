package org.celimited.manager.data.remote.dto.todayStats

import kotlinx.serialization.Serializable

@Serializable
data class TodayAttendanceDataDto(
    val total: AttendanceStatsDto? = null,
    val units: List<UnitAttendanceDto>? = null
)

@Serializable
data class AttendanceStatsDto(
    val totalSr: Int? = null,
    val present: Int? = null,
    val absent: Int? = null,
    val onLeave: Int? = null,
    val visited: Int? = null,
    val productive: Int? = null,
    val idle: Int? = null,
    val active: Int? = null,
    val idleThresholdMinutes: Int? = null,
    val activeWindowMinutes: Int? = null
)

@Serializable
data class UnitAttendanceDto(
    val unitId: Int? = null,
    val unitName: String? = null,
    val levelKey: String? = null,
    val totalSr: Int? = null,
    val present: Int? = null,
    val absent: Int? = null,
    val onLeave: Int? = null,
    val visited: Int? = null,
    val productive: Int? = null,
    val idle: Int? = null,
    val active: Int? = null,
    val idleThresholdMinutes: Int? = null,
    val activeWindowMinutes: Int? = null
)

@Serializable
data class TodayProductivityDataDto(
    val total: ProductivityStatsDto? = null,
    val units: List<UnitProductivityDto>? = null
)

@Serializable
data class ProductivityStatsDto(
    val call: CallProductivityDto? = null,
    val sku: SkuProductivityDto? = null,
    val memo: MemoMetricsDto? = null
)

@Serializable
data class UnitProductivityDto(
    val unitId: Int? = null,
    val unitName: String? = null,
    val levelKey: String? = null,
    val call: CallProductivityDto? = null,
    val sku: SkuProductivityDto? = null,
    val memo: MemoMetricsDto? = null
)

@Serializable
data class CallProductivityDto(
    val scheduled: Double? = null,
    val visited: Double? = null,
    val productive: Double? = null,
    val remaining: Double? = null,
    val strikeRate: Double? = null,
    val productiveRate: Double? = null
)

@Serializable
data class SkuProductivityDto(
    val lpc: Double? = null,
    val tlsd: Double? = null,
    val pcase: Double? = null
)

@Serializable
data class MemoMetricsDto(
    val avgMemo: Double? = null,
    val avgMemoValue: Double? = null,
    val lastDeliveryMemo: Double? = null,
    val lastDeliveryValue: Double? = null,
    val lastDeliveryWindowMinutes: Int? = null
)

@Serializable
data class SrListDataDto(
    val items: List<SrStatDto>? = null,
    val page: Int? = null,
    val pageSize: Int? = null,
    val totalCount: Int? = null
)

/** today-stats/srs?userId=: one SR row. Field names (incl. misspellings) match the API. */
@Serializable
data class SrStatDto(
    val srId: Int? = null,
    val srCode: String? = null,
    val srName: String? = null,
    val srDesignation: String? = null,
    val contactNo: String? = null,
    val attendanceTime: String? = null,
    val firstOrderTime: String? = null,
    val lastOrderTime: String? = null,
    val levelName: String? = null,
    val schedulecall: Double? = null,
    val placement: Double? = null,
    val successfullMemo: Double? = null,
    val strikeRate: Double? = null,
    val productiveRate: Double? = null,
    val achievement: Double? = null,
    val lpsc: Double? = null,
    val targetValue: Double? = null,
    val orderValue: Double? = null
)

/** today-stats/order-summary/: every My Summary card in one response. */
@Serializable
data class TodaySummaryDataDto(
    val attendance: AttendanceStatsDto? = null,
    val callProductivity: CallProductivityDto? = null,
    val skuProductivity: SkuProductivityDto? = null,
    val memo: MemoMetricsDto? = null,
    val runningTrack: RunningTrackDto? = null,
    val inventoryStatus: InventoryStatsDto? = null,
    val outletStatus: OutletStatusDto? = null
)

@Serializable
data class RunningTrackDto(
    val targetAchievement: RunningTargetAchievementDto? = null,
    val totalTargetAchievement: RunningTotalTargetAchievementDto? = null
)

/** runningTrack.targetAchievement — the "Today" block reads the todays* fields. */
@Serializable
data class RunningTargetAchievementDto(
    val todaysTarget: Double? = null,
    val todaysOrder: Double? = null,
    val todaysAchievement: Double? = null,
    val tillDateTarget: Double? = null,
    val tillDateOrder: Double? = null,
    val tillDateAchievement: Double? = null
)

/** runningTrack.totalTargetAchievement — feeds the "Till date" block. */
@Serializable
data class RunningTotalTargetAchievementDto(
    val totalTarget: Double? = null,
    val totalOrder: Double? = null,
    val totalAchievement: Double? = null
)

@Serializable
data class InventoryStatsDto(
    val stockValue: Double? = null,
    val openingStock: Double? = null,
    val dbCount: Int? = null,
    val zeroStock: Int? = null,
    val currentStock: Double? = null,
    val zeroLifting: Int? = null
)

@Serializable
data class OutletStatusDto(
    // The API's keys are singular (activeOutlet, not activeOutlets).
    val activeOutlet: Int? = null,
    val zeroSalesOutlet: Int? = null,
    val activeSku: Int? = null,
    val zeroSalesSku: Int? = null,
    val orderCount: Int? = null
)
