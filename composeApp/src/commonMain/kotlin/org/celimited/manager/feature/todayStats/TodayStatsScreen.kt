package org.celimited.manager.feature.todayStats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_box
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_order_status
import manager.composeapp.generated.resources.ic_icon_peoples
import manager.composeapp.generated.resources.ic_icon_trending_up
import manager.composeapp.generated.resources.ic_icon_truck
import manager.composeapp.generated.resources.ic_icon_warning
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.component.TopBar
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TodayStatsRoute(
    onBackClick: () -> Unit,
    onSrClick: (srId: Int) -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: TodayStatsViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is TodayStatsUiEffect.NavigateToSrDetail -> onSrClick(effect.srId)
                TodayStatsUiEffect.SessionExpired -> onSessionExpired()
            }
        }
    }

    TodayStatsScreen(
        uiState = uiState,
        onBackClick = onBackClick,
        onTabSelected = viewModel::onTabSelected,
        onLevelSelected = viewModel::onLevelSelected,
        onTeamOrderGroupSelected = viewModel::onTeamOrderGroupSelected,
        onTeamOrderUnitSelected = viewModel::onTeamOrderUnitSelected,
        onTeamOrdersReload = viewModel::onTeamOrdersReload,
        onSrQueryChange = viewModel::onSrQueryChange,
        onSrClick = viewModel::onSrClick,
        onDismissError = viewModel::onDismissError,
        onRetry = viewModel::onRetry
    )
}

/** Stateless screen: renders [uiState] only, so it can be previewed without a ViewModel. */
@Composable
fun TodayStatsScreen(
    uiState: TodayStatsUiState,
    onBackClick: () -> Unit,
    onTabSelected: (TodayStatsTab) -> Unit,
    onLevelSelected: (String) -> Unit,
    onTeamOrderGroupSelected: (BreakdownGroup) -> Unit,
    onTeamOrderUnitSelected: (Int) -> Unit,
    onTeamOrdersReload: () -> Unit,
    onSrQueryChange: (String) -> Unit,
    onSrClick: (Int) -> Unit,
    onDismissError: () -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "Today's Order",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                TopTabs(selected = uiState.selectedTab, onSelect = onTabSelected)
            }
            Box(Modifier.weight(1f)) {
                when (uiState.selectedTab) {
                    TodayStatsTab.MySummary -> MySummaryContent(
                        summary = uiState.summary,
                        isSummaryLoading = uiState.isSummaryLoading
                    )
                    TodayStatsTab.MyTeam -> MyTeamContent(
                        levels = uiState.levels,
                        selectedLevelKey = uiState.selectedLevelKey,
                        isLoading = uiState.isLevelsLoading || uiState.isTeamLoading,
                        team = uiState.team,
                        orderGroup = uiState.teamOrderGroup,
                        orderUnitId = uiState.teamOrderUnitId,
                        orders = uiState.teamOrders,
                        isOrdersLoading = uiState.isTeamOrdersLoading,
                        onLevelSelected = onLevelSelected,
                        onOrderGroupSelected = onTeamOrderGroupSelected,
                        onOrderUnitSelected = onTeamOrderUnitSelected,
                        onOrdersReload = onTeamOrdersReload
                    )
                    TodayStatsTab.Sr -> SrListContent(
                        query = uiState.srQuery,
                        items = uiState.srs,
                        isLoading = uiState.isSrLoading,
                        onQueryChange = onSrQueryChange,
                        onSrClick = onSrClick
                    )
                }
            }
        }
    }

    uiState.errorDialog?.let { error ->
        ErrorDialog(message = error.message, onClose = onDismissError, onRetry = onRetry)
    }
}

// ---------- My Summary ----------

