package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.AttendanceRepository

/** Loads (and caches) the four data sets the attendance popup needs. Called right after login. */
class LoadAttendanceDataUseCase(
    private val repository: AttendanceRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(forceRefresh: Boolean = false): DataResult<Unit> =
        withContext(dispatcherProvider.io) {
            val user = userSession.getUser()
            if (user == null || user.userId.isBlank()) {
                return@withContext DataResult.Error(AppError.Unauthorized("Please log in again"))
            }
            repository.loadAttendanceData(userId = user.userId, forceRefresh = forceRefresh)
        }
}
