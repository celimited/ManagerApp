package org.celimited.manager.feature.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.datetime.LocalDate
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.domain.usecase.attendance.ApplyLeaveUseCase
import org.celimited.manager.domain.usecase.attendance.GetLeaveReasonsUseCase
import org.celimited.manager.model.attendance.ApplyLeaveParams
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import org.celimited.manager.domain.usecase.attendance.GetCurrentLocationUseCase
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.domain.usecase.attendance.GetMonthlyAttendanceUseCase
import org.celimited.manager.model.attendance.MonthlyAttendanceSummary
import org.celimited.manager.utils.currentMonthNumber
import org.celimited.manager.utils.currentYearNumber

class AttendanceLeaveViewModel(
    private val getMonthlyAttendanceUseCase: GetMonthlyAttendanceUseCase,
    private val getLeaveReasonsUseCase: GetLeaveReasonsUseCase,
    private val applyLeaveUseCase: ApplyLeaveUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AttendanceLeaveUiState())
    val uiState: StateFlow<AttendanceLeaveUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<AttendanceLeaveUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var loadJob: Job? = null

    // What the error dialog's "Try again" re-runs; set alongside a retryable errorDialog.
    private var retryAction: (() -> Unit)? = null

    // Loaded on the first sheet open and reused; null until a load succeeds.
    private var leaveReasons: ImmutableList<DropdownOption>? = null

    /**
     * Called each time the screen is entered. ViewModels are Activity-scoped under Navigation 3,
     * so without this a return visit would reopen on whatever month was left selected.
     */
    fun onScreenOpened() {
        _uiState.value = AttendanceLeaveUiState()
        load()
    }

    fun onPreviousMonth() {
        val state = _uiState.value
        val (year, month) = if (state.month == 1) state.year - 1 to 12 else state.year to state.month - 1
        showMonth(year, month)
    }

    fun onNextMonth() {
        val state = _uiState.value
        if (!state.canGoNext) return
        val (year, month) = if (state.month == 12) state.year + 1 to 1 else state.year to state.month + 1
        showMonth(year, month)
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

    fun onApplyLeaveClick() {
        if (_uiState.value.applyLeave != null) return
        val cached = leaveReasons
        _uiState.update {
            it.copy(applyLeave = ApplyLeaveFormUi(reasons = cached ?: persistentListOf(), isLoadingReasons = cached == null))
        }
        if (cached == null) loadLeaveReasons()
    }

    fun onFromDateSelected(date: LocalDate) {
        updateLeaveForm { form ->
            // A To Date before the new From Date is no longer valid.
            val toDate = form.toDate?.takeIf { it >= date }
            form.copy(fromDate = date, toDate = toDate, errorMessage = null)
        }
    }

    fun onToDateSelected(date: LocalDate) {
        updateLeaveForm { it.copy(toDate = date, errorMessage = null) }
    }

    fun onLeaveReasonSelected(id: Int) {
        updateLeaveForm { it.copy(reasonId = id, errorMessage = null) }
    }

    fun onLeaveRemarksChanged(value: String) {
        updateLeaveForm { it.copy(remarks = value) }
    }

    fun onApplyLeaveDismissed() {
        if (_uiState.value.applyLeave?.isSubmitting == true) return
        _uiState.update { it.copy(applyLeave = null) }
    }

    fun onSubmitLeave() {
        val form = _uiState.value.applyLeave ?: return
        if (form.isSubmitting) return

        val fromDate = form.fromDate
        val toDate = form.toDate
        val reasonId = form.reasonId
        val error = when {
            fromDate == null -> "Please select a from date."
            toDate == null -> "Please select a to date."
            toDate < fromDate -> "To date can't be before from date."
            reasonId == null -> "Please select a reason."
            else -> null
        }
        if (error != null || fromDate == null || toDate == null || reasonId == null) {
            updateLeaveForm { it.copy(errorMessage = error) }
            return
        }

        updateLeaveForm { it.copy(isSubmitting = true, errorMessage = null) }
        // The request carries the device location, so permission comes first.
        sendEffect(AttendanceLeaveUiEffect.RequestLocationPermission)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val form = _uiState.value.applyLeave ?: return
        if (!form.isSubmitting) return
        if (!granted) {
            showLeaveFormError("Location permission is required to apply for leave.")
            return
        }
        val fromDate = form.fromDate ?: return
        val toDate = form.toDate ?: return
        val reasonId = form.reasonId ?: return

        viewModelScope.launch {
            val location = when (val result = getCurrentLocationUseCase()) {
                is DataResult.Success -> result.data
                is DataResult.Error -> {
                    updateLeaveForm { it.copy(isSubmitting = false) }
                    showError(result.error.toUserMessage(), retry = null)
                    return@launch
                }
                is DataResult.Loading -> return@launch
            }
            val params = ApplyLeaveParams(
                fromDate = fromDate,
                toDate = toDate,
                leaveReasonId = reasonId,
                remarks = form.remarks,
                latitude = location.latitude,
                longitude = location.longitude
            )
            when (val result = applyLeaveUseCase(params)) {
                is DataResult.Success -> {
                    _uiState.update {
                        it.copy(
                            applyLeave = null,
                            leaveResult = LeaveResultUi(
                                isSuccess = true,
                                message = result.data.ifBlank { "Leave applied successfully." }
                            )
                        )
                    }
                    load()
                }
                is DataResult.Error -> _uiState.update { state ->
                    state.copy(
                        applyLeave = state.applyLeave?.copy(isSubmitting = false),
                        leaveResult = LeaveResultUi(isSuccess = false, message = result.error.toUserMessage())
                    )
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    fun onLeaveResultClosed() {
        _uiState.update { it.copy(leaveResult = null) }
    }

    private fun loadLeaveReasons() {
        viewModelScope.launch {
            when (val result = getLeaveReasonsUseCase()) {
                is DataResult.Success -> {
                    val options = result.data.map { DropdownOption(it.id, it.name) }.toImmutableList()
                    leaveReasons = options
                    updateLeaveForm { it.copy(reasons = options, isLoadingReasons = false) }
                }
                is DataResult.Error -> {
                    updateLeaveForm { it.copy(isLoadingReasons = false) }
                    showError(result.error.toUserMessage()) {
                        updateLeaveForm { it.copy(isLoadingReasons = true) }
                        loadLeaveReasons()
                    }
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun showLeaveFormError(message: String) {
        updateLeaveForm { it.copy(isSubmitting = false, errorMessage = message) }
    }

    /** [retry] null shows the dialog without a "Try again" button. */
    private fun showError(message: String, retry: (() -> Unit)?) {
        retryAction = retry
        _uiState.update { it.copy(errorDialog = ErrorDialogUi(message, canRetry = retry != null)) }
    }

    private fun sendEffect(effect: AttendanceLeaveUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private fun updateLeaveForm(transform: (ApplyLeaveFormUi) -> ApplyLeaveFormUi) {
        _uiState.update { state ->
            val form = state.applyLeave ?: return@update state
            state.copy(applyLeave = transform(form))
        }
    }

    private fun showMonth(year: Int, month: Int) {
        _uiState.update {
            it.copy(
                year = year,
                month = month,
                summary = MonthlySummaryUi(),
                days = persistentMapOf()
            )
        }
        load()
    }

    private fun load() {
        // A slower response for a month the user already left must not overwrite the new one.
        loadJob?.cancel()
        val (year, month) = _uiState.value.let { it.year to it.month }
        _uiState.update {
            it.copy(isLoading = true, canGoNext = isBeforeCurrentMonth(year, month))
        }
        loadJob = viewModelScope.launch {
            when (val result = getMonthlyAttendanceUseCase(year = year, month = month)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        summary = result.data.summary.toUi(),
                        days = result.data.days.toImmutableMap()
                    )
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isLoading = false) }
                    showError(result.error.toUserMessage(), retry = ::load)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun isBeforeCurrentMonth(year: Int, month: Int): Boolean {
        val currentYear = currentYearNumber()
        return year < currentYear || (year == currentYear && month < currentMonthNumber())
    }

    private fun MonthlyAttendanceSummary.toUi() = MonthlySummaryUi(
        percentage = percentage,
        present = present,
        late = late,
        leave = leave,
        absent = absent
    )

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
