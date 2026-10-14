package org.celimited.manager.feature.todayStats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.format.formatDayMonth
import org.celimited.manager.core.common.format.formatTakaCompact
import org.celimited.manager.domain.usecase.attendance.GetSubordinateHierarchyLevelsUseCase
import org.celimited.manager.domain.usecase.todayStats.GetOrderBreakdownUseCase
import org.celimited.manager.domain.usecase.todayStats.GetSrStatsListUseCase
import org.celimited.manager.domain.usecase.todayStats.GetTodayStatsOverviewUseCase
import org.celimited.manager.model.todayStats.SrSummary
import org.celimited.manager.model.todayStats.TargetPeriod
import org.celimited.manager.model.todayStats.TodayStatsOverview
import org.celimited.manager.model.todayStats.TodayStatsScope
import kotlin.math.roundToInt

class TodayStatsViewModel(
    private val getTodayStatsOverviewUseCase: GetTodayStatsOverviewUseCase,
    private val getOrderBreakdownUseCase: GetOrderBreakdownUseCase,
    private val getSrStatsListUseCase: GetSrStatsListUseCase,
    private val getSubordinateHierarchyLevelsUseCase: GetSubordinateHierarchyLevelsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayStatsUiState())
    val uiState: StateFlow<TodayStatsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<TodayStatsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var summaryJob: Job? = null
    private var breakdownJob: Job? = null
    private var levelsJob: Job? = null
    private var teamJob: Job? = null
    private var teamOrdersJob: Job? = null
    private var srJob: Job? = null
    private var srMoreJob: Job? = null

    // What the error dialog's "Try again" re-runs; set alongside the errorDialog.
    private var retryAction: (() -> Unit)? = null

    // Several loads can fail at once; only navigate to login once.
    private var sessionExpiredSent = false

    /**
     * Called each time the screen is entered. The ViewModel is activity-scoped, so this refreshes the
     * selected tab on a return visit while keeping the tab (e.g. coming back from SR detail).
     */
    fun onScreenOpened() {
        sessionExpiredSent = false
        loadTab(_uiState.value.selectedTab)
    }

    fun onTabSelected(tab: TodayStatsTab) {
        if (tab == _uiState.value.selectedTab) return
        _uiState.update { it.copy(selectedTab = tab) }
        // Tabs load lazily on first visit; after that they keep their data until the screen is reopened.
        val state = _uiState.value
        val alreadyLoaded = when (tab) {
            TodayStatsTab.MySummary -> state.summary != null
            TodayStatsTab.MyTeam -> state.team != null
            TodayStatsTab.Sr -> state.srs.isNotEmpty()
        }
        if (!alreadyLoaded) loadTab(tab)
    }

    // ---------- My Summary ----------

    fun onBreakdownGroupSelected(group: BreakdownGroup) {
        if (group == _uiState.value.breakdownGroup) return
        _uiState.update { it.copy(breakdownGroup = group, breakdown = persistentListOf()) }
        loadBreakdown()
    }

    fun onBreakdownQueryChange(query: String) {
        _uiState.update { it.copy(breakdownQuery = query) }
        loadBreakdown(debounce = true)
    }

    // ---------- My Team ----------

    fun onLevelSelected(levelKey: String) {
        if (levelKey == _uiState.value.selectedLevelKey) return
        // Clear the old level's rows so they never show under the new chip.
        _uiState.update {
            it.copy(
                selectedLevelKey = levelKey,
                team = null,
                teamOrderUnitId = null,
                teamOrders = persistentListOf()
            )
        }
        loadTeam()
    }

    fun onTeamOrderGroupSelected(group: BreakdownGroup) {
        if (group == _uiState.value.teamOrderGroup) return
        _uiState.update { it.copy(teamOrderGroup = group) }
        loadTeamOrders()
    }

    fun onTeamOrderUnitSelected(unitId: Int) {
        // Picking the selected unit again clears the filter back to all units.
        val next = unitId.takeIf { it != _uiState.value.teamOrderUnitId }
        _uiState.update { it.copy(teamOrderUnitId = next) }
        loadTeamOrders()
    }

    fun onTeamOrdersReload() {
        loadTeamOrders()
    }

    // ---------- SR ----------

    fun onSrQueryChange(query: String) {
        _uiState.update { it.copy(srQuery = query) }
        loadSrs(debounce = true)
    }

    fun onSrClick(srId: Int) {
        viewModelScope.launch { _uiEffect.send(TodayStatsUiEffect.NavigateToSrDetail(srId)) }
    }

    // ---------- Errors ----------

    fun onRetry() {
        val action = retryAction
        onDismissError()
        action?.invoke()
    }

    fun onDismissError() {
        retryAction = null
        _uiState.update { it.copy(errorDialog = null) }
    }

    // ---------- Loading ----------

    private fun loadTab(tab: TodayStatsTab) = when (tab) {
        TodayStatsTab.MySummary -> {
            loadSummary()
            loadBreakdown()
        }
        TodayStatsTab.MyTeam -> if (_uiState.value.selectedLevelKey == null) loadLevels() else loadTeam()
        TodayStatsTab.Sr -> loadSrs()
    }

    private fun loadSummary() {
        summaryJob?.cancel()
        _uiState.update { it.copy(isSummaryLoading = true) }
        summaryJob = viewModelScope.launch {
            when (val result = getTodayStatsOverviewUseCase(TodayStatsScope())) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isSummaryLoading = false, summary = result.data.toSummaryUi())
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isSummaryLoading = false) }
                    handleError(result.error, retry = ::loadSummary)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun loadBreakdown(debounce: Boolean = false) {
        // A slower response for a group/query the user already left must not overwrite the new one.
        breakdownJob?.cancel()
        breakdownJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isBreakdownLoading = true) }
            val state = _uiState.value
            val result = getOrderBreakdownUseCase(state.breakdownGroup.toOrderGroupBy(), TodayStatsScope(), state.breakdownQuery)
            when (result) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isBreakdownLoading = false, breakdown = result.data.map { item -> item.toUi() }.toImmutableList())
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isBreakdownLoading = false) }
                    handleError(result.error) { loadBreakdown() }
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun loadLevels() {
        if (levelsJob?.isActive == true) return
        _uiState.update { it.copy(isLevelsLoading = true) }
        levelsJob = viewModelScope.launch {
            when (val result = getSubordinateHierarchyLevelsUseCase()) {
                is DataResult.Success -> {
                    val levels = result.data.levels.map { LevelChipUi(key = it.key, label = it.name) }.toImmutableList()
                    _uiState.update {
                        it.copy(isLevelsLoading = false, levels = levels, selectedLevelKey = levels.firstOrNull()?.key)
                    }
                    if (levels.isNotEmpty()) loadTeam()
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isLevelsLoading = false) }
                    handleError(result.error, retry = ::loadLevels)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun loadTeam() {
        val levelKey = _uiState.value.selectedLevelKey ?: return
        teamJob?.cancel()
        _uiState.update { it.copy(isTeamLoading = true) }
        teamJob = viewModelScope.launch {
            when (val result = getTodayStatsOverviewUseCase(TodayStatsScope(levelKey = levelKey))) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isTeamLoading = false, team = result.data.toTeamUi())
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isTeamLoading = false) }
                    handleError(result.error, retry = ::loadTeam)
                }
                is DataResult.Loading -> Unit
            }
        }
        loadTeamOrders()
    }

    private fun loadTeamOrders() {
        val state = _uiState.value
        val levelKey = state.selectedLevelKey ?: return
        teamOrdersJob?.cancel()
        _uiState.update { it.copy(isTeamOrdersLoading = true) }
        teamOrdersJob = viewModelScope.launch {
            val scope = TodayStatsScope(levelKey = levelKey, unitId = state.teamOrderUnitId)
            when (val result = getOrderBreakdownUseCase(state.teamOrderGroup.toOrderGroupBy(), scope, search = "")) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isTeamOrdersLoading = false, teamOrders = result.data.map { item -> item.toUi() }.toImmutableList())
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isTeamOrdersLoading = false) }
                    handleError(result.error, retry = ::loadTeamOrders)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    /** Reloads from page 1, replacing the list (tab open, search change, retry). */
    private fun loadSrs(debounce: Boolean = false) {
        srJob?.cancel()
        // A next-page response for the old query must not append to the new list.
        srMoreJob?.cancel()
        _uiState.update { it.copy(isSrLoadingMore = false) }
        srJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isSrLoading = true) }
            when (val result = getSrStatsListUseCase(_uiState.value.srQuery, page = 1)) {
                is DataResult.Success -> {
                    val page = result.data
                    val srs = page.items.map { sr -> sr.toUi() }.toImmutableList()
                    _uiState.update {
                        it.copy(
                            isSrLoading = false,
                            srs = srs,
                            srPage = page.page,
                            srHasMore = page.items.isNotEmpty() && srs.size < page.totalCount
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isSrLoading = false) }
                    handleError(result.error) { loadSrs() }
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    fun onSrLoadMore() {
        val state = _uiState.value
        if (!state.srHasMore || state.isSrLoading || state.isSrLoadingMore) return
        _uiState.update { it.copy(isSrLoadingMore = true) }
        srMoreJob = viewModelScope.launch {
            when (val result = getSrStatsListUseCase(state.srQuery, page = state.srPage + 1)) {
                is DataResult.Success -> {
                    val page = result.data
                    _uiState.update {
                        val known = it.srs.mapTo(HashSet()) { sr -> sr.srId }
                        val srs = (it.srs + page.items.filter { sr -> sr.srId !in known }.map { sr -> sr.toUi() })
                            .toImmutableList()
                        it.copy(
                            isSrLoadingMore = false,
                            srs = srs,
                            srPage = page.page,
                            srHasMore = page.items.isNotEmpty() && srs.size < page.totalCount
                        )
                    }
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isSrLoadingMore = false) }
                    handleError(result.error, retry = ::onSrLoadMore)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    /** One session-expired effect per screen visit; the first error's dialog (and retry) wins. */
    private suspend fun handleError(error: AppError, retry: () -> Unit) {
        if (error is AppError.Unauthorized) {
            if (!sessionExpiredSent) {
                sessionExpiredSent = true
                _uiEffect.send(TodayStatsUiEffect.SessionExpired)
            }
            return
        }
        if (_uiState.value.errorDialog != null) return
        retryAction = retry
        _uiState.update { it.copy(errorDialog = ErrorDialogUi(error.toUserMessage())) }
    }

    // ---------- Mapping ----------

    private fun TodayStatsOverview.toSummaryUi(): SummaryUi {
        val att = attendance.total
        val prod = productivity.total
        val tgt = target.total
        val mtd = tgt.monthToDate
        val mtdPercent = mtd?.achievementPercent?.roundToInt() ?: 0
        val mtdAchieved = formatTakaCompact(mtd?.achieved ?: 0.0)
        val mtdTarget = formatTakaCompact(mtd?.target ?: 0.0)
        return SummaryUi(
            header = AchievementHeaderUi(
                percent = mtdPercent,
                achieved = mtdAchieved,
                target = mtdTarget,
                periodLabel = mtd?.let { "Total · Month to date · Day ${it.dayOfPeriod} of ${it.daysInPeriod}" }
                    ?: "Total · Month to date"
            ),
            attendance = AttendanceUi(
                totalSr = att.totalSr,
                presentPercent = if (att.totalSr > 0) (att.present * 100f / att.totalSr).roundToInt() else 0,
                present = att.present,
                absent = att.absent,
                onLeave = att.onLeave,
                visited = att.visited,
                productive = att.productive,
                idle = att.idle,
                active = att.active
            ),
            call = CallProductivityUi(
                scheduled = countText(prod.call.scheduled),
                visited = countText(prod.call.visited),
                productive = countText(prod.call.productive),
                remaining = countText(prod.call.remaining),
                strikeRate = prod.call.strikeRate.roundToInt(),
                productiveRate = prod.call.productiveRate.roundToInt()
            ),
            sku = SkuProductivityUi(
                lpc = decimalText(prod.sku.lpc),
                tlsd = countText(prod.sku.tlsd),
                pCase = countText(prod.sku.pCase)
            ),
            runningTrack = RunningTrackUi(
                achieved = mtdAchieved,
                target = mtdTarget,
                percent = mtdPercent,
                expectedPercent = mtd?.expectedPercent?.roundToInt() ?: 0,
                today = tgt.today.toTrackCard("Today"),
                tillDate = tgt.tillDate.toTrackCard("Till date")
            ),
            inventory = inventory.total.let {
                InventoryUi(
                    currentStockValue = formatTakaCompact(it.currentStockValue),
                    openingStock = formatTakaCompact(it.openingStockValue),
                    dbCount = countText(it.dbCount),
                    outOfStock = countText(it.outOfStockSkuCount),
                    currentStock = countText(it.currentStockQty)
                )
            },
            zeroSales = zeroSales.total.let {
                ZeroSalesUi(
                    activeOutlets = countText(it.activeOutlets),
                    zeroSalesOutlets = countText(it.zeroSalesOutlets),
                    activeSkus = countText(it.activeSkus),
                    zeroSalesSkus = countText(it.zeroSalesSkus)
                )
            },
            keyMetrics = prod.memo?.let { memo ->
                persistentListOf(
                    KeyMetricUi(decimalText(memo.avgMemo), null, "Avg. Memo"),
                    KeyMetricUi(formatTakaCompact(memo.avgMemoValue), null, "Avg Memo Value"),
                    KeyMetricUi(countText(memo.lastDeliveryMemo), null, "Last Delivery Memo"),
                    KeyMetricUi(
                        formatTakaCompact(memo.lastDeliveryValue),
                        "(${durationText(memo.lastDeliveryWindowMinutes)})",
                        "Last Delivery Value"
                    )
                )
            } ?: persistentListOf()
        )
    }

    private fun TargetPeriod.toTrackCard(title: String): TrackCardUi {
        val from = fromDate
        val to = toDate
        return TrackCardUi(
            title = title,
            dateLabel = when {
                from == null && to == null -> ""
                from == null || to == null || from == to -> formatDayMonth((to ?: from)!!)
                else -> "${formatDayMonth(from)} - ${formatDayMonth(to)}"
            },
            target = formatTakaCompact(target),
            order = formatTakaCompact(order),
            percent = achievementPercent.roundToInt()
        )
    }

    private fun TodayStatsOverview.toTeamUi(): TeamUi {
        val stockByUnit = inventory.units.associate { it.unitId to it.stats.currentStockQty }
        return TeamUi(
            attendance = attendance.units.map {
                UnitAttendanceUi(
                    unitId = it.unitId,
                    name = it.unitName,
                    totalSr = it.stats.totalSr,
                    present = it.stats.present,
                    absent = it.stats.absent,
                    onLeave = it.stats.onLeave
                )
            }.toImmutableList(),
            productivity = productivity.units.map {
                val call = it.stats.call
                UnitProductivityUi(
                    unitId = it.unitId,
                    name = it.unitName,
                    scheduled = countText(call.scheduled),
                    visited = countText(call.visited),
                    productive = countText(call.productive),
                    visitedFraction = if (call.scheduled > 0) (call.visited / call.scheduled).toFloat().coerceIn(0f, 1f) else 0f,
                    strikeRate = percentText(call.strikeRate),
                    productivePercent = percentText(call.productiveRate),
                    lpc = decimalText(it.stats.sku.lpc)
                )
            }.toImmutableList(),
            target = target.units.map {
                UnitTargetUi(
                    unitId = it.unitId,
                    name = it.unitName,
                    today = it.stats.today.toRow(),
                    tillDate = it.stats.tillDate.toRow()
                )
            }.toImmutableList(),
            zeroSales = zeroSales.units.map {
                UnitZeroSalesUi(
                    unitId = it.unitId,
                    name = it.unitName,
                    activeOutlets = countText(it.stats.activeOutlets),
                    activeSkus = countText(it.stats.activeSkus),
                    orderCount = countText(it.stats.orderCount),
                    zeroSales = countText(it.stats.zeroSalesOutlets),
                    zeroSkus = countText(it.stats.zeroSalesSkus),
                    currentStock = stockByUnit[it.unitId]?.let(::countText) ?: "--"
                )
            }.toImmutableList(),
            unitOptions = attendance.units.map { DropdownOption(it.unitId, it.unitName) }.toImmutableList()
        )
    }

    private fun TargetPeriod.toRow() = TargetRowUi(
        target = formatTakaCompact(target),
        order = formatTakaCompact(order),
        achievement = percentText(achievementPercent)
    )

    private fun SrSummary.toUi() = SrCardUi(
        srId = srId,
        name = name,
        location = locationName,
        territory = territoryName,
        isOnline = isOnline,
        checkIn = timeText(checkInAt),
        firstOrder = timeText(firstOrderAt),
        lastOrder = timeText(lastOrderAt),
        strikeRate = percentText(strikeRate),
        productiveRate = percentText(productiveRate),
        lpc = decimalText(lpc),
        orderAchievement = percentText(orderAchievementPercent)
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
