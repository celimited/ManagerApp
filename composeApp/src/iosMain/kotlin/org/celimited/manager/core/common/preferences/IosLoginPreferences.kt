package org.celimited.manager.core.common.preferences

import platform.Foundation.NSUserDefaults

class IosLoginPreferences : LoginPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun saveLoginId(loginId: String) {
        defaults.setObject(loginId, forKey = KEY_LOGIN_ID)
    }

    override suspend fun getLoginId(): String? = defaults.stringForKey(KEY_LOGIN_ID)

    private companion object {
        const val KEY_LOGIN_ID = "login_id"
    }
}
