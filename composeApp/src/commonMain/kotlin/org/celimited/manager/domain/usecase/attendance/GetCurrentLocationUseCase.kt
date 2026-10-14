package org.celimited.manager.domain.usecase.attendance

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.location.GeoLocation
import org.celimited.manager.core.common.location.LocationProvider

class GetCurrentLocationUseCase(
    private val locationProvider: LocationProvider
) {
    suspend operator fun invoke(): DataResult<GeoLocation> = locationProvider.getCurrentLocation()
}
