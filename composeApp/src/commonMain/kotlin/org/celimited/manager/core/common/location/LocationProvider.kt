package org.celimited.manager.core.common.location

import org.celimited.manager.core.common.DataResult

data class GeoLocation(
    val latitude: Double,
    val longitude: Double
)

interface LocationProvider {
    /** One-shot current device location. Assumes location permission was already granted. */
    suspend fun getCurrentLocation(): DataResult<GeoLocation>
}
