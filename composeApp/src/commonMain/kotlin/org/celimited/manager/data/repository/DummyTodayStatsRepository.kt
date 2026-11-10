package org.celimited.manager.data.repository

import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.AttendanceStats
import org.celimited.manager.model.todayStats.CallProductivity
import org.celimited.manager.model.todayStats.InventoryStats
import org.celimited.manager.model.todayStats.MemoMetrics
import org.celimited.manager.model.todayStats.MonthToDateTarget
import org.celimited.manager.model.todayStats.OrderBreakdownItem
import org.celimited.manager.model.todayStats.OrderGroupBy
import org.celimited.manager.model.todayStats.ProductivityStats
import org.celimited.manager.model.todayStats.SectionStats
import org.celimited.manager.model.todayStats.SkuProductivity
import org.celimited.manager.model.todayStats.SrDetail
import org.celimited.manager.model.todayStats.SrOrderItem
import org.celimited.manager.model.todayStats.SrOutletStatus
import org.celimited.manager.model.todayStats.SrProfile
import org.celimited.manager.model.todayStats.SrSummary
import org.celimited.manager.model.todayStats.SrTarget
import org.celimited.manager.model.todayStats.SrTimeline
import org.celimited.manager.model.todayStats.SrWorkCounts
import org.celimited.manager.model.todayStats.TargetPeriod
import org.celimited.manager.model.todayStats.TargetStats
import org.celimited.manager.model.todayStats.TeamSummary
import org.celimited.manager.model.todayStats.TodayStatsScope
import org.celimited.manager.model.todayStats.TodaySummary
import org.celimited.manager.model.todayStats.UnitStats
import org.celimited.manager.model.todayStats.ZeroSalesStats

/**
 * In-memory stand-in until the /today-stats/ endpoints in docs/openapi.yaml exist. Swap the
 * TodayStatsRepository binding in TodayStatsModule for the real implementation then.
 *
 * Numbers are illustrative only: units at each level are derived from fixed seeds so switching chips
 * visibly changes the cards.
 */
class DummyTodayStatsRepository : TodayStatsRepository {
    override suspend fun getSummary(userId: String, userLevel: Int): DataResult<TodaySummary> {
        val scope = TodayStatsScope()
        val target = (getTarget(scope) as DataResult.Success).data.total
        return DataResult.Success(
            TodaySummary(
                attendance = (getAttendance(scope) as DataResult.Success).data.total,
                productivity = (getProductivity(scope) as DataResult.Success).data.total,
                today = target.today,
                tillDate = target.tillDate,
                inventory = (getInventory(scope) as DataResult.Success).data.total,
                outlets = (getZeroSales(scope) as DataResult.Success).data.total
            )
        )
    }


    /** Reuses the per-unit dummy sections for a fixed level, since the dummy has no userLevel mapping. */
    override suspend fun getTeamSummary(userId: String, userLevel: Int): DataResult<TeamSummary> {
        val scope = TodayStatsScope(levelKey = "TERRITORY")
        return DataResult.Success(
            TeamSummary(
                attendance = (getAttendance(scope) as DataResult.Success).data.units,
                productivity = (getProductivity(scope) as DataResult.Success).data.units,
                target = (getTarget(scope) as DataResult.Success).data.units,
                outlets = (getZeroSales(scope) as DataResult.Success).data.units
            )
        )
    }

    override suspend fun getAttendance(scope: TodayStatsScope) = section(scope) { seed ->
        val total = 30 + seed * 7
        val absent = 2 + seed % 4
        val leave = 1 + seed % 3
        val present = total - absent - leave
        AttendanceStats(
            totalSr = total,
            present = present,
            absent = absent,
            onLeave = leave,
            visited = present - 1,
            productive = present - 3,
            idle = 2 + seed % 5,
            active = present / 2
        )
    }

    override suspend fun getProductivity(scope: TodayStatsScope) = section(scope, fillTotalOnly = true) { seed ->
        val scheduled = 80.0 + seed * 25
        val visited = scheduled * 0.84
        val productive = visited * 0.4
        ProductivityStats(
            call = CallProductivity(
                scheduled = scheduled,
                visited = visited,
                productive = productive,
                remaining = scheduled - visited,
                strikeRate = 50.0 + seed % 20,
                productiveRate = 60.0 + seed % 25
            ),
            sku = SkuProductivity(lpc = 2.0 + (seed % 9) / 10.0, tlsd = 400.0 + seed * 40, pCase = 60.0 + seed * 6),
            memo = null
        )
    }

