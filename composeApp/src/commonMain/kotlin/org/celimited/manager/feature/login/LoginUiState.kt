package org.celimited.manager.feature.login

import androidx.compose.runtime.Immutable
import org.celimited.manager.component.ErrorDialogUi

@Immutable
data class LoginUiState(
    val loginId: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)
