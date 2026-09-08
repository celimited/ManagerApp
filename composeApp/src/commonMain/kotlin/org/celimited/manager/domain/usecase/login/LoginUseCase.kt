package org.celimited.manager.domain.usecase.login

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.domain.repository.AuthRepository
import org.celimited.manager.model.login.AuthUser

class LoginUseCase(
    private val authRepository: AuthRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    operator fun invoke(loginId: String, password: String, deviceInfo: DeviceInfo): Flow<DataResult<AuthUser>> =
        flow {
            emit(DataResult.Loading)
            emit(authRepository.login(loginId, password, deviceInfo))
        }.flowOn(dispatcherProvider.io)
}
