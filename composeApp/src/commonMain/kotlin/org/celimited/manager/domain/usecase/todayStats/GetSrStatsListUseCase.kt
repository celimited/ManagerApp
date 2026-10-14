package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.SrPage
import org.celimited.manager.model.todayStats.TodayStatsScope

class GetSrStatsListUseCase(
    private val repository: TodayStatsRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    /** [page] is 1-based. */
    suspend operator fun invoke(search: String, page: Int): DataResult<SrPage> =
        withContext(dispatcherProvider.io) {
            repository.getSrList(TodayStatsScope(), search.trim().ifEmpty { null }, page, PAGE_SIZE)
        }

    private companion object {
        const val PAGE_SIZE = 20
    }
}
