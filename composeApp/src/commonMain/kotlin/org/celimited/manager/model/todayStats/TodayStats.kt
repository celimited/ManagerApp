package org.celimited.manager.model.todayStats

import kotlinx.datetime.LocalDate

// Mirrors the /today-stats/* section endpoints in docs/openapi.yaml. Money is taka, percents are 0–100.

/**
 * Whose numbers to fetch. Both null = the logged-in user's own totals (My Summary); [levelKey] set =
 * also one row per unit at that level (My Team); [unitId] narrows to a single unit.
 */
data class TodayStatsScope(
    val levelKey: String? = null,
    val unitId: Int? = null
)

/** One unit row at a hierarchy level (web MhId / MhName). */
data class UnitStats<T>(
    val unitId: Int,
    val unitName: String,
    val stats: T
)

/** A section's totals for the whole scope, plus per-unit rows when a level was requested. */
data class SectionStats<T>(
    val total: T,
    val units: List<UnitStats<T>>
)

/** Everything on the My Summary / My Team cards for one [TodayStatsScope]. */
data class TodayStatsOverview(
    val attendance: SectionStats<AttendanceStats>,
    val productivity: SectionStats<ProductivityStats>,
    val target: SectionStats<TargetStats>,
    val inventory: SectionStats<InventoryStats>,
    val zeroSales: SectionStats<ZeroSalesStats>
)

data class AttendanceStats(
    val totalSr: Int,
    val present: Int,
    val absent: Int,
    val onLeave: Int,
    val visited: Int,
    val productive: Int,
    val idle: Int,
    val active: Int
)

data class CallProductivity(
    val scheduled: Double,
    val visited: Double,
    val productive: Double,
    val remaining: Double,
    val strikeRate: Double,
    val productiveRate: Double
)

data class SkuProductivity(
    val lpc: Double,
    val tlsd: Double,
    val pCase: Double
)

data class MemoMetrics(
    val avgMemo: Double,
    val avgMemoValue: Double,
    val lastDeliveryMemo: Double,
    val lastDeliveryValue: Double,
    val lastDeliveryWindowMinutes: Int
)

/** [memo] is only present on the section total. */
data class ProductivityStats(
    val call: CallProductivity,
    val sku: SkuProductivity,
    val memo: MemoMetrics?
)

data class TargetPeriod(
    val target: Double,
    val order: Double,
    val achievementPercent: Double,
    val fromDate: LocalDate?,
    val toDate: LocalDate?
)

data class MonthToDateTarget(
    val target: Double,
    val achieved: Double,
    val achievementPercent: Double,
    val expectedPercent: Double,
    val dayOfPeriod: Int,
    val daysInPeriod: Int
)

/** [monthToDate] is only present on the section total. */
data class TargetStats(
    val today: TargetPeriod,
    val tillDate: TargetPeriod,
    val monthToDate: MonthToDateTarget?
)

data class InventoryStats(
    val currentStockValue: Double,
    val openingStockValue: Double,
    val dbCount: Int,
    val outOfStockSkuCount: Int,
    val currentStockQty: Double
)

data class ZeroSalesStats(
    val activeOutlets: Int,
    val zeroSalesOutlets: Int,
    val activeSkus: Int,
    val zeroSalesSkus: Int,
    val orderCount: Int
)

enum class OrderGroupBy { PROJECT, PRODUCT, SKU, SKU_WISE }

data class OrderBreakdownItem(
    val id: Int,
    val name: String,
    val memoCount: Int,
    val totalMemoCount: Int,
    val memoPercent: Double,
    val orderVsTargetQtyPercent: Double,
    val orderValue: Double,
    val targetValue: Double
)
