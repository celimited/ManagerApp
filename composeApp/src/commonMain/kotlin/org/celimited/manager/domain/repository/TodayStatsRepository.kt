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
import org.celimited.manager.model.todayStats.SrSummary
import org.celimited.manager.model.todayStats.TargetStats
import org.celimited.manager.model.todayStats.TeamSummary
import org.celimited.manager.model.todayStats.TodayStatsScope
import org.celimited.manager.model.todayStats.TodaySummary
import org.celimited.manager.model.todayStats.ZeroSalesStats

/** Today's Stats, one call per /today-stats/ endpoint. Always for today; nothing is cached. */
interface TodayStatsRepository {
    /** All My Summary cards for the logged-in user, from today-stats/order-summary/. */
    suspend fun getSummary(userId: String, userLevel: Int): DataResult<TodaySummary>

    /** My Team cards for one hierarchy level, from today-stats/order-summary-by-hierarchy. */
    suspend fun getTeamSummary(userId: String, userLevel: Int): DataResult<TeamSummary>

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

    /** Every SR under [userId], from today-stats/srs (not paginated). */
    suspend fun getSrList(userId: String): DataResult<List<SrSummary>>

    suspend fun getSrDetail(srId: Int): DataResult<SrDetail>

    suspend fun getSrOrders(srId: Int, groupBy: OrderGroupBy, search: String?): DataResult<List<SrOrderItem>>
}
