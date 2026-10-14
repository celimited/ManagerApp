package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.flow.first
import org.celimited.manager.core.common.location.GeoLocation
import org.celimited.manager.core.common.location.haversineMeters
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.CheckInOption
import org.celimited.manager.model.attendance.Distributor
import org.celimited.manager.model.attendance.GeofenceConfig
import org.celimited.manager.model.attendance.GeofenceResult
import kotlin.math.roundToInt

/** Checks the current location against the selected distributor's geofence. */
class CheckGeofenceUseCase(
    private val repository: AttendanceRepository
) {
    suspend operator fun invoke(distributorId: Int, currentLocation: GeoLocation): GeofenceResult {
        val data = repository.observeAttendanceData().first()
        val distributor = data?.distributors?.firstOrNull { it.id == distributorId }
        return evaluateGeofence(distributor, data?.geofenceConfigs.orEmpty(), currentLocation)
    }
}

/**
 * The geofence only applies when the distributor has a matching config, valid coordinates and a
 * positive radius. Outside the radius, the config's check-in option decides what happens.
 */
internal fun evaluateGeofence(
    distributor: Distributor?,
    configs: List<GeofenceConfig>,
    currentLocation: GeoLocation
): GeofenceResult {
    val target = distributor?.location ?: return GeofenceResult.Allowed
    val config = configs.firstOrNull { it.salespointId == distributor.id } ?: return GeofenceResult.Allowed
    val radius = config.defaultRadiusMeters
    if (radius <= 0) return GeofenceResult.Allowed

    val distance = haversineMeters(currentLocation, target)
    if (distance <= radius) return GeofenceResult.Allowed

    return when (config.checkInOption) {
        CheckInOption.BLOCK -> GeofenceResult.Blocked(distance.roundToInt(), radius)
        CheckInOption.WARNING -> GeofenceResult.NeedsConfirmation(distance.roundToInt(), radius)
        CheckInOption.BYPASS -> GeofenceResult.Allowed
    }
}
