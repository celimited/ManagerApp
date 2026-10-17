package org.celimited.manager.domain.usecase.login

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.domain.repository.AuthRepository

/** Ends the session locally. The saved login ID is kept so the Login screen stays prefilled. */
class LogoutUseCase(
    private val authRepository: AuthRepository,
    private val attendanceRepository: AttendanceRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke() = withContext(dispatcherProvider.io) {
        authRepository.logout()
        attendanceRepository.clearSessionCache()
    }
}
