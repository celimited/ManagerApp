package org.celimited.manager.core.common.preferences

interface LoginPreferences {
    suspend fun saveLoginId(loginId: String)
    suspend fun getLoginId(): String?
}
