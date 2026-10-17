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
import org.celimited.manager.feature.aiSalesReport.AISalesReportRoute
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
import org.celimited.manager.feature.addIssue.AddIssueRoute
import org.celimited.manager.feature.distributorDetail.DistributorDetailRoute
import org.celimited.manager.feature.distributorList.DistributorListRoute
import org.celimited.manager.feature.todayStats.SrDetailRoute
import org.celimited.manager.feature.todayStats.TodayStatsRoute

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
                    subclass(Screen.AISalesReport::class, Screen.AISalesReport.serializer())
                    subclass(Screen.TodayStats::class, Screen.TodayStats.serializer())
                    subclass(Screen.SrDetail::class, Screen.SrDetail.serializer())
                    subclass(Screen.DistributorList::class, Screen.DistributorList.serializer())
                    subclass(Screen.DistributorDetail::class, Screen.DistributorDetail.serializer())
                    subclass(Screen.AddIssue::class, Screen.AddIssue.serializer())
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
                                println("Login page")
                                backStack.clear()
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
                            },
                            onAISalesReportClick = {
                                backStack.add(Screen.AISalesReport)
                            },
                            onTodayStatsClick = {
                                backStack.add(Screen.TodayStats)
                            },
                            onStockStatusClick = {
                                backStack.add(Screen.DistributorList)
                            },
                            onLogout = {
                                backStack.clear()
                                backStack.add(Screen.Login)
                            }
                        )
                    }

                    is Screen.DistributorList -> {
                        DistributorListRoute(
                            onBackClick = {
                                backStack.removeLast()
                            },
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Screen.Login)
                            },
                            onDistributorClick = { distributorId ->
                                backStack.add(Screen.DistributorDetail(distributorId))
                            }
                        )
                    }

                    is Screen.DistributorDetail -> {
                        DistributorDetailRoute(
                            distributorId = key.distributorId,
                            onBackClick = {
                                backStack.removeLast()
                            },
                            onAddIssueClick = {
                                backStack.add(Screen.AddIssue(key.distributorId))
                            },
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Screen.Login)
                            }
                        )
                    }

                    is Screen.AddIssue -> {
                        AddIssueRoute(
                            distributorId = key.distributorId,
                            onBackClick = {
                                backStack.removeLast()
                            },
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Screen.Login)
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

                    is Screen.AISalesReport -> {
                        AISalesReportRoute (
                            onBackClick = {
                                backStack.removeLast()
                            },
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Screen.Login)
                            }
                        )
                    }

                    is Screen.TeamAttendance -> {
                        TeamAttendanceRoute(
                            onBackClick = {
                                backStack.removeLast()
                            },
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Screen.Login)
                            }
                        )
                    }

                    is Screen.TodayStats -> {
                        TodayStatsRoute(
                            onBackClick = {
                                backStack.removeLast()
                            },
                            onSrClick = { srId ->
                                backStack.add(Screen.SrDetail(srId))
                            },
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Screen.Login)
                            }
                        )
                    }

                    is Screen.SrDetail -> {
                        SrDetailRoute(
                            srId = key.srId,
                            onBackClick = {
                                backStack.removeLast()
                            },
                            onSessionExpired = {
                                backStack.clear()
                                backStack.add(Screen.Login)
                            }
                        )
                    }

                    else -> error("Unknown nav key: $key")
                }

            }

        }
    )
}