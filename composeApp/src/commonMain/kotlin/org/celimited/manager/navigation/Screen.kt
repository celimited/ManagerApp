package org.celimited.manager.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen: NavKey {

    @Serializable
    data object Home: NavKey, Screen
    @Serializable
    data object Login: NavKey, Screen
    @Serializable
    data object ForgotPassword: NavKey, Screen
    @Serializable
    data object ResetPassword: NavKey, Screen
    @Serializable
    data object Otp: NavKey, Screen
    @Serializable
    data object Approvals: NavKey, Screen
    @Serializable
    data object OrderStatus: NavKey, Screen
    @Serializable
    data object RetailVisit: NavKey, Screen
    @Serializable
    data object Menu: NavKey, Screen
    @Serializable
    data object Attendance: NavKey, Screen
    @Serializable
    data object TeamAttendance: NavKey, Screen
}