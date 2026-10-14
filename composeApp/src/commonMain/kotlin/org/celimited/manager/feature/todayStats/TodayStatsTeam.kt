package org.celimited.manager.feature.todayStats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_box
import manager.composeapp.generated.resources.ic_icon_call
import manager.composeapp.generated.resources.ic_icon_chevron_down
import manager.composeapp.generated.resources.ic_icon_order_status
import manager.composeapp.generated.resources.ic_icon_peoples
import manager.composeapp.generated.resources.ic_icon_trending_up
import org.celimited.manager.component.DropdownField
import org.celimited.manager.component.DropdownOption
import org.jetbrains.compose.resources.painterResource

private val GroupOptions: ImmutableList<DropdownOption> =
    BreakdownGroup.entries.map { DropdownOption(it.ordinal, it.label) }.toImmutableList()

/** My Team tab: hierarchy level chips, then per-unit cards for the selected level. */
@Composable
internal fun MyTeamContent(
    levels: ImmutableList<LevelChipUi>,
    selectedLevelKey: String?,
    isLoading: Boolean,
    team: TeamUi?,
    orderGroup: BreakdownGroup,
    orderUnitId: Int?,
    orders: ImmutableList<BreakdownItemUi>,
    isOrdersLoading: Boolean,
    onLevelSelected: (String) -> Unit,
    onOrderGroupSelected: (BreakdownGroup) -> Unit,
    onOrderUnitSelected: (Int) -> Unit,
    onOrdersReload: () -> Unit
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (levels.isNotEmpty()) {
            PillChips(
                options = levels,
                selected = levels.firstOrNull { it.key == selectedLevelKey },
                label = { it.label },
                onSelect = { onLevelSelected(it.key) },
                outlined = true
            )
        }

        when {
            team == null && isLoading -> LoadingBox()
            team == null -> EmptyText(if (levels.isEmpty()) "No team levels available" else "No stats available")
            else -> {
                TeamAttendanceCard(team.attendance)
                TeamProductivityCard(team.productivity)
                TeamTargetCard(team.target)
                TeamZeroSalesCard(team.zeroSales)
                ProductOrderStatusCard(
                    unitOptions = team.unitOptions,
                    group = orderGroup,
                    unitId = orderUnitId,
                    orders = orders,
                    isLoading = isOrdersLoading,
                    onGroupSelected = onOrderGroupSelected,
                    onUnitSelected = onOrderUnitSelected,
                    onReload = onOrdersReload
                )
            }
        }
    }
}

