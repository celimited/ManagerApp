package org.celimited.manager.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.celimited.manager.domain.usecase.home.GetCurrentUserUseCase

class HomeViewModel(
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            _uiState.update {
                it.copy(
                    userName = user?.fullName.orEmpty(),
                    userRole = user?.role.orEmpty()
                )
            }
        }
    }
}
