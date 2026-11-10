package org.celimited.manager.domain.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.model.home.DashboardInfo

interface HomeRepository {
    /** Today's Order + MTD Performance for today, from home/dashboard-info. Not cached. */
    suspend fun getDashboardInfo(userId: String, userLevel: Int): DataResult<DashboardInfo>
}
