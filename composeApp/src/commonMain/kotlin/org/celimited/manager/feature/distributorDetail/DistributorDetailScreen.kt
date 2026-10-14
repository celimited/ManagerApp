package org.celimited.manager.feature.distributorDetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.launch
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_add
import manager.composeapp.generated.resources.ic_icon_box
import manager.composeapp.generated.resources.ic_icon_call
import manager.composeapp.generated.resources.ic_icon_chevron_down
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_trending_down
import manager.composeapp.generated.resources.ic_icon_trending_up
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.component.LockedAlpha
import org.celimited.manager.component.TopBar
import org.celimited.manager.component.showUnderDevelopment
import org.celimited.manager.core.common.Feature
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

private val TitleColor = Color(0xFF111827)
private val Accent = Color(0xFF582FFF)
private val SubtleText = Color(0xFF6B7280)
private val CardBorder = Color(0xFFE5E7EB)
private val Lavender = Color(0xFFEEEAFF)
private val LavenderLight = Color(0xFFF3F1FF)
private val Green = Color(0xFF16A34A)
private val Orange = Color(0xFFEA580C)
private val Red = Color(0xFFE11D48)
private val Track = Color(0xFFEDEBFA)

@Composable
fun DistributorDetailRoute(
    distributorId: Int,
    onBackClick: () -> Unit,
    onAddIssueClick: () -> Unit,
    onSessionExpired: () -> Unit,
    // ViewModels are activity-scoped here (no per-entry ViewModelStore), so key by id.
    viewModel: DistributorDetailViewModel = koinViewModel(
        key = "distributor-detail-$distributorId",
        parameters = { parametersOf(distributorId) }
    )
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                DistributorDetailUiEffect.SessionExpired -> onSessionExpired()
                // Launched so a showing snackbar doesn't block the next effect.
                DistributorDetailUiEffect.ShowUnderDevelopment ->
                    launch { snackbarHostState.showUnderDevelopment() }
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopBar(
                title = "Distributor Details",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        DistributorDetailScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            name = uiState.name,
            address = uiState.address,
            lastVisit = uiState.lastVisit,
            selectedTab = uiState.selectedTab,
            overview = uiState.overview,
            isOverviewLoading = uiState.isOverviewLoading,
            stockTab = uiState.stockTab,
            isStockSummaryLoading = uiState.isStockSummaryLoading,
            issues = uiState.issues,
            issueQuery = uiState.issueQuery,
            onTabSelected = viewModel::onTabSelected,
            onIssueQueryChange = viewModel::onIssueQueryChange,
            onIssueFilterClick = {},
            onIssueClick = {},
            // UI only — actions to be wired when their APIs exist.
            onCallClick = {},
            onAddIssueClick = onAddIssueClick,
            onAddNoteClick = {},
            onHistoryClick = {},
            onLockedFeatureClick = viewModel::onLockedFeatureClick,
            onApprovalBannerClick = {},
            onViewSuggestionsClick = {},
            onStockSummaryItemClick = {}
        )
    }

    uiState.errorDialog?.let { error ->
        ErrorDialog(message = error.message, onClose = viewModel::onDismissError, onRetry = viewModel::onRetry)
    }
}

@Composable
fun DistributorDetailScreen(
    modifier: Modifier,
    name: String,
    address: String,
    lastVisit: String,
    selectedTab: DistributorDetailTab,
    overview: DistributorOverviewUi?,
    isOverviewLoading: Boolean,
    stockTab: DistributorStockTabUi,
    isStockSummaryLoading: Boolean,
    issues: ImmutableList<DistributorIssueUi>,
    issueQuery: String,
    onTabSelected: (DistributorDetailTab) -> Unit,
    onIssueQueryChange: (String) -> Unit,
    onIssueFilterClick: () -> Unit,
    onIssueClick: (issueId: Int) -> Unit,
    onCallClick: () -> Unit,
    onAddIssueClick: () -> Unit,
    onAddNoteClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onLockedFeatureClick: () -> Unit,
    onApprovalBannerClick: () -> Unit,
    onViewSuggestionsClick: () -> Unit,
    onStockSummaryItemClick: (id: String) -> Unit
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        DistributorHeader(name, address, lastVisit, onCallClick)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionButton(Res.drawable.ic_icon_add, "Add Issue", Feature.AddIssue, onAddIssueClick, onLockedFeatureClick, Modifier.weight(1f))
            ActionButton(Res.drawable.ic_icon_add, "Add Note", Feature.AddNote, onAddNoteClick, onLockedFeatureClick, Modifier.weight(1f))
            ActionButton(Res.drawable.ic_icon_clock, "History", Feature.DistributorHistory, onHistoryClick, onLockedFeatureClick, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        DetailTabs(selectedTab, onTabSelected)
        Spacer(Modifier.height(12.dp))

        when (selectedTab) {
            DistributorDetailTab.Overview -> OverviewTab(overview, isOverviewLoading)
            DistributorDetailTab.Stock -> StockTabContent(
                stock = stockTab,
                isSummaryLoading = isStockSummaryLoading,
                onApprovalBannerClick = onApprovalBannerClick,
                onViewSuggestionsClick = onViewSuggestionsClick,
                onSummaryItemClick = onStockSummaryItemClick
            )
            DistributorDetailTab.Issues -> IssuesTabContent(
                issues = issues,
                query = issueQuery,
                onQueryChange = onIssueQueryChange,
                onFilterClick = onIssueFilterClick,
                onIssueClick = onIssueClick
            )
            DistributorDetailTab.Notes -> EmptyTab("No notes added yet")
        }
        Spacer(Modifier.height(24.dp))
    }
}

