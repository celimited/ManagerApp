package org.celimited.manager.core.di

import org.celimited.manager.data.remote.home.HomeRemoteDataSource
import org.celimited.manager.data.repository.HomeRepositoryImpl
import org.celimited.manager.domain.repository.HomeRepository
import org.celimited.manager.domain.usecase.home.GetCurrentUserUseCase
import org.celimited.manager.domain.usecase.home.GetDashboardInfoUseCase
import org.celimited.manager.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    single { HomeRemoteDataSource(get(), get()) }
    single<HomeRepository> { HomeRepositoryImpl(get()) }
    factory { GetCurrentUserUseCase(get()) }
    factory { GetDashboardInfoUseCase(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
