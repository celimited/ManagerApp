package org.celimited.manager.core.common.format

import kotlin.math.abs
import kotlin.math.round

// Kotlin common has no String.format / DecimalFormat, so compact notation is assembled by hand.
fun formatCompact(value: Float): String {
    val magnitude = abs(value)
    val (scaled, suffix) = when {
        magnitude >= 1_000_000_000f -> value / 1_000_000_000f to "B"
        magnitude >= 1_000_000f -> value / 1_000_000f to "M"
        magnitude >= 1_000f -> value / 1_000f to "K"
        else -> value to ""
    }
    return oneDecimal(scaled) + suffix
}

/**
 * Taka in the South-Asian compact form used across the app: 3909000 → "৳39.09L",
 * 28000000 → "৳2.8Cr", 95000 → "৳95,000". Up to two decimals, trailing zeros dropped.
 */
fun formatTakaCompact(value: Double): String {
    val magnitude = abs(value)
    val sign = if (value < 0) "-" else ""
    val body = when {
        magnitude >= 10_000_000 -> twoDecimals(magnitude / 10_000_000) + "Cr"
        magnitude >= 100_000 -> twoDecimals(magnitude / 100_000) + "L"
        else -> groupThousands(round(magnitude).toLong())
    }
    return "$sign৳$body"
}

private fun twoDecimals(value: Double): String {
    val hundredths = round(value * 100).toLong()
    val whole = hundredths / 100
    val fraction = (hundredths % 100).toString().padStart(2, '0').trimEnd('0')
    return if (fraction.isEmpty()) "$whole" else "$whole.$fraction"
}

private fun groupThousands(value: Long): String =
    value.toString().reversed().chunked(3).joinToString(",").reversed()

private fun oneDecimal(value: Float): String {
    val rounded = round(abs(value) * 10f) / 10f
    val whole = rounded.toLong()
    val fraction = round((rounded - whole) * 10f).toLong()
    val sign = if (value < 0f && rounded != 0f) "-" else ""
    return if (fraction == 0L) "$sign$whole" else "$sign$whole.$fraction"
}
