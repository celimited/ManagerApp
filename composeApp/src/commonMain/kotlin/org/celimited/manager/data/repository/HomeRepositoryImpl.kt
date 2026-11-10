package org.celimited.manager.data.repository

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.network.safeApiCall
import org.celimited.manager.data.mapper.home.toDomain
import org.celimited.manager.data.remote.home.HomeRemoteDataSource
import org.celimited.manager.domain.repository.HomeRepository
import org.celimited.manager.model.home.DashboardInfo
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class HomeRepositoryImpl(
    private val remoteDataSource: HomeRemoteDataSource,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) }
) : HomeRepository {

    override suspend fun getDashboardInfo(userId: String, userLevel: Int): DataResult<DashboardInfo> =
        when (val result = safeApiCall { remoteDataSource.getDashboardInfo(userId, userLevel, today().toString()) }) {
            is DataResult.Success -> DataResult.Success(result.data.toDomain())
            is DataResult.Error -> result
            DataResult.Loading -> DataResult.Loading
        }
}
