package org.celimited.manager.feature.distributorDetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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
import org.celimited.manager.core.common.format.formatShortDate
import org.celimited.manager.core.common.format.formatTakaCompact
import org.celimited.manager.domain.usecase.distributor.GetDistributorOverviewUseCase
import org.celimited.manager.domain.usecase.distributor.GetDistributorStockSummaryUseCase
import org.celimited.manager.domain.usecase.distributor.ObserveDistributorsUseCase
import org.celimited.manager.model.distributor.DistributorOverview
import org.celimited.manager.model.distributor.IndentSuggestion
import org.celimited.manager.model.distributor.SkuRef
import org.celimited.manager.model.distributor.StockHealth
import org.celimited.manager.model.distributor.StockSummaryCounts
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.roundToInt

class DistributorDetailViewModel(
    private val distributorId: Int,
    private val observeDistributorsUseCase: ObserveDistributorsUseCase,
    private val getDistributorStockSummaryUseCase: GetDistributorStockSummaryUseCase,
    private val getDistributorOverviewUseCase: GetDistributorOverviewUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistributorDetailUiState())
    val uiState: StateFlow<DistributorDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<DistributorDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var stockSummaryJob: Job? = null
    private var overviewJob: Job? = null

    // Both loads run in parallel; this keeps two Unauthorized failures from navigating to login twice.
    private var sessionExpiredSent = false

    init {
        // Name / code / address come from the distributors cached at login.
        viewModelScope.launch {
            observeDistributorsUseCase().collect { distributors ->
                val distributor = distributors?.firstOrNull { it.id == distributorId } ?: return@collect
                _uiState.update {
                    it.copy(name = distributor.name, code = distributor.code, address = distributor.address)
                }
            }
        }
    }

    /**
     * Called each time the page is entered. The ViewModel is keyed by distributor id and outlives the
     * back-stack entry, so this is what refreshes overview and stock on a return visit.
     */
    fun onScreenOpened() {
        loadAll()
    }

    fun onTabSelected(tab: DistributorDetailTab) {
        if (tab.isLocked) {
            onLockedFeatureClick()
            return
        }
        _uiState.update { it.copy(selectedTab = tab) }
    }

    /** Tap on an action or tab that isn't built yet. */
    fun onLockedFeatureClick() {
        viewModelScope.launch { _uiEffect.send(DistributorDetailUiEffect.ShowUnderDevelopment) }
    }

    fun onRetry() {
        onDismissError()
        loadAll()
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorDialog = null) }
    }

    // Local filter over dummy data; the issues API will take `search` as a query param instead.
    fun onIssueQueryChange(query: String) {
        val term = query.trim()
        val filtered = if (term.isEmpty()) {
            DistributorDetailDummyData.issues
        } else {
            DistributorDetailDummyData.issues.filter {
                it.title.contains(term, ignoreCase = true) || it.assignedTo.contains(term, ignoreCase = true)
            }.toImmutableList()
        }
        _uiState.update { it.copy(issueQuery = query, issues = filtered) }
    }

    private fun loadAll() {
        sessionExpiredSent = false
        loadOverview()
        loadStockSummary()
    }

    private fun loadOverview() {
        overviewJob?.cancel()
        _uiState.update { it.copy(isOverviewLoading = true) }
        overviewJob = viewModelScope.launch {
            when (val result = getDistributorOverviewUseCase(distributorId)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isOverviewLoading = false,
                        overview = result.data.toUi(),
                        lastVisit = result.data.lastVisitDate?.let(::formatShortDate).orEmpty()
                    )
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isOverviewLoading = false) }
                    handleError(result.error)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun loadStockSummary() {
        stockSummaryJob?.cancel()
        _uiState.update { it.copy(isStockSummaryLoading = true) }
        stockSummaryJob = viewModelScope.launch {
            when (val result = getDistributorStockSummaryUseCase(distributorId)) {
                is DataResult.Success -> _uiState.update {
                    val data = result.data
                    it.copy(
                        isStockSummaryLoading = false,
                        stockTab = it.stockTab.copy(
                            pendingApprovalCount = data.pendingApproval.indentCount,
                            pendingApprovalValue = formatTakaCompact(data.pendingApproval.totalValue),
                            suggestionText = data.indentSuggestion.toSuggestionText(),
                            totalSkus = data.summary.totalSkus,
                            summary = data.summary.toTiles(),
                            health = data.health.toHealthBuckets()
                        )
                    )
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isStockSummaryLoading = false) }
                    handleError(result.error)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    /** Shared by both loaders: one session-expired effect, and the first error's dialog wins. */
    private suspend fun handleError(error: AppError) {
        if (error is AppError.Unauthorized) {
            if (!sessionExpiredSent) {
                sessionExpiredSent = true
                _uiEffect.send(DistributorDetailUiEffect.SessionExpired)
            }
        } else {
            _uiState.update { it.copy(errorDialog = it.errorDialog ?: ErrorDialogUi(error.toUserMessage())) }
        }
    }

    private fun DistributorOverview.toUi() = DistributorOverviewUi(
        growthPercent = formatPercentChange(growth.growthPercent),
        isGrowthNegative = growth.growthPercent < 0,
        mtdPercent = growth.mtdAchievementPercent.roundToInt(),
        ytdPercent = growth.ytdAchievementPercent.roundToInt(),
        targetPercent = target.achievementPercent.roundToInt(),
        targetAchieved = formatTakaCompact(target.achievedValue),
        target = formatTakaCompact(target.targetValue),
        weeklyChange = formatPercentChange(target.weeklyChangePercent),
        isWeeklyChangeNegative = target.weeklyChangePercent < 0,
        invoiceReceived = invoice.invoiceReceived,
        lastInvoiceDate = invoice.lastInvoiceDate?.let(::formatShortDate) ?: "—",
        paymentStatus = invoice.paymentStatus.toTitleCase().ifEmpty { "—" },
        isPaymentSettled = invoice.paymentStatus.equals("PAID", ignoreCase = true),
        stock = DistributorStockUi(
            zeroStockSkus = stockStatus.zeroStockSkus.toNames(),
            slowMovingSkus = stockStatus.slowMovingSkus.toNames(),
            slowMovingThresholdDays = stockStatus.slowMovingThresholdDays,
            healthPercent = stockStatus.stockHealthIndex.roundToInt(),
            inStockPercent = stockStatus.inStockPercent.roundToInt(),
            lowPercent = stockStatus.lowStockPercent.roundToInt()
        )
    )

    private fun List<SkuRef>.toNames(): ImmutableList<String> = map { it.name.ifEmpty { it.code } }.toImmutableList()

    /** "PENDING" / "PARTIALLY_PAID" → "Pending" / "Partially Paid". */
    private fun String.toTitleCase(): String = trim()
        .split('_', ' ')
        .filter { it.isNotEmpty() }
        .joinToString(" ") { word -> word.lowercase().replaceFirstChar { it.uppercase() } }

    private fun StockSummaryCounts.toTiles(): ImmutableList<StockSummaryItemUi> = persistentListOf(
        StockSummaryItemUi(
            id = "stock_value",
            label = "STOCK VALUE",
            value = formatTakaCompact(stockValue),
            note = "${formatPercentChange(stockValueChangePercent)} vs previous period",
            tone = if (stockValueChangePercent < 0) StockSummaryTone.Danger else StockSummaryTone.Positive
        ),
        StockSummaryItemUi(
            id = "below_cover",
            label = "SKU BELOW $coverThresholdDays DAYS",
            value = skuCount(skuBelowCoverCount),
            note = "Below $coverThresholdDays days coverage",
            tone = StockSummaryTone.Warning
        ),
        StockSummaryItemUi(
            id = "zero_stock",
            label = "ZERO STOCK",
            value = skuCount(zeroStockSkuCount),
            note = "Out of stock",
            tone = StockSummaryTone.Danger
        ),
        StockSummaryItemUi(
            id = "pending_indent",
            label = "PENDING INDENT",
            value = formatTakaCompact(pendingIndentValue),
            note = "$pendingIndentCount indents awaiting",
            tone = StockSummaryTone.Info
        ),
        StockSummaryItemUi(
            id = "expired",
            label = "EXPIRED SKUS",
            value = skuCount(expiredSkuCount),
            note = "Already expired",
            tone = StockSummaryTone.Danger
        ),
        StockSummaryItemUi(
            id = "nearly_expired",
            label = "NEARLY EXPIRED SKUS",
            value = skuCount(nearlyExpiredSkuCount),
            note = "Within $nearlyExpiredWithinDays days",
            tone = StockSummaryTone.Warning
        )
    )

    /** Empty when nothing is suggested, which hides the card. */
    private fun IndentSuggestion.toSuggestionText(): String {
        if (skuCount <= 0) return ""
        return "${skuCount(skuCount)} fall below $belowCoverDays days even after pending indent. " +
            "Estimated ${formatTakaCompact(estimatedValue)} to reach $targetCoverDays-day cover."
    }

    /** Unknown levels are dropped; sorted worst-first regardless of API order. */
    private fun List<StockHealth>.toHealthBuckets(): ImmutableList<StockHealthBucketUi> =
        mapNotNull { health ->
            val level = when (health.level.lowercase().filter { it.isLetter() }) {
                "outofstock" -> StockHealthLevel.Zero
                "critical" -> StockHealthLevel.Critical
                "lowstock" -> StockHealthLevel.Watch
                "healthy" -> StockHealthLevel.Healthy
                else -> return@mapNotNull null
            }
            StockHealthBucketUi(level, health.skuCount)
        }.sortedBy { it.level.ordinal }.toImmutableList()

    private fun skuCount(count: Int) =if (count == 1) "1 SKU" else "$count SKUs"

    /** 4.8 → "+4.8%", -2.25 → "-2.3%", 0 → "0%". */
    private fun formatPercentChange(percent: Double): String {
        val tenths = round(abs(percent) * 10).toLong()
        val text = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
        val sign = when {
            tenths == 0L -> ""
            percent < 0 -> "-"
            else -> "+"
        }
        return "$sign$text%"
    }

    // Prefer whatever message the API itself returned; fall back to a generic
    // notice only when there's no server response to read one from at all.
    private fun AppError.toUserMessage(): String = when (this) {
        is AppError.Business -> message
        is AppError.Server -> message ?: "Something went wrong. Please try again."
        is AppError.Unauthorized -> message ?: "Something went wrong. Please try again."
        is AppError.Unknown -> message ?: "Something went wrong. Please try again."
        is AppError.NoConnection -> "No internet connection. Please try again."
        is AppError.Timeout -> "The request timed out. Please try again."
        is AppError.Serialization -> "Unexpected response from server."
    }
}