@Composable
private fun TeamAttendanceCard(units: ImmutableList<UnitAttendanceUi>) {
    CollapsibleStatsCard(Res.drawable.ic_icon_peoples, "Attendance") {
        UnitRows(units, itemKey = { it.unitId }) { unit ->
            UnitName(unit.name)
            Row {
                LabeledValue("Total SR", unit.totalSr.toString(), modifier = Modifier.weight(1f))
                LabeledValue("Present", unit.present.toString(), Green, Modifier.weight(1f))
                LabeledValue("Absent", unit.absent.toString(), Red, Modifier.weight(1f))
                LabeledValue("On Leave", unit.onLeave.toString(), Blue, Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            AttendanceBar(unit.present, unit.absent, unit.onLeave)
        }
    }
}

/** Present / absent / on-leave split as one segmented bar. */
@Composable
private fun AttendanceBar(present: Int, absent: Int, onLeave: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(Track),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        listOf(present to Green, absent to Red, onLeave to Blue)
            .filter { it.first > 0 }
            .forEach { (count, color) ->
                Box(
                    Modifier
                        .weight(count.toFloat())
                        .fillMaxHeight()
                        .background(color)
                )
            }
    }
}

@Composable
private fun TeamProductivityCard(units: ImmutableList<UnitProductivityUi>) {
    CollapsibleStatsCard(Res.drawable.ic_icon_call, "Call & SKU Productivity") {
        UnitRows(units, itemKey = { it.unitId }) { unit ->
            UnitName(unit.name)
            Row {
                LabeledValue("Scheduled", unit.scheduled, modifier = Modifier.weight(1f))
                LabeledValue("Visited", unit.visited, Accent, Modifier.weight(1f))
                LabeledValue("Productive", unit.productive, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            ProgressBar(unit.visitedFraction, Accent, height = 4.dp)
            Spacer(Modifier.height(10.dp))
            Row {
                LabeledValue("Strike rate", unit.strikeRate, modifier = Modifier.weight(1f))
                LabeledValue("Productive %", unit.productivePercent, modifier = Modifier.weight(1f))
                LabeledValue("LPSC", unit.lpc, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TeamTargetCard(units: ImmutableList<UnitTargetUi>) {
    CollapsibleStatsCard(Res.drawable.ic_icon_trending_up, "Target & Inventory") {
        UnitRows(units, itemKey = { it.unitId }) { unit ->
            UnitName(unit.name)
            TargetTableRow("", "Target", "Order", "Achievement", isHeader = true)
            Spacer(Modifier.height(6.dp))
            TargetTableRow("Today", unit.today.target, unit.today.order, unit.today.achievement)
            Spacer(Modifier.height(6.dp))
            TargetTableRow("Till Date", unit.tillDate.target, unit.tillDate.order, unit.tillDate.achievement)
        }
    }
}

@Composable
private fun TargetTableRow(label: String, target: String, order: String, achievement: String, isHeader: Boolean = false) {
    val size = if (isHeader) 11.sp else 13.sp
    val weight = if (isHeader) FontWeight.Normal else FontWeight.Bold
    val color = if (isHeader) SubtleText else TitleColor
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 12.sp, color = SubtleText, modifier = Modifier.weight(1f))
        Text(target, fontSize = size, fontWeight = weight, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
        Text(order, fontSize = size, fontWeight = weight, color = color, textAlign = TextAlign.End, modifier = Modifier.weight(1.2f))
        Row(
            modifier = Modifier.weight(1.3f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isHeader) {
                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Green)
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(achievement, fontSize = size, fontWeight = weight, color = if (isHeader) SubtleText else Green)
        }
    }
}

@Composable
private fun TeamZeroSalesCard(units: ImmutableList<UnitZeroSalesUi>) {
    CollapsibleStatsCard(Res.drawable.ic_icon_box, "Zero sales SKU") {
        UnitRows(units, itemKey = { it.unitId }) { unit ->
            UnitName(unit.name)
            Row {
                LabeledValue("Active Outlets", unit.activeOutlets, Green, Modifier.weight(1f))
                LabeledValue("Active SKU", unit.activeSkus, Green, Modifier.weight(1f))
                LabeledValue("No. Order", unit.orderCount, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row {
                LabeledValue("Zero sales", unit.zeroSales, Red, Modifier.weight(1f))
                LabeledValue("Zero SKU", unit.zeroSkus, Red, Modifier.weight(1f))
                LabeledValue("Current Stock", unit.currentStock, Blue, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ProductOrderStatusCard(
    unitOptions: ImmutableList<DropdownOption>,
    group: BreakdownGroup,
    unitId: Int?,
    orders: ImmutableList<BreakdownItemUi>,
    isLoading: Boolean,
    onGroupSelected: (BreakdownGroup) -> Unit,
    onUnitSelected: (Int) -> Unit,
    onReload: () -> Unit
) {
    CollapsibleStatsCard(Res.drawable.ic_icon_order_status, "Product wise order status") {
        Row(verticalAlignment = Alignment.CenterVertically) {
            DropdownField(
                options = GroupOptions,
                selectedId = group.ordinal,
                onSelected = { onGroupSelected(BreakdownGroup.entries[it]) },
                placeholder = "Type",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            DropdownField(
                options = unitOptions,
                selectedId = unitId,
                onSelected = onUnitSelected,
                placeholder = "All units",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Reload",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onReload)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
        when {
            orders.isEmpty() && isLoading -> LoadingBox()
            orders.isEmpty() -> EmptyText("No orders found")
            else -> UnitRows(orders, itemKey = { it.id }) { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    UnitName(item.name, Modifier.weight(1f))
                    Icon(
                        painter = painterResource(Res.drawable.ic_icon_chevron_down),
                        contentDescription = null,
                        tint = SubtleText,
                        modifier = Modifier
                            .size(14.dp)
                            .rotate(-90f)
                    )
                }
                Row {
                    LabeledValue("Memo", item.memo, modifier = Modifier.weight(1f))
                    LabeledValue("Memo %", item.memoPercent, modifier = Modifier.weight(1f))
                    LabeledValue("Order", item.orderVsTargetUom, modifier = Modifier.weight(1f))
                    LabeledValue("Order Value", item.orderValue, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/** Unit blocks separated by thin dividers; [itemKey] keeps rows stable when a level changes. */
@Composable
private fun <T> UnitRows(
    items: ImmutableList<T>,
    itemKey: (T) -> Any,
    row: @Composable ColumnScope.(T) -> Unit
) {
    if (items.isEmpty()) {
        EmptyText("No units at this level")
        return
    }
    items.forEachIndexed { index, item ->
        key(itemKey(item)) {
            if (index > 0) CardDivider()
            Column(content = { row(item) })
        }
    }
}

@Composable
private fun UnitName(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TitleColor,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

