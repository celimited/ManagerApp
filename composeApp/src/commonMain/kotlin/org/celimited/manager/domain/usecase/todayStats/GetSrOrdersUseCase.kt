package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.OrderGroupBy
import org.celimited.manager.model.todayStats.SrOrderItem

class GetSrOrdersUseCase(
    private val repository: TodayStatsRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(srId: Int, groupBy: OrderGroupBy, search: String): DataResult<List<SrOrderItem>> =
        withContext(dispatcherProvider.io) {
            repository.getSrOrders(srId, groupBy, search.trim().ifEmpty { null })
        }
}
