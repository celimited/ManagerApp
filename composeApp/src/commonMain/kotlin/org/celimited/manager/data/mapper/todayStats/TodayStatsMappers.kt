package org.celimited.manager.data.mapper.todayStats

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import org.celimited.manager.data.remote.dto.todayStats.AttendanceStatsDto
import org.celimited.manager.data.remote.dto.todayStats.CallProductivityDto
import org.celimited.manager.data.remote.dto.todayStats.InventoryStatsDto
import org.celimited.manager.data.remote.dto.todayStats.MemoMetricsDto
import org.celimited.manager.data.remote.dto.todayStats.OutletStatusDto
import org.celimited.manager.data.remote.dto.todayStats.ProductivityStatsDto
import org.celimited.manager.data.remote.dto.todayStats.SkuProductivityDto
import org.celimited.manager.data.remote.dto.todayStats.SrListDataDto
import org.celimited.manager.data.remote.dto.todayStats.SrOutletStatusDto
import org.celimited.manager.data.remote.dto.todayStats.SrPerformanceDataDto
import org.celimited.manager.data.remote.dto.todayStats.SrPerformanceInfoDto
import org.celimited.manager.data.remote.dto.todayStats.SrProductivityDto
import org.celimited.manager.data.remote.dto.todayStats.SrTargetAchievementDto
import org.celimited.manager.data.remote.dto.todayStats.SrStatDto
import org.celimited.manager.data.remote.dto.todayStats.TeamSummaryDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodayAttendanceDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodayProductivityDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodaySummaryDataDto
import org.celimited.manager.data.remote.dto.todayStats.UnitAttendanceDto
import org.celimited.manager.model.todayStats.AttendanceStats
import org.celimited.manager.model.todayStats.CallProductivity
import org.celimited.manager.model.todayStats.InventoryStats
import org.celimited.manager.model.todayStats.MemoMetrics
import org.celimited.manager.model.todayStats.ProductivityStats
import org.celimited.manager.model.todayStats.SectionStats
import org.celimited.manager.model.todayStats.SkuProductivity
import org.celimited.manager.model.todayStats.SrDetail
import org.celimited.manager.model.todayStats.SrOutletStatus
import org.celimited.manager.model.todayStats.SrProfile
import org.celimited.manager.model.todayStats.SrSummary
import org.celimited.manager.model.todayStats.SrTarget
import org.celimited.manager.model.todayStats.SrTimeline
import org.celimited.manager.model.todayStats.SrWorkCounts
import org.celimited.manager.model.todayStats.TargetPeriod
import org.celimited.manager.model.todayStats.TargetStats
import org.celimited.manager.model.todayStats.TeamSummary
import org.celimited.manager.model.todayStats.TodaySummary
import org.celimited.manager.model.todayStats.UnitStats
import org.celimited.manager.model.todayStats.ZeroSalesStats

fun TodayAttendanceDataDto.toDomain(): SectionStats<AttendanceStats> = SectionStats(
    total = (total ?: AttendanceStatsDto()).toDomain(),
    units = units.orEmpty().mapNotNull { it.toDomain() }
)

private fun AttendanceStatsDto.toDomain() = AttendanceStats(
    totalSr = totalSr ?: 0,
    present = present ?: 0,
    absent = absent ?: 0,
    onLeave = onLeave ?: 0,
    visited = visited ?: 0,
    productive = productive ?: 0,
    idle = idle ?: 0,
    active = active ?: 0
)

/** Rows without a unitId can't be keyed or filtered on, so they're dropped. */
private fun UnitAttendanceDto.toDomain(): UnitStats<AttendanceStats>? {
    val id = unitId ?: return null
    return UnitStats(
        unitId = id,
        unitName = unitName.orEmpty(),
        stats = AttendanceStats(
            totalSr = totalSr ?: 0,
            present = present ?: 0,
            absent = absent ?: 0,
            onLeave = onLeave ?: 0,
            visited = visited ?: 0,
            productive = productive ?: 0,
            idle = idle ?: 0,
            active = active ?: 0
        )
    )
}

