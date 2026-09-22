package org.celimited.manager.domain.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.model.aiSalesReport.QueryLogHistory
import org.celimited.manager.model.aiSalesReport.SalesReportResult

interface AISalesReportRepository {
    suspend fun askAI(
        userId: String,
        role: String,
        prompt: String,
        queryLogId: Int = 0
    ): DataResult<SalesReportResult>

    suspend fun getPreviousPrompts(userId: String): DataResult<QueryLogHistory>
}
