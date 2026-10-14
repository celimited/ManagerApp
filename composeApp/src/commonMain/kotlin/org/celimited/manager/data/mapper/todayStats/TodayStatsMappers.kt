package org.celimited.manager.data.mapper.todayStats

import kotlinx.datetime.LocalDateTime
import org.celimited.manager.data.remote.dto.todayStats.AttendanceStatsDto
import org.celimited.manager.data.remote.dto.todayStats.CallProductivityDto
import org.celimited.manager.data.remote.dto.todayStats.MemoMetricsDto
import org.celimited.manager.data.remote.dto.todayStats.ProductivityStatsDto
import org.celimited.manager.data.remote.dto.todayStats.SkuProductivityDto
import org.celimited.manager.data.remote.dto.todayStats.SrListDataDto
import org.celimited.manager.data.remote.dto.todayStats.SrStatDto
import org.celimited.manager.data.remote.dto.todayStats.TodayAttendanceDataDto
import org.celimited.manager.data.remote.dto.todayStats.TodayProductivityDataDto
import org.celimited.manager.data.remote.dto.todayStats.UnitAttendanceDto
import org.celimited.manager.model.todayStats.AttendanceStats
import org.celimited.manager.model.todayStats.CallProductivity
import org.celimited.manager.model.todayStats.MemoMetrics
import org.celimited.manager.model.todayStats.ProductivityStats
import org.celimited.manager.model.todayStats.SectionStats
import org.celimited.manager.model.todayStats.SkuProductivity
import org.celimited.manager.model.todayStats.SrPage
import org.celimited.manager.model.todayStats.SrSummary
import org.celimited.manager.model.todayStats.UnitStats

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
        SkuProductivity(lpc = it.lpc ?: 0.0, tlsd = it.tlsd ?: 0.0, pCase = it.pCase ?: 0.0)
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

fun SrListDataDto.toDomain(requestedPage: Int): SrPage {
    val srs = items.orEmpty().mapNotNull { it.toDomain() }
    return SrPage(items = srs, page = page ?: requestedPage, totalCount = totalCount ?: srs.size)
}

private fun SrStatDto.toDomain(): SrSummary? {
    val id = srId ?: return null
    return SrSummary(
        srId = id,
        name = name.orEmpty(),
        photoUrl = photoUrl?.takeIf { it.isNotBlank() },
        locationName = locationName.orEmpty(),
        territoryName = territoryName.orEmpty(),
        isOnline = isOnline ?: false,
        checkInAt = checkInAt.toLocalDateTimeOrNull(),
        firstOrderAt = firstOrderAt.toLocalDateTimeOrNull(),
        lastOrderAt = lastOrderAt.toLocalDateTimeOrNull(),
        strikeRate = strikeRate ?: 0.0,
        productiveRate = productiveRate ?: 0.0,
        lpc = lpc ?: 0.0,
        orderAchievementPercent = orderAchievementPercent ?: 0.0
    )
}

private fun String?.toLocalDateTimeOrNull(): LocalDateTime? =
    this?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }
