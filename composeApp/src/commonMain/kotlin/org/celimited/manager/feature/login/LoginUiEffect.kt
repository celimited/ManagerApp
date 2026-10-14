package org.celimited.manager.feature.login

sealed interface LoginUiEffect {
    data object NavigateToHome : LoginUiEffect
    /** Local validation only; API failures go to [LoginUiState.errorDialog]. */
    data class ShowError(val message: String) : LoginUiEffect
}
