package org.celimited.manager.model.attendance

sealed interface AttendancePopupDecision {
    data object None : AttendancePopupDecision
    /** [minutesRemaining] is null when the deadline couldn't be read. */
    data class MarkAttendance(val minutesRemaining: Long?) : AttendancePopupDecision
    data object Late : AttendancePopupDecision
    data class OnLeave(val leave: LeaveInfo) : AttendancePopupDecision
    data class LeavePending(val leave: LeaveInfo) : AttendancePopupDecision
    data object WrongDeviceTime : AttendancePopupDecision
}

data class AttendancePopupContent(
    val decision: AttendancePopupDecision,
    val distributors: List<Distributor>,
    val lateReasons: List<LateReason>
)

sealed interface GeofenceResult {
    /** Inside the radius, or the distributor has no geofence to check against. */
    data object Allowed : GeofenceResult
    data class Blocked(val distanceMeters: Int, val radiusMeters: Int) : GeofenceResult
    data class NeedsConfirmation(val distanceMeters: Int, val radiusMeters: Int) : GeofenceResult
}

data class SaveAttendanceParams(
    val salespointId: Int,
    val latitude: Double,
    val longitude: Double,
    val lateReason: LateReason?,
    val remarks: String?
)

/** What the Home "Attendance" card shows. */
sealed interface AttendanceCardStatus {
    /** today-status hasn't loaded, or returned a status we don't recognise. */
    data object Unknown : AttendanceCardStatus
    data object NotGiven : AttendanceCardStatus
    data object TimePassed : AttendanceCardStatus
    /** [epochMillis] and [isLate] are null when the check-in wasn't made on this device this session. */
    data class CheckedIn(val epochMillis: Long?, val isLate: Boolean?) : AttendanceCardStatus
    data object LeavePending : AttendanceCardStatus
    data object OnLeave : AttendanceCardStatus
}
