package org.celimited.manager.feature.aiSalesReport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.koalaplot.core.bar.VerticalBarPlot
import io.github.koalaplot.core.bar.verticalSolidBar
import io.github.koalaplot.core.line.LinePlot2
import io.github.koalaplot.core.pie.DefaultSlice
import io.github.koalaplot.core.pie.PieChart
import io.github.koalaplot.core.style.LineStyle
import io.github.koalaplot.core.util.ExperimentalKoalaPlotApi
import io.github.koalaplot.core.xygraph.AxisContent
import io.github.koalaplot.core.xygraph.CategoryAxisModel
import io.github.koalaplot.core.xygraph.Point
import io.github.koalaplot.core.xygraph.XYGraph
import io.github.koalaplot.core.xygraph.XYGraphScope
import io.github.koalaplot.core.xygraph.autoScaleRange
import io.github.koalaplot.core.xygraph.rememberAxisStyle
import io.github.koalaplot.core.xygraph.rememberFloatLinearAxisModel
import org.celimited.manager.core.common.format.formatCompact

private val ChartHeight = 320.dp
private const val MAX_AXIS_LABELS = 12
private const val MAX_PIE_SLICES = 12

/** Held as ARGB so the PDF renderer, which has no Compose types, can share the same palette. */
internal val chartPaletteArgb = listOf(
    0xFF582FFF,
    0xFF7C3AED,
    0xFFAD46FF,
    0xFF4B2AC4,
    0xFF6D42FF,
    0xFFCBBFFF,
    0xFF16A34A,
    0xFFFF902F,
    0xFFFF4D6D,
    0xFF2D1B69,
    0xFF9CA3AF,
    0xFFFFB800,
    0xFF364153
)

private val chartPalette = chartPaletteArgb.map { Color(it) }

@Composable
fun ReportBarChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    ReportXYGraph(labels = labels, values = values, modifier = modifier) {
        VerticalBarPlot(
            xData = values.indices.toList(),
            yData = values,
            bar = verticalSolidBar(chartPalette.first())
        )
    }
}

@Composable
fun ReportLineChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    ReportXYGraph(labels = labels, values = values, modifier = modifier) {
        LinePlot2(
            data = values.mapIndexed { index, value -> Point(index, value) },
            lineStyle = LineStyle(brush = SolidColor(chartPalette.first()), strokeWidth = 2.dp)
        )
    }
}

@OptIn(ExperimentalKoalaPlotApi::class)
@Composable
fun ReportPieChart(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val slices = remember(labels, values) { topSlices(labels, values) }

    Column(modifier = modifier) {
        Box(
            modifier = Modifier.fillMaxWidth().height(ChartHeight),
            contentAlignment = Alignment.Center
        ) {
            PieChart(
                values = slices.map { it.value },
                slice = { index -> DefaultSlice(color = chartPalette[index % chartPalette.size]) },
                holeSize = 0.45f
            )
        }

        slices.forEachIndexed { index, slice ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(chartPalette[index % chartPalette.size], CircleShape)
                )
                Text(
                    text = slice.label,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = formatCompact(slice.value),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

@Composable
private fun ReportXYGraph(
    labels: List<String>,
    values: List<Float>,
    modifier: Modifier = Modifier,
    content: @Composable XYGraphScope<Int, Float>.() -> Unit
) {
    // Categories are row indices so repeated or blank labels never collide on the axis.
    val indices = remember(values.size) { values.indices.toList() }
    val labelStride = remember(labels.size) { ((labels.size + MAX_AXIS_LABELS - 1) / MAX_AXIS_LABELS).coerceAtLeast(1) }

    XYGraph(
        xAxisModel = remember(indices) { CategoryAxisModel(indices) },
        yAxisModel = rememberFloatLinearAxisModel((values + 0f).autoScaleRange()),
        xAxisContent = AxisContent(
            labels = { index ->
                if (index % labelStride == 0) {
                    Text(
                        text = labels.getOrNull(index).orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            },
            title = {},
            style = rememberAxisStyle(labelRotation = 45)
        ),
        yAxisContent = AxisContent(
            labels = { value ->
                Text(
                    text = formatCompact(value),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(end = 4.dp)
                )
            },
            title = {},
            style = rememberAxisStyle()
        ),
        modifier = modifier.fillMaxWidth().height(ChartHeight),
        content = content
    )
}

private data class PieSlice(val label: String, val value: Float)

/** Collapses everything past the largest [MAX_PIE_SLICES] values into a single "Others" slice. */
private fun topSlices(labels: List<String>, values: List<Float>): List<PieSlice> {
    val slices = values.mapIndexed { index, value ->
        PieSlice(labels.getOrNull(index).orEmpty(), value)
    }
    if (slices.size <= MAX_PIE_SLICES) return slices

    val sorted = slices.sortedByDescending { it.value }
    val top = sorted.take(MAX_PIE_SLICES)
    val others = sorted.drop(MAX_PIE_SLICES).sumOf { it.value.toDouble() }.toFloat()
    return top + PieSlice("Others", others)
}
