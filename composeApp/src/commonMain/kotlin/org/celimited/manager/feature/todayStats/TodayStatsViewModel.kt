package org.celimited.manager.feature.todayStats

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
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.format.formatDayMonth
import org.celimited.manager.core.common.format.formatTakaCompact
import org.celimited.manager.domain.usecase.attendance.GetSubordinateHierarchyLevelsUseCase
import org.celimited.manager.domain.usecase.todayStats.GetOrderBreakdownUseCase
import org.celimited.manager.domain.usecase.todayStats.GetSrStatsListUseCase
import org.celimited.manager.domain.usecase.todayStats.GetTeamSummaryUseCase
import org.celimited.manager.domain.usecase.todayStats.GetTodaySummaryUseCase
import org.celimited.manager.model.todayStats.SrSummary
import org.celimited.manager.model.todayStats.TargetPeriod
import org.celimited.manager.model.todayStats.TeamSummary
import org.celimited.manager.model.todayStats.TodayStatsScope
import org.celimited.manager.model.todayStats.TodaySummary
import kotlin.math.roundToInt

class TodayStatsViewModel(
    private val getTodaySummaryUseCase: GetTodaySummaryUseCase,
    private val getTeamSummaryUseCase: GetTeamSummaryUseCase,
    private val getOrderBreakdownUseCase: GetOrderBreakdownUseCase,
    private val getSrStatsListUseCase: GetSrStatsListUseCase,
    private val getSubordinateHierarchyLevelsUseCase: GetSubordinateHierarchyLevelsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TodayStatsUiState())
    val uiState: StateFlow<TodayStatsUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<TodayStatsUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var summaryJob: Job? = null
    private var levelsJob: Job? = null
    private var teamJob: Job? = null
    private var teamOrdersJob: Job? = null
    private var srJob: Job? = null

    // Full SR list from the last load; the search query filters it locally into uiState.srs.
    private var allSrs: List<SrSummary> = emptyList()

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
            TodayStatsTab.Sr -> allSrs.isNotEmpty()
        }
        if (!alreadyLoaded) loadTab(tab)
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
        _uiState.update { it.copy(srQuery = query, srs = filterSrs(query)) }
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
        TodayStatsTab.MySummary -> loadSummary()
        TodayStatsTab.MyTeam -> if (_uiState.value.selectedLevelKey == null) loadLevels() else loadTeam()
        TodayStatsTab.Sr -> loadSrs()
    }

    private fun loadSummary() {
        summaryJob?.cancel()
        _uiState.update { it.copy(isSummaryLoading = true) }
        summaryJob = viewModelScope.launch {
            when (val result = getTodaySummaryUseCase()) {
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

    private fun loadLevels() {
        if (levelsJob?.isActive == true) return
        _uiState.update { it.copy(isLevelsLoading = true) }
        levelsJob = viewModelScope.launch {
            when (val result = getSubordinateHierarchyLevelsUseCase()) {
                is DataResult.Success -> {
                    val levels = result.data.levels.map { LevelChipUi(key = it.key, label = it.name, userLevel = it.userLevel) }.toImmutableList()
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
        val state = _uiState.value
        val levelKey = state.selectedLevelKey ?: return
        teamJob?.cancel()
        // Without the level's numeric userLevel the API can't be called; the card area shows "No stats available".
        val userLevel = state.levels.firstOrNull { it.key == levelKey }?.userLevel
        if (userLevel == null) {
            _uiState.update { it.copy(isTeamLoading = false, team = null) }
            return
        }
        _uiState.update { it.copy(isTeamLoading = true) }
        teamJob = viewModelScope.launch {
            when (val result = getTeamSummaryUseCase(userLevel)) {
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
        // "Product wise order status" is hidden, so its data isn't loaded. Restore this call
        // together with ProductOrderStatusCard in MyTeamContent.
        // loadTeamOrders()
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

    /** Fetches every SR (the API has no paging or search); the current query is applied locally. */
    private fun loadSrs() {
        srJob?.cancel()
        _uiState.update { it.copy(isSrLoading = true) }
        srJob = viewModelScope.launch {
            when (val result = getSrStatsListUseCase()) {
                is DataResult.Success -> {
                    allSrs = result.data
                    _uiState.update { it.copy(isSrLoading = false, srs = filterSrs(it.srQuery)) }
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isSrLoading = false) }
                    handleError(result.error, retry = ::loadSrs)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    /** Matches name, SR code or contact number, case-insensitively. */
    private fun filterSrs(query: String): ImmutableList<SrCardUi> {
        val q = query.trim()
        val matches = if (q.isEmpty()) allSrs else allSrs.filter {
            it.name.contains(q, ignoreCase = true) ||
                it.srCode.contains(q, ignoreCase = true) ||
                it.contactNo.contains(q)
        }
        return matches.map { it.toUi() }.toImmutableList()
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

    private fun TodaySummary.toSummaryUi(): SummaryUi {
        val att = attendance
        val prod = productivity
        return SummaryUi(
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
                today = today.toTrackCard("Today"),
                tillDate = tillDate.toTrackCard("Till date")
            ),
            inventory = inventory.let {
                InventoryUi(
                    currentStockValue = formatTakaCompact(it.currentStockValue),
                    openingStock = formatTakaCompact(it.openingStockValue),
                    dbCount = countText(it.dbCount),
                    outOfStock = countText(it.outOfStockSkuCount),
                    currentStock = countText(it.currentStockQty)
                )
            },
            zeroSales = outlets.let {
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

    private fun TeamSummary.toTeamUi(): TeamUi {
        return TeamUi(
            attendance = attendance.map {
                UnitAttendanceUi(
                    unitId = it.unitId,
                    name = it.unitName,
                    totalSr = it.stats.totalSr,
                    present = it.stats.present,
                    absent = it.stats.absent,
                    onLeave = it.stats.onLeave
                )
            }.toImmutableList(),
            productivity = productivity.map {
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
            target = target.map {
                UnitTargetUi(
                    unitId = it.unitId,
                    name = it.unitName,
                    today = it.stats.today.toRow(),
                    tillDate = it.stats.tillDate.toRow()
                )
            }.toImmutableList(),
            // No. Order and Current Stock aren't in order-summary-by-hierarchy.
            zeroSales = outlets.map {
                UnitZeroSalesUi(
                    unitId = it.unitId,
                    name = it.unitName,
                    activeOutlets = countText(it.stats.activeOutlets),
                    activeSkus = countText(it.stats.activeSkus),
                    orderCount = "--",
                    zeroSales = countText(it.stats.zeroSalesOutlets),
                    zeroSkus = countText(it.stats.zeroSalesSkus),
                    currentStock = "--"
                )
            }.toImmutableList(),
            unitOptions = attendance.map { DropdownOption(it.unitId, it.unitName) }.toImmutableList()
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
        designation = designation,
        levelName = levelName,
        // Attendance given today = online/present.
        isOnline = attendanceAt != null,
        checkIn = timeText(attendanceAt),
        firstOrder = timeText(firstOrderAt),
        lastOrder = timeText(lastOrderAt),
        strikeRate = percentText(strikeRate),
        productiveRate = percentText(productiveRate),
        lpc = decimalText(lpc),
        orderAchievement = percentText(achievementPercent)
    )
}
