package org.celimited.manager.model.attendance

enum class DayAttendanceStatus { PRESENT, LATE, LEAVE, ABSENT, NONE }

data class MonthlyAttendanceSummary(
    /** 0–100. */
    val percentage: Int,
    val present: Int,
    val late: Int,
    val leave: Int,
    val absent: Int
)

data class MonthlyAttendance(
    val summary: MonthlyAttendanceSummary,
    /** Keyed by day of month. Days the API didn't list are absent from the map. */
    val days: Map<Int, DayAttendanceStatus>
)
