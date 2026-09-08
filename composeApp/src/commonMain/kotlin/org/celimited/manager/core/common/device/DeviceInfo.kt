package org.celimited.manager.core.common.device

data class DeviceInfo(
    val deviceId: String,
    val deviceType: String,
    val os: String,
    val appVersion: String
)
