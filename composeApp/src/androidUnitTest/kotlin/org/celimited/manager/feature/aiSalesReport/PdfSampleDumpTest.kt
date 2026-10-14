package org.celimited.manager.feature.aiSalesReport

import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import java.io.File
import kotlin.test.Test

/**
 * TEMPORARY verification harness — writes sample PDFs to disk so they can be opened in a real
 * viewer. Delete once the output has been eyeballed.
 */
class PdfSampleDumpTest {

    private fun sampleResult(rowCount: Int): ReportResultUi {
        val outlets = listOf(
            "A J Enterprise", "Miravis Duo Depot", "Rahman Traders (North)",
            "Karim & Sons \\ Co", "Dhaka Central Wholesale Distribution Point Limited"
        )
        return ReportResultUi(
            columns = persistentListOf("Outlet", "Region", "Total Sales", "Growth %"),
            rows = (1..rowCount).map { index ->
                persistentListOf(
                    "${outlets[index % outlets.size]} #$index",
                    if (index % 2 == 0) "Dhaka" else "Chattogram",
                    (125000 + index * 4137).toString(),
                    ((index % 30) - 10).toString()
                ) as kotlinx.collections.immutable.ImmutableList<String>
            }.toImmutableList()
        )
    }

    private fun dump(name: String, bytes: ByteArray) {
        val out = File(System.getProperty("pdfOutDir") ?: "build/pdf-samples")
        out.mkdirs()
        File(out, name).writeBytes(bytes)
        println("WROTE ${File(out, name).absolutePath} (${bytes.size} bytes)")
    }

    @Test
    fun `dump samples`() {
        val many = sampleResult(60)
        val chart = many.toChartData()

        dump(
            "report-bar.pdf",
            buildReportPdf(
                prompt = "Compare monthly sales trend in 2025 across every outlet in the northern region",
                result = many,
                chartData = chart,
                viewFormat = ResultViewFormat.Bar,
                labelColumnIndex = 0,
                metricIndex = 0
            )
        )

        dump(
            "report-line.pdf",
            buildReportPdf(
                prompt = "Monthly trend",
                result = many,
                chartData = chart,
                viewFormat = ResultViewFormat.Line,
                labelColumnIndex = 0,
                metricIndex = 0
            )
        )

        val few = sampleResult(6)
        dump(
            "report-pie.pdf",
            buildReportPdf(
                prompt = "Share of sales by outlet",
                result = few,
                chartData = few.toChartData(),
                viewFormat = ResultViewFormat.Pie,
                labelColumnIndex = 0,
                metricIndex = 0
            )
        )

        dump(
            "report-table-only.pdf",
            buildReportPdf(
                prompt = "Outlets with no billing",
                result = ReportResultUi(
                    columns = persistentListOf("Outlet", "Status"),
                    rows = persistentListOf(
                        persistentListOf("A J Enterprise (North)", "No billing"),
                        persistentListOf("Karim & Sons", "No billing")
                    )
                ),
                chartData = null,
                viewFormat = ResultViewFormat.Table,
                labelColumnIndex = 0,
                metricIndex = 0
            )
        )
    }
}
