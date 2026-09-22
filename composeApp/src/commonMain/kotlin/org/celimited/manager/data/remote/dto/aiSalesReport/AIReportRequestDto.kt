package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable

@Serializable
data class AIReportRequestDto(
    val UserID: String,
    val Role: String,
    val UserPrompt: String,
    val QueryLogID: Int
)
