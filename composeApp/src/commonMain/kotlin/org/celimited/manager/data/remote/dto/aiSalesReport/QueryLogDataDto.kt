package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable

@Serializable
data class QueryLogDataDto(
    val aiResponses: List<QueryLogItemDto> = emptyList(),
    val usageRemaining: Int = 0
)
