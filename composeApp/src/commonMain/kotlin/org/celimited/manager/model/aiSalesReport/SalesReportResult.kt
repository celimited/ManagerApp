package org.celimited.manager.model.aiSalesReport

data class SalesReportResult(
    val columns: List<String>,
    val rows: List<List<String>>,
    val usageRemaining: Int,
    /** Server-assigned id for this query, needed when asking for a summary of the result. */
    val queryLogId: Int
)
