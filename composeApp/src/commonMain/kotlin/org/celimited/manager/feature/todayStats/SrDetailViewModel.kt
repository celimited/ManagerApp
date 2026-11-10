package org.celimited.manager.feature.todayStats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.celimited.manager.core.common.format.formatTakaCompact
import org.celimited.manager.domain.usecase.todayStats.GetSrDetailUseCase
import org.celimited.manager.model.todayStats.SrDetail
import kotlin.math.roundToInt

class SrDetailViewModel(
    private val srId: Int,
    private val getSrDetailUseCase: GetSrDetailUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SrDetailUiState())
    val uiState: StateFlow<SrDetailUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<SrDetailUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var detailJob: Job? = null
    private var retryAction: (() -> Unit)? = null
    private var sessionExpiredSent = false

    /** Called each time the page is entered; the ViewModel is keyed by SR id and outlives the back-stack entry. */
    fun onScreenOpened() {
        sessionExpiredSent = false
        loadDetail()
    }

    fun onCallClick() {
        val phone = _uiState.value.profile?.phoneNo?.takeIf { it.isNotBlank() } ?: return
        viewModelScope.launch { _uiEffect.send(SrDetailUiEffect.OpenUri("tel:${dialableNumber(phone)}")) }
    }

    fun onWhatsAppClick() {
        val url = _uiState.value.profile?.whatsAppUrl ?: return
        viewModelScope.launch { _uiEffect.send(SrDetailUiEffect.OpenUri(url)) }
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
            srCode = profile.srCode,
            territory = profile.territoryName,
            isOnline = profile.isOnline,
            phoneNo = profile.phoneNo,
            whatsAppUrl = profile.whatsAppUrl
        ),
        timeline = SrTimelineUi(
            spent = timeline.spentHours.ifBlank { "--" },
            loggedInTime = timeText(timeline.attendanceAt),
            visited = timeline.firstVisitAt?.let(::timeText) ?: "No",
            firstOrderTime = timeText(timeline.firstOrderAt),
            firstOrderOutlet = timeline.firstOrderOutlet.orEmpty(),
            lastOrderTime = timeText(timeline.lastOrderAt),
            lastOrderOutlet = timeline.lastOrderOutlet.orEmpty()
        ),
        work = SrWorkUi(
            scheduled = countText(work.scheduled),
            visited = countText(work.visited),
            productive = countText(work.productive),
            lpc = decimalText(work.lpc),
            remaining = countText(work.remaining),
            strikeRate = percentText(work.strikeRate),
            productiveRate = percentText(work.productiveRate)
        ),
        target = SrTargetUi(
            todayTarget = formatTakaCompact(target.todayTarget),
            todayOrder = formatTakaCompact(target.todayOrder),
            todayAchievementPercent = target.todayAchievement.roundToInt(),
            tillDateTarget = formatTakaCompact(target.tillDateTarget),
            tillDateOrder = formatTakaCompact(target.tillDateOrder),
            tillDateAchievementPercent = target.tillDateAchievement.roundToInt()
        ),
        outlets = SrOutletStatusUi(
            activeOutlets = countText(outlets.activeOutlets),
            zeroSalesOutlets = countText(outlets.zeroSalesOutlets),
            activeSkus = countText(outlets.activeSkus),
            zeroSalesSkus = countText(outlets.zeroSalesSkus)
        )
    )

    /** The API sends BD mobiles without the leading 0 ("1755621876"); the dialer needs "01755621876". */
    private fun dialableNumber(phone: String): String {
        val digits = phone.trim()
        return if (digits.length == 10 && digits.startsWith("1") && digits.all { it.isDigit() }) "0$digits" else digits
    }
}
