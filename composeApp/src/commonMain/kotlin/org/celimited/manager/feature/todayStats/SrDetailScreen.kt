package org.celimited.manager.feature.todayStats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_attendance
import manager.composeapp.generated.resources.ic_icon_call
import manager.composeapp.generated.resources.ic_icon_cart
import manager.composeapp.generated.resources.ic_icon_chat
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_location
import manager.composeapp.generated.resources.ic_icon_order_status
import manager.composeapp.generated.resources.ic_icon_trending_up
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.component.TopBar
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val CallBlue = Color(0xFF2F80ED)
private val ChatGreen = Color(0xFF25D366)
private val OrderGroups: ImmutableList<BreakdownGroup> = BreakdownGroup.entries.toImmutableList()

@Composable
fun SrDetailRoute(
    srId: Int,
    onBackClick: () -> Unit,
    onSessionExpired: () -> Unit,
    // ViewModels are activity-scoped here (no per-entry ViewModelStore), so key by id.
    viewModel: SrDetailViewModel = koinViewModel(
        key = "sr-detail-$srId",
        parameters = { parametersOf(srId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                SrDetailUiEffect.SessionExpired -> onSessionExpired()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "SR Details",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        SrDetailScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            profile = uiState.profile,
            timeline = uiState.timeline,
            work = uiState.work,
            target = uiState.target,
            isLoading = uiState.isLoading,
            orderGroup = uiState.orderGroup,
            orderQuery = uiState.orderQuery,
            orders = uiState.orders,
            isOrdersLoading = uiState.isOrdersLoading,
            onOrderGroupSelected = viewModel::onOrderGroupSelected,
            onOrderQueryChange = viewModel::onOrderQueryChange,
            // UI only — call / WhatsApp / map actions to be wired when the API provides the data.
            onCallClick = {},
            onWhatsAppClick = {},
            onMapViewClick = {}
        )
    }

    uiState.errorDialog?.let { error ->
        ErrorDialog(message = error.message, onClose = viewModel::onDismissError, onRetry = viewModel::onRetry)
    }
}

@Composable
fun SrDetailScreen(
    modifier: Modifier,
    profile: SrProfileUi?,
    timeline: SrTimelineUi?,
    work: SrWorkUi?,
    target: SrTargetUi?,
    isLoading: Boolean,
    orderGroup: BreakdownGroup,
    orderQuery: String,
    orders: ImmutableList<SrOrderUi>,
    isOrdersLoading: Boolean,
    onOrderGroupSelected: (BreakdownGroup) -> Unit,
    onOrderQueryChange: (String) -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onMapViewClick: () -> Unit
) {
    var isSearchOpen by rememberSaveable { mutableStateOf(orderQuery.isNotEmpty()) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (profile == null) {
            item(key = "loading") { if (isLoading) LoadingBox() else EmptyText("SR details unavailable") }
        } else {
            item(key = "territory") {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Territory") }
                        append("  ${profile.territory}")
                    },
                    fontSize = 13.sp,
                    color = TitleColor
                )
            }
            item(key = "profile") { ProfileCard(profile, onCallClick, onWhatsAppClick) }
            timeline?.let { item(key = "timeline") { WorkTimelineCard(it) } }
            work?.let { item(key = "work") { WorkSummaryCard(it, onMapViewClick) } }
            target?.let { item(key = "target") { TargetCard(it) } }
        }

        item(key = "orders_header") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Order Details", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleColor)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillChips(
                        options = OrderGroups,
                        selected = orderGroup,
                        label = { it.label },
                        onSelect = onOrderGroupSelected,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    SearchToggleButton(active = isSearchOpen) {
                        if (isSearchOpen && orderQuery.isNotEmpty()) onOrderQueryChange("")
                        isSearchOpen = !isSearchOpen
                    }
                }
                if (isSearchOpen) SearchField(orderQuery, "Search ${orderGroup.label}", onOrderQueryChange)
            }
        }

        when {
            orders.isEmpty() && isOrdersLoading -> item(key = "orders_loading") { LoadingBox() }
            orders.isEmpty() -> item(key = "orders_empty") { EmptyText("No orders found") }
            else -> items(orders, key = { "order_${it.id}" }) { OrderCard(it) }
        }
    }
}

