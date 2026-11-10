package org.celimited.manager.data.mapper.home

import kotlinx.serialization.json.Json
import org.celimited.manager.core.network.ApiEnvelope
import org.celimited.manager.data.remote.dto.home.DashboardInfoDataDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DashboardInfoMappersTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun decodesLiveDashboardInfoResponse() {
        val body = """
            {
              "data": {
                "toDayOrder": {
                  "totalSr": 3, "presentSr": 0, "lastActiveSR": 0, "orderStarted": 0,
                  "idleSr": 0, "callProductivity": 0, "visitProductivity": 0, "lpc": 0.0
                },
                "mtdPerformance": {
                  "mtdAchive": 0.00000, "ytdAchive": 100.0000, "tillDateTarget": 0.0000,
                  "mtdGrowth": 702.4300, "ytdGrowth": 100.0000, "m2m": 100.0000,
                  "mtdSalesValue": 4122396.0000, "ytdSalesValue": 0.00, "strikeRate": 20.32,
                  "lpc": 4.87, "orderVsDelivery": 84.74, "coverage": 19.82
                }
              },
              "success": true,
              "statusCode": 200,
              "message": "Operation completed successfully."
            }
        """.trimIndent()

        val envelope = json.decodeFromString<ApiEnvelope<DashboardInfoDataDto>>(body)
        val info = assertNotNull(envelope.data).toDomain()

        assertEquals(3, info.todayOrder.totalSr)
        assertEquals(0, info.todayOrder.presentSr)
        val mtd = assertNotNull(info.mtdPerformance)
        assertEquals(702.43, mtd.mtdGrowth)
        assertEquals(4122396.0, mtd.mtdSalesValue)
        assertEquals(19.82, mtd.coverage)
    }
}
