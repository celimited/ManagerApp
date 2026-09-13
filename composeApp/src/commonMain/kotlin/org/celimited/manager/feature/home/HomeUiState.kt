package org.celimited.manager.feature.home

import androidx.compose.runtime.Immutable

@Immutable
data class HomeUiState(
    val userName: String = "",
    val userRole: String = ""
)
