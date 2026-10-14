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
import org.celimited.manager.model.todayStats.SrPage
import org.celimited.manager.model.todayStats.SrProfile
import org.celimited.manager.model.todayStats.SrSummary
import org.celimited.manager.model.todayStats.SrTarget
import org.celimited.manager.model.todayStats.SrTimeline
import org.celimited.manager.model.todayStats.SrWorkCounts
import org.celimited.manager.model.todayStats.SrWorkMode
import org.celimited.manager.model.todayStats.TargetPeriod
import org.celimited.manager.model.todayStats.TargetStats
import org.celimited.manager.model.todayStats.TodayStatsScope
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

    override suspend fun getSrList(
        scope: TodayStatsScope,
        search: String?,
        page: Int,
        pageSize: Int
    ): DataResult<SrPage> {
        delay(LATENCY_MS)
        val matches = srs.filter { search == null || it.name.contains(search, ignoreCase = true) }
        val items = matches.drop((page - 1) * pageSize).take(pageSize)
        return DataResult.Success(SrPage(items = items, page = page, totalCount = matches.size))
    }

    override suspend fun getSrDetail(srId: Int): DataResult<SrDetail> {
        delay(LATENCY_MS)
        val sr = srs.firstOrNull { it.srId == srId } ?: srs.first()
        return DataResult.Success(
            SrDetail(
                profile = SrProfile(
                    srId = srId,
                    name = sr.name,
                    designation = "Senior Sales Representative",
                    photoUrl = null,
                    phoneNo = "+8801712345678",
                    whatsAppUrl = "https://wa.me/8801712345678",
                    locationName = sr.locationName,
                    territoryName = sr.territoryName,
                    isOnline = sr.isOnline
                ),
                timeline = SrTimeline(
                    loggedInAt = at(8, 45),
                    workMode = SrWorkMode.ON_FIELD,
                    spentMinutes = 90,
                    hasVisited = true,
                    firstOrderAt = at(11, 45),
                    firstOrderOutlet = "Fresh mart",
                    lastOrderAt = at(14, 45),
                    lastOrderOutlet = "Fresh mart"
                ),
                work = SrWorkCounts(scheduled = 20, visited = 16, productive = 12, lpc = 1.24),
                target = SrTarget(todayTarget = 20_000.0, orderValue = 16_000.0, achievementPercent = 16.0)
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
            name = name,
            photoUrl = null,
            locationName = location,
            territoryName = "Territory",
            isOnline = index % 2 == 0,
            checkInAt = at(10, 0),
            firstOrderAt = at(8, 45),
            lastOrderAt = at(17, 55),
            strikeRate = 17.54,
            productiveRate = 80.0,
            lpc = 4.22,
            orderAchievementPercent = 46.0
        )
    }

    private companion object {
        const val LATENCY_MS = 400L
        const val TOTAL_SEED = 10
        val TODAY = LocalDate(2026, 10, 6)
        val MONTH_START = LocalDate(2026, 10, 1)
    }
}
