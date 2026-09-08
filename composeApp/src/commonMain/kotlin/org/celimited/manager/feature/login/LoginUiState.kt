package org.celimited.manager.feature.login

import androidx.compose.runtime.Immutable

@Immutable
data class LoginUiState(
    val loginId: String = "",
    val password: String = "",
    val isLoading: Boolean = false
)
