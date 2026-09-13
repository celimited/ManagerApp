package org.celimited.manager.core.network

import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult

suspend fun <T> safeApiCall(block: suspend () -> ApiEnvelope<T>): DataResult<T> {
    return try {
        val envelope = block()
        val data = envelope.data
        if (envelope.success && data != null) {
            DataResult.Success(data)
        } else {
            DataResult.Error(AppError.Business(envelope.message ?: "Request failed"))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DataResult.Error(e.toAppError())
    }
}

/**
 * Like [safeApiCall], but for endpoints that return the response body directly
 * instead of wrapping it in the app's [ApiEnvelope] shape.
 */
suspend fun <T> safeRawApiCall(block: suspend () -> T): DataResult<T> {
    return try {
        DataResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DataResult.Error(e.toAppError())
    }
}

private suspend fun Exception.toAppError(): AppError = when (this) {
    is ClientRequestException -> {
        val message = response.apiMessage()
        if (response.status == HttpStatusCode.Unauthorized) {
            AppError.Unauthorized(message)
        } else {
            AppError.Server(response.status.value, message)
        }
    }
    is ServerResponseException -> AppError.Server(response.status.value, response.apiMessage())
    is HttpRequestTimeoutException -> AppError.Timeout
    is SerializationException -> AppError.Serialization(message)
    is IOException -> AppError.NoConnection
    else -> AppError.Unknown(message)
}

/** Pulls the API's own "message" field out of an error response body, if present. */
private suspend fun HttpResponse.apiMessage(): String? = runCatching {
    body<JsonObject>()["message"]?.jsonPrimitive?.content
}.getOrNull()
