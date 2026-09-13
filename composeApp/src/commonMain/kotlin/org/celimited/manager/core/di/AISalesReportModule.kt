package org.celimited.manager.core.di

import org.celimited.manager.data.remote.aiSalesReport.AISalesReportRemoteDataSource
import org.celimited.manager.data.repository.AISalesReportRepositoryImpl
import org.celimited.manager.domain.repository.AISalesReportRepository
import org.celimited.manager.domain.usecase.aiSalesReport.AskAISalesReportUseCase
import org.celimited.manager.feature.aiSalesReport.AISalesReportViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val aiSalesReportModule = module {
    single { AISalesReportRemoteDataSource(get()) }
    single<AISalesReportRepository> { AISalesReportRepositoryImpl(get()) }
    factory { AskAISalesReportUseCase(get(), get(), get()) }
    viewModel { AISalesReportViewModel(get()) }
}
