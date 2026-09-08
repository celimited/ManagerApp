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
    } catch (e: ClientRequestException) {
        val message = e.response.apiMessage()
        if (e.response.status == HttpStatusCode.Unauthorized) {
            DataResult.Error(AppError.Unauthorized(message))
        } else {
            DataResult.Error(AppError.Server(e.response.status.value, message))
        }
    } catch (e: ServerResponseException) {
        DataResult.Error(AppError.Server(e.response.status.value, e.response.apiMessage()))
    } catch (e: HttpRequestTimeoutException) {
        DataResult.Error(AppError.Timeout)
    } catch (e: SerializationException) {
        DataResult.Error(AppError.Serialization(e.message))
    } catch (e: IOException) {
        DataResult.Error(AppError.NoConnection)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        DataResult.Error(AppError.Unknown(e.message))
    }
}

/** Pulls the API's own "message" field out of an error response body, if present. */
private suspend fun HttpResponse.apiMessage(): String? = runCatching {
    body<JsonObject>()["message"]?.jsonPrimitive?.content
}.getOrNull()
