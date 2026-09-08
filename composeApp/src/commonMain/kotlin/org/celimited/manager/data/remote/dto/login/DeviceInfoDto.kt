package org.celimited.manager.data.remote.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class DeviceInfoDto(
    val deviceId: String,
    val deviceType: String,
    val os: String,
    val appVersion: String
)
