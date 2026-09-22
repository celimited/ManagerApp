package org.celimited.manager.data.mapper.aiSalesReport

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.jsonPrimitive
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportResponseDto
import org.celimited.manager.data.remote.dto.aiSalesReport.GetQueryLogResponseDto
import org.celimited.manager.data.remote.dto.aiSalesReport.QueryLogItemDto
import org.celimited.manager.model.aiSalesReport.QueryLog
import org.celimited.manager.model.aiSalesReport.QueryLogHistory
import org.celimited.manager.model.aiSalesReport.SalesReportResult

fun AIReportResponseDto.toDomain(): SalesReportResult {
    val dataRows = data?.aiResponses.orEmpty()
    val columns = dataRows.firstOrNull()?.keys?.toList().orEmpty()
    val rows = dataRows.map { row ->
        columns.map { column -> row[column]?.jsonPrimitive?.content.orEmpty() }
    }
    return SalesReportResult(
        columns = columns,
        rows = rows,
        usageRemaining = data?.usageRemaining ?: 0
    )
}

fun QueryLogItemDto.toDomain(): QueryLog = QueryLog(
    queryLogId = queryLogID,
    userPrompt = userPrompt,
    executedAt = LocalDateTime.parse(executedAt)
)

fun GetQueryLogResponseDto.toDomain(): QueryLogHistory {
    val queryLogData = data
    return QueryLogHistory(
        logs = queryLogData?.aiResponses.orEmpty().map { it.toDomain() },
        usageRemaining = queryLogData?.usageRemaining ?: 0
    )
}
