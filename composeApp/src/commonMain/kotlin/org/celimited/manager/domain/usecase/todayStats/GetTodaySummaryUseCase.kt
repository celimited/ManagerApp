package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.TodaySummary

/** Loads every My Summary card for the logged-in user in one call. */
class GetTodaySummaryUseCase(
    private val repository: TodayStatsRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): DataResult<TodaySummary> = withContext(dispatcherProvider.io) {
        val user = userSession.getUser()
        if (user == null || user.userId.isBlank()) {
            DataResult.Error(AppError.Unauthorized("Please log in again"))
        } else {
            repository.getSummary(userId = user.userId, userLevel = user.userLevel)
        }
    }
}
