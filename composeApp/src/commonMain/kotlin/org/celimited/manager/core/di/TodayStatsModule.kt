package org.celimited.manager.core.di

import org.celimited.manager.data.remote.todayStats.TodayStatsRemoteDataSource
import org.celimited.manager.data.repository.DummyTodayStatsRepository
import org.celimited.manager.data.repository.TodayStatsRepositoryImpl
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.domain.usecase.todayStats.GetOrderBreakdownUseCase
import org.celimited.manager.domain.usecase.todayStats.GetSrDetailUseCase
import org.celimited.manager.domain.usecase.todayStats.GetSrOrdersUseCase
import org.celimited.manager.domain.usecase.todayStats.GetSrStatsListUseCase
import org.celimited.manager.domain.usecase.todayStats.GetTeamSummaryUseCase
import org.celimited.manager.domain.usecase.todayStats.GetTodaySummaryUseCase
import org.celimited.manager.feature.todayStats.SrDetailViewModel
import org.celimited.manager.feature.todayStats.TodayStatsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// The hierarchy chips reuse GetSubordinateHierarchyLevelsUseCase from attendanceModule.
val todayStatsModule = module {
    single { TodayStatsRemoteDataSource(get(), get()) }
    // Endpoints not yet live fall back to the dummy repository.
    single<TodayStatsRepository> { TodayStatsRepositoryImpl(get(), DummyTodayStatsRepository()) }
    factory { GetTodaySummaryUseCase(get(), get(), get()) }
    factory { GetTeamSummaryUseCase(get(), get(), get()) }
    factory { GetOrderBreakdownUseCase(get(), get()) }
    factory { GetSrStatsListUseCase(get(), get(), get()) }
    factory { GetSrDetailUseCase(get(), get()) }
    factory { GetSrOrdersUseCase(get(), get()) }
    viewModel {
        TodayStatsViewModel(
            getTodaySummaryUseCase = get(),
            getTeamSummaryUseCase = get(),
            getOrderBreakdownUseCase = get(),
            getSrStatsListUseCase = get(),
            getSubordinateHierarchyLevelsUseCase = get()
        )
    }
    viewModel { params ->
        SrDetailViewModel(
            srId = params.get(),
            getSrDetailUseCase = get()
        )
    }
}