@Composable
private fun MySummaryContent(
    summary: SummaryUi?,
    isSummaryLoading: Boolean
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (summary == null) {
            item(key = "summary_loading") {
                if (isSummaryLoading) LoadingBox() else EmptyText("No stats available")
            }
        } else {
            item(key = "attendance") { AttendanceCard(summary.attendance) }
            item(key = "call") { CallProductivityCard(summary.call) }
            item(key = "sku") { SkuProductivityCard(summary.sku) }
            item(key = "running") { RunningTrackCard(summary.runningTrack) }
            item(key = "inventory") { InventoryCard(summary.inventory) }
            item(key = "zero_sales") { ZeroSalesCard(summary.zeroSales) }
            if (summary.keyMetrics.isNotEmpty()) {
                item(key = "key_metrics") { KeyMetricsCard(summary.keyMetrics) }
            }
        }
    }
}

@Composable
private fun AttendanceCard(attendance: AttendanceUi) {
    StatsCard(
        icon = Res.drawable.ic_icon_peoples,
        title = "Attendance",
        subtitle = "Total SR · ${attendance.totalSr}",
        trailing = { Badge("${attendance.presentPercent}% Present", Tone.Positive) }
    ) {
        TileRow {
            StatTile(attendance.present.toString(), "Present", tileModifier(), Tone.Positive)
            StatTile(attendance.absent.toString(), "Absent", tileModifier(), Tone.Negative)
            StatTile(attendance.onLeave.toString(), "On Leave", tileModifier(), Tone.Warning)
        }
        CardDivider()
        Row(modifier = Modifier.fillMaxWidth()) {
            PlainStat(attendance.visited.toString(), "Visited", Modifier.weight(1f))
            PlainStat(attendance.productive.toString(), "Productive", Modifier.weight(1f))
            PlainStat(attendance.idle.toString(), "Idle", Modifier.weight(1f))
            PlainStat(attendance.active.toString(), "Active", Modifier.weight(1f))
        }
    }
}

@Composable
private fun CallProductivityCard(call: CallProductivityUi) {
    StatsCard(
        icon = Res.drawable.ic_icon_clock,
        title = "Call Productivity",
        subtitle = "Planned vs visited outlets"
    ) {
        TileRow {
            StatTile(call.scheduled, "Scheduled", tileModifier())
            StatTile(call.visited, "Visited", tileModifier())
            StatTile(call.productive, "Productive", tileModifier(), Tone.Positive)
            StatTile(call.remaining, "Remaining", tileModifier(), Tone.Caution)
        }
        CardDivider()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RingChip(call.strikeRate, "Strike Rate", Accent, Modifier.weight(1f))
            RingChip(call.productiveRate, "Productivity", Orange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SkuProductivityCard(sku: SkuProductivityUi) {
    StatsCard(
        icon = Res.drawable.ic_icon_box,
        title = "SKU Productivity",
        subtitle = "Lines sold per productive call",
        trailing = { Badge("${sku.lpc} LPC", Tone.Warning) }
    ) {
        TileRow {
            StatTile(sku.lpc, "LPC", tileModifier(), Tone.Highlight)
            StatTile(sku.tlsd, "TLSD", tileModifier())
            StatTile(sku.pCase, "P-Case", tileModifier())
        }
    }
}

@Composable
private fun RunningTrackCard(track: RunningTrackUi) {
    StatsCard(
        icon = Res.drawable.ic_icon_trending_up,
        title = "Running Track",
        subtitle = "Today vs till date"
    ) {
        TileRow {
            TrackCard(track.today, Green, tileModifier())
            TrackCard(track.tillDate, Orange, tileModifier())
        }
    }
}

@Composable
private fun TrackCard(card: TrackCardUi, color: Color, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TileBg)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(card.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TitleColor, modifier = Modifier.weight(1f))
            if (card.dateLabel.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Track)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(card.dateLabel, fontSize = 9.sp, color = TitleColor, maxLines = 1)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row {
            LabeledValue("Target", card.target, Accent, Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text("Order", fontSize = 11.sp, color = SubtleText)
                Spacer(Modifier.height(2.dp))
                Text(card.order, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Orange, maxLines = 1)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row {
            Text("Achievement", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TitleColor, modifier = Modifier.weight(1f))
            Text("${card.percent}%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(4.dp))
        ProgressBar(card.percent.toProgress(), color)
    }
}

@Composable
private fun InventoryCard(inventory: InventoryUi) {
    StatsCard(
        icon = Res.drawable.ic_icon_truck,
        title = "Inventory Status",
        subtitle = "Current Stock Status"
    ) {
        TileRow {
            StatTile(inventory.currentStockValue, "Current Stock Value", tileModifier())
            StatTile(inventory.openingStock, "Opening Stock", tileModifier())
        }
        Spacer(Modifier.height(8.dp))
        TileRow {
            StatTile(inventory.dbCount, "DB", tileModifier())
            StatTile(inventory.outOfStock, "Out of Stock", tileModifier(), Tone.Negative)
            StatTile(inventory.currentStock, "Current Stock", tileModifier())
        }
    }
}

@Composable
private fun ZeroSalesCard(zeroSales: ZeroSalesUi) {
    StatsCard(icon = Res.drawable.ic_icon_warning, title = "Zero Sales Outlets") {
        TileRow {
            StatTile(zeroSales.activeOutlets, "Active Outlets", tileModifier())
            StatTile(zeroSales.zeroSalesOutlets, "Zero Sales", tileModifier(), Tone.Negative)
        }
        Spacer(Modifier.height(8.dp))
        TileRow {
            StatTile(zeroSales.activeSkus, "Active SKU", tileModifier())
            StatTile(zeroSales.zeroSalesSkus, "Zero SKU", tileModifier(), Tone.Negative)
        }
    }
}

@Composable
private fun KeyMetricsCard(metrics: ImmutableList<KeyMetricUi>) {
    StatsCard(icon = Res.drawable.ic_icon_order_status, title = "Key Sales Metrics") {
        TileRow {
            metrics.forEach { metric ->
                StatTile(metric.value, metric.label, tileModifier(), valueSuffix = metric.suffix)
            }
        }
    }
}

@Composable
private fun RingChip(percent: Int, label: String, color: Color, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TileBg)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        PercentRing(percent = percent, color = color, size = 38.dp, strokeWidth = 4.dp, fontSize = 9)
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TitleColor)
    }
}

@Composable
private fun PlainStat(value: String, label: String, modifier: Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleColor, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 12.sp, color = SubtleText, maxLines = 1)
    }
}

