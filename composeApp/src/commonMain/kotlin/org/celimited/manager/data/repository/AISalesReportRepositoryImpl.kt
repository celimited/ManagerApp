package org.celimited.manager.data.repository

import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.network.safeRawApiCall
import org.celimited.manager.data.mapper.aiSalesReport.buildSummaryData
import org.celimited.manager.data.mapper.aiSalesReport.toDomain
import org.celimited.manager.data.remote.aiSalesReport.AISalesReportRemoteDataSource
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportRequestDto
import org.celimited.manager.data.remote.dto.aiSalesReport.AISummaryRequestDto
import org.celimited.manager.data.remote.dto.aiSalesReport.GetQueryLogResponseDto
import org.celimited.manager.domain.repository.AISalesReportRepository
import org.celimited.manager.model.aiSalesReport.QueryLogHistory
import org.celimited.manager.model.aiSalesReport.ReportSummary
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

    override suspend fun askForSummary(
        userId: String,
        role: String,
        prompt: String,
        queryLogId: Int,
        columns: List<String>,
        rows: List<List<String>>
    ): DataResult<ReportSummary> {
        val request = AISummaryRequestDto(
            UserID = userId,
            Role = role,
            UserPrompt = prompt,
            QueryLogID = queryLogId,
            Data = buildSummaryData(columns, rows)
        )

        return when (val result = safeRawApiCall { remoteDataSource.askForSummary(request) }) {
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

    override suspend fun getPreviousPrompts(userId: String): DataResult<QueryLogHistory> =
        fetchQueryLogs { remoteDataSource.getPreviousPrompts(userId) }

    override suspend fun getAIFAQs(userId: String): DataResult<QueryLogHistory> =
        fetchQueryLogs { remoteDataSource.getAIFAQs(userId) }

    /** Both query-log endpoints return the same envelope, so they share this unwrapping. */
    private suspend fun fetchQueryLogs(
        call: suspend () -> GetQueryLogResponseDto
    ): DataResult<QueryLogHistory> {
        return when (val result = safeRawApiCall { call() }) {
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
