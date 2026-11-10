package org.celimited.manager.domain.usecase.home

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.HomeRepository
import org.celimited.manager.model.home.DashboardInfo

/** Loads Home's Today's Order and MTD Performance blocks for the logged-in user. */
class GetDashboardInfoUseCase(
    private val repository: HomeRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(): DataResult<DashboardInfo> = withContext(dispatcherProvider.io) {
        val user = userSession.getUser()
        if (user == null || user.userId.isBlank()) {
            DataResult.Error(AppError.Unauthorized("Please log in again"))
        } else {
            repository.getDashboardInfo(userId = user.userId, userLevel = user.userLevel)
        }
    }
}
