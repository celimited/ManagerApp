package org.celimited.manager.core.common.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import kotlin.coroutines.resume

class AndroidLocationProvider(private val context: Context) : LocationProvider {

    private val locationManager: LocationManager?
        get() = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    @SuppressLint("MissingPermission") // Callers request permission first; SecurityException is caught below.
    override suspend fun getCurrentLocation(): DataResult<GeoLocation> {
        val manager = locationManager
            ?: return DataResult.Error(AppError.Business("Location service is unavailable on this device."))

        val hasFine = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        val hasCoarse = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) {
            return DataResult.Error(AppError.Business("Location permission is required to mark attendance."))
        }

        // GPS needs precise permission; with approximate only, fall back to the network provider.
        val providers = buildList {
            if (hasFine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        if (providers.isEmpty()) {
            return DataResult.Error(AppError.Business("Please turn on location and try again."))
        }

        return try {
            val recent = providers
                .mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
                .filter { it.ageMillis() <= MAX_LAST_KNOWN_AGE_MILLIS }
                .maxByOrNull { it.elapsedRealtimeNanos }

            val location = recent ?: withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
                requestSingleFix(manager, providers.first())
            }

            if (location != null) {
                DataResult.Success(GeoLocation(location.latitude, location.longitude))
            } else {
                DataResult.Error(AppError.Business("Couldn't get your current location. Please try again."))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            DataResult.Error(AppError.Business("Location permission is required to mark attendance."))
        } catch (e: Exception) {
            DataResult.Error(AppError.Unknown(e.message))
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestSingleFix(manager: LocationManager, provider: String): Location? =
        suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    // Required on API < 30 where these are abstract.
                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) = Unit
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }
        }

    private fun Location.ageMillis(): Long =
        (SystemClock.elapsedRealtimeNanos() - elapsedRealtimeNanos) / 1_000_000

    private companion object {
        const val MAX_LAST_KNOWN_AGE_MILLIS = 2 * 60 * 1000L
        const val LOCATION_TIMEOUT_MILLIS = 15_000L
    }
}