@Composable
internal fun CardDivider() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .height(1.dp)
            .background(CardBorder)
    )
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
private fun TodayStatsScreenPreview() {
    TodayStatsScreen(
        uiState = TodayStatsUiState(
            summary = SummaryUi(
                attendance = AttendanceUi(
                    totalSr = 48, presentPercent = 88, present = 42, absent = 4, onLeave = 2,
                    visited = 40, productive = 35, idle = 3, active = 39
                ),
                call = CallProductivityUi(
                    scheduled = "820", visited = "740", productive = "610", remaining = "80",
                    strikeRate = 82, productiveRate = 74
                ),
                sku = SkuProductivityUi(lpc = "3.4", tlsd = "2,074", pCase = "1,150"),
                runningTrack = RunningTrackUi(
                    today = TrackCardUi("Today", "26 Oct", "৳ 57K", "৳ 49K", 86),
                    tillDate = TrackCardUi("Till Date", "1–26 Oct", "৳ 14.9L", "৳ 12.4L", 83)
                ),
                inventory = InventoryUi(
                    currentStockValue = "৳ 8.2L", openingStock = "৳ 9.0L",
                    dbCount = "6", outOfStock = "12", currentStock = "3,420"
                ),
                zeroSales = ZeroSalesUi(
                    activeOutlets = "1,240", zeroSalesOutlets = "86",
                    activeSkus = "142", zeroSalesSkus = "9"
                ),
                keyMetrics = persistentListOf(
                    KeyMetricUi("৳ 2.1K", null, "Avg Memo"),
                    KeyMetricUi("610", null, "Memos"),
                    KeyMetricUi("12", "%", "Growth")
                )
            )
        ),
        onBackClick = {},
        onTabSelected = {},
        onLevelSelected = {},
        onTeamOrderGroupSelected = {},
        onTeamOrderUnitSelected = {},
        onTeamOrdersReload = {},
        onSrQueryChange = {},
        onSrClick = {},
        onDismissError = {},
        onRetry = {}
    )
}
