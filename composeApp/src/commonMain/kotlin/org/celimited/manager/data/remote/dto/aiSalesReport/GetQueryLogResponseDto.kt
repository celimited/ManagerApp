package org.celimited.manager.data.remote.dto.aiSalesReport

import kotlinx.serialization.Serializable

@Serializable
data class GetQueryLogResponseDto(
    val data: QueryLogDataDto? = null,
    val statusCode: Int = 0,
    val message: String? = null
)
