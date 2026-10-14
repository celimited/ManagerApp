package org.celimited.manager.core.common.location

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.Foundation.NSError
import platform.darwin.NSObject
import kotlin.coroutines.resume

class IosLocationProvider : LocationProvider {

    // CLLocationManager holds its delegate weakly, so the in-flight request's manager and delegate
    // are kept here until it completes. Only touched on the main thread.
    private var activeManager: CLLocationManager? = null
    private var activeDelegate: SingleLocationDelegate? = null

    override suspend fun getCurrentLocation(): DataResult<GeoLocation> = withContext(Dispatchers.Main) {
        if (!CLLocationManager.locationServicesEnabled()) {
            return@withContext DataResult.Error(AppError.Business("Please turn on location and try again."))
        }

        withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine { continuation ->
                val manager = CLLocationManager()
                val status = manager.authorizationStatus
                if (status != kCLAuthorizationStatusAuthorizedWhenInUse &&
                    status != kCLAuthorizationStatusAuthorizedAlways
                ) {
                    continuation.resume(
                        DataResult.Error(AppError.Business("Location permission is required to mark attendance."))
                    )
                    return@suspendCancellableCoroutine
                }

                val delegate = SingleLocationDelegate { result ->
                    release()
                    if (continuation.isActive) continuation.resume(result)
                }
                activeManager = manager
                activeDelegate = delegate
                manager.delegate = delegate
                manager.desiredAccuracy = kCLLocationAccuracyBest
                continuation.invokeOnCancellation { release() }
                manager.requestLocation()
            }
        } ?: DataResult.Error(AppError.Business("Couldn't get your current location. Please try again."))
    }

    private fun release() {
        activeManager?.stopUpdatingLocation()
        activeManager?.delegate = null
        activeManager = null
        activeDelegate = null
    }

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 15_000L
    }
}

private class SingleLocationDelegate(
    private val onResult: (DataResult<GeoLocation>) -> Unit
) : NSObject(), CLLocationManagerDelegateProtocol {

    @OptIn(ExperimentalForeignApi::class)
    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        val geo = location.coordinate.useContents { GeoLocation(latitude, longitude) }
        onResult(DataResult.Success(geo))
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        onResult(DataResult.Error(AppError.Business("Couldn't get your current location. Please try again.")))
    }
}
