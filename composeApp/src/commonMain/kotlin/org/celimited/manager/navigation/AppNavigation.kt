package org.celimited.manager.navigation

import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.celimited.manager.component.BottomNavigation
import org.celimited.manager.component.MainContainer
import org.celimited.manager.feature.approval.ApprovalsRoute
import org.celimited.manager.feature.attendance.AttendanceLeaveRoute
import org.celimited.manager.feature.forgotPassword.ForgotPasswordRoute
import org.celimited.manager.feature.home.HomeRoute
import org.celimited.manager.feature.login.LoginRoute
import org.celimited.manager.feature.menu.MenuRoute
import org.celimited.manager.feature.orderStatus.OrderStatusRoute
import org.celimited.manager.feature.otp.OTPRoute
import org.celimited.manager.feature.resetPassword.ResetPasswordRoute
import org.celimited.manager.feature.retailVisit.RetailVisitRoute
import org.celimited.manager.feature.teamAttendance.TeamAttendanceRoute

@Composable
fun AppNavigation(){
    val backStack = rememberNavBackStack(
        configuration = SavedStateConfiguration {
            serializersModule = SerializersModule {
                polymorphic(NavKey::class) {
                    subclass(Screen.Home::class, Screen.Home.serializer())
                    subclass(Screen.Login::class, Screen.Login.serializer())
                    subclass(Screen.ForgotPassword::class, Screen.ForgotPassword.serializer())
                    subclass(Screen.ResetPassword::class, Screen.ResetPassword.serializer())
                    subclass(Screen.Otp::class, Screen.Otp.serializer())
                    subclass(Screen.Attendance::class, Screen.Attendance.serializer())
                }
            }
        },
        Screen.Login
    )

    NavDisplay(
        backStack = backStack,
        entryProvider = { key ->

            NavEntry(key) {
                when (key) {
                    is Screen.Login -> {
                        LoginRoute(
                            onLogin = {
                                backStack.add(Screen.Home)
                            },
                            onForgotPasswordClick = {
                                backStack.add(Screen.ForgotPassword)
                            }
                        )
                    }

                    is Screen.ForgotPassword -> {
                        ForgotPasswordRoute(
                            onOtp = {
                                backStack.add(Screen.Otp)
                            }
                        )
                    }

                    is Screen.ResetPassword -> {
                        ResetPasswordRoute(
                            onResetDone = {
                                backStack.add(Screen.Home)
                            }
                        )
                    }

                    is Screen.Home -> {
                        MainContainer(
                            onAttendanceCardClick = {
                                backStack.add(Screen.Attendance)
                            }
                        )
                    }

                    is Screen.Otp -> {
                        OTPRoute(
                            onResetPassword = {
                                backStack.add(Screen.ResetPassword)
                            }
                        )
                    }

                    is Screen.Attendance -> {
                        AttendanceLeaveRoute(
                            onBackClick = {
                                backStack.removeLast()
                            },
                            teamAttendanceClick = {
                                backStack.add(Screen.TeamAttendance)
                            }
                        )
                    }

                    is Screen.TeamAttendance -> {
                        TeamAttendanceRoute(
                            onBackClick = {
                                backStack.removeLast()
                            }
                        )
                    }

                    else -> error("Unknown nav key: $key")
                }

            }

        }
    )
}