package org.celimited.manager.feature.aiSalesReport

import org.celimited.manager.core.common.format.formatCompact
import org.celimited.manager.core.common.pdf.PdfColor
import org.celimited.manager.core.common.pdf.PdfDocumentBuilder
import org.celimited.manager.core.common.pdf.helveticaWidth
import org.celimited.manager.core.common.pdf.truncateToWidth
import org.celimited.manager.core.common.pdf.wrapToWidth
import kotlin.math.abs
import kotlin.math.max

private const val MARGIN = 40f
private const val TITLE_SIZE = 16f
private const val PROMPT_SIZE = 10f
private const val TABLE_FONT_SIZE = 9f
private const val ROW_HEIGHT = 18f
private const val CELL_PADDING = 6f
private const val CHART_HEIGHT = 240f
private const val MAX_CHART_LABELS = 12
private const val MAX_PIE_SLICES = 12

private val InkColor = PdfColor.fromArgb(0xFF111827)
private val MutedColor = PdfColor.fromArgb(0xFF6B7280)
private val RuleColor = PdfColor.fromArgb(0xFFE5E7EB)
private val HeaderFill = PdfColor.fromArgb(0xFFF3F0FF)
private val StripeFill = PdfColor.fromArgb(0xFFFAFAFC)
private val BrandColor = PdfColor.fromArgb(0xFF582FFF)

/**
 * Renders a query result as a PDF: title, the prompt that produced it, the chart the user is
 * looking at, then the full data table across as many pages as it takes.
 */
fun buildReportPdf(
    prompt: String,
    result: ReportResultUi,
    chartData: ChartDataUi?,
    viewFormat: ResultViewFormat,
    labelColumnIndex: Int,
    metricIndex: Int
): ByteArray {
    val pdf = PdfDocumentBuilder()
    val contentWidth = pdf.pageWidth - MARGIN * 2
    var y = pdf.pageHeight - MARGIN

    // --- Header -------------------------------------------------------------------------------
    y -= TITLE_SIZE
    pdf.text(MARGIN, y, "AI Sales Report", TITLE_SIZE, bold = true, color = InkColor)
    y -= 16f

    wrapToWidth(prompt, contentWidth, PROMPT_SIZE).take(3).forEach { line ->
        pdf.text(MARGIN, y, line, PROMPT_SIZE, color = MutedColor)
        y -= PROMPT_SIZE + 3f
    }

    y -= 6f
    pdf.line(MARGIN, y, pdf.pageWidth - MARGIN, y, width = 1f, color = RuleColor)
    y -= 18f

    // --- Chart --------------------------------------------------------------------------------
    if (chartData != null) {
        val labelColumn = chartData.labelColumns.getOrNull(labelColumnIndex)
            ?: chartData.labelColumns.firstOrNull()
        val metric = chartData.metrics.getOrNull(metricIndex) ?: chartData.metrics.firstOrNull()

        if (labelColumn != null && metric != null && metric.values.isNotEmpty()) {
            // On the Table tab there is no chart on screen to mirror, so Bar is the default.
            val chartFormat = if (viewFormat == ResultViewFormat.Table) ResultViewFormat.Bar else viewFormat

            pdf.text(MARGIN, y, metric.name, 11f, bold = true, color = InkColor)
            y -= 12f

            val chartBottom = y - CHART_HEIGHT
            when (chartFormat) {
                ResultViewFormat.Bar -> pdf.drawBarChart(labelColumn.labels, metric.values, MARGIN, chartBottom, contentWidth, CHART_HEIGHT)
                ResultViewFormat.Line -> pdf.drawLineChart(labelColumn.labels, metric.values, MARGIN, chartBottom, contentWidth, CHART_HEIGHT)
                ResultViewFormat.Pie -> pdf.drawPieChart(labelColumn.labels, metric.values, MARGIN, chartBottom, contentWidth, CHART_HEIGHT)
                ResultViewFormat.Table -> Unit
            }
            y = chartBottom - 24f
        }
    }

    // --- Table --------------------------------------------------------------------------------
    val widths = columnWidths(result, contentWidth)
    val rightAligned = result.columns.indices.map { result.isNumericColumn(it) }

    // A chart plus a header can leave too little room for the table to start on page one.
    if (y < MARGIN + ROW_HEIGHT * 3) {
        pdf.newPage()
        y = pdf.pageHeight - MARGIN
    }

    y = pdf.drawTableHeader(result.columns, widths, y)

    result.rows.forEachIndexed { index, row ->
        if (y - ROW_HEIGHT < MARGIN + 20f) {
            pdf.newPage()
            y = pdf.pageHeight - MARGIN
            y = pdf.drawTableHeader(result.columns, widths, y)
        }
        pdf.drawTableRow(row, widths, rightAligned, y, striped = index % 2 == 1)
        y -= ROW_HEIGHT
    }

    pdf.drawFooters()
    return pdf.build()
}

