package org.celimited.manager.feature.addIssue

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
import kotlinx.datetime.LocalDate
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.domain.usecase.attendance.GetCurrentLocationUseCase
import org.celimited.manager.domain.usecase.distributor.GetTeamMembersUseCase
import org.celimited.manager.domain.usecase.distributor.SaveIssueUseCase
import org.celimited.manager.model.distributor.IssueCategory
import org.celimited.manager.model.distributor.IssuePriority
import org.celimited.manager.model.distributor.SaveIssueParams

class AddIssueViewModel(
    private val distributorId: Int,
    private val getTeamMembersUseCase: GetTeamMembersUseCase,
    private val saveIssueUseCase: SaveIssueUseCase,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddIssueUiState())
    val uiState: StateFlow<AddIssueUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<AddIssueUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    // Loaded on the first open and reused; null until a load succeeds.
    private var teamMembers: ImmutableList<DropdownOption>? = null
    private var saveJob: Job? = null

    // What the error dialog's "Try again" re-runs; set alongside a retryable errorDialog.
    private var retryAction: (() -> Unit)? = null

    /**
     * Called each time the screen is entered. ViewModels are Activity-scoped under Navigation 3,
     * so without this a return visit would reopen with the previous form still filled in.
     */
    fun onScreenOpened() {
        saveJob?.cancel()
        val cached = teamMembers
        _uiState.value = AddIssueUiState(
            teamMembers = cached ?: _uiState.value.teamMembers,
            isLoadingTeamMembers = cached == null
        )
        if (cached == null) loadTeamMembers()
    }

    fun onTitleChanged(value: String) {
        _uiState.update { it.copy(title = value.take(ISSUE_TITLE_MAX_LENGTH), errorMessage = null) }
    }

    fun onCategorySelected(category: IssueCategory) {
        _uiState.update { it.copy(category = category) }
    }

    fun onPrioritySelected(priority: IssuePriority) {
        _uiState.update { it.copy(priority = priority) }
    }

    fun onAssigneeSelected(userId: Int) {
        _uiState.update { it.copy(assigneeId = userId, errorMessage = null) }
    }

    fun onDeadlineSelected(date: LocalDate) {
        _uiState.update { it.copy(deadline = date, errorMessage = null) }
    }

    fun onDescriptionChanged(value: String) {
        _uiState.update { it.copy(description = value.take(ISSUE_DESCRIPTION_MAX_LENGTH), errorMessage = null) }
    }

    fun onRetryTeamMembers() {
        if (_uiState.value.isLoadingTeamMembers) return
        _uiState.update { it.copy(isLoadingTeamMembers = true, errorMessage = null) }
        loadTeamMembers()
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

    fun onSave() {
        val state = _uiState.value
        if (state.isSubmitting) return
        val error = when {
            state.title.isBlank() -> "Please enter an issue title."
            state.assigneeId == null -> "Please select a team member."
            state.deadline == null -> "Please select a deadline."
            state.description.isBlank() -> "Please enter a description."
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(errorMessage = error) }
            return
        }
        _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
        // The request carries the device location, so permission comes first.
        sendEffect(AddIssueUiEffect.RequestLocationPermission)
    }

    fun onLocationPermissionResult(granted: Boolean) {
        val state = _uiState.value
        if (!state.isSubmitting) return
        if (!granted) {
            showFormError("Location permission is required to save an issue.")
            return
        }
        val assigneeId = state.assigneeId ?: return
        val deadline = state.deadline ?: return

        saveJob = viewModelScope.launch {
            val location = when (val result = getCurrentLocationUseCase()) {
                is DataResult.Success -> result.data
                is DataResult.Error -> {
                    _uiState.update { it.copy(isSubmitting = false) }
                    showError(result.error.toUserMessage(), retry = null)
                    return@launch
                }
                is DataResult.Loading -> return@launch
            }
            val params = SaveIssueParams(
                distributorId = distributorId,
                title = state.title,
                category = state.category,
                priority = state.priority,
                assignToUserId = assigneeId,
                deadline = deadline,
                description = state.description,
                latitude = location.latitude,
                longitude = location.longitude
            )
            when (val result = saveIssueUseCase(params)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        result = IssueResultUi(
                            isSuccess = true,
                            message = result.data.ifBlank { "Issue saved successfully." }
                        )
                    )
                }
                is DataResult.Error -> {
                    if (result.error is AppError.Unauthorized) {
                        _uiState.update { it.copy(isSubmitting = false) }
                        _uiEffect.send(AddIssueUiEffect.SessionExpired)
                        return@launch
                    }
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            result = IssueResultUi(isSuccess = false, message = result.error.toUserMessage())
                        )
                    }
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    fun onResultClosed() {
        val wasSuccess = _uiState.value.result?.isSuccess == true
        _uiState.update { it.copy(result = null) }
        if (wasSuccess) sendEffect(AddIssueUiEffect.NavigateBack)
    }

    private fun loadTeamMembers() {
        viewModelScope.launch {
            when (val result = getTeamMembersUseCase(distributorId)) {
                is DataResult.Success -> {
                    val options = result.data.map { member ->
                        val label = if (member.designation.isBlank()) member.name else "${member.name} (${member.designation})"
                        DropdownOption(member.userId, label)
                    }.toImmutableList()
                    teamMembers = options
                    _uiState.update { it.copy(teamMembers = options, isLoadingTeamMembers = false) }
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isLoadingTeamMembers = false) }
                    if (result.error is AppError.Unauthorized) {
                        _uiEffect.send(AddIssueUiEffect.SessionExpired)
                    } else {
                        showError(result.error.toUserMessage(), retry = ::onRetryTeamMembers)
                    }
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun showFormError(message: String) {
        _uiState.update { it.copy(isSubmitting = false, errorMessage = message) }
    }

    /** [retry] null shows the dialog without a "Try again" button. */
    private fun showError(message: String, retry: (() -> Unit)?) {
        retryAction = retry
        _uiState.update { it.copy(errorDialog = ErrorDialogUi(message, canRetry = retry != null)) }
    }

    private fun sendEffect(effect: AddIssueUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
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
