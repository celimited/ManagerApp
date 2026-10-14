package org.celimited.manager.core.di

import org.celimited.manager.data.local.attendance.AttendanceLocalDataSource
import org.celimited.manager.data.remote.attendance.AttendanceRemoteDataSource
import org.celimited.manager.data.repository.AttendanceRepositoryImpl
import org.celimited.manager.domain.repository.AttendanceRepository
import org.celimited.manager.domain.usecase.attendance.ApplyLeaveUseCase
import org.celimited.manager.domain.usecase.attendance.CheckGeofenceUseCase
import org.celimited.manager.domain.usecase.attendance.ConsumeAutoAttendancePopupUseCase
import org.celimited.manager.domain.usecase.attendance.GetAttendancePopupUseCase
import org.celimited.manager.domain.usecase.attendance.GetCurrentLocationUseCase
import org.celimited.manager.domain.usecase.attendance.GetLeaveReasonsUseCase
import org.celimited.manager.domain.usecase.attendance.GetMonthlyAttendanceUseCase
import org.celimited.manager.domain.usecase.attendance.LoadAttendanceDataUseCase
import org.celimited.manager.domain.usecase.attendance.ObserveAttendanceCardUseCase
import org.celimited.manager.domain.usecase.attendance.GetSubordinateHierarchyLevelsUseCase
import org.celimited.manager.domain.usecase.attendance.GetTeamAttendanceSummaryUseCase
import org.celimited.manager.domain.usecase.attendance.SubmitAttendanceUseCase
import org.celimited.manager.feature.attendance.AttendanceLeaveViewModel
import org.celimited.manager.feature.teamAttendance.TeamAttendanceViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val attendanceModule = module {
    single { AttendanceRemoteDataSource(get(), get()) }
    single { AttendanceLocalDataSource() }
    single<AttendanceRepository> { AttendanceRepositoryImpl(get(), get()) }
    factory { LoadAttendanceDataUseCase(get(), get(), get()) }
    factory { GetAttendancePopupUseCase(get(), get()) }
    factory { ConsumeAutoAttendancePopupUseCase(get()) }
    factory { GetCurrentLocationUseCase(get()) }
    factory { CheckGeofenceUseCase(get()) }
    factory { SubmitAttendanceUseCase(get(), get(), get(), get()) }
    factory { ObserveAttendanceCardUseCase(get()) }
    factory { GetMonthlyAttendanceUseCase(get(), get(), get()) }
    factory { GetLeaveReasonsUseCase(get(), get()) }
    factory { ApplyLeaveUseCase(get(), get(), get()) }
    factory { GetSubordinateHierarchyLevelsUseCase(get(), get()) }
    factory { GetTeamAttendanceSummaryUseCase(get(), get()) }
    viewModel { AttendanceLeaveViewModel(get(), get(), get(), get()) }
    viewModel { TeamAttendanceViewModel(get(), get()) }
}
