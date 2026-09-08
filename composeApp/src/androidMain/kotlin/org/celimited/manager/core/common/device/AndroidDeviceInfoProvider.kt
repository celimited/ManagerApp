package org.celimited.manager.core.common.device

import android.content.Context
import android.os.Build
import android.provider.Settings

class AndroidDeviceInfoProvider(private val context: Context) : DeviceInfoProvider {
    override fun getDeviceInfo(): DeviceInfo {
        val deviceId = runCatching {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        }.getOrNull() ?: "unknown"

        val appVersion = runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "unknown"

        return DeviceInfo(
            deviceId = deviceId,
            deviceType = "mobile",
            os = "Android ${Build.VERSION.RELEASE}",
            appVersion = appVersion
        )
    }
}
