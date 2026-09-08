package org.celimited.manager.data.mapper.login

import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.core.common.token.AuthTokens
import org.celimited.manager.data.remote.dto.login.DeviceInfoDto
import org.celimited.manager.data.remote.dto.login.TokensDto
import org.celimited.manager.data.remote.dto.login.UserDto
import org.celimited.manager.model.login.AuthUser

fun DeviceInfo.toDto(): DeviceInfoDto = DeviceInfoDto(
    deviceId = deviceId,
    deviceType = deviceType,
    os = os,
    appVersion = appVersion
)

fun UserDto.toDomain(): AuthUser = AuthUser(
    userId = userId.orEmpty(),
    fullName = fullName.orEmpty(),
    email = email.orEmpty(),
    phone = phone.orEmpty(),
    profilePictureUrl = profilePictureUrl.orEmpty(),
    role = role.orEmpty(),
    status = status.orEmpty()
)

fun TokensDto.toDomain(): AuthTokens = AuthTokens(
    accessToken = accessToken.orEmpty(),
    refreshToken = refreshToken.orEmpty(),
    tokenType = tokenType,
    expiresIn = expiresIn ?: 0,
    refreshTokenExpiresIn = refreshTokenExpiresIn ?: 0
)
