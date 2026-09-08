package org.celimited.manager.feature.login

sealed interface LoginUiEffect {
    data object NavigateToHome : LoginUiEffect
    data class ShowError(val message: String) : LoginUiEffect
}
