package org.celimited.manager.core.common.session

import org.celimited.manager.model.login.AuthUser

interface UserSession {
    suspend fun saveUser(user: AuthUser)
    suspend fun getUser(): AuthUser?
    suspend fun clearUser()
}
