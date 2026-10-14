package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable

@Serializable
data class AISummaryResponseDto(
    val data: AISummaryDataDto? = null,
    val statusCode: Int = 0,
    val message: String? = null
)

@Serializable
data class AISummaryDataDto(
    val aiSummary: AISummaryDto? = null,
    val usageRemaining: Int = 0,
    val summaryLogID: Int = 0
)

@Serializable
data class AISummaryDto(
    val summary: String = "",
    val highlights: List<String> = emptyList(),
    val actions: List<String> = emptyList(),
    val message: String = ""
)
