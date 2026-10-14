package org.celimited.manager.domain.usecase.todayStats

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.DispatcherProvider
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.TodayStatsOverview
import org.celimited.manager.model.todayStats.TodayStatsScope

/**
 * Loads the five card sections (attendance, productivity, target, inventory, zero sales) for [TodayStatsScope]
 * in parallel. The cards are shown together, so the first failing section fails the whole overview.
 */
class GetTodayStatsOverviewUseCase(
    private val repository: TodayStatsRepository,
    private val dispatcherProvider: DispatcherProvider
) {
    suspend operator fun invoke(scope: TodayStatsScope): DataResult<TodayStatsOverview> =
        withContext(dispatcherProvider.io) {
            coroutineScope {
                val attendance = async { repository.getAttendance(scope) }
                val productivity = async { repository.getProductivity(scope) }
                val target = async { repository.getTarget(scope) }
                val inventory = async { repository.getInventory(scope) }
                val zeroSales = async { repository.getZeroSales(scope) }

                DataResult.Success(
                    TodayStatsOverview(
                        attendance = attendance.await().dataOr { return@coroutineScope it },
                        productivity = productivity.await().dataOr { return@coroutineScope it },
                        target = target.await().dataOr { return@coroutineScope it },
                        inventory = inventory.await().dataOr { return@coroutineScope it },
                        zeroSales = zeroSales.await().dataOr { return@coroutineScope it }
                    )
                )
            }
        }

    private inline fun <T> DataResult<T>.dataOr(onFailure: (DataResult.Error) -> Nothing): T = when (this) {
        is DataResult.Success -> data
        is DataResult.Error -> onFailure(this)
        // Repositories return a final result, never Loading.
        is DataResult.Loading -> error("Unexpected Loading from TodayStatsRepository")
    }
}
