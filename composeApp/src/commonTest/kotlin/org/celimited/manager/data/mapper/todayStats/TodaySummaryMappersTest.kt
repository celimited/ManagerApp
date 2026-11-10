package org.celimited.manager.data.mapper.todayStats

import kotlinx.serialization.json.Json
import org.celimited.manager.core.network.ApiEnvelope
import org.celimited.manager.data.remote.dto.todayStats.TodaySummaryDataDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TodaySummaryMappersTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun outletStatusUsesTheApisSingularKeys() {
        val body = """
            {
              "data": {
                "outletStatus": {
                  "activeOutlet": 352, "zeroSalesOutlet": 42, "activeSku": 736,
                  "zeroSalesSku": 368, "srId": 0, "mhId": 0, "mhName": null
                },
                "parentIds": [2243]
              },
              "success": true,
              "statusCode": 200,
              "message": "Operation completed successfully."
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<TodaySummaryDataDto>>(body)
        val outlets = assertNotNull(envelope.data).toDomain().outlets

        assertEquals(352, outlets.activeOutlets)
        assertEquals(42, outlets.zeroSalesOutlets)
        assertEquals(736, outlets.activeSkus)
        assertEquals(368, outlets.zeroSalesSkus)
    }
}
