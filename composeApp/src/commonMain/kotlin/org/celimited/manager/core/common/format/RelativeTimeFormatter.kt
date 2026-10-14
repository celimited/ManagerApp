package org.celimited.manager.core.common.format

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private val MONTH_NAMES = arrayOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)

private const val MINUTE = 60L
private const val HOUR = 3_600L
private const val DAY = 86_400L
private const val WEEK = 604_800L
private const val MONTH = 2_592_000L

/**
 * Stays relative all the way out — never falls back to a date, because callers pair this with a
 * date heading, and a date in both places just reads as a repetition.
 */
@OptIn(ExperimentalTime::class)
fun formatRelativeTime(dateTime: LocalDateTime): String {
    val zone = TimeZone.currentSystemDefault()
    val now = Clock.System.now()
    val then = dateTime.toInstant(zone)
    val seconds = (now - then).inWholeSeconds

    return when {
        seconds < MINUTE -> "Just now"
        seconds < HOUR -> "${seconds / MINUTE}m ago"
        seconds < DAY -> "${seconds / HOUR}h ago"
        seconds < WEEK -> "${seconds / DAY}d ago"
        seconds < MONTH -> "${seconds / WEEK}w ago"
        else -> "${seconds / MONTH}mo ago"
    }
}

/** Heading for a day's worth of items: "Today", "Yesterday", or the date itself. */
@OptIn(ExperimentalTime::class)
fun formatDayGroup(dateTime: LocalDateTime): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    return when (dateTime.date) {
        today -> "Today"
        today.minus(DatePeriod(days = 1)) -> "Yesterday"
        else -> formatShortDate(dateTime)
    }
}

/** "Sep 22, 2026 - 3:47 PM" */
fun formatDateTime(dateTime: LocalDateTime): String =
    "${formatShortDate(dateTime)} · ${formatTimeOfDay(dateTime)}"

internal fun formatShortDate(dateTime: LocalDateTime): String = formatShortDate(dateTime.date)

/** "Feb 15, 2026" */
fun formatShortDate(date: LocalDate): String {
    val month = MONTH_NAMES[date.month.number - 1]
    return "$month ${date.day}, ${date.year}"
}

/** "6 Jan" */
fun formatDayMonth(date: LocalDate): String = "${date.day} ${MONTH_NAMES[date.month.number - 1]}"

/** "3:47 PM".Kotlin common has no date formatter, so the 12-hour clock is assembled by hand. */
fun formatTimeOfDay(dateTime: LocalDateTime): String {
    val hour = when (val h = dateTime.hour % 12) {
        0 -> 12
        else -> h
    }
    val minute = dateTime.minute.toString().padStart(2, '0')
    val meridiem = if (dateTime.hour < 12) "AM" else "PM"
    return "$hour:$minute $meridiem"
}
