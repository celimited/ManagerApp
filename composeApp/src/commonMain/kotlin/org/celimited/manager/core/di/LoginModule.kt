package org.celimited.manager.core.di

import org.celimited.manager.data.remote.login.AuthRemoteDataSource
import org.celimited.manager.data.repository.AuthRepositoryImpl
import org.celimited.manager.domain.repository.AuthRepository
import org.celimited.manager.domain.usecase.login.LoginUseCase
import org.celimited.manager.feature.login.LoginViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val loginModule = module {
    single { AuthRemoteDataSource(get()) }
    single<AuthRepository> { AuthRepositoryImpl(get(), get()) }
    factory { LoginUseCase(get(), get()) }
    viewModel { LoginViewModel(get(), get()) }
}
