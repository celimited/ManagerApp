package org.celimited.manager.data.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.network.safeApiCall
import org.celimited.manager.data.mapper.todayStats.toDomain
import org.celimited.manager.data.remote.todayStats.TodayStatsRemoteDataSource
import org.celimited.manager.domain.repository.TodayStatsRepository
import org.celimited.manager.model.todayStats.AttendanceStats
import org.celimited.manager.model.todayStats.ProductivityStats
import org.celimited.manager.model.todayStats.SectionStats
import org.celimited.manager.model.todayStats.SrPage
import org.celimited.manager.model.todayStats.TodayStatsScope
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Real /today-stats/ calls where the endpoint exists; everything else is delegated to [fallback]
 * (DummyTodayStatsRepository) until its endpoint lands.
 */
@OptIn(ExperimentalTime::class)
class TodayStatsRepositoryImpl(
    private val remoteDataSource: TodayStatsRemoteDataSource,
    private val fallback: TodayStatsRepository,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
) : TodayStatsRepository by fallback {

    override suspend fun getAttendance(scope: TodayStatsScope): DataResult<SectionStats<AttendanceStats>> {
        // My Team (level rows) still uses dummy data until level/unit ids are wired up.
        if (scope.levelKey != null) return fallback.getAttendance(scope)
        return safeApiCall { remoteDataSource.getAttendance(LEVEL_ID, UNIT_ID, today().toString()) }
            .map { it.toDomain() }
    }

    override suspend fun getProductivity(scope: TodayStatsScope): DataResult<SectionStats<ProductivityStats>> {
        if (scope.levelKey != null) return fallback.getProductivity(scope)
        return safeApiCall { remoteDataSource.getProductivity(LEVEL_ID, UNIT_ID, today().toString()) }
            .map { it.toDomain() }
    }

    override suspend fun getSrList(
        scope: TodayStatsScope,
        search: String?,
        page: Int,
        pageSize: Int
    ): DataResult<SrPage> =
        safeApiCall { remoteDataSource.getSrList(LEVEL_ID, UNIT_ID, today().toString(), search, page, pageSize) }
            .map { it.toDomain(requestedPage = page) }

    private inline fun <T, R> DataResult<T>.map(transform: (T) -> R): DataResult<R> = when (this) {
        is DataResult.Success -> DataResult.Success(transform(data))
        is DataResult.Error -> this
        is DataResult.Loading -> this
    }

    private companion object {
        // TODO: replace with the logged-in user's real level/unit ids once the API exposes them.
        const val LEVEL_ID = 1
        const val UNIT_ID = 2
    }
}
