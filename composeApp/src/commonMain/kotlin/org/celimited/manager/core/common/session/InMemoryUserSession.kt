package org.celimited.manager.core.common.session

import org.celimited.manager.model.login.AuthUser

/**
 * Session-scoped placeholder — the user is lost on process death. Swap for a
 * persistent (SQLDelight/secure-storage) implementation later without touching callers.
 */
class InMemoryUserSession : UserSession {
    private var user: AuthUser? = null

    override suspend fun saveUser(user: AuthUser) {
        this.user = user
    }

    override suspend fun getUser(): AuthUser? = user

    override suspend fun clearUser() {
        user = null
    }
}