// -- Table ---------------------------------------------------------------------------------------

/** Natural content widths, scaled to fit [available] and floored so no column collapses. */
private fun columnWidths(result: ReportResultUi, available: Float): List<Float> {
    if (result.columns.isEmpty()) return emptyList()

    val natural = result.columns.mapIndexed { index, column ->
        var widest = helveticaWidth(column, TABLE_FONT_SIZE, bold = true)
        result.rows.forEach { row ->
            val cell = row.getOrNull(index).orEmpty()
            widest = max(widest, helveticaWidth(cell, TABLE_FONT_SIZE))
        }
        widest + CELL_PADDING * 2
    }

    val total = natural.sum()
    if (total <= available) return natural

    val minimum = 40f
    val scale = available / total
    return natural.map { max(minimum, it * scale) }
}

private fun PdfDocumentBuilder.drawTableHeader(
    columns: List<String>,
    widths: List<Float>,
    top: Float
): Float {
    fillRect(MARGIN, top - ROW_HEIGHT, widths.sum(), ROW_HEIGHT, HeaderFill)
    var x = MARGIN
    columns.forEachIndexed { index, column ->
        val width = widths.getOrElse(index) { 0f }
        val label = truncateToWidth(column, width - CELL_PADDING * 2, TABLE_FONT_SIZE, bold = true)
        text(x + CELL_PADDING, top - ROW_HEIGHT + CELL_PADDING, label, TABLE_FONT_SIZE, bold = true, color = InkColor)
        x += width
    }
    line(MARGIN, top - ROW_HEIGHT, MARGIN + widths.sum(), top - ROW_HEIGHT, 0.5f, RuleColor)
    return top - ROW_HEIGHT
}

private fun PdfDocumentBuilder.drawTableRow(
    row: List<String>,
    widths: List<Float>,
    rightAligned: List<Boolean>,
    top: Float,
    striped: Boolean
) {
    if (striped) fillRect(MARGIN, top - ROW_HEIGHT, widths.sum(), ROW_HEIGHT, StripeFill)

    var x = MARGIN
    widths.forEachIndexed { index, width ->
        val cell = row.getOrNull(index).orEmpty()
        val inner = width - CELL_PADDING * 2
        val value = truncateToWidth(cell, inner, TABLE_FONT_SIZE)
        val baseline = top - ROW_HEIGHT + CELL_PADDING
        if (rightAligned.getOrElse(index) { false }) {
            textRightAligned(x + width - CELL_PADDING, baseline, value, TABLE_FONT_SIZE, color = InkColor)
        } else {
            text(x + CELL_PADDING, baseline, value, TABLE_FONT_SIZE, color = InkColor)
        }
        x += width
    }
    line(MARGIN, top - ROW_HEIGHT, MARGIN + widths.sum(), top - ROW_HEIGHT, 0.3f, RuleColor)
}

/** Written after layout, since the total page count is only known once every row is placed. */
private fun PdfDocumentBuilder.drawFooters() {
    forEachPage { index, total ->
        textCentered(pageWidth / 2f, MARGIN - 14f, "Page ${index + 1} of $total", 8f, color = MutedColor)
    }
}

// -- Charts --------------------------------------------------------------------------------------

private fun paletteColor(index: Int): PdfColor =
    PdfColor.fromArgb(chartPaletteArgb[index % chartPaletteArgb.size])

/** Axis frame shared by the bar and line charts; returns the plot area as (left, bottom, w, h). */
private fun PdfDocumentBuilder.drawAxes(
    values: List<Float>,
    left: Float,
    bottom: Float,
    width: Float,
    height: Float
): PlotArea {
    val maxValue = values.maxOrNull() ?: 0f
    val minValue = minOf(0f, values.minOrNull() ?: 0f)
    val span = (maxValue - minValue).takeIf { it > 0f } ?: 1f

    val axisLabelWidth = helveticaWidth(formatCompact(maxValue), 8f) + 8f
    val plotLeft = left + axisLabelWidth
    val plotBottom = bottom + 26f
    val plotWidth = width - axisLabelWidth
    val plotHeight = height - 26f

    // Four gridlines with their values down the left edge.
    repeat(5) { step ->
        val fraction = step / 4f
        val y = plotBottom + plotHeight * fraction
        line(plotLeft, y, plotLeft + plotWidth, y, 0.3f, RuleColor)
        textRightAligned(plotLeft - 4f, y - 3f, formatCompact(minValue + span * fraction), 8f, color = MutedColor)
    }

    return PlotArea(plotLeft, plotBottom, plotWidth, plotHeight, minValue, span)
}