fun TodayProductivityDataDto.toDomain(): SectionStats<ProductivityStats> = SectionStats(
    total = (total ?: ProductivityStatsDto()).let { productivityStats(it.call, it.sku, it.memo) },
    units = units.orEmpty().mapNotNull { unit ->
        val id = unit.unitId ?: return@mapNotNull null
        UnitStats(
            unitId = id,
            unitName = unit.unitName.orEmpty(),
            stats = productivityStats(unit.call, unit.sku, unit.memo)
        )
    }
)

private fun productivityStats(
    call: CallProductivityDto?,
    sku: SkuProductivityDto?,
    memo: MemoMetricsDto?
) = ProductivityStats(
    call = (call ?: CallProductivityDto()).let {
        CallProductivity(
            scheduled = it.scheduled ?: 0.0,
            visited = it.visited ?: 0.0,
            productive = it.productive ?: 0.0,
            remaining = it.remaining ?: 0.0,
            strikeRate = it.strikeRate ?: 0.0,
            productiveRate = it.productiveRate ?: 0.0
        )
    },
    sku = (sku ?: SkuProductivityDto()).let {
        SkuProductivity(lpc = it.lpc ?: 0.0, tlsd = it.tlsd ?: 0.0, pCase = it.pcase ?: 0.0)
    },
    memo = memo?.let {
        MemoMetrics(
            avgMemo = it.avgMemo ?: 0.0,
            avgMemoValue = it.avgMemoValue ?: 0.0,
            lastDeliveryMemo = it.lastDeliveryMemo ?: 0.0,
            lastDeliveryValue = it.lastDeliveryValue ?: 0.0,
            lastDeliveryWindowMinutes = it.lastDeliveryWindowMinutes ?: 0
        )
    }
)

/** [today] anchors bare "HH:mm[:ss]" times. Rows without an srId can't be keyed or opened, so they're dropped. */
fun SrListDataDto.toDomain(today: LocalDate): List<SrSummary> =
    items.orEmpty().mapNotNull { it.toDomain(today) }

private fun SrStatDto.toDomain(today: LocalDate): SrSummary? {
    val id = srId ?: return null
    return SrSummary(
        srId = id,
        srCode = srCode.orEmpty(),
        name = srName.orEmpty(),
        designation = srDesignation.orEmpty(),
        contactNo = contactNo.orEmpty(),
        levelName = levelName.orEmpty(),
        attendanceAt = attendanceTime.toDateTimeOrNull(today),
        firstOrderAt = firstOrderTime.toDateTimeOrNull(today),
        lastOrderAt = lastOrderTime.toDateTimeOrNull(today),
        scheduledCalls = schedulecall ?: 0.0,
        placement = placement ?: 0.0,
        successfulMemo = successfullMemo ?: 0.0,
        strikeRate = strikeRate ?: 0.0,
        productiveRate = productiveRate ?: 0.0,
        achievementPercent = achievement ?: 0.0,
        lpc = lpsc ?: 0.0,
        targetValue = targetValue ?: 0.0,
        orderValue = orderValue ?: 0.0
    )
}

