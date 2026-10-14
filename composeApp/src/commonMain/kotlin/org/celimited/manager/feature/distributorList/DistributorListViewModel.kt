package org.celimited.manager.feature.distributorList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.celimited.manager.component.ErrorDialogUi
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.domain.usecase.distributor.ObserveDistributorsUseCase
import org.celimited.manager.domain.usecase.distributor.RefreshDistributorsUseCase
import org.celimited.manager.model.attendance.Distributor

class DistributorListViewModel(
    private val observeDistributorsUseCase: ObserveDistributorsUseCase,
    private val refreshDistributorsUseCase: RefreshDistributorsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DistributorListUiState())
    val uiState: StateFlow<DistributorListUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<DistributorListUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    private val query = MutableStateFlow("")

    init {
        viewModelScope.launch {
            combine(observeDistributorsUseCase(), query) { distributors, q ->
                distributors.orEmpty().filterBy(q).map { it.toUi() }.toImmutableList()
            }.collect { items ->
                _uiState.update { it.copy(distributors = items) }
            }
        }
        // Distributors normally come from the login-time cache; fetch only if that load failed.
        viewModelScope.launch {
            if (observeDistributorsUseCase().first() == null) {
                _uiState.update { it.copy(isInitialLoading = true) }
                refresh()
                _uiState.update { it.copy(isInitialLoading = false) }
            }
        }
    }

    fun onRefresh() {
        if (_uiState.value.isRefreshing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            refresh()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun onRetry() {
        onDismissError()
        onRefresh()
    }

    fun onDismissError() {
        _uiState.update { it.copy(errorDialog = null) }
    }

    fun onDistributorClick(distributorId: Int) {
        viewModelScope.launch { _uiEffect.send(DistributorListUiEffect.NavigateToDetail(distributorId)) }
    }

    fun onQueryChange(value: String) {
        query.value = value
        _uiState.update { it.copy(query = value) }
    }

    private suspend fun refresh() {
        when (val result = refreshDistributorsUseCase()) {
            is DataResult.Success -> Unit
            is DataResult.Error -> {
                val error = result.error
                // Any cached list stays on screen underneath the dialog.
                if (error is AppError.Unauthorized) {
                    _uiEffect.send(DistributorListUiEffect.SessionExpired)
                } else {
                    _uiState.update { it.copy(errorDialog = ErrorDialogUi(error.toUserMessage())) }
                }
            }
            is DataResult.Loading -> Unit
        }
    }

    private fun List<Distributor>.filterBy(q: String): List<Distributor> {
        val term = q.trim()
        if (term.isEmpty()) return this
        return filter { it.name.contains(term, ignoreCase = true) || it.code.contains(term, ignoreCase = true) }
    }

    private fun Distributor.toUi() = DistributorItemUi(id = id, name = name, code = code, address = address)

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
