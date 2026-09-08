package org.celimited.manager.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.device.DeviceInfoProvider
import org.celimited.manager.domain.usecase.login.LoginUseCase

class LoginViewModel(
    private val loginUseCase: LoginUseCase,
    private val deviceInfoProvider: DeviceInfoProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _uiEffect = Channel<LoginUiEffect>(Channel.BUFFERED)
    val uiEffect = _uiEffect.receiveAsFlow()

    fun onLoginIdChanged(value: String) {
        _uiState.update { it.copy(loginId = value) }
    }

    fun onPasswordChanged(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onLoginClicked() {
        val state = _uiState.value
        if (state.loginId.isBlank() || state.password.isBlank()) {
            sendEffect(LoginUiEffect.ShowError("Please enter your login ID and password"))
            return
        }

        viewModelScope.launch {
            loginUseCase(state.loginId, state.password, deviceInfoProvider.getDeviceInfo()).collect { result ->
                when (result) {
                    is DataResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                    is DataResult.Success -> {
                        _uiState.update { it.copy(isLoading = false) }
                        sendEffect(LoginUiEffect.NavigateToHome)
                    }
                    is DataResult.Error -> {
                        _uiState.update { it.copy(isLoading = false) }
                        sendEffect(LoginUiEffect.ShowError(result.error.toUserMessage()))
                    }
                }
            }
        }
    }

    private fun sendEffect(effect: LoginUiEffect) {
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
