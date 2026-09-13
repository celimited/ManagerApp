package org.celimited.manager.domain.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.model.aiSalesReport.SalesReportResult

interface AISalesReportRepository {
    suspend fun askAI(userId: String, role: String, prompt: String): DataResult<SalesReportResult>
}
