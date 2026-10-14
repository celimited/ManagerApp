package org.celimited.manager.domain.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.core.common.device.DeviceInfo
import org.celimited.manager.model.distributor.DistributorOverview
import org.celimited.manager.model.distributor.DistributorStockSummary
import org.celimited.manager.model.distributor.SaveIssueParams
import org.celimited.manager.model.distributor.TeamMember

interface DistributorRepository {
    /** People an issue for [distributorId] can be assigned to. Not cached. */
    suspend fun getTeamMembers(distributorId: Int): DataResult<List<TeamMember>>

    /** Stock tab data (approvals, indent suggestion, summary, health) for [distributorId]. Not cached. */
    suspend fun getStockSummary(distributorId: Int): DataResult<DistributorStockSummary>

    /** Overview tab data (growth, target, invoice, stock status) for [distributorId]. Not cached. */
    suspend fun getOverview(distributorId: Int): DataResult<DistributorOverview>

    /** Returns the API's success message. */
    suspend fun saveIssue(params: SaveIssueParams, deviceInfo: DeviceInfo): DataResult<String>
}
