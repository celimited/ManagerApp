package org.celimited.manager.core.common.device

import platform.Foundation.NSBundle
import platform.UIKit.UIDevice

class IosDeviceInfoProvider : DeviceInfoProvider {
    override fun getDeviceInfo(): DeviceInfo {
        val deviceId = UIDevice.currentDevice.identifierForVendor?.UUIDString() ?: "unknown"
        val appVersion = NSBundle.mainBundle.infoDictionary
            ?.get("CFBundleShortVersionString") as? String ?: "unknown"

        return DeviceInfo(
            deviceId = deviceId,
            deviceType = "mobile",
            os = "iOS ${UIDevice.currentDevice.systemVersion}",
            appVersion = appVersion
        )
    }
}
