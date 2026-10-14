package org.celimited.manager.domain.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.model.aiSalesReport.QueryLogHistory
import org.celimited.manager.model.aiSalesReport.ReportSummary
import org.celimited.manager.model.aiSalesReport.SalesReportResult

interface AISalesReportRepository {
    suspend fun askAI(
        userId: String,
        role: String,
        prompt: String,
        queryLogId: Int = 0
    ): DataResult<SalesReportResult>

    /** Asks the AI to interpret a result it already produced. */
    suspend fun askForSummary(
        userId: String,
        role: String,
        prompt: String,
        queryLogId: Int,
        columns: List<String>,
        rows: List<List<String>>
    ): DataResult<ReportSummary>

    suspend fun getPreviousPrompts(userId: String): DataResult<QueryLogHistory>

    /** Suggested starter prompts. Same shape as [getPreviousPrompts] — the backend returns the
     *  query-log envelope for both. */
    suspend fun getAIFAQs(userId: String): DataResult<QueryLogHistory>
}
