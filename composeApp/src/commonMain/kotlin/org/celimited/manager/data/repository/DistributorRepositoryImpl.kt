package org.celimited.manager.data.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.core.network.safeApiCall
import org.celimited.manager.core.network.safeApiCallNoData
import org.celimited.manager.data.mapper.distributor.toDomain
import org.celimited.manager.data.mapper.distributor.toRequestDto
import org.celimited.manager.data.mapper.distributor.toTeamMembers
import org.celimited.manager.data.remote.distributor.DistributorRemoteDataSource
import org.celimited.manager.domain.repository.DistributorRepository
import org.celimited.manager.model.distributor.DistributorOverview
import org.celimited.manager.model.distributor.DistributorStockSummary
import org.celimited.manager.model.distributor.SaveIssueParams
import org.celimited.manager.model.distributor.TeamMember

class DistributorRepositoryImpl(
    private val remoteDataSource: DistributorRemoteDataSource
) : DistributorRepository {

    override suspend fun getTeamMembers(distributorId: Int): DataResult<List<TeamMember>> =
        when (val result = safeApiCall { remoteDataSource.getTeamMembers(distributorId) }) {
            is DataResult.Success -> DataResult.Success(result.data.toTeamMembers())
            is DataResult.Error -> result
            is DataResult.Loading -> result
        }

    override suspend fun getStockSummary(distributorId: Int): DataResult<DistributorStockSummary> =
        when (val result = safeApiCall { remoteDataSource.getStockSummary(distributorId) }) {
            is DataResult.Success -> DataResult.Success(result.data.toDomain())
            is DataResult.Error -> result
            is DataResult.Loading -> result
        }

    override suspend fun getOverview(distributorId: Int): DataResult<DistributorOverview> =
        when (val result = safeApiCall { remoteDataSource.getOverview(distributorId) }) {
            is DataResult.Success -> DataResult.Success(result.data.toDomain())
            is DataResult.Error -> result
            is DataResult.Loading -> result
        }

    override suspend fun saveIssue(params: SaveIssueParams, deviceInfo: DeviceInfo): DataResult<String> =
        safeApiCallNoData { remoteDataSource.saveIssue(params.toRequestDto(deviceInfo)) }
}
