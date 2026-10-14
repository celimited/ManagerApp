package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.SrDetail

class GetSrDetailUseCase(
    private val repository: TodayStatsRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(srId: Int): DataResult<SrDetail> =
        withContext(dispatcherProvider.io) { repository.getSrDetail(srId) }
}
