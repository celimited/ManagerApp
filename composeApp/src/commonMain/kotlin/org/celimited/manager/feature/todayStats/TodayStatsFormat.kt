package org.celimited.manager.feature.todayStats

import kotlinx.datetime.LocalDateTime
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.format.formatCompact
import org.celimited.manager.core.common.format.formatTakaCompact
import org.celimited.manager.core.common.format.formatTimeOfDay
import org.celimited.manager.model.todayStats.OrderBreakdownItem
import org.celimited.manager.model.todayStats.OrderGroupBy
import kotlin.math.abs
import kotlin.math.round

// Domain -> display formatting shared by TodayStatsViewModel and SrDetailViewModel.

internal fun BreakdownGroup.toOrderGroupBy(): OrderGroupBy = when (this) {
    BreakdownGroup.Project -> OrderGroupBy.PROJECT
    BreakdownGroup.Product -> OrderGroupBy.PRODUCT
    BreakdownGroup.Sku -> OrderGroupBy.SKU
    BreakdownGroup.SkuWise -> OrderGroupBy.SKU_WISE
}

internal fun OrderBreakdownItem.toUi() = BreakdownItemUi(
    id = id,
    name = name,
    memo = "$memoCount/$totalMemoCount",
    memoPercent = percentText(memoPercent),
    orderVsTargetUom = percentText(orderVsTargetQtyPercent),
    orderValue = formatTakaCompact(orderValue)
)

/** 273.0 → "273", 1371.0 → "1.4K". */
internal fun countText(value: Double): String = formatCompact(value.toFloat())

internal fun countText(value: Int): String = countText(value.toDouble())

/** Up to two decimals, trailing zeros dropped: 17.54 → "17.54", 4.6 → "4.6", 80.0 → "80". */
internal fun decimalText(value: Double): String {
    val hundredths = round(abs(value) * 100).toLong()
    val whole = hundredths / 100
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    val sign = if (value < 0 && hundredths != 0L) "-" else ""
    return if (fraction.isEmpty()) "$sign$whole" else "$sign$whole.$fraction"
}

internal fun percentText(value: Double): String = "${decimalText(value)}%"

/** "8:45 AM", or "--" when it hasn't happened yet. */
internal fun timeText(value: LocalDateTime?): String = value?.let(::formatTimeOfDay) ?: "--"

/** 90 → "1h 30m", 45 → "45m". */
internal fun durationText(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "${rest}m"
        rest == 0 -> "${hours}h"
        else -> "${hours}h ${rest}m"
    }
}

/** Fraction for progress indicators, clamped so >100% doesn't overflow. */
internal fun Int.toProgress(): Float = (this / 100f).coerceIn(0f, 1f)

// Prefer whatever message the API itself returned; fall back to a generic
// notice only when there's no server response to read one from at all.
internal fun AppError.toUserMessage(): String = when (this) {
    is AppError.Business -> message
    is AppError.Server -> message ?: "Something went wrong. Please try again."
    is AppError.Unauthorized -> message ?: "Something went wrong. Please try again."
    is AppError.Unknown -> message ?: "Something went wrong. Please try again."
    is AppError.NoConnection -> "No internet connection. Please try again."
    is AppError.Timeout -> "The request timed out. Please try again."
    is AppError.Serialization -> "Unexpected response from server."
}