/** Rows without a unit id can't be keyed or filtered on, so they're dropped (same rule as the other unit rows). */
fun TeamSummaryDataDto.toDomain(): TeamSummary = TeamSummary(
    attendance = attendance.orEmpty().mapNotNull { row ->
        val id = row.mhNodeId ?: row.srid ?: return@mapNotNull null
        val total = row.totalSrCount ?: 0
        val present = row.attendanceGivenSR ?: 0
        val onLeave = row.leaveSR ?: 0
        UnitStats(
            unitId = id,
            unitName = unitName(row.mhName, row.name),
            stats = AttendanceStats(
                totalSr = total,
                present = present,
                absent = (total - present - onLeave).coerceAtLeast(0),
                onLeave = onLeave,
                visited = row.visitSRCount ?: 0,
                productive = 0,
                idle = 0,
                active = 0
            )
        )
    },
    productivity = callProductivity.orEmpty().mapNotNull { row ->
        val id = row.mhId ?: return@mapNotNull null
        UnitStats(
            unitId = id,
            unitName = unitName(row.mhName),
            stats = ProductivityStats(
                call = CallProductivity(
                    scheduled = row.scheduled ?: 0.0,
                    visited = row.visited ?: 0.0,
                    productive = row.productive ?: 0.0,
                    remaining = ((row.scheduled ?: 0.0) - (row.visited ?: 0.0)).coerceAtLeast(0.0),
                    strikeRate = row.strikeRate ?: 0.0,
                    productiveRate = row.productivity ?: 0.0
                ),
                sku = SkuProductivity(lpc = row.lpc ?: 0.0, tlsd = 0.0, pCase = 0.0),
                memo = null
            )
        )
    },
    target = targetInventory.orEmpty().mapNotNull { row ->
        val id = row.mhId ?: return@mapNotNull null
        UnitStats(
            unitId = id,
            unitName = unitName(row.mhName),
            stats = TargetStats(
                today = TargetPeriod(
                    target = row.todaysTarget ?: 0.0,
                    order = row.todaysOrder ?: 0.0,
                    achievementPercent = row.todaysAchievement ?: 0.0,
                    fromDate = null,
                    toDate = null
                ),
                tillDate = TargetPeriod(
                    target = row.tillDateTarget ?: 0.0,
                    order = row.tillDateOrder ?: 0.0,
                    achievementPercent = row.tillDateAchievement ?: 0.0,
                    fromDate = null,
                    toDate = null
                ),
                monthToDate = null
            )
        )
    },
    outlets = outletStatus.orEmpty().mapNotNull { row ->
        val id = row.mhId ?: return@mapNotNull null
        UnitStats(
            unitId = id,
            unitName = unitName(row.mhName),
            stats = ZeroSalesStats(
                activeOutlets = row.activeOutlet ?: 0,
                zeroSalesOutlets = row.zeroSalesOutlet ?: 0,
                activeSkus = row.activeSku ?: 0,
                zeroSalesSkus = row.zeroSalesSku ?: 0,
                orderCount = 0
            )
        )
    }
)

private fun unitName(vararg candidates: String?): String =
    candidates.firstOrNull { !it.isNullOrBlank() }.orEmpty()

/** [srId] is the requested id, used when the response omits srid. [today] anchors bare times. */
fun SrPerformanceDataDto.toDomain(srId: Int, today: LocalDate): SrDetail {
    val info = srData ?: SrPerformanceInfoDto()
    val prod = srProductivity ?: SrProductivityDto()
    val target = targetAchievement ?: SrTargetAchievementDto()
    val outlets = srOutletStatus ?: SrOutletStatusDto()
    val attendanceAt = info.attendanceTime.toDateTimeOrNull(today)
    return SrDetail(
        profile = SrProfile(
            srId = info.srid ?: srId,
            srCode = info.srCode.orEmpty(),
            name = info.srName.orEmpty(),
            designation = info.srDesignation.orEmpty(),
            phoneNo = info.contactNo?.takeIf { it.isNotBlank() } ?: info.phoneNo.orEmpty(),
            whatsAppUrl = info.whatsAppUrl?.takeIf { it.isNotBlank() },
            territoryName = info.territoryName.orEmpty(),
            isOnline = attendanceAt != null
        ),
        timeline = SrTimeline(
            attendanceAt = attendanceAt,
            firstVisitAt = info.firstVisitTime.toDateTimeOrNull(today),
            firstOrderAt = info.firstOrderTime.toDateTimeOrNull(today),
            firstOrderOutlet = info.firstOrderOutlet?.takeIf { it.isNotBlank() },
            lastOrderAt = info.lastOrderTime.toDateTimeOrNull(today),
            lastOrderOutlet = info.lastOrderOutlet?.takeIf { it.isNotBlank() },
            spentHours = info.spentHours?.trim().orEmpty()
        ),
        work = SrWorkCounts(
            scheduled = prod.scheduled ?: 0.0,
            visited = prod.visited ?: info.visited ?: 0.0,
            productive = prod.productive ?: 0.0,
            remaining = prod.remaining ?: 0.0,
            strikeRate = prod.strikeRate ?: 0.0,
            productiveRate = prod.productivity ?: 0.0,
            lpc = info.lpsc ?: 0.0
        ),
        target = SrTarget(
            todayTarget = target.todaysTarget ?: 0.0,
            todayOrder = target.todaysOrder ?: 0.0,
            todayAchievement = target.todaysAchievement ?: 0.0,
            tillDateTarget = target.tillDateTarget ?: 0.0,
            tillDateOrder = target.tillDateOrder ?: 0.0,
            tillDateAchievement = target.tillDateAchievement ?: 0.0
        ),
        outlets = SrOutletStatus(
            activeOutlets = outlets.activeOutlet ?: 0,
            zeroSalesOutlets = outlets.zeroSalesOutlet ?: 0,
            activeSkus = outlets.activeSku ?: 0,
            zeroSalesSkus = outlets.zeroSalesSku ?: 0
        )
    )
}

/**
 * Accepts ISO date-time ("2026-11-08T09:15:00", with or without fraction), a bare time ("09:15[:00]"),
 * or a 12-hour time ("08:35 AM", as getSrPerformance sends).
 */
private fun String?.toDateTimeOrNull(today: LocalDate): LocalDateTime? {
    val text = this?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    runCatching { return LocalDateTime.parse(text) }
    runCatching { return LocalDateTime(today, LocalTime.parse(text)) }
    text.toTwelveHourTimeOrNull()?.let { return LocalDateTime(today, it) }
    return null
}

private val twelveHourTime = Regex("""^(\d{1,2}):(\d{2})(?::(\d{2}))?\s*([AaPp])\.?[Mm]\.?$""")

private fun String.toTwelveHourTimeOrNull(): LocalTime? {
    val (h, m, s, meridiem) = twelveHourTime.matchEntire(this)?.destructured ?: return null
    val hour12 = h.toInt().takeIf { it in 1..12 } ?: return null
    val hour24 = (hour12 % 12) + if (meridiem.equals("p", ignoreCase = true)) 12 else 0
    return runCatching { LocalTime(hour24, m.toInt(), s.ifEmpty { "0" }.toInt()) }.getOrNull()
}

fun TodaySummaryDataDto.toDomain() = TodaySummary(
    attendance = (attendance ?: AttendanceStatsDto()).toDomain(),
    productivity = productivityStats(callProductivity, skuProductivity, memo),
    today = runningTrack?.targetAchievement.let {
        targetPeriod(it?.todaysTarget, it?.todaysOrder, it?.todaysAchievement)
    },
    tillDate = runningTrack?.totalTargetAchievement.let {
        targetPeriod(it?.totalTarget, it?.totalOrder, it?.totalAchievement)
    },
    inventory = (inventoryStatus ?: InventoryStatsDto()).let {
        InventoryStats(
            currentStockValue = it.stockValue ?: 0.0,
            openingStockValue = it.openingStock ?: 0.0,
            dbCount = it.dbCount ?: 0,
            outOfStockSkuCount = it.zeroStock ?: 0,
            currentStockQty = it.currentStock ?: 0.0
        )
    },
    outlets = (outletStatus ?: OutletStatusDto()).let {
        ZeroSalesStats(
            activeOutlets = it.activeOutlet ?: 0,
            zeroSalesOutlets = it.zeroSalesOutlet ?: 0,
            activeSkus = it.activeSku ?: 0,
            zeroSalesSkus = it.zeroSalesSku ?: 0,
            orderCount = it.orderCount ?: 0
        )
    }
)

/** The order-summary API sends no period dates for running track. */
private fun targetPeriod(target: Double?, order: Double?, achievementPercent: Double?) = TargetPeriod(
    target = target ?: 0.0,
    order = order ?: 0.0,
    achievementPercent = achievementPercent ?: 0.0,
    fromDate = null,
    toDate = null
)
