package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class AIReportDataDto(
    val aiResponses: List<JsonObject> = emptyList(),
    val usageRemaining: Int = 0,
    /** The log row the server created for this query — sent back when asking for a summary of it. */
    val queryLogID: Int = 0
)
