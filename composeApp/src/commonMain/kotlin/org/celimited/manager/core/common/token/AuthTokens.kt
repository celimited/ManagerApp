package org.celimited.manager.core.common.token

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String?,
    val expiresIn: Int,
    val refreshTokenExpiresIn: Int
)