    override suspend fun getTarget(scope: TodayStatsScope) = section(scope, fillTotalOnly = true) { seed ->
        val todayTarget = 150_000.0 + seed * 20_000
        val tillTarget = todayTarget * 6
        TargetStats(
            today = TargetPeriod(
                target = todayTarget,
                order = todayTarget * (0.55 + (seed % 4) / 10.0),
                achievementPercent = 55.0 + (seed % 4) * 10,
                fromDate = TODAY,
                toDate = TODAY
            ),
            tillDate = TargetPeriod(
                target = tillTarget,
                order = tillTarget * (0.6 + (seed % 3) / 10.0),
                achievementPercent = 60.0 + (seed % 3) * 10,
                fromDate = MONTH_START,
                toDate = TODAY
            ),
            monthToDate = null
        )
    }

    override suspend fun getInventory(scope: TodayStatsScope) = section(scope) { seed ->
        InventoryStats(
            currentStockValue = 40_000.0 + seed * 9_000,
            openingStockValue = 8_000.0 + seed * 1_800,
            dbCount = 10 + seed * 3,
            outOfStockSkuCount = 2_000 + seed * 500,
            currentStockQty = 6_000.0 + seed * 1_300
        )
    }

    override suspend fun getZeroSales(scope: TodayStatsScope) = section(scope) { seed ->
        ZeroSalesStats(
            activeOutlets = 20 + seed * 2,
            zeroSalesOutlets = 5 + seed,
            activeSkus = 40 + seed,
            zeroSalesSkus = 8 + seed % 6,
            orderCount = 6 + seed
        )
    }

    override suspend fun getOrderBreakdown(
        groupBy: OrderGroupBy,
        scope: TodayStatsScope,
        search: String?
    ): DataResult<List<OrderBreakdownItem>> {
        delay(LATENCY_MS)
        val prefix = when (groupBy) {
            OrderGroupBy.PROJECT -> "Project"
            OrderGroupBy.PRODUCT -> "Product"
            OrderGroupBy.SKU -> "SKU"
            OrderGroupBy.SKU_WISE -> "Starship SKU"
        }
        val offset = (scope.unitId ?: 0) % 7
        val items = (1..6).map { index ->
            val memo = 10 + (index + offset) * 5
            OrderBreakdownItem(
                id = groupBy.ordinal * 100 + index,
                name = "$prefix $index",
                memoCount = memo,
                totalMemoCount = 100,
                memoPercent = memo.toDouble(),
                orderVsTargetQtyPercent = 15.0 + index * 4,
                orderValue = 2_000.0 * index + offset * 150,
                targetValue = 10_000.0 * index
            )
        }
        return DataResult.Success(items.filter { search == null || it.name.contains(search, ignoreCase = true) })
    }

    override suspend fun getSrList(userId: String): DataResult<List<SrSummary>> {
        delay(LATENCY_MS)
        return DataResult.Success(srs)
    }

    override suspend fun getSrDetail(srId: Int): DataResult<SrDetail> {
        delay(LATENCY_MS)
        val sr = srs.firstOrNull { it.srId == srId } ?: srs.first()
        return DataResult.Success(
            SrDetail(
                profile = SrProfile(
                    srId = srId,
                    srCode = sr.srCode,
                    name = sr.name,
                    designation = sr.designation,
                    phoneNo = "01712345678",
                    whatsAppUrl = "https://wa.me/8801712345678",
                    territoryName = "Territory",
                    isOnline = sr.attendanceAt != null
                ),
                timeline = SrTimeline(
                    attendanceAt = at(8, 45),
                    firstVisitAt = at(9, 30),
                    firstOrderAt = at(11, 45),
                    firstOrderOutlet = "Fresh mart",
                    lastOrderAt = at(14, 45),
                    lastOrderOutlet = "Fresh mart",
                    spentHours = "1h 30m"
                ),
                work = SrWorkCounts(
                    scheduled = 20.0,
                    visited = 16.0,
                    productive = 12.0,
                    remaining = 4.0,
                    strikeRate = 75.0,
                    productiveRate = 60.0,
                    lpc = 1.24
                ),
                target = SrTarget(
                    todayTarget = 20_000.0,
                    todayOrder = 16_000.0,
                    todayAchievement = 80.0,
                    tillDateTarget = 400_000.0,
                    tillDateOrder = 250_000.0,
                    tillDateAchievement = 62.5
                ),
                outlets = SrOutletStatus(activeOutlets = 16, zeroSalesOutlets = 4, activeSkus = 184, zeroSalesSkus = 99)
            )
        )
    }

