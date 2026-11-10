package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.TeamSummary

/** Loads the My Team cards for one hierarchy level ([userLevel] from subordinate-hierarchy-levels). */
class GetTeamSummaryUseCase(
    private val repository: TodayStatsRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(userLevel: Int): DataResult<TeamSummary> = withContext(dispatcherProvider.io) {
        val user = userSession.getUser()
        if (user == null || user.userId.isBlank()) {
            DataResult.Error(AppError.Unauthorized("Please log in again"))
        } else {
            repository.getTeamSummary(userId = user.userId, userLevel = userLevel)
        }
    }
}
