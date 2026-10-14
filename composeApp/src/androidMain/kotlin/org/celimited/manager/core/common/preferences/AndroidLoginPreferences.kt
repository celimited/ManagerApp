package org.celimited.manager.core.common.preferences

import android.content.Context

class AndroidLoginPreferences(private val context: Context) : LoginPreferences {
    private val prefs by lazy { context.getSharedPreferences("login_prefs", Context.MODE_PRIVATE) }

    override suspend fun saveLoginId(loginId: String) {
        prefs.edit().putString(KEY_LOGIN_ID, loginId).apply()
    }

    override suspend fun getLoginId(): String? = prefs.getString(KEY_LOGIN_ID, null)

    private companion object {
        const val KEY_LOGIN_ID = "login_id"
    }
}
