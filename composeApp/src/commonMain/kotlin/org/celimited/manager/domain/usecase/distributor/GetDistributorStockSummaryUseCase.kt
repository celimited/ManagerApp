package org.celimited.manager.domain.usecase.distributor

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.DistributorRepository
import org.celimited.manager.model.distributor.DistributorStockSummary

class GetDistributorStockSummaryUseCase(
    private val repository: DistributorRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(distributorId: Int): DataResult<DistributorStockSummary> =
        withContext(dispatcherProvider.io) { repository.getStockSummary(distributorId) }
}
