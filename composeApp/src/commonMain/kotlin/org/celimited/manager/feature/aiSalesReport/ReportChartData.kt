package org.celimited.manager.feature.aiSalesReport

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

enum class ResultViewFormat { Table, Bar, Line, Pie }

@Immutable
data class ChartDataUi(
    val labelColumns: ImmutableList<LabelColumnUi>,
    val metrics: ImmutableList<MetricUi>,
    val allowsPie: Boolean
)

@Immutable
data class LabelColumnUi(
    val name: String,
    val labels: ImmutableList<String>
)

@Immutable
data class MetricUi(
    val name: String,
    val values: ImmutableList<Float>
)

private const val MIN_CHART_POINTS = 2

fun ReportResultUi.toChartData(): ChartDataUi? {
    if (rows.isEmpty() || columns.isEmpty()) return null

    val numericColumns = columns.indices.filter { index -> isNumericColumn(index) }
    if (numericColumns.isEmpty()) return null

    val textColumns = columns.indices.filter { it !in numericColumns }

    return if (rows.size == 1 && numericColumns.size >= MIN_CHART_POINTS) {
        pivotSingleRow(numericColumns, textColumns)
    } else {
        rowsAsCategories(numericColumns, textColumns)
    }
}

/** One aggregate row: each numeric column becomes its own category. */
private fun ReportResultUi.pivotSingleRow(
    numericColumns: List<Int>,
    textColumns: List<Int>
): ChartDataUi {
    val row = rows.first()
    val values = numericColumns.map { row.getOrNull(it).toChartFloat() }
    val seriesName = textColumns.firstOrNull()?.let { row.getOrNull(it) }?.takeIf { it.isNotBlank() }
        ?: "Value"

    return ChartDataUi(
        labelColumns = listOf(
            LabelColumnUi(
                name = "Column",
                labels = numericColumns.map { columns[it] }.toImmutableList()
            )
        ).toImmutableList(),
        metrics = listOf(
            MetricUi(name = seriesName, values = values.toImmutableList())
        ).toImmutableList(),
        allowsPie = values.none { it < 0f }
    )
}

/** Normal case: each row is a data point, categories come from a text column. */
private fun ReportResultUi.rowsAsCategories(
    numericColumns: List<Int>,
    textColumns: List<Int>
): ChartDataUi? {
    if (rows.size < MIN_CHART_POINTS) return null

    val labelColumns = if (textColumns.isEmpty()) {
        listOf(
            LabelColumnUi(
                name = "Row",
                labels = rows.indices.map { (it + 1).toString() }.toImmutableList()
            )
        )
    } else {
        textColumns.map { index ->
            LabelColumnUi(
                name = columns[index],
                labels = rows.map { row -> row.getOrNull(index).orEmpty() }.toImmutableList()
            )
        }
    }

    val metrics = numericColumns.map { index ->
        MetricUi(
            name = columns[index],
            values = rows.map { row -> row.getOrNull(index).toChartFloat() }.toImmutableList()
        )
    }

    return ChartDataUi(
        labelColumns = labelColumns.toImmutableList(),
        metrics = metrics.toImmutableList(),
        allowsPie = metrics.none { metric -> metric.values.any { it < 0f } }
    )
}

/** Internal rather than private so the PDF table can right-align the same columns. */
internal fun ReportResultUi.isNumericColumn(index: Int): Boolean {
    var sawValue = false
    rows.forEach { row ->
        val cell = row.getOrNull(index).orEmpty()
        if (cell.isNotBlank()) {
            if (cell.toChartDouble() == null) return false
            sawValue = true
        }
    }
    return sawValue
}

private fun String?.toChartFloat(): Float = this?.toChartDouble()?.toFloat() ?: 0f

private fun String.toChartDouble(): Double? =
    replace(",", "").trim().toDoubleOrNull()
