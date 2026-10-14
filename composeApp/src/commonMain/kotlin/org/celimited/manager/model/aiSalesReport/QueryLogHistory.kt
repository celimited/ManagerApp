package org.celimited.manager.model.aiSalesReport

data class QueryLogHistory(
    val logs: List<QueryLog>,
    val usageRemaining: Int
)
