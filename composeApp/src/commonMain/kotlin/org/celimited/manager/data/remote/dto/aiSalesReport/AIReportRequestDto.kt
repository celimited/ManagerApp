package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable

@Serializable
data class AIReportRequestDto(
    val userID: String,
    val role: String,
    val userPrompt: String
)
