package org.celimited.manager.core.common.token

interface TokenStorage {
    suspend fun saveTokens(tokens: AuthTokens)
    suspend fun getTokens(): AuthTokens?
    suspend fun clearTokens()
}
