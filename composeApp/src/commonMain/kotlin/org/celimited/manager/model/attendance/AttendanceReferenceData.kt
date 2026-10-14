package org.celimited.manager.model.attendance

import org.celimited.manager.core.common.location.GeoLocation

data class LateReason(
    val id: Int,
    val name: String
)

data class Distributor(
    val id: Int,
    val name: String,
    val code: String,
    val address: String,
    /** Null when the API sent blank or invalid coordinates. */
    val location: GeoLocation?
)

enum class CheckInOption { BLOCK, WARNING, BYPASS }

data class GeofenceConfig(
    val salespointId: Int,
    val checkInOption: CheckInOption,
    val defaultRadiusMeters: Int
)

/** Everything the attendance popup needs, cached for the session. Null pieces failed to load. */
data class AttendanceSessionData(
    val todayAttendance: TodayAttendance? = null,
    val lateReasons: List<LateReason>? = null,
    val geofenceConfigs: List<GeofenceConfig>? = null,
    val distributors: List<Distributor>? = null,
    /** Kept apart from [todayAttendance] so a today-status refresh doesn't drop it. */
    val localCheckIn: LocalCheckIn? = null
) {
    val isComplete: Boolean
        get() = todayAttendance != null && lateReasons != null &&
            geofenceConfigs != null && distributors != null
}
