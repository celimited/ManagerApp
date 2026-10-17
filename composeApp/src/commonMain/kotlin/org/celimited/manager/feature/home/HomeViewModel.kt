package org.celimited.manager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.format.formatShortDate
import org.celimited.manager.core.common.format.formatTimeOfDay
import org.celimited.manager.core.common.location.GeoLocation
import org.celimited.manager.domain.usecase.attendance.CheckGeofenceUseCase
import org.celimited.manager.domain.usecase.attendance.ConsumeAutoAttendancePopupUseCase
import org.celimited.manager.domain.usecase.attendance.GetAttendancePopupUseCase
import org.celimited.manager.domain.usecase.attendance.GetCurrentLocationUseCase
import org.celimited.manager.domain.usecase.attendance.ObserveAttendanceCardUseCase
import org.celimited.manager.domain.usecase.attendance.SubmitAttendanceUseCase
import org.celimited.manager.domain.usecase.home.GetCurrentUserUseCase
import org.celimited.manager.domain.usecase.login.LogoutUseCase
import org.celimited.manager.model.attendance.AttendanceCardStatus
import org.celimited.manager.model.attendance.AttendancePopupContent
import org.celimited.manager.model.attendance.AttendancePopupDecision
import org.celimited.manager.model.attendance.GeofenceResult
import org.celimited.manager.model.attendance.LateReason
import org.celimited.manager.model.attendance.LeaveInfo
import org.celimited.manager.model.attendance.SaveAttendanceParams
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class HomeViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getAttendancePopupUseCase: GetAttendancePopupUseCase,
    private val consumeAutoAttendancePopupUseCase: ConsumeAutoAttendancePopupUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase,
    private val checkGeofenceUseCase: CheckGeofenceUseCase,
    private val submitAttendanceUseCase: SubmitAttendanceUseCase,
    private val observeAttendanceCardUseCase: ObserveAttendanceCardUseCase,
    private val logoutUseCase: LogoutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<HomeUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    // Location held while the user decides on the out-of-range warning.
    private var pendingLocation: GeoLocation? = null

    init {
        loadCurrentUser()
        observeAttendanceCard()
    }

    private fun observeAttendanceCard() {
        viewModelScope.launch {
            observeAttendanceCardUseCase().collect { status ->
                _uiState.update { it.copy(attendanceCard = status.toUi()) }
            }
        }
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            _uiState.update {
                it.copy(
                    userName = user?.fullName.orEmpty(),
                    userRole = user?.role.orEmpty()
                )
            }
        }
    }

    /**
     * Called each time Home is shown. The popup auto-shows only once per login (the flag lives in
     * the session cache, so it also works if this ViewModel outlives a re-login); afterwards it's
     * reopened via the My Attendance card.
     */
    fun onScreenShown() {
        // This ViewModel can outlive a logout, so re-read the user for whoever is logged in now.
        loadCurrentUser()
        if (!consumeAutoAttendancePopupUseCase()) return
        viewModelScope.launch {
            val popup = getAttendancePopupUseCase().toUi()
            _uiState.update { it.copy(attendancePopup = popup) }
        }
    }

    fun onAttendanceCardClick() {
        sendEffect(HomeUiEffect.NavigateToAttendance)
    }

    fun onTodayStatsClick() {
        sendEffect(HomeUiEffect.NavigateToTodayStats)
    }

    fun onStockStatusClick() {
        sendEffect(HomeUiEffect.NavigateToDistributorList)
    }

    /** Tap on a section that isn't built yet. */
    fun onLockedFeatureClick() {
        sendEffect(HomeUiEffect.ShowUnderDevelopment)
    }

    fun onLogoutClick() {
        _uiState.update { it.copy(showLogoutConfirm = true) }
    }

    fun onLogoutDismissed() {
        _uiState.update { it.copy(showLogoutConfirm = false) }
    }

    fun onLogoutConfirmed() {
        _uiState.update { it.copy(showLogoutConfirm = false) }
        viewModelScope.launch {
            logoutUseCase()
            sendEffect(HomeUiEffect.NavigateToLogin)
        }
    }

    fun onGiveAttendanceClick() {
        if (_uiState.value.attendancePopup != AttendancePopupUi.None) return
        viewModelScope.launch {
            val content = getAttendancePopupUseCase()
            when (content.decision) {
                is AttendancePopupDecision.MarkAttendance,
                AttendancePopupDecision.Late,
                AttendancePopupDecision.WrongDeviceTime ->
                    _uiState.update { it.copy(attendancePopup = content.toUi()) }
                else -> sendEffect(HomeUiEffect.NavigateToAttendance)
            }
        }
    }

    fun onDistributorSelected(id: Int) {
        updateForm { it.copy(selectedDistributorId = id, errorMessage = null) }
    }

    fun onLateReasonSelected(id: Int) {
        updateForm { it.copy(selectedReasonId = id, errorMessage = null) }
    }

    fun onRemarksChanged(value: String) {
        updateForm { it.copy(remarks = value) }
    }

    fun onSubmitAttendance() {
        val popup = _uiState.value.attendancePopup as? AttendancePopupUi.CheckIn ?: return
        val form = popup.form
        if (form.isSubmitting) return

        val error = when {
            form.selectedDistributorId == null -> "Please select a location."
            popup.isLate && form.selectedReasonId == null -> "Please select a reason."
            else -> null
        }
        if (error != null) {
            updateForm { it.copy(errorMessage = error) }
            return
        }

        updateForm { it.copy(isSubmitting = true, errorMessage = null) }
        sendEffect(HomeUiEffect.RequestLocationPermission)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val popup = _uiState.value.attendancePopup as? AttendancePopupUi.CheckIn ?: return
        if (!granted) {
            showFormError("Location permission is required to mark attendance.")
            return
        }
        val distributorId = popup.form.selectedDistributorId ?: return

        viewModelScope.launch {
            val location = when (val result = getCurrentLocationUseCase()) {
                is DataResult.Success -> result.data
                is DataResult.Error -> {
                    showApiError(result.error.toUserMessage())
                    return@launch
                }
                is DataResult.Loading -> return@launch
            }

            when (val geofence = checkGeofenceUseCase(distributorId, location)) {
                GeofenceResult.Allowed -> submit(location)
                is GeofenceResult.Blocked -> showFormError(
                    "You are ${geofence.distanceMeters} m away from the selected location. " +
                        "You must be within ${geofence.radiusMeters} m to mark attendance."
                )
                is GeofenceResult.NeedsConfirmation -> {
                    pendingLocation = location
                    updateCheckIn {
                        it.copy(
                            form = it.form.copy(isSubmitting = false),
                            geofenceWarning = GeofenceWarningUi(geofence.distanceMeters, geofence.radiusMeters)
                        )
                    }
                }
            }
        }
    }

    fun onGeofenceWarningConfirmed() {
        val location = pendingLocation ?: return
        pendingLocation = null
        updateCheckIn {
            it.copy(geofenceWarning = null, form = it.form.copy(isSubmitting = true, errorMessage = null))
        }
        viewModelScope.launch { submit(location) }
    }

    fun onGeofenceWarningDismissed() {
        pendingLocation = null
        updateCheckIn { it.copy(geofenceWarning = null) }
    }

    /** Skip / Close on any popup. */
    fun onAttendancePopupDismissed() {
        val popup = _uiState.value.attendancePopup
        if (popup is AttendancePopupUi.CheckIn && popup.form.isSubmitting) return
        pendingLocation = null
        _uiState.update { it.copy(attendancePopup = AttendancePopupUi.None) }
    }

    private suspend fun submit(location: GeoLocation) {
        val popup = _uiState.value.attendancePopup as? AttendancePopupUi.CheckIn ?: return
        val form = popup.form
        val distributorId = form.selectedDistributorId ?: return
        val lateReason = if (popup.isLate) {
            form.lateReasons.firstOrNull { it.id == form.selectedReasonId }
                ?.let { LateReason(id = it.id, name = it.label) }
        } else {
            null
        }

        val params = SaveAttendanceParams(
            salespointId = distributorId,
            latitude = location.latitude,
            longitude = location.longitude,
            lateReason = lateReason,
            remarks = if (popup.isLate) form.remarks else null
        )
        when (val result = submitAttendanceUseCase(params)) {
            is DataResult.Success -> _uiState.update {
                it.copy(
                    attendancePopup = AttendancePopupUi.Success(
                        result.data.ifBlank { "Attendance saved successfully." }
                    )
                )
            }
            is DataResult.Error -> showApiError(result.error.toUserMessage())
            is DataResult.Loading -> Unit
        }
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorDialog = null) }
    }

    private fun showFormError(message: String) {
        updateForm { it.copy(isSubmitting = false, errorMessage = message) }
    }

    /** No "Try again" — the check-in popup stays open, so the user just taps Submit again. */
    private fun showApiError(message: String) {
        updateForm { it.copy(isSubmitting = false) }
        _uiState.update { it.copy(errorDialog = ErrorDialogUi(message, canRetry = false)) }
    }

    private fun updateForm(transform: (AttendanceFormUi) -> AttendanceFormUi) {
        updateCheckIn { it.copy(form = transform(it.form)) }
    }

    private fun updateCheckIn(transform: (AttendancePopupUi.CheckIn) -> AttendancePopupUi.CheckIn) {
        _uiState.update { state ->
            val popup = state.attendancePopup as? AttendancePopupUi.CheckIn ?: return@update state
            state.copy(attendancePopup = transform(popup))
        }
    }

    private fun sendEffect(effect: HomeUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private fun AttendancePopupContent.toUi(): AttendancePopupUi {
        val form = AttendanceFormUi(
            distributors = distributors.map { DropdownOption(it.id, it.name) }.toImmutableList(),
            lateReasons = lateReasons.map { DropdownOption(it.id, it.name) }.toImmutableList()
        )
        return when (val decision = decision) {
            AttendancePopupDecision.None -> AttendancePopupUi.None
            is AttendancePopupDecision.MarkAttendance ->
                AttendancePopupUi.CheckIn(isLate = false, minutesRemaining = decision.minutesRemaining, form = form)
            AttendancePopupDecision.Late ->
                AttendancePopupUi.CheckIn(isLate = true, minutesRemaining = null, form = form)
            is AttendancePopupDecision.OnLeave -> decision.leave.toUi(isApproved = true)
            is AttendancePopupDecision.LeavePending -> decision.leave.toUi(isApproved = false)
            AttendancePopupDecision.WrongDeviceTime -> AttendancePopupUi.WrongDeviceTime
        }
    }

    @OptIn(ExperimentalTime::class)
    private fun AttendanceCardStatus.toUi(): AttendanceCardUi = when (this) {
        AttendanceCardStatus.Unknown -> AttendanceCardUi.Unknown
        AttendanceCardStatus.NotGiven -> AttendanceCardUi.NotGiven
        AttendanceCardStatus.TimePassed -> AttendanceCardUi.TimePassed
        is AttendanceCardStatus.CheckedIn -> AttendanceCardUi.CheckedIn(
            time = epochMillis?.let {
                formatTimeOfDay(
                    Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
                )
            },
            isLate = isLate
        )
        AttendanceCardStatus.LeavePending -> AttendanceCardUi.LeavePending
        AttendanceCardStatus.OnLeave -> AttendanceCardUi.OnLeave
    }

    private fun LeaveInfo.toUi(isApproved: Boolean) = AttendancePopupUi.Leave(
        isApproved = isApproved,
        fromDate = fromDate?.let(::formatShortDate).orEmpty(),
        toDate = toDate?.let(::formatShortDate).orEmpty()
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
