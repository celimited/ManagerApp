package org.celimited.manager.domain.usecase.aiSalesReport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.AISalesReportRepository
import org.celimited.manager.model.aiSalesReport.SalesReportResult


class AskAISalesReportUseCase(
    private val repository: AISalesReportRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    operator fun invoke(prompt: String): Flow<DataResult<SalesReportResult>> = flow {
        emit(DataResult.Loading)
        val user = userSession.getUser()
        if (user == null || user.userId.isBlank()) {
            emit(DataResult.Error(AppError.Unauthorized("Please log in again")))
            return@flow
        }
        emit(
            repository.askAI(
                userId = user.userId,
                role = user.role,
                prompt = prompt
            )
        )
    }.flowOn(dispatcherProvider.io)
}
