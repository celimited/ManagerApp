package org.celimited.manager.feature.aiSalesReport

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AISalesReportUiState(
    val promptInput: String = "",
    val isLoading: Boolean = false,
    val result: ReportResultUi? = null,
    val chartData: ChartDataUi? = null,
    val viewFormat: ResultViewFormat = ResultViewFormat.Table,
    val selectedLabelColumnIndex: Int = 0,
    val selectedMetricIndex: Int = 0,
    val errorMessage: String? = null
)

@Immutable
data class ReportResultUi(
    val columns: ImmutableList<String> = persistentListOf(),
    val rows: ImmutableList<ImmutableList<String>> = persistentListOf()
)
