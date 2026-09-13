package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AIReportResponseDto(
    val data: List<JsonObject> = emptyList(),
    val statusCode: Int = 0,
    val message: String? = null
)
