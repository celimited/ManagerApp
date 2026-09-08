package org.celimited.manager.domain.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.model.login.AuthUser

interface AuthRepository {
    suspend fun login(loginId: String, password: String, deviceInfo: DeviceInfo): DataResult<AuthUser>
}
