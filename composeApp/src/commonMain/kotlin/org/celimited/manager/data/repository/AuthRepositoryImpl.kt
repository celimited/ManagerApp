package org.celimited.manager.data.repository

import org.celimited.manager.core.common.AppError
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.core.common.token.TokenStorage
import org.celimited.manager.core.network.safeApiCall
import org.celimited.manager.data.mapper.login.toDomain
import org.celimited.manager.data.mapper.login.toDto
import org.celimited.manager.data.remote.dto.login.LoginRequestDto
import org.celimited.manager.data.remote.login.AuthRemoteDataSource
import org.celimited.manager.domain.repository.AuthRepository
import org.celimited.manager.model.login.AuthUser

class AuthRepositoryImpl(
    private val remoteDataSource: AuthRemoteDataSource,
    private val tokenStorage: TokenStorage
) : AuthRepository {

    override suspend fun login(loginId: String, password: String, deviceInfo: DeviceInfo): DataResult<AuthUser> {
        val request = LoginRequestDto(
            loginId = loginId,
            password = password,
            deviceInfo = deviceInfo.toDto()
        )

        val result = safeApiCall { remoteDataSource.login(request) }
        if (result is DataResult.Success) {
            val user = result.data.user
            val tokens = result.data.tokens
            if (user == null || tokens == null) {
                return DataResult.Error(AppError.Unknown("Incomplete response from server"))
            }
            tokenStorage.saveTokens(tokens.toDomain())
            return DataResult.Success(user.toDomain())
        }
        return DataResult.Error((result as DataResult.Error).error)
    }
}
