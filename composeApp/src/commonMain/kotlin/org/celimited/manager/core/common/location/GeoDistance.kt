package org.celimited.manager.core.common.location

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0

/** Great-circle (haversine) distance between two points, in meters. */
fun haversineMeters(from: GeoLocation, to: GeoLocation): Double {
    val lat1 = from.latitude.toRadians()
    val lat2 = to.latitude.toRadians()
    val dLat = (to.latitude - from.latitude).toRadians()
    val dLon = (to.longitude - from.longitude).toRadians()

    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_METERS * asin(min(1.0, sqrt(a)))
}

private fun Double.toRadians(): Double = this * PI / 180.0
