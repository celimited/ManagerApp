package org.celimited.manager.feature.aiSalesReport

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.csv.toCsv
import org.celimited.manager.core.common.format.formatDateTime
import org.celimited.manager.core.common.format.formatDayGroup
import org.celimited.manager.core.common.format.formatRelativeTime
import org.celimited.manager.domain.usecase.aiSalesReport.AskAISalesReportUseCase
import org.celimited.manager.domain.usecase.aiSalesReport.AskForSummaryUseCase
import org.celimited.manager.domain.usecase.aiSalesReport.GetAIFAQsUseCase
import org.celimited.manager.domain.usecase.aiSalesReport.GetPreviousPromptsUseCase
import org.celimited.manager.model.aiSalesReport.QueryLog
import org.celimited.manager.model.aiSalesReport.ReportSummary
import org.celimited.manager.model.aiSalesReport.SalesReportResult

class AISalesReportViewModel(
    private val askAISalesReportUseCase: AskAISalesReportUseCase,
    private val getPreviousPromptsUseCase: GetPreviousPromptsUseCase,
    private val getAIFAQsUseCase: GetAIFAQsUseCase,
    private val askForSummaryUseCase: AskForSummaryUseCase,
    private val dispatcherProvider: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(AISalesReportUiState())
    val uiState: StateFlow<AISalesReportUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<AISalesReportUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var hasLoadedPreviousPrompts = false
    private var isLoadingFaqs = false

    // This ViewModel is Activity-scoped: Navigation 3 gives back-stack entries no ViewModelStore of
    // their own, so it survives a back press and would otherwise replay the previous visit's state.
    private var isFirstEntry = true
    private var submitJob: Job? = null

    // What the error dialog's "Try again" re-runs. A failed query captures its own prompt and
    // queryLogId — going through onSubmitClicked() would lose the queryLogId of a prompt that came
    // from the FAQ list or the history sheet.
    private var retryAction: (() -> Unit)? = null

    init {
        loadPreviousPrompts()
        loadFaqs()
    }

    /** Called each time the screen is entered, so every visit starts on a clean page. */
    fun onScreenOpened() {
        if (isFirstEntry) {
            // Construction already ran init{} against a default state — nothing stale to clear.
            isFirstEntry = false
            return
        }

        // A request from the previous visit would otherwise land after the reset and refill it.
        submitJob?.cancel()
        submitJob = null

        _uiState.value = AISalesReportUiState()
        hasLoadedPreviousPrompts = false
        retryAction = null

        loadPreviousPrompts()
        loadFaqs()
    }

    fun onPromptChanged(value: String) {
        _uiState.update { it.copy(promptInput = value) }
    }

    fun onSubmitClicked() {
        submitPrompt(prompt = _uiState.value.promptInput.trim(), queryLogId = 0)
    }

    fun onHistoryClicked() {
        _uiState.update { it.copy(isHistorySheetVisible = true) }
        if (!hasLoadedPreviousPrompts) {
            loadPreviousPrompts()
        }
    }

    fun onDismissHistorySheet() {
        _uiState.update { it.copy(isHistorySheetVisible = false) }
    }

    fun onPreviousPromptSelected(item: QueryLogUi) {
        _uiState.update { it.copy(promptInput = item.userPrompt, isHistorySheetVisible = false) }
        submitPrompt(prompt = item.userPrompt, queryLogId = item.queryLogId)
    }

    fun onFaqSelected(item: QueryLogUi) {
        _uiState.update { it.copy(promptInput = item.userPrompt) }
        submitPrompt(prompt = item.userPrompt, queryLogId = item.queryLogId)
    }

    fun onDismissError() {
        retryAction = null
        _uiState.update { it.copy(errorDialog = null) }
    }

    fun onRetryClicked() {
        val action = retryAction
        onDismissError()
        action?.invoke()
    }

    private fun submitPrompt(prompt: String, queryLogId: Int) {
        if (prompt.isBlank() || _uiState.value.isLoading) return

        submitJob = viewModelScope.launch {
            askAISalesReportUseCase(prompt = prompt, queryLogId = queryLogId).collect { result ->
                when (result) {
                    is DataResult.Loading -> _uiState.update {
                        it.copy(isLoading = true)
                    }

                    is DataResult.Success -> _uiState.update {
                        val resultUi = result.data.toUi()
                        it.copy(
                            isLoading = false,
                            result = resultUi,
                            chartData = resultUi.toChartData(),
                            viewFormat = ResultViewFormat.Table,
                            selectedLabelColumnIndex = 0,
                            selectedMetricIndex = 0,
                            usageRemaining = result.data.usageRemaining,
                            queryLogId = result.data.queryLogId,
                            // A new result invalidates any summary of the previous one.
                            summary = null
                        )
                    }

                    is DataResult.Error -> {
                        if (result.error is AppError.Unauthorized) {
                            sendEffect(AISalesReportUiEffect.SessionExpired)
                        }
                        _uiState.update { it.copy(isLoading = false) }
                        showError(result.error.toUserMessage()) {
                            submitPrompt(prompt = prompt, queryLogId = queryLogId)
                        }
                    }
                }
            }
        }
    }

    private fun loadPreviousPrompts() {
        if (_uiState.value.isHistoryLoading) return

        viewModelScope.launch {
            getPreviousPromptsUseCase().collect { result ->
                when (result) {
                    is DataResult.Loading -> _uiState.update {
                        it.copy(isHistoryLoading = true)
                    }

                    is DataResult.Success -> {
                        hasLoadedPreviousPrompts = true
                        _uiState.update {
                            it.copy(
                                isHistoryLoading = false,
                                previousPrompts = result.data.logs.toGroups(),
                                usageRemaining = result.data.usageRemaining
                            )
                        }
                    }

                    is DataResult.Error -> {
                        if (result.error is AppError.Unauthorized) {
                            _uiState.update { it.copy(isHistorySheetVisible = false, isHistoryLoading = false) }
                            sendEffect(AISalesReportUiEffect.SessionExpired)
                        } else {
                            // The background load at screen entry fails silently — opening the sheet
                            // retries it. Only a failure the user is looking at gets the dialog.
                            val wasSheetVisible = _uiState.value.isHistorySheetVisible
                            _uiState.update { it.copy(isHistorySheetVisible = false, isHistoryLoading = false) }
                            if (wasSheetVisible) showError(result.error.toUserMessage(), retry = ::onHistoryClicked)
                        }
                    }
                }
            }
        }
    }

    /**
     * FAQs are a convenience shortcut, so failures are swallowed — the card simply doesn't render.
     *
     * Note this deliberately ignores the response's `usageRemaining`: the FAQ endpoint always
     * returns 0 there, which would disable the send button if it reached the state.
     */
    private fun loadFaqs() {
        if (isLoadingFaqs) return

        viewModelScope.launch {
            isLoadingFaqs = true
            getAIFAQsUseCase().collect { result ->
                if (result is DataResult.Success) {
                    _uiState.update {
                        it.copy(faqs = result.data.logs.map { log -> log.toUi() }.toImmutableList())
                    }
                }
            }
            isLoadingFaqs = false
        }
    }

    fun onViewFormatSelected(format: ResultViewFormat) {
        _uiState.update { it.copy(viewFormat = format) }
    }

    fun onLabelColumnSelected(index: Int) {
        _uiState.update { it.copy(selectedLabelColumnIndex = index) }
    }

    fun onMetricSelected(index: Int) {
        _uiState.update { it.copy(selectedMetricIndex = index) }
    }

    fun onSummaryClicked() {
        // The sheet opens straight away and shows its own spinner while the request runs.
        _uiState.update { it.copy(isSummarySheetVisible = true) }
        loadSummary()
    }

    fun onDismissSummarySheet() {
        _uiState.update { it.copy(isSummarySheetVisible = false) }
    }

    private fun loadSummary() {
        val state = _uiState.value
        val result = state.result ?: return
        if (state.isSummaryLoading) return

        viewModelScope.launch {
            askForSummaryUseCase(
                prompt = state.promptInput,
                queryLogId = state.queryLogId,
                columns = result.columns,
                rows = result.rows
            ).collect { summaryResult ->
                when (summaryResult) {
                    is DataResult.Loading -> _uiState.update {
                        it.copy(isSummaryLoading = true)
                    }

                    is DataResult.Success -> _uiState.update {
                        it.copy(
                            isSummaryLoading = false,
                            summary = summaryResult.data.toUi(),
                            usageRemaining = summaryResult.data.usageRemaining
                        )
                    }

                    is DataResult.Error -> {
                        if (summaryResult.error is AppError.Unauthorized) {
                            _uiState.update { it.copy(isSummarySheetVisible = false, isSummaryLoading = false) }
                            sendEffect(AISalesReportUiEffect.SessionExpired)
                        } else {
                            // Close the empty sheet; "Try again" reopens it and reloads.
                            _uiState.update { it.copy(isSummarySheetVisible = false, isSummaryLoading = false) }
                            showError(summaryResult.error.toUserMessage(), retry = ::onSummaryClicked)
                        }
                    }
                }
            }
        }
    }

    fun onDownloadClicked() {
        _uiState.update { it.copy(isDownloadSheetVisible = true) }
    }

    fun onDownloadFormatSelected(format: DownloadFormat) {
        _uiState.update { it.copy(downloadFormat = format) }
    }

    fun onDismissDownloadSheet() {
        _uiState.update { it.copy(isDownloadSheetVisible = false) }
    }

    fun onConfirmDownload() {
        val state = _uiState.value
        val result = state.result ?: return
        if (result.rows.isEmpty()) return

        _uiState.update { it.copy(isDownloadSheetVisible = false) }

        // Rendering a long table to PDF is real work, so it stays off the main thread.
        viewModelScope.launch(dispatcherProvider.default) {
            val bytes = when (state.downloadFormat) {
                DownloadFormat.Csv -> toCsv(result.columns, result.rows).encodeToByteArray()
                DownloadFormat.Pdf -> buildReportPdf(
                    prompt = state.promptInput,
                    result = result,
                    chartData = state.chartData,
                    viewFormat = state.viewFormat,
                    labelColumnIndex = state.selectedLabelColumnIndex,
                    metricIndex = state.selectedMetricIndex
                )
            }
            _uiEffect.send(
                AISalesReportUiEffect.DownloadFile(
                    suggestedName = "sales_report",
                    extension = state.downloadFormat.extension,
                    bytes = bytes
                )
            )
        }
    }

    /** [retry] null shows the dialog without a "Try again" button. */
    private fun showError(message: String, retry: (() -> Unit)?) {
        retryAction = retry
        _uiState.update { it.copy(errorDialog = ErrorDialogUi(message, canRetry = retry != null)) }
    }

    private fun sendEffect(effect: AISalesReportUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private fun SalesReportResult.toUi(): ReportResultUi = ReportResultUi(
        columns = columns.toImmutableList(),
        rows = rows.map { it.toImmutableList() }.toImmutableList()
    )

    private fun ReportSummary.toUi(): ReportSummaryUi = ReportSummaryUi(
        summary = summary,
        highlights = highlights.toImmutableList(),
        actions = actions.toImmutableList()
    )

    private fun QueryLog.toUi(): QueryLogUi = QueryLogUi(
        queryLogId = queryLogId,
        userPrompt = userPrompt,
        relativeTime = formatRelativeTime(executedAt),
        executedAt = formatDateTime(executedAt)
    )

    /** Day-by-day sections, newest first — the endpoint's own ordering is not relied on. */
    private fun List<QueryLog>.toGroups(): ImmutableList<PromptGroupUi> =
        groupBy { it.executedAt.date }
            .entries
            .sortedByDescending { it.key }
            .map { (_, logs) ->
                PromptGroupUi(
                    label = formatDayGroup(logs.first().executedAt),
                    items = logs.sortedByDescending { it.executedAt }
                        .map { it.toUi() }
                        .toImmutableList()
                )
            }
            .toImmutableList()

    private fun AppError.toUserMessage(): String = when (this) {
        is AppError.Business -> message.collapseRepeatedLines()
        is AppError.Server -> message ?: "Something went wrong. Please try again."
        is AppError.Unauthorized -> message ?: "Something went wrong. Please try again."
        is AppError.Unknown -> message ?: "Something went wrong. Please try again."
        is AppError.NoConnection -> "No internet connection. Please try again."
        is AppError.Timeout -> "The request timed out. Please try again."
        is AppError.Serialization -> "Unexpected response from server."
    }

    // The backend repeats the same SQL error once per statement in the batch.
    private fun String.collapseRepeatedLines(): String =
        split('\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .joinToString("\n")
}