    override suspend fun getSrOrders(srId: Int, groupBy: OrderGroupBy, search: String?): DataResult<List<SrOrderItem>> {
        delay(LATENCY_MS)
        val items = (1..4).map { index ->
            SrOrderItem(
                id = groupBy.ordinal * 100 + index,
                name = if (index % 2 == 1) "Starship" else "Mortein",
                subName = if (index % 2 == 1) "Starship FCMP 10 gm" else "Mortein Coil 10 pcs",
                qty = 24.0 / index,
                memoCount = 5 - index,
                specialDiscount = 0.0,
                promoDiscount = 0.0,
                totalDiscount = 0.0,
                freeValue = 0.0,
                orderQtyUom = 0.73,
                freeQty = 0.0
            )
        }
        return DataResult.Success(
            items.filter {
                search == null || it.name.contains(search, ignoreCase = true) || it.subName.contains(search, ignoreCase = true)
            }
        )
    }

    /**
     * Builds a section: the total comes from a large seed, unit rows (only when a level is requested) from
     * small per-unit seeds. [fillTotalOnly] adds the total-only blocks (memo metrics, month-to-date target).
     */
    private suspend fun <T> section(
        scope: TodayStatsScope,
        fillTotalOnly: Boolean = false,
        build: (seed: Int) -> T
    ): DataResult<SectionStats<T>> {
        delay(LATENCY_MS)
        val levelKey = scope.levelKey
        val unitNames = if (levelKey == null) emptyList() else unitNamesFor(levelKey)
        val units = unitNames.mapIndexed { index, name ->
            val unitId = levelKey.hashCode().mod(1000) + index
            UnitStats(unitId = unitId, unitName = name, stats = build(index + 1))
        }.filter { scope.unitId == null || it.unitId == scope.unitId }
        var total = build(TOTAL_SEED)
        if (fillTotalOnly) total = total.withTotalOnlyBlocks()
        return DataResult.Success(SectionStats(total = total, units = units))
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> T.withTotalOnlyBlocks(): T = when (this) {
        is ProductivityStats -> copy(
            memo = MemoMetrics(
                avgMemo = 4.6,
                avgMemoValue = 25_000.0,
                lastDeliveryMemo = 21.0,
                lastDeliveryValue = 34_000.0,
                lastDeliveryWindowMinutes = 60
            )
        ) as T
        is TargetStats -> copy(
            monthToDate = MonthToDateTarget(
                target = 34_000_000.0,
                achieved = 24_000_000.0,
                achievementPercent = 71.0,
                expectedPercent = 70.0,
                dayOfPeriod = 21,
                daysInPeriod = 30
            )
        ) as T
        else -> this
    }

    private fun unitNamesFor(levelKey: String): List<String> = when (levelKey.uppercase()) {
        "NATIONAL" -> listOf("National")
        "DIVISION" -> listOf("Dhaka Division", "Chattogram Division", "Khulna Division")
        "REGION" -> listOf("North Region", "South Region", "East Region")
        "AREA" -> listOf("Gazipur Area", "Savar Area", "Tongi Area")
        "TERRITORY" -> listOf("Gazipur North", "Gazipur South", "Savar Central", "Tongi East")
        else -> listOf("Unit 1", "Unit 2", "Unit 3")
    }

    private fun at(hour: Int, minute: Int) = LocalDateTime(TODAY.year, TODAY.month, TODAY.day, hour, minute)

    private val srs: List<SrSummary> = listOf(
        "Al Amin" to "Hasan Market SLD",
        "Kathryn" to "Pasadena SLD",
        "Judith" to "Great Falls SLD",
        "Audrey" to "Coppell SLD",
        "Savannah" to "Stockton SLD"
    ).mapIndexed { index, (name, location) ->
        SrSummary(
            srId = index + 1,
            srCode = "7000000${index + 1}",
            name = name,
            designation = "Sales Promotion Officer",
            contactNo = "171234567${index}",
            levelName = location,
            attendanceAt = if (index % 2 == 0) at(10, 0) else null,
            firstOrderAt = at(8, 45),
            lastOrderAt = at(17, 55),
            scheduledCalls = 20.0,
            placement = 16.0,
            successfulMemo = 12.0,
            strikeRate = 17.54,
            productiveRate = 80.0,
            achievementPercent = 46.0,
            lpc = 4.22,
            targetValue = 20_000.0,
            orderValue = 9_200.0
        )
    }

    private companion object {
        const val LATENCY_MS = 400L
        const val TOTAL_SEED = 10
        val TODAY = LocalDate(2026, 10, 6)
        val MONTH_START = LocalDate(2026, 10, 1)
    }
}
