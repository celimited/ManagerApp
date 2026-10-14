package org.celimited.manager.data.remote.aiSalesReport

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportRequestDto
import org.celimited.manager.data.remote.dto.aiSalesReport.AIReportResponseDto
import org.celimited.manager.data.remote.dto.aiSalesReport.AISummaryRequestDto
import org.celimited.manager.data.remote.dto.aiSalesReport.AISummaryResponseDto
import org.celimited.manager.data.remote.dto.aiSalesReport.GetQueryLogResponseDto

class AISalesReportRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun askAI(request: AIReportRequestDto): AIReportResponseDto =
        httpClient.post("AIReportGenerator/AskOpenAI") {
            setBody(request)
        }.body()

    suspend fun askForSummary(request: AISummaryRequestDto): AISummaryResponseDto =
        httpClient.post("AIReportGenerator/AskOpenAIForSummary") {
            setBody(request)
        }.body()

    suspend fun getPreviousPrompts(userId: String): GetQueryLogResponseDto =
        httpClient.get("AIReportGenerator/GetCurrentUserAllQueryLog") {
            parameter("UserID", userId)
        }.body()

    /** Shares [GetQueryLogResponseDto] with [getPreviousPrompts] — both endpoints return the
     *  same envelope; the FAQ payload's extra `aiSummary` key is dropped by `ignoreUnknownKeys`. */
    suspend fun getAIFAQs(userId: String): GetQueryLogResponseDto =
        httpClient.get("AIReportGenerator/GetAIFAQs") {
            parameter("UserID", userId)
        }.body()
}
