package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.SrSummary

/** Loads every SR under the logged-in user for the SR tab. */
class GetSrStatsListUseCase(
    private val repository: TodayStatsRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): DataResult<List<SrSummary>> = withContext(dispatcherProvider.io) {
        val user = userSession.getUser()
        if (user == null || user.userId.isBlank()) {
            DataResult.Error(AppError.Unauthorized("Please log in again"))
        } else {
            repository.getSrList(userId = user.userId)
        }
    }
}
