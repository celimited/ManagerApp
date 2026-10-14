package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable

@Serializable
data class QueryLogItemDto(
    val queryLogID: Int = 0,
    val userPrompt: String = "",
    val executedAt: String = ""
)
