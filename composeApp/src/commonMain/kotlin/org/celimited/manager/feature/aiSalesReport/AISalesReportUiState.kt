package org.celimited.manager.feature.aiSalesReport

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.celimited.manager.component.ErrorDialogUi

@Immutable
data class AISalesReportUiState(
    val promptInput: String = "",
    val isLoading: Boolean = false,
    val result: ReportResultUi? = null,
    val chartData: ChartDataUi? = null,
    val viewFormat: ResultViewFormat = ResultViewFormat.Table,
    val selectedLabelColumnIndex: Int = 0,
    val selectedMetricIndex: Int = 0,
    /** API failure popup (query, summary or history); null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null,
    val isHistorySheetVisible: Boolean = false,
    val isHistoryLoading: Boolean = false,
    val previousPrompts: ImmutableList<PromptGroupUi> = persistentListOf(),
    val usageRemaining: Int? = null,
    /** Suggested starter prompts, shown while no result is on screen. Empty if the fetch failed. */
    val faqs: ImmutableList<QueryLogUi> = persistentListOf(),
    val isDownloadSheetVisible: Boolean = false,
    val downloadFormat: DownloadFormat = DownloadFormat.Pdf,
    /** Server-assigned id for the current result, sent when asking for its summary. */
    val queryLogId: Int = 0,
    val isSummarySheetVisible: Boolean = false,
    val isSummaryLoading: Boolean = false,
    val summary: ReportSummaryUi? = null
)

@Immutable
data class ReportSummaryUi(
    val summary: String,
    val highlights: ImmutableList<String>,
    val actions: ImmutableList<String>
)

enum class DownloadFormat(val label: String, val extension: String) {
    Pdf("PDF document", "pdf"),
    Csv("CSV file", "csv")
}

@Immutable
data class ReportResultUi(
    val columns: ImmutableList<String> = persistentListOf(),
    val rows: ImmutableList<ImmutableList<String>> = persistentListOf()
)

@Immutable
data class QueryLogUi(
    val queryLogId: Int,
    val userPrompt: String,
    /** "4h ago" — the trailing time on a history row. */
    val relativeTime: String,
    /** "Sep 22, 2026 · 3:47 PM" — shown beneath the prompt. */
    val executedAt: String
)

/** A day's worth of history rows, under one heading. */
@Immutable
data class PromptGroupUi(
    val label: String,
    val items: ImmutableList<QueryLogUi>
)
