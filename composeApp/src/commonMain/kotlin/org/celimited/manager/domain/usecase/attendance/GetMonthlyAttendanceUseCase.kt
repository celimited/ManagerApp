package org.celimited.manager.domain.usecase.attendance

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.model.attendance.MonthlyAttendance

/** The logged-in user's attendance summary and calendar for one month ([month] is 1–12). */
class GetMonthlyAttendanceUseCase(
    private val repository: AttendanceRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(year: Int, month: Int): DataResult<MonthlyAttendance> =
        withContext(dispatcherProvider.io) {
            val user = userSession.getUser()
            if (user == null || user.userId.isBlank()) {
                return@withContext DataResult.Error(AppError.Unauthorized("Please log in again"))
            }
            repository.getMonthlyAttendance(userId = user.userId, year = year, month = month)
        }
}