@Composable
private fun ProfileCard(profile: SrProfileUi, onCallClick: () -> Unit, onWhatsAppClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(isOnline = profile.isOnline, size = 56)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(profile.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleColor)
            Spacer(Modifier.height(2.dp))
            Text(profile.designation, fontSize = 12.sp, color = SubtleText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_location),
                    contentDescription = null,
                    tint = TitleColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(profile.location, fontSize = 12.sp, color = TitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        ActionCircle(Res.drawable.ic_icon_call, "Call", CallBlue, onCallClick)
        Spacer(Modifier.width(10.dp))
        ActionCircle(Res.drawable.ic_icon_chat, "WhatsApp", ChatGreen, onWhatsAppClick)
    }
}

@Composable
private fun ActionCircle(icon: DrawableResource, description: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun WorkTimelineCard(timeline: SrTimelineUi) {
    StatsCard(
        icon = Res.drawable.ic_icon_clock,
        title = "Work Timeline",
        trailing = { Badge("Spent: ${timeline.spent}", Tone.Highlight) }
    ) {
        TileRow {
            TimelineTile(Res.drawable.ic_icon_attendance, "Logged In", "${timeline.loggedInTime} • ${timeline.workMode}", tileModifier())
            TimelineTile(Res.drawable.ic_icon_location, "Visited", timeline.visited, tileModifier())
        }
        Spacer(Modifier.height(8.dp))
        TileRow {
            TimelineTile(
                Res.drawable.ic_icon_order_status,
                "1st Order received",
                listOf(timeline.firstOrderTime, timeline.firstOrderOutlet).filter { it.isNotEmpty() }.joinToString(" • "),
                tileModifier()
            )
            TimelineTile(
                Res.drawable.ic_icon_cart,
                "Last Order received",
                listOf(timeline.lastOrderTime, timeline.lastOrderOutlet).filter { it.isNotEmpty() }.joinToString(" • "),
                tileModifier()
            )
        }
    }
}

@Composable
private fun TimelineTile(icon: DrawableResource, title: String, detail: String, modifier: Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(TileBg)
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        IconTile(icon, size = 28.dp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TitleColor)
            Spacer(Modifier.height(4.dp))
            Text(detail, fontSize = 11.sp, color = SubtleText)
        }
    }
}

@Composable
private fun WorkSummaryCard(work: SrWorkUi, onMapViewClick: () -> Unit) {
    StatsCard(
        icon = Res.drawable.ic_icon_location,
        title = "Work Summary",
        trailing = {
            Text(
                text = "Map View",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Accent,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(onClick = onMapViewClick)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    ) {
        TileRow {
            StatTile(work.scheduled, "Scheduled", tileModifier(), Tone.Highlight)
            StatTile(work.visited, "Visited", tileModifier(), Tone.Positive)
            StatTile(work.productive, "Productive", tileModifier(), Tone.Positive)
            StatTile(work.lpc, "LPC", tileModifier(), Tone.Caution)
        }
    }
}

@Composable
private fun TargetCard(target: SrTargetUi) {
    StatsCard(icon = Res.drawable.ic_icon_trending_up, title = "Target") {
        TileRow {
            StatTile(target.todayTarget, "Today's Target", tileModifier())
            StatTile(target.orderValue, "Order Value", tileModifier(), Tone.Positive)
            Column(
                modifier = tileModifier()
                    .clip(RoundedCornerShape(12.dp))
                    .background(TileBg)
                    .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Achievement", fontSize = 12.sp, color = SubtleText)
                Spacer(Modifier.height(4.dp))
                Text("${target.achievementPercent}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Red)
                Spacer(Modifier.height(6.dp))
                ProgressBar(target.achievementPercent.toProgress(), Red, height = 5.dp)
            }
        }
    }
}

@Composable
private fun OrderCard(order: SrOrderUi) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(order.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleColor, modifier = Modifier.weight(1f))
            InlineValue("Qty", order.qty)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(order.subName, fontSize = 12.sp, color = SubtleText, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            InlineValue("Memo", order.memo)
        }
        CardDivider()
        Row {
            InlineValue("Special Discount", order.specialDiscount, Modifier.weight(1f))
            InlineValue("Promo Discount", order.promoDiscount, Modifier.weight(1f))
            InlineValue("Total Discount", order.totalDiscount, Modifier.weight(1f))
        }
        Spacer(Modifier.height(6.dp))
        Row {
            InlineValue("Free Value", order.freeValue, Modifier.weight(1f))
            InlineValue("Order in UOM", order.orderQtyUom, Modifier.weight(1f))
            InlineValue("Free Qty", order.freeQty, Modifier.weight(1f))
        }
    }
}

@Composable
private fun InlineValue(label: String, value: String, modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            append("$label: ")
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Accent)) { append(value) }
        },
        fontSize = 11.sp,
        color = SubtleText,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
    )
}
