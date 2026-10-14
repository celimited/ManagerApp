package org.celimited.manager.domain.usecase.distributor

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.DistributorRepository
import org.celimited.manager.model.distributor.DistributorOverview

class GetDistributorOverviewUseCase(
    private val repository: DistributorRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(distributorId: Int): DataResult<DistributorOverview> =
        withContext(dispatcherProvider.io) { repository.getOverview(distributorId) }
}
