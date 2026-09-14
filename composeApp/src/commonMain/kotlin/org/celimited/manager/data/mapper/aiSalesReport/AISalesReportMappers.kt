package org.celimited.manager.data.mapper.aiSalesReport

import kotlinx.serialization.json.jsonPrimitive
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportResponseDto
import org.celimited.manager.model.aiSalesReport.SalesReportResult

fun AIReportResponseDto.toDomain(): SalesReportResult {
    val dataRows = data.orEmpty()
    val columns = dataRows.firstOrNull()?.keys?.toList().orEmpty()
    val rows = dataRows.map { row ->
        columns.map { column -> row[column]?.jsonPrimitive?.content.orEmpty() }
    }
    return SalesReportResult(columns = columns, rows = rows)
}
