package org.celimited.manager.data.remote.login

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import org.celimited.manager.core.network.ApiEnvelope
import org.celimited.manager.data.remote.dto.login.LoginRequestDto
import org.celimited.manager.data.remote.dto.login.LoginResponseDataDto

class AuthRemoteDataSource(private val httpClient: HttpClient) {
    suspend fun login(request: LoginRequestDto): ApiEnvelope<LoginResponseDataDto> =
        httpClient.post("auth/login") {
            setBody(request)
        }.body()
}