// ---------- Header / actions / tabs ----------

@Composable
private fun DistributorHeader(name: String, address: String, lastVisit: String, onCallClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (address.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(address, fontSize = 13.sp, color = SubtleText, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            if (lastVisit.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text("Last visit: $lastVisit", fontSize = 11.sp, color = Color(0xFF9CA3AF))
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Accent)
                .clickable(onClick = onCallClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_icon_call),
                contentDescription = "Call",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: DrawableResource,
    label: String,
    feature: Feature,
    onClick: () -> Unit,
    onLockedClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val locked = feature.isLocked
    Row(
        modifier = modifier
            .alpha(if (locked) LockedAlpha else 1f)
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
            .clickable(onClick = if (locked) onLockedClick else onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = Accent, maxLines = 1)
    }
}

@Composable
private fun DetailTabs(selected: DistributorDetailTab, onSelect: (DistributorDetailTab) -> Unit) {
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            DistributorDetailTab.entries.forEach { tab ->
                val isSelected = tab == selected
                Column(
                    modifier = Modifier
                        .alpha(if (tab.isLocked) LockedAlpha else 1f)
                        .width(IntrinsicSize.Max) // underline spans exactly the label width
                        .clip(RoundedCornerShape(6.dp))
                        .clickable { onSelect(tab) }
                        .padding(horizontal = 8.dp)
                        .padding(top = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) Accent else SubtleText
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (isSelected) Accent else Color.Transparent)
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder)
        )
    }
}

// ---------- Tab contents ----------

@Composable
private fun OverviewTab(overview: DistributorOverviewUi?, isLoading: Boolean) {
    if (isLoading) {
        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
        }
        return
    }
    // Load failed: the error dialog offers a retry.
    if (overview == null) return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GrowthAnalyticsCard(overview)
        TargetAchievementCard(overview)
        InvoiceStatusCard(overview)
        Spacer(Modifier.height(4.dp))
        StockStatusContent(overview.stock)
    }
}

@Composable
private fun EmptyTab(message: String) {
    Text(
        text = message,
        fontSize = 13.sp,
        color = SubtleText,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp)
    )
}

@Composable
private fun DetailCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp),
        content = content
    )
}

@Composable
private fun CardTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.6.sp,
        color = Color(0xFF374151),
        modifier = modifier
    )
}

@Composable
private fun GrowthAnalyticsCard(overview: DistributorOverviewUi) {
    DetailCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CardTitle("GROWTH ANALYTICS", Modifier.weight(1f))
            Text(overview.growthPercent, fontSize = 14.sp, color = if (overview.isGrowthNegative) Red else Green)
        }
        Spacer(Modifier.height(14.dp))
        PerformanceBar("MTD Performance", overview.mtdPercent)
        Spacer(Modifier.height(14.dp))
        PerformanceBar("YTD Performance", overview.ytdPercent)
    }
}

@Composable
private fun PerformanceBar(label: String, percent: Int) {
    Row {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TitleColor, modifier = Modifier.weight(1f))
        Text("$percent%", fontSize = 12.sp, color = Green)
    }
    Spacer(Modifier.height(6.dp))
    LinearProgressIndicator(
        progress = { (percent / 100f).coerceIn(0f, 1f) },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50)),
        color = Green,
        trackColor = Track,
        drawStopIndicator = {}
    )
}

