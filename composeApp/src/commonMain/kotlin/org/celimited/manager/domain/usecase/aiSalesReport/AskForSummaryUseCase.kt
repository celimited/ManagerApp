package org.celimited.manager.domain.usecase.aiSalesReport

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.session.UserSession
import org.celimited.manager.domain.repository.AISalesReportRepository
import org.celimited.manager.model.aiSalesReport.ReportSummary

class AskForSummaryUseCase(
    private val repository: AISalesReportRepository,
    private val userSession: UserSession,
    private val dispatcherProvider: DispatcherProvider
) {
    operator fun invoke(
        prompt: String,
        queryLogId: Int,
        columns: List<String>,
        rows: List<List<String>>
    ): Flow<DataResult<ReportSummary>> = flow {
        emit(DataResult.Loading)
        val user = userSession.getUser()
        if (user == null || user.userId.isBlank()) {
            emit(DataResult.Error(AppError.Unauthorized("Please log in again")))
            return@flow
        }
        emit(
            repository.askForSummary(
                userId = user.userId,
                role = user.role,
                prompt = prompt,
                queryLogId = queryLogId,
                columns = columns,
                rows = rows
            )
        )
    }.flowOn(dispatcherProvider.io)
}
