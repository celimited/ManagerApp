package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray

/** PascalCase property names deliberately match the backend contract, as in [AIReportRequestDto]. */
@Serializable
data class AISummaryRequestDto(
    val UserID: String,
    val Role: String,
    val UserPrompt: String,
    val QueryLogID: Int,
    /** The result rows, keyed by column name — the data the summary is drawn from. */
    val Data: JsonArray
)
