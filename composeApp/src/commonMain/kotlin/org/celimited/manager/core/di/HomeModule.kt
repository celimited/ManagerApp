package org.celimited.manager.core.di

import org.celimited.manager.domain.usecase.home.GetCurrentUserUseCase
import org.celimited.manager.feature.home.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    factory { GetCurrentUserUseCase(get()) }
    viewModel { HomeViewModel(get()) }
}
