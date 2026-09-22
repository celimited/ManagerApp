package org.celimited.manager.data.repository

import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.network.safeRawApiCall
import org.celimited.manager.data.mapper.aiSalesReport.toDomain
import org.celimited.manager.data.remote.aiSalesReport.AISalesReportRemoteDataSource
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportRequestDto
import org.celimited.manager.domain.repository.AISalesReportRepository
import org.celimited.manager.model.aiSalesReport.QueryLogHistory
import org.celimited.manager.model.aiSalesReport.SalesReportResult

class AISalesReportRepositoryImpl(
    private val remoteDataSource: AISalesReportRemoteDataSource
) : AISalesReportRepository {

    override suspend fun askAI(
        userId: String,
        role: String,
        prompt: String,
        queryLogId: Int
    ): DataResult<SalesReportResult> {
        val request = AIReportRequestDto(
            UserID = userId,
            Role = role,
            UserPrompt = prompt,
            QueryLogID = queryLogId
        )

        return when (val result = safeRawApiCall { remoteDataSource.askAI(request) }) {
            is DataResult.Success -> {
                val response = result.data
                if (response.statusCode != 200) {
                    DataResult.Error(AppError.Business(response.message ?: "Request failed"))
                } else {
                    try {
                        DataResult.Success(response.toDomain())
                    } catch (e: Exception) {
                        DataResult.Error(AppError.Serialization(e.message))
                    }
                }
            }
            is DataResult.Error -> result
            is DataResult.Loading -> result
        }
    }

    override suspend fun getPreviousPrompts(userId: String): DataResult<QueryLogHistory> {
        return when (val result = safeRawApiCall { remoteDataSource.getPreviousPrompts(userId) }) {
            is DataResult.Success -> {
                val response = result.data
                if (response.statusCode != 200) {
                    DataResult.Error(AppError.Business(response.message ?: "Request failed"))
                } else {
                    try {
                        DataResult.Success(response.toDomain())
                    } catch (e: Exception) {
                        DataResult.Error(AppError.Serialization(e.message))
                    }
                }
            }
            is DataResult.Error -> result
            is DataResult.Loading -> result
        }
    }
}
