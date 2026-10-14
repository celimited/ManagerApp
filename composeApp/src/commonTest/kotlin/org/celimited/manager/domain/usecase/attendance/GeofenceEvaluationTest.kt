package org.celimited.manager.domain.usecase.attendance

import org.celimited.manager.core.common.location.GeoLocation
import org.celimited.manager.core.common.location.haversineMeters
import org.celimited.manager.model.attendance.CheckInOption
import org.celimited.manager.model.attendance.Distributor
import org.celimited.manager.model.attendance.GeofenceConfig
import org.celimited.manager.model.attendance.GeofenceResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeofenceEvaluationTest {

    private val distributorLocation = GeoLocation(23.780636, 90.279541)
    private val distributor = Distributor(
        id = 7645,
        name = "M/S. SHATHI ENTERPRISE",
        code = "0021137541",
        address = "KOLAROA, SATKHIRA",
        location = distributorLocation
    )

    // ~0.001° of latitude is ~111 m.
    private val nearby = GeoLocation(23.780836, 90.279541) // ~22 m away
    private val farAway = GeoLocation(23.781636, 90.279541) // ~111 m away

    private fun config(option: CheckInOption, radius: Int = 50) =
        listOf(GeofenceConfig(salespointId = 7645, checkInOption = option, defaultRadiusMeters = radius))

    @Test
    fun haversineMatchesKnownDistance() {
        // Dhaka to Chattogram, ~209.2 km great-circle.
        val dhaka = GeoLocation(23.7389, 90.3953)
        val chattogram = GeoLocation(22.3569, 91.7832)
        val km = haversineMeters(dhaka, chattogram) / 1000
        assertTrue(abs(km - 209.2) < 0.5, "unexpected distance $km km")
        assertEquals(0.0, haversineMeters(dhaka, dhaka))
    }

    @Test
    fun insideRadiusIsAllowedForEveryOption() {
        CheckInOption.entries.forEach { option ->
            assertEquals(GeofenceResult.Allowed, evaluateGeofence(distributor, config(option), nearby))
        }
    }

    @Test
    fun outsideRadiusWithBlockIsBlocked() {
        val result = evaluateGeofence(distributor, config(CheckInOption.BLOCK), farAway)
        assertIs<GeofenceResult.Blocked>(result)
        assertEquals(50, result.radiusMeters)
        assertTrue(result.distanceMeters in 100..120)
    }

    @Test
    fun outsideRadiusWithWarningNeedsConfirmation() {
        assertIs<GeofenceResult.NeedsConfirmation>(
            evaluateGeofence(distributor, config(CheckInOption.WARNING), farAway)
        )
    }

    @Test
    fun outsideRadiusWithBypassIsAllowed() {
        assertEquals(GeofenceResult.Allowed, evaluateGeofence(distributor, config(CheckInOption.BYPASS), farAway))
    }

    @Test
    fun noMatchingConfigIsAllowed() {
        val otherSalespoint = listOf(GeofenceConfig(1, CheckInOption.BLOCK, 50))
        assertEquals(GeofenceResult.Allowed, evaluateGeofence(distributor, otherSalespoint, farAway))
    }

    @Test
    fun distributorWithoutCoordinatesIsAllowed() {
        val noLocation = distributor.copy(location = null)
        assertEquals(GeofenceResult.Allowed, evaluateGeofence(noLocation, config(CheckInOption.BLOCK), farAway))
    }

    @Test
    fun nonPositiveRadiusIsAllowed() {
        assertEquals(GeofenceResult.Allowed, evaluateGeofence(distributor, config(CheckInOption.BLOCK, 0), farAway))
    }
}
