package org.celimited.manager.feature.aiSalesReport

import kotlinx.collections.immutable.toImmutableList
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReportChartDataTest {

    private fun result(columns: List<String>, vararg rows: List<String>) = ReportResultUi(
        columns = columns.toImmutableList(),
        rows = rows.map { it.toImmutableList() }.toImmutableList()
    )

    @Test
    fun `one text and one numeric column charts rows as categories`() {
        val chart = result(
            listOf("outlet Name", "total Sales"),
            listOf("Bismillah Store", "18678191.3630"),
            listOf("Lazz pharma", "15904269.0580")
        ).toChartData()

        assertTrue(chart != null)
        assertEquals(1, chart.labelColumns.size)
        assertEquals("outlet Name", chart.labelColumns.first().name)
        assertContentEquals(listOf("Bismillah Store", "Lazz pharma"), chart.labelColumns.first().labels)
        assertEquals(1, chart.metrics.size)
        assertEquals("total Sales", chart.metrics.first().name)
        assertEquals(18678191.363f, chart.metrics.first().values.first())
        assertTrue(chart.allowsPie)
    }

    @Test
    fun `single row with several numeric columns pivots columns into categories`() {
        val chart = result(
            listOf("outlet Name", "Jan Sales", "Feb Sales", "Mar Sales"),
            listOf("Bismillah Store", "120", "95", "140")
        ).toChartData()

        assertTrue(chart != null)
        assertContentEquals(
            listOf("Jan Sales", "Feb Sales", "Mar Sales"),
            chart.labelColumns.first().labels
        )
        assertEquals(1, chart.metrics.size)
        assertEquals("Bismillah Store", chart.metrics.first().name)
        assertContentEquals(listOf(120f, 95f, 140f), chart.metrics.first().values)
    }

    @Test
    fun `several text columns are all offered as category choices`() {
        val chart = result(
            listOf("region", "outlet Name", "total Sales"),
            listOf("Dhaka", "Bismillah Store", "120"),
            listOf("Ctg", "Lazz pharma", "95")
        ).toChartData()

        assertTrue(chart != null)
        assertEquals(listOf("region", "outlet Name"), chart.labelColumns.map { it.name })
        assertEquals(listOf("total Sales"), chart.metrics.map { it.name })
    }

    @Test
    fun `several numeric columns become separate metrics`() {
        val chart = result(
            listOf("outlet Name", "qty", "total Sales"),
            listOf("Bismillah Store", "10", "120"),
            listOf("Lazz pharma", "4", "95")
        ).toChartData()

        assertTrue(chart != null)
        assertEquals(listOf("qty", "total Sales"), chart.metrics.map { it.name })
    }

    @Test
    fun `rows with no text column fall back to row ordinals`() {
        val chart = result(
            listOf("total Sales"),
            listOf("120"),
            listOf("95")
        ).toChartData()

        assertTrue(chart != null)
        assertContentEquals(listOf("1", "2"), chart.labelColumns.first().labels)
    }

    @Test
    fun `repeated label values are each kept as their own point`() {
        val chart = result(
            listOf("outlet Name", "total Sales"),
            listOf("Bismillah Store", "120"),
            listOf("Bismillah Store", "95")
        ).toChartData()

        assertTrue(chart != null)
        assertContentEquals(listOf("Bismillah Store", "Bismillah Store"), chart.labelColumns.first().labels)
        assertContentEquals(listOf(120f, 95f), chart.metrics.first().values)
    }

    @Test
    fun `negative values disable pie but still allow bar and line`() {
        val chart = result(
            listOf("outlet Name", "growth"),
            listOf("Bismillah Store", "-12"),
            listOf("Lazz pharma", "95")
        ).toChartData()

        assertTrue(chart != null)
        assertFalse(chart.allowsPie)
    }

    @Test
    fun `numbers with thousands separators are still numeric`() {
        val chart = result(
            listOf("outlet Name", "total Sales"),
            listOf("Bismillah Store", "18,678,191.36"),
            listOf("Lazz pharma", "15,904,269.05")
        ).toChartData()

        assertTrue(chart != null)
        assertEquals(1, chart.metrics.size)
        assertEquals(18678191.36f, chart.metrics.first().values.first())
    }

    @Test
    fun `text only result is not chartable`() {
        assertNull(
            result(
                listOf("outlet Name", "region"),
                listOf("Bismillah Store", "Dhaka"),
                listOf("Lazz pharma", "Ctg")
            ).toChartData()
        )
    }

    @Test
    fun `single data point is not chartable`() {
        assertNull(
            result(
                listOf("outlet Name", "total Sales"),
                listOf("Bismillah Store", "120")
            ).toChartData()
        )
    }

    @Test
    fun `empty result is not chartable`() {
        assertNull(result(listOf("outlet Name", "total Sales")).toChartData())
    }
}
