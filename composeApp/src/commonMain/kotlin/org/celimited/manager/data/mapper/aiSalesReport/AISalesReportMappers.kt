package org.celimited.manager.data.mapper.aiSalesReport

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportResponseDto
import org.celimited.manager.data.remote.dto.aiSalesReport.AISummaryResponseDto
import org.celimited.manager.data.remote.dto.aiSalesReport.GetQueryLogResponseDto
import org.celimited.manager.data.remote.dto.aiSalesReport.QueryLogItemDto
import org.celimited.manager.model.aiSalesReport.QueryLog
import org.celimited.manager.model.aiSalesReport.QueryLogHistory
import org.celimited.manager.model.aiSalesReport.ReportSummary
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
        usageRemaining = data?.usageRemaining ?: 0,
        queryLogId = data?.queryLogID ?: 0
    )
}

fun AISummaryResponseDto.toDomain(): ReportSummary {
    val summary = data?.aiSummary
    return ReportSummary(
        summary = summary?.summary.orEmpty(),
        highlights = summary?.highlights.orEmpty(),
        actions = summary?.actions.orEmpty(),
        usageRemaining = data?.usageRemaining ?: 0
    )
}

/**
 * Rebuilds the result rows as the JSON the summary endpoint expects — one object per row, keyed by
 * column name.
 *
 * The rows reach the domain as strings ([AIReportResponseDto.toDomain] flattens the original
 * objects), so the value types are re-derived here rather than carrying `JsonObject` through the
 * domain and UI layers.
 */
internal fun buildSummaryData(columns: List<String>, rows: List<List<String>>): JsonArray =
    JsonArray(
        rows.map { row ->
            JsonObject(
                columns.mapIndexed { index, column ->
                    column to row.getOrNull(index).orEmpty().toJsonValue()
                }.toMap()
            )
        }
    )

private fun String.toJsonValue(): JsonElement {
    val cleaned = replace(",", "").trim()
    return cleaned.toLongOrNull()?.let(::JsonPrimitive)
        ?: cleaned.toDoubleOrNull()?.let(::JsonPrimitive)
        ?: JsonPrimitive(this)
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
