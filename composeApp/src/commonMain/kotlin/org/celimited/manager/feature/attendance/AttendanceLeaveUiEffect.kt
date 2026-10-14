package org.celimited.manager.feature.attendance

sealed interface AttendanceLeaveUiEffect {
    /** Ask for location permission; the answer comes back via onLocationPermissionResult. */
    data object RequestLocationPermission : AttendanceLeaveUiEffect
}
