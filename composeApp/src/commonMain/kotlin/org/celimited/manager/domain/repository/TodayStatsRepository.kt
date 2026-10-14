package org.celimited.manager.domain.repository

import org.celimited.manager.core.common.DataResult
import org.celimited.manager.model.todayStats.AttendanceStats
import org.celimited.manager.model.todayStats.InventoryStats
import org.celimited.manager.model.todayStats.OrderBreakdownItem
import org.celimited.manager.model.todayStats.OrderGroupBy
import org.celimited.manager.model.todayStats.ProductivityStats
import org.celimited.manager.model.todayStats.SectionStats
import org.celimited.manager.model.todayStats.SrDetail
import org.celimited.manager.model.todayStats.SrOrderItem
import org.celimited.manager.model.todayStats.SrPage
import org.celimited.manager.model.todayStats.TargetStats
import org.celimited.manager.model.todayStats.TodayStatsScope
import org.celimited.manager.model.todayStats.ZeroSalesStats

/** Today's Stats, one call per /today-stats/ endpoint. Always for today; nothing is cached. */
interface TodayStatsRepository {
    suspend fun getAttendance(scope: TodayStatsScope): DataResult<SectionStats<AttendanceStats>>

    suspend fun getProductivity(scope: TodayStatsScope): DataResult<SectionStats<ProductivityStats>>

    suspend fun getTarget(scope: TodayStatsScope): DataResult<SectionStats<TargetStats>>

    suspend fun getInventory(scope: TodayStatsScope): DataResult<SectionStats<InventoryStats>>

    suspend fun getZeroSales(scope: TodayStatsScope): DataResult<SectionStats<ZeroSalesStats>>

    suspend fun getOrderBreakdown(
        groupBy: OrderGroupBy,
        scope: TodayStatsScope,
        search: String?
    ): DataResult<List<OrderBreakdownItem>>

    /** [page] is 1-based. */
    suspend fun getSrList(scope: TodayStatsScope, search: String?, page: Int, pageSize: Int): DataResult<SrPage>

    suspend fun getSrDetail(srId: Int): DataResult<SrDetail>

    suspend fun getSrOrders(srId: Int, groupBy: OrderGroupBy, search: String?): DataResult<List<SrOrderItem>>
}
