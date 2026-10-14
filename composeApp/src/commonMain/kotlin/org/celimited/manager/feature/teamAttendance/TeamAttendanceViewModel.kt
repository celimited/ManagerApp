package org.celimited.manager.feature.teamAttendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import org.celimited.manager.domain.usecase.attendance.GetSubordinateHierarchyLevelsUseCase
import org.celimited.manager.domain.usecase.attendance.GetTeamAttendanceSummaryUseCase
import org.celimited.manager.model.attendance.HierarchyLevel
import org.celimited.manager.model.attendance.TeamAttendanceCounts
import org.celimited.manager.model.attendance.TeamManager

class TeamAttendanceViewModel(
    private val getSubordinateHierarchyLevelsUseCase: GetSubordinateHierarchyLevelsUseCase,
    private val getTeamAttendanceSummaryUseCase: GetTeamAttendanceSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TeamAttendanceUiState())
    val uiState: StateFlow<TeamAttendanceUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<TeamAttendanceUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private var levelsJob: Job? = null
    private var summaryJob: Job? = null

    // What the error dialog's "Try again" re-runs; set alongside the errorDialog.
    private var retryAction: (() -> Unit)? = null

    /**
     * Called each time the screen is entered. ViewModels are Activity-scoped under Navigation 3, so
     * without this a return visit would show the previous visit's counts.
     */
    fun onScreenOpened() {
        levelsJob?.cancel()
        summaryJob?.cancel()
        retryAction = null
        _uiState.value = TeamAttendanceUiState()
        loadLevels()
    }

    fun onLevelSelected(key: String) {
        if (key == _uiState.value.selectedLevelKey) return
        // Clear the old level's data so it never shows under the new tab.
        _uiState.update {
            it.copy(selectedLevelKey = key, summary = TeamSummaryUi(), managers = persistentListOf())
        }
        loadSummary(key)
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

    private fun loadLevels() {
        if (levelsJob?.isActive == true) return
        _uiState.update { it.copy(isLoadingLevels = true) }
        levelsJob = viewModelScope.launch {
            when (val result = getSubordinateHierarchyLevelsUseCase()) {
                is DataResult.Success -> {
                    val levels = result.data.levels.map { it.toUi() }.toImmutableList()
                    _uiState.update { state ->
                        state.copy(
                            isLoadingLevels = false,
                            levels = levels,
                            // Keep the user's tab if it still exists, otherwise start on the first one.
                            selectedLevelKey = state.selectedLevelKey
                                ?.takeIf { key -> levels.any { it.key == key } }
                                ?: levels.firstOrNull()?.key
                        )
                    }
                    _uiState.value.selectedLevelKey?.let(::loadSummary)
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isLoadingLevels = false) }
                    handleError(result.error, retry = ::loadLevels)
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private fun loadSummary(levelKey: String) {
        // A slower response for a tab the user already left must not overwrite the new one.
        summaryJob?.cancel()
        _uiState.update { it.copy(isLoadingSummary = true) }
        summaryJob = viewModelScope.launch {
            when (val result = getTeamAttendanceSummaryUseCase(levelKey)) {
                is DataResult.Success -> _uiState.update {
                    it.copy(
                        isLoadingSummary = false,
                        summary = result.data.counts.toUi(),
                        managers = result.data.managers.map { manager -> manager.toUi() }.toImmutableList()
                    )
                }
                is DataResult.Error -> {
                    _uiState.update { it.copy(isLoadingSummary = false) }
                    handleError(result.error) {
                        _uiState.value.selectedLevelKey?.let(::loadSummary)
                    }
                }
                is DataResult.Loading -> Unit
            }
        }
    }

    private suspend fun handleError(error: AppError, retry: () -> Unit) {
        if (error is AppError.Unauthorized) {
            _uiEffect.send(TeamAttendanceUiEffect.SessionExpired)
            return
        }
        retryAction = retry
        _uiState.update { it.copy(errorDialog = ErrorDialogUi(error.toUserMessage())) }
    }

    private fun HierarchyLevel.toUi() = HierarchyLevelTabUi(
        key = key,
        name = name,
        label = "$name\n($presentCount/$totalCount)"
    )

    private fun TeamAttendanceCounts.toUi() = TeamSummaryUi(
        total = total,
        present = present,
        late = late,
        leave = leave,
        absent = absent
    )

    private fun TeamManager.toUi() = ManagerItemUi(
        userId = userId,
        name = fullName,
        initials = fullName.split(' ')
            .filter { part -> part.firstOrNull()?.isLetter() == true }
            .take(2)
            .joinToString("") { part -> part.first().uppercase() },
        designation = designation,
        location = locationPath,
        status = status,
        checkInTime = checkInTime
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
