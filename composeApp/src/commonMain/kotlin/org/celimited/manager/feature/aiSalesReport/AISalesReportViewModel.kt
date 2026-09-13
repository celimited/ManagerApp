package org.celimited.manager.feature.aiSalesReport

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
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.domain.usecase.aiSalesReport.AskAISalesReportUseCase
import org.celimited.manager.model.aiSalesReport.SalesReportResult

class AISalesReportViewModel(
    private val askAISalesReportUseCase: AskAISalesReportUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AISalesReportUiState())
    val uiState: StateFlow<AISalesReportUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<AISalesReportUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onPromptChanged(value: String) {
        _uiState.update { it.copy(promptInput = value) }
    }

    fun onSubmitClicked() {
        val prompt = _uiState.value.promptInput.trim()
        if (prompt.isBlank() || _uiState.value.isLoading) return

        viewModelScope.launch {
            askAISalesReportUseCase(prompt).collect { result ->
                when (result) {
                    is DataResult.Loading -> _uiState.update {
                        it.copy(isLoading = true, errorMessage = null)
                    }

                    is DataResult.Success -> _uiState.update {
                        it.copy(isLoading = false, errorMessage = null, result = result.data.toUi())
                    }

                    is DataResult.Error -> {
                        if (result.error is AppError.Unauthorized) {
                            sendEffect(AISalesReportUiEffect.SessionExpired)
                        }
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = result.error.toUserMessage())
                        }
                    }
                }
            }
        }
    }

    private fun sendEffect(effect: AISalesReportUiEffect) {
        viewModelScope.launch { _uiEffect.send(effect) }
    }

    private fun SalesReportResult.toUi(): ReportResultUi = ReportResultUi(
        columns = columns.toImmutableList(),
        rows = rows.map { it.toImmutableList() }.toImmutableList()
    )

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
