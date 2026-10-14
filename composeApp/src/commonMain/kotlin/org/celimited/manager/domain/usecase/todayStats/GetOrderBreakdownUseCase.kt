package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.OrderBreakdownItem
import org.celimited.manager.model.todayStats.OrderGroupBy
import org.celimited.manager.model.todayStats.TodayStatsScope

class GetOrderBreakdownUseCase(
    private val repository: TodayStatsRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(
        groupBy: OrderGroupBy,
        scope: TodayStatsScope,
        search: String
    ): DataResult<List<OrderBreakdownItem>> = withContext(dispatcherProvider.io) {
        repository.getOrderBreakdown(groupBy, scope, search.trim().ifEmpty { null })
    }
}
