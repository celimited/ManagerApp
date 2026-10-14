package org.celimited.manager.model.aiSalesReport

import kotlinx.datetime.LocalDateTime

data class QueryLog(
    val queryLogId: Int,
    val userPrompt: String,
    val executedAt: LocalDateTime
)
