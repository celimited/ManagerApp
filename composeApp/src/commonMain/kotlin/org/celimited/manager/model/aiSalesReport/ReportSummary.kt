package org.celimited.manager.model.aiSalesReport

data class ReportSummary(
    val summary: String,
    val highlights: List<String>,
    val actions: List<String>,
    val usageRemaining: Int
)
