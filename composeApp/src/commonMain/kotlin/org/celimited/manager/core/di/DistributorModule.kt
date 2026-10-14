package org.celimited.manager.core.di

import org.celimited.manager.data.remote.distributor.DistributorRemoteDataSource
import org.celimited.manager.data.repository.DistributorRepositoryImpl
import org.celimited.manager.domain.repository.DistributorRepository
import org.celimited.manager.domain.usecase.distributor.GetDistributorOverviewUseCase
import org.celimited.manager.domain.usecase.distributor.GetDistributorStockSummaryUseCase
import org.celimited.manager.domain.usecase.distributor.GetTeamMembersUseCase
import org.celimited.manager.domain.usecase.distributor.ObserveDistributorsUseCase
import org.celimited.manager.domain.usecase.distributor.RefreshDistributorsUseCase
import org.celimited.manager.domain.usecase.distributor.SaveIssueUseCase
import org.celimited.manager.feature.addIssue.AddIssueViewModel
import org.celimited.manager.feature.distributorDetail.DistributorDetailViewModel
import org.celimited.manager.feature.distributorList.DistributorListViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

// The distributor list lives in the attendance session cache, so its use cases take the
// AttendanceRepository from attendanceModule; issue-related calls go through DistributorRepository.
val distributorModule = module {
    single { DistributorRemoteDataSource(get(), get()) }
    single<DistributorRepository> { DistributorRepositoryImpl(get()) }
    factory { ObserveDistributorsUseCase(get()) }
    factory { RefreshDistributorsUseCase(get(), get()) }
    factory { GetTeamMembersUseCase(get(), get()) }
    factory { SaveIssueUseCase(get(), get(), get()) }
    factory { GetDistributorStockSummaryUseCase(get(), get()) }
    factory { GetDistributorOverviewUseCase(get(), get()) }
    viewModel { DistributorListViewModel(get(), get()) }
    viewModel { params ->
        DistributorDetailViewModel(
            distributorId = params.get(),
            observeDistributorsUseCase = get(),
            getDistributorStockSummaryUseCase = get(),
            getDistributorOverviewUseCase = get()
        )
    }
    viewModel { params ->
        AddIssueViewModel(
            distributorId = params.get(),
            getTeamMembersUseCase = get(),
            saveIssueUseCase = get(),
            getCurrentLocationUseCase = get()
        )
    }
}
