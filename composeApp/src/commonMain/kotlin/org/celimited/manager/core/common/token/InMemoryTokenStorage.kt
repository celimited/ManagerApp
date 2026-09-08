package org.celimited.manager.core.common.token

/**
 * Session-scoped placeholder — tokens are lost on process death. Swap for a
 * persistent (SQLDelight/secure-storage) implementation later without touching callers.
 */
class InMemoryTokenStorage : TokenStorage {
    private var tokens: AuthTokens? = null

    override suspend fun saveTokens(tokens: AuthTokens) {
        this.tokens = tokens
    }

    override suspend fun getTokens(): AuthTokens? = tokens

    override suspend fun clearTokens() {
        tokens = null
    }
}
