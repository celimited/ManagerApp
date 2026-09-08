package org.celimited.manager.data.remote.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val loginId: String,
    val password: String,
    val deviceInfo: DeviceInfoDto
)
