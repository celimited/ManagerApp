package org.celimited.manager.domain.usecase.login

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.preferences.LoginPreferences

class GetSavedLoginIdUseCase(
    private val loginPreferences: LoginPreferences,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): String? = withContext(dispatcherProvider.io) {
        loginPreferences.getLoginId()
    }
}
