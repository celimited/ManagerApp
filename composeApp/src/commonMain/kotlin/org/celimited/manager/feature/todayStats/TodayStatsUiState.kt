package org.celimited.manager.feature.todayStats

import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import org.celimited.manager.component.DropdownOption
import org.celimited.manager.component.ErrorDialogUi

enum class TodayStatsTab(val label: String) {
    MySummary("My Summary"),
    MyTeam("My Team"),
    Sr("SR")
}

/** Order breakdown grouping, shared by My Summary, My Team and SR detail. */
enum class BreakdownGroup(val label: String) {
    Project("Project"),
    Product("Product"),
    Sku("SKU"),
    SkuWise("SKU-wise")
}

// ---------- My Summary ----------

@Immutable
data class AchievementHeaderUi(
    val percent: Int,
    val achieved: String,
    val target: String,
    /** e.g. "Total · Month to date · Day 21 of 30" */
    val periodLabel: String
)

@Immutable
data class AttendanceUi(
    val totalSr: Int,
    val presentPercent: Int,
    val present: Int,
    val absent: Int,
    val onLeave: Int,
    val visited: Int,
    val productive: Int,
    val idle: Int,
    val active: Int
)

@Immutable
data class CallProductivityUi(
    val scheduled: String,
    val visited: String,
    val productive: String,
    val remaining: String,
    val strikeRate: Int,
    val productiveRate: Int
)

@Immutable
data class SkuProductivityUi(
    val lpc: String,
    val tlsd: String,
    val pCase: String
)

@Immutable
data class TrackCardUi(
    val title: String,
    val dateLabel: String,
    val target: String,
    val order: String,
    val percent: Int
)

@Immutable
data class RunningTrackUi(
    val achieved: String,
    val target: String,
    val percent: Int,
    val expectedPercent: Int,
    val today: TrackCardUi,
    val tillDate: TrackCardUi
)

@Immutable
data class InventoryUi(
    val currentStockValue: String,
    val openingStock: String,
    val dbCount: String,
    val outOfStock: String,
    val currentStock: String
)

@Immutable
data class ZeroSalesUi(
    val activeOutlets: String,
    val zeroSalesOutlets: String,
    val activeSkus: String,
    val zeroSalesSkus: String
)

@Immutable
data class KeyMetricUi(
    val value: String,
    val suffix: String?,
    val label: String
)

@Immutable
data class SummaryUi(
    val header: AchievementHeaderUi,
    val attendance: AttendanceUi,
    val call: CallProductivityUi,
    val sku: SkuProductivityUi,
    val runningTrack: RunningTrackUi,
    val inventory: InventoryUi,
    val zeroSales: ZeroSalesUi,
    val keyMetrics: ImmutableList<KeyMetricUi>
)

@Immutable
data class BreakdownItemUi(
    val id: Int,
    val name: String,
    /** "20/100" */
    val memo: String,
    val memoPercent: String,
    val orderVsTargetUom: String,
    val orderValue: String
)

// ---------- My Team ----------

@Immutable
data class LevelChipUi(
    val key: String,
    val label: String
)

@Immutable
data class UnitAttendanceUi(
    val unitId: Int,
    val name: String,
    val totalSr: Int,
    val present: Int,
    val absent: Int,
    val onLeave: Int
)

@Immutable
data class UnitProductivityUi(
    val unitId: Int,
    val name: String,
    val scheduled: String,
    val visited: String,
    val productive: String,
    /** visited / scheduled, 0..1, for the bar under the counts. */
    val visitedFraction: Float,
    val strikeRate: String,
    val productivePercent: String,
    val lpc: String
)

@Immutable
data class TargetRowUi(
    val target: String,
    val order: String,
    val achievement: String
)

@Immutable
data class UnitTargetUi(
    val unitId: Int,
    val name: String,
    val today: TargetRowUi,
    val tillDate: TargetRowUi
)

@Immutable
data class UnitZeroSalesUi(
    val unitId: Int,
    val name: String,
    val activeOutlets: String,
    val activeSkus: String,
    val orderCount: String,
    val zeroSales: String,
    val zeroSkus: String,
    val currentStock: String
)

@Immutable
data class TeamUi(
    val attendance: ImmutableList<UnitAttendanceUi>,
    val productivity: ImmutableList<UnitProductivityUi>,
    val target: ImmutableList<UnitTargetUi>,
    val zeroSales: ImmutableList<UnitZeroSalesUi>,
    /** Units at the selected level, for the "Product wise order status" unit dropdown. */
    val unitOptions: ImmutableList<DropdownOption>
)

// ---------- SR ----------

@Immutable
data class SrCardUi(
    val srId: Int,
    val name: String,
    val location: String,
    val territory: String,
    val isOnline: Boolean,
    val checkIn: String,
    val firstOrder: String,
    val lastOrder: String,
    val strikeRate: String,
    val productiveRate: String,
    val lpc: String,
    val orderAchievement: String
)

@Immutable
data class TodayStatsUiState(
    val selectedTab: TodayStatsTab = TodayStatsTab.MySummary,

    // My Summary
    val summary: SummaryUi? = null,
    val isSummaryLoading: Boolean = false,
    val breakdownGroup: BreakdownGroup = BreakdownGroup.SkuWise,
    val breakdownQuery: String = "",
    val breakdown: ImmutableList<BreakdownItemUi> = persistentListOf(),
    val isBreakdownLoading: Boolean = false,

    // My Team
    val levels: ImmutableList<LevelChipUi> = persistentListOf(),
    val selectedLevelKey: String? = null,
    val isLevelsLoading: Boolean = false,
    val team: TeamUi? = null,
    val isTeamLoading: Boolean = false,
    val teamOrderGroup: BreakdownGroup = BreakdownGroup.Project,
    /** Null = all units at the selected level. */
    val teamOrderUnitId: Int? = null,
    val teamOrders: ImmutableList<BreakdownItemUi> = persistentListOf(),
    val isTeamOrdersLoading: Boolean = false,

    // SR
    val srQuery: String = "",
    val srs: ImmutableList<SrCardUi> = persistentListOf(),
    val isSrLoading: Boolean = false,
    /** Last loaded page (1-based); 0 = nothing loaded yet. */
    val srPage: Int = 0,
    val srHasMore: Boolean = false,
    val isSrLoadingMore: Boolean = false,

    /** API failure popup; null when no dialog is shown. */
    val errorDialog: ErrorDialogUi? = null
)
