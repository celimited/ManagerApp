package org.celimited.manager.feature.todayStats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.format.formatTakaCompact
import org.celimited.manager.domain.usecase.todayStats.GetSrDetailUseCase
import org.celimited.manager.domain.usecase.todayStats.GetSrOrdersUseCase
import org.celimited.manager.model.todayStats.SrDetail
import org.celimited.manager.model.todayStats.SrOrderItem
import org.celimited.manager.model.todayStats.SrWorkMode
import kotlin.math.roundToInt

class SrDetailViewModel(
    private val srId: Int,
    private val getSrDetailUseCase: GetSrDetailUseCase,
    private val getSrOrdersUseCase: GetSrOrdersUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SrDetailUiState())
    val uiState: StateFlow<SrDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SrDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var detailJob: Job? = null
    private var ordersJob: Job? = null
    private var retryAction: (() -> Unit)? = null
    private var sessionExpiredSent = false

    /** Called each time the page is entered; the ViewModel is keyed by SR id and outlives the back-stack entry. */
    fun onScreenOpened() {
        sessionExpiredSent = false
        loadDetail()
        loadOrders()
    }

    fun onOrderGroupSelected(group: BreakdownGroup) {
        if (group == _uiState.value.orderGroup) return
        _uiState.update { it.copy(orderGroup = group) }
        loadOrders()
    }

    fun onOrderQueryChange(query: String) {
        _uiState.update { it.copy(orderQuery = query) }
        loadOrders(debounce = true)
    }

    fun onRetry() {
        val action = retryAction
        onDismissError()
        action?.invoke()
    }

    fun onDismissError() {
        retryAction = null
        _uiState.update { it.copy(errorDialog = null) }
    }

    private fun loadDetail() {
        detailJob?.cancel()
        _uiState.update { it.copy(isLoading = true) }
        detailJob = viewModelScope.launch {
            when (val result = getSrDetailUseCase(srId)) {
                is DataResult.Success -> _uiState.update { result.data.applyTo(it).copy(isLoading = false) }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    handleError(result.error, retry = ::loadDetail)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun loadOrders(debounce: Boolean = false) {
        ordersJob?.cancel()
        ordersJob = viewModelScope.launch {
            if (debounce) delay(SEARCH_DEBOUNCE_MS)
            _uiState.update { it.copy(isOrdersLoading = true) }
            val state = _uiState.value
            when (val result = getSrOrdersUseCase(srId, state.orderGroup.toOrderGroupBy(), state.orderQuery)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(isOrdersLoading = false, orders = result.data.map { item -> item.toUi() }.toImmutableList())
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isOrdersLoading = false) }
                    handleError(result.error) { loadOrders() }
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private suspend fun handleError(error: AppError, retry: () -> Unit) {
        if (error is AppError.Unauthorized) {
            if (!sessionExpiredSent) {
                sessionExpiredSent = true
                _uiEffect.send(SrDetailUiEffect.SessionExpired)
            }
            return
        }
        if (_uiState.value.errorDialog != null) return
        retryAction = retry
        _uiState.update { it.copy(errorDialog = ErrorDialogUi(error.toUserMessage())) }
    }

    private fun SrDetail.applyTo(state: SrDetailUiState) = state.copy(
        profile = SrProfileUi(
            name = profile.name,
            designation = profile.designation,
            location = profile.locationName,
            territory = profile.territoryName,
            isOnline = profile.isOnline,
            phoneNo = profile.phoneNo,
            whatsAppUrl = profile.whatsAppUrl
        ),
        timeline = SrTimelineUi(
            spent = durationText(timeline.spentMinutes),
            loggedInTime = timeText(timeline.loggedInAt),
            workMode = when (timeline.workMode) {
                SrWorkMode.ON_FIELD -> "On-Field"
                SrWorkMode.OFFICE -> "Office"
                SrWorkMode.OTHER -> "Other"
            },
            visited = if (timeline.hasVisited) "Yes" else "No",
            firstOrderTime = timeText(timeline.firstOrderAt),
            firstOrderOutlet = timeline.firstOrderOutlet.orEmpty(),
            lastOrderTime = timeText(timeline.lastOrderAt),
            lastOrderOutlet = timeline.lastOrderOutlet.orEmpty()
        ),
        work = SrWorkUi(
            scheduled = countText(work.scheduled),
            visited = countText(work.visited),
            productive = countText(work.productive),
            lpc = decimalText(work.lpc)
        ),
        target = SrTargetUi(
            todayTarget = formatTakaCompact(target.todayTarget),
            orderValue = formatTakaCompact(target.orderValue),
            achievementPercent = target.achievementPercent.roundToInt()
        )
    )

    private fun SrOrderItem.toUi() = SrOrderUi(
        id = id,
        name = name,
        subName = subName,
        qty = decimalText(qty),
        memo = countText(memoCount),
        specialDiscount = decimalText(specialDiscount),
        promoDiscount = decimalText(promoDiscount),
        totalDiscount = decimalText(totalDiscount),
        freeValue = decimalText(freeValue),
        orderQtyUom = decimalText(orderQtyUom),
        freeQty = decimalText(freeQty)
    )

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
    }
}
