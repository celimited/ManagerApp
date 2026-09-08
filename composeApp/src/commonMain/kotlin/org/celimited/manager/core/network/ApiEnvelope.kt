package org.celimited.manager.core.network

import kotlinx.serialization.Serializable

@Serializable
data class ApiEnvelope<T>(
    val data: T? = null,
    val success: Boolean = false,
    val statusCode: Int = 0,
    val message: String? = null
)
