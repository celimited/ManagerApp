package org.celimited.manager.feature.teamAttendance

sealed interface TeamAttendanceUiEffect {
    data object SessionExpired : TeamAttendanceUiEffect
}
