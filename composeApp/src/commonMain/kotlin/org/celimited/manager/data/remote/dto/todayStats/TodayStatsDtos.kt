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
    val pCase: Double? = null
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

@Serializable
data class SrStatDto(
    val srId: Int? = null,
    val name: String? = null,
    val photoUrl: String? = null,
    val locationName: String? = null,
    val territoryName: String? = null,
    val isOnline: Boolean? = null,
    val checkInAt: String? = null,
    val firstOrderAt: String? = null,
    val lastOrderAt: String? = null,
    val strikeRate: Double? = null,
    val productiveRate: Double? = null,
    val lpc: Double? = null,
    val orderAchievementPercent: Double? = null
)
