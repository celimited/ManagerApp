package org.celimited.manager.domain.usecase.aiSalesReport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.AISalesReportRepository
import org.celimited.manager.model.aiSalesReport.QueryLogHistory

class GetAIFAQsUseCase(
    private val repository: AISalesReportRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    operator fun invoke(): Flow<DataResult<QueryLogHistory>> = flow {
        emit(DataResult.Loading)
        val user = userSession.getUser()
        if (user == null || user.userId.isBlank()) {
            emit(DataResult.Error(AppError.Unauthorized("Please log in again")))
            return@flow
        }
        emit(repository.getAIFAQs(userId = user.userId))
    }.flowOn(dispatcherProvider.io)
}
