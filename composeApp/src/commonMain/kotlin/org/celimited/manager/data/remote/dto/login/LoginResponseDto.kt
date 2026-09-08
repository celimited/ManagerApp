package org.celimited.manager.data.remote.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponseDataDto(
    val user: UserDto? = null,
    val tokens: TokensDto? = null
)

@Serializable
data class UserDto(
    val userId: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val profilePictureUrl: String? = null,
    val role: String? = null,
    val status: String? = null
)

@Serializable
data class TokensDto(
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val tokenType: String? = null,
    val expiresIn: Int? = null,
    val refreshTokenExpiresIn: Int? = null
)
