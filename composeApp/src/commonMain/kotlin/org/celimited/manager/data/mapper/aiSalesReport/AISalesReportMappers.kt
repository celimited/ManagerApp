package org.celimited.manager.data.mapper.aiSalesReport

import kotlinx.serialization.json.jsonPrimitive
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportResponseDto
import org.celimited.manager.model.aiSalesReport.SalesReportResult

fun AIReportResponseDto.toDomain(): SalesReportResult {
    val columns = data.firstOrNull()?.keys?.toList().orEmpty()
    val rows = data.map { row ->
        columns.map { column -> row[column]?.jsonPrimitive?.content.orEmpty() }
    }
    return SalesReportResult(columns = columns, rows = rows)
}