private class PlotArea(
    val left: Float,
    val bottom: Float,
    val width: Float,
    val height: Float,
    val minValue: Float,
    val span: Float
) {
    fun yFor(value: Float): Float = bottom + (value - minValue) / span * height
}

/** Every nth label, so a long series does not turn its axis into a smear. */
private fun PdfDocumentBuilder.drawCategoryLabels(labels: List<String>, area: PlotArea, slotWidth: Float) {
    val stride = ((labels.size + MAX_CHART_LABELS - 1) / MAX_CHART_LABELS).coerceAtLeast(1)
    labels.forEachIndexed { index, label ->
        if (index % stride != 0) return@forEachIndexed
        val centerX = area.left + slotWidth * (index + 0.5f)
        val shortened = truncateToWidth(label, slotWidth * stride, 7f)
        textCentered(centerX, area.bottom - 12f, shortened, 7f, color = MutedColor)
    }
}

private fun PdfDocumentBuilder.drawBarChart(
    labels: List<String>,
    values: List<Float>,
    left: Float,
    bottom: Float,
    width: Float,
    height: Float
) {
    val area = drawAxes(values, left, bottom, width, height)
    val slot = area.width / values.size
    val barWidth = (slot * 0.6f).coerceAtMost(40f)
    val zeroY = area.yFor(0f)

    values.forEachIndexed { index, value ->
        val x = area.left + slot * index + (slot - barWidth) / 2f
        val valueY = area.yFor(value)
        val barBottom = minOf(zeroY, valueY)
        val barHeight = abs(valueY - zeroY)
        fillRect(x, barBottom, barWidth, barHeight, BrandColor)
    }

    line(area.left, zeroY, area.left + area.width, zeroY, 0.6f, MutedColor)
    drawCategoryLabels(labels, area, slot)
}

private fun PdfDocumentBuilder.drawLineChart(
    labels: List<String>,
    values: List<Float>,
    left: Float,
    bottom: Float,
    width: Float,
    height: Float
) {
    val area = drawAxes(values, left, bottom, width, height)
    val slot = area.width / values.size

    val points = values.mapIndexed { index, value ->
        (area.left + slot * (index + 0.5f)) to area.yFor(value)
    }
    polyline(points, 1.5f, BrandColor)
    points.forEach { (x, y) -> fillRect(x - 2f, y - 2f, 4f, 4f, BrandColor) }

    line(area.left, area.yFor(0f), area.left + area.width, area.yFor(0f), 0.6f, MutedColor)
    drawCategoryLabels(labels, area, slot)
}

private fun PdfDocumentBuilder.drawPieChart(
    labels: List<String>,
    values: List<Float>,
    left: Float,
    bottom: Float,
    width: Float,
    height: Float
) {
    val slices = topSlicesForPdf(labels, values)
    val total = slices.sumOf { it.second.toDouble() }.toFloat()
    if (total <= 0f) return

    val radius = (height / 2f) - 10f
    val centerX = left + radius + 10f
    val centerY = bottom + height / 2f

    var angle = 90f
    slices.forEachIndexed { index, (_, value) ->
        val sweep = -(value / total) * 360f
        fillSector(centerX, centerY, radius, angle, sweep, paletteColor(index))
        angle += sweep
    }

    // Legend to the right of the pie.
    val legendX = centerX + radius + 20f
    val legendWidth = left + width - legendX
    var legendY = centerY + (slices.size * 12f) / 2f
    slices.forEachIndexed { index, (label, value) ->
        fillRect(legendX, legendY, 7f, 7f, paletteColor(index))
        val entry = "${label.ifBlank { "-" }}  ${formatCompact(value)}"
        text(
            legendX + 12f,
            legendY,
            truncateToWidth(entry, legendWidth - 12f, 8f),
            8f,
            color = InkColor
        )
        legendY -= 12f
    }
}

/** Mirrors the on-screen pie: everything past the largest slices collapses into "Others". */
private fun topSlicesForPdf(labels: List<String>, values: List<Float>): List<Pair<String, Float>> {
    val slices = values.mapIndexed { index, value -> labels.getOrNull(index).orEmpty() to value }
    if (slices.size <= MAX_PIE_SLICES) return slices

    val sorted = slices.sortedByDescending { it.second }
    val others = sorted.drop(MAX_PIE_SLICES).sumOf { it.second.toDouble() }.toFloat()
    return sorted.take(MAX_PIE_SLICES) + ("Others" to others)
}
