package org.celimited.manager.core.common.location

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.darwin.NSObject

@Composable
actual fun rememberLocationPermissionRequester(onResult: (Boolean) -> Unit): () -> Unit {
    val currentOnResult by rememberUpdatedState(onResult)
    val requester = remember { IosLocationPermissionRequester() }
    return { requester.request { granted -> currentOnResult(granted) } }
}

private class IosLocationPermissionRequester : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    private var pending: ((Boolean) -> Unit)? = null

    fun request(callback: (Boolean) -> Unit) {
        manager.delegate = this
        when (manager.authorizationStatus) {
            kCLAuthorizationStatusAuthorizedWhenInUse, kCLAuthorizationStatusAuthorizedAlways -> callback(true)
            kCLAuthorizationStatusNotDetermined -> {
                pending = callback
                manager.requestWhenInUseAuthorization()
            }
            else -> callback(false)
        }
    }

    // iOS 14+. Also fires once when the delegate is set; ignored unless a request is pending.
    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        val callback = pending ?: return
        val status = manager.authorizationStatus
        if (status == kCLAuthorizationStatusNotDetermined) return
        pending = null
        callback(status == kCLAuthorizationStatusAuthorizedWhenInUse || status == kCLAuthorizationStatusAuthorizedAlways)
    }
}