@Composable
private fun TargetAchievementCard(overview: DistributorOverviewUi) {
    DetailCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (overview.targetPercent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier.size(72.dp),
                    color = Green,
                    strokeWidth = 6.dp,
                    trackColor = Green.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )
                Text("${overview.targetPercent}%", fontSize = 14.sp, color = Green)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                CardTitle("TARGET ACHIEVEMENT")
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(overview.targetAchieved, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TitleColor)
                    Text("  of  ", fontSize = 12.sp, color = SubtleText)
                    Text(overview.target, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleColor)
                }
                Spacer(Modifier.height(6.dp))
                val weeklyColor = if (overview.isWeeklyChangeNegative) Red else Green
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(
                            if (overview.isWeeklyChangeNegative) Res.drawable.ic_icon_trending_down else Res.drawable.ic_icon_trending_up
                        ),
                        contentDescription = null,
                        tint = weeklyColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${overview.weeklyChange} from last week", fontSize = 13.sp, color = weeklyColor)
                }
            }
        }
    }
}

@Composable
private fun InvoiceStatusCard(overview: DistributorOverviewUi) {
    DetailCard {
        CardTitle("INVOICE STATUS")
        Spacer(Modifier.height(14.dp))
        InvoiceRow("Invoice Received") {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .border(1.5.dp, TitleColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(if (overview.invoiceReceived) "✓" else "✕", fontSize = 9.sp, color = TitleColor)
            }
            Spacer(Modifier.width(6.dp))
            Text(if (overview.invoiceReceived) "Yes" else "No", fontSize = 13.sp, color = TitleColor)
        }
        Spacer(Modifier.height(12.dp))
        InvoiceRow("Last Invoice Date") {
            Text(overview.lastInvoiceDate, fontSize = 13.sp, color = TitleColor)
        }
        Spacer(Modifier.height(12.dp))
        InvoiceRow("Payment Status") {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (overview.isPaymentSettled) Color(0xFFDCFCE7) else Color(0xFFFFE4CC))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(overview.paymentStatus, fontSize = 11.sp, color = if (overview.isPaymentSettled) Green else Orange)
            }
        }
    }
}

@Composable
private fun InvoiceRow(label: String, value: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 13.sp, color = Color(0xFF4B5563), modifier = Modifier.weight(1f))
        value()
    }
}

// ---------- Stock ----------

@Composable
private fun StockStatusContent(stock: DistributorStockUi) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Stock Status", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = TitleColor)
        SkuGroupCard(
            icon = Res.drawable.ic_icon_box,
            title = "Zero Stock SKU",
            subtitle = null,
            items = stock.zeroStockSkus,
            initiallyExpanded = true
        )
        SkuGroupCard(
            icon = Res.drawable.ic_icon_trending_down,
            title = "Slow Moving SKU",
            subtitle = "Low turnover (${stock.slowMovingThresholdDays}+ days)",
            items = stock.slowMovingSkus,
            initiallyExpanded = false
        )
        StockHealthCard(stock)
    }
}

@Composable
private fun SkuGroupCard(
    icon: DrawableResource,
    title: String,
    subtitle: String?,
    items: ImmutableList<String>,
    initiallyExpanded: Boolean
) {
    var expanded by rememberSaveable(title) { mutableStateOf(initiallyExpanded) }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")

    DetailCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TitleColor)
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(subtitle, fontSize = 11.sp, color = SubtleText)
                }
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Lavender)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text("${items.size} ITEMS", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = Accent)
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                painter = painterResource(Res.drawable.ic_icon_chevron_down),
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = TitleColor,
                modifier = Modifier
                    .size(16.dp)
                    .rotate(chevronRotation)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items.forEach { sku ->
                    Text(
                        text = sku,
                        fontSize = 13.sp,
                        color = TitleColor,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(LavenderLight)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StockHealthCard(stock: DistributorStockUi) {
    DetailCard {
        Text("Stock Health Index", fontSize = 12.sp, color = TitleColor)
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Track)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((stock.healthPercent / 100f).coerceIn(0f, 1f))
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1F1059), Color(0xFF5B3FD6))))
            )
            Text(
                text = "${stock.healthPercent}%",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Green,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
            )
        }
        Spacer(Modifier.height(10.dp))
        Row {
            LegendDot(Green, "In Stock (${stock.inStockPercent}%)")
            Spacer(Modifier.weight(1f))
            LegendDot(Color(0xFFF97316), "Low (${stock.lowPercent}%)")
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 12.sp, color = color)
    }
}
