package org.celimited.manager.feature.distributorDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_next
import manager.composeapp.generated.resources.ic_icon_sparkle
import manager.composeapp.generated.resources.ic_icon_trending_up
import manager.composeapp.generated.resources.ic_icon_warning
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val StockTitle = Color(0xFF111827)
private val StockSubtle = Color(0xFF6B7280)
private val StockBorder = Color(0xFFE5E7EB)
private val StockAccent = Color(0xFF582FFF)

private val ToneGreen = Color(0xFF16A34A)
private val ToneOrange = Color(0xFFEA8A0C)
private val ToneRed = Color(0xFFE11D48)
private val TonePurple = Color(0xFF7C3AED)

private fun StockSummaryTone.color(): Color = when (this) {
    StockSummaryTone.Positive -> ToneGreen
    StockSummaryTone.Warning -> ToneOrange
    StockSummaryTone.Danger -> ToneRed
    StockSummaryTone.Info -> TonePurple
}

private fun StockSummaryTone.icon(): DrawableResource = when (this) {
    StockSummaryTone.Positive -> Res.drawable.ic_icon_trending_up
    StockSummaryTone.Warning, StockSummaryTone.Info -> Res.drawable.ic_icon_clock
    StockSummaryTone.Danger -> Res.drawable.ic_icon_warning
}

private fun StockHealthLevel.color(): Color = when (this) {
    StockHealthLevel.Zero -> Color(0xFFE11D1D)
    StockHealthLevel.Critical -> Color(0xFFFF6A3D)
    StockHealthLevel.Watch -> Color(0xFFFBB33A)
    StockHealthLevel.Healthy -> Color(0xFF16C45A)
}

@Composable
internal fun StockTabContent(
    stock: DistributorStockTabUi,
    isSummaryLoading: Boolean,
    onApprovalBannerClick: () -> Unit,
    onViewSuggestionsClick: () -> Unit,
    onSummaryItemClick: (id: String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        if (!isSummaryLoading && stock.pendingApprovalCount > 0) {
            ApprovalBanner(stock.pendingApprovalCount, stock.pendingApprovalValue, onApprovalBannerClick)
        }
        if (!isSummaryLoading && stock.suggestionText.isNotEmpty()) {
            SuggestionCard(stock.suggestionText, onViewSuggestionsClick)
        }

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text("Summary", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StockTitle, modifier = Modifier.weight(1f))
            if (!isSummaryLoading && stock.summary.isNotEmpty()) {
                Text("${stock.totalSkus} SKUs", fontSize = 13.sp, color = StockSubtle)
            }
        }
        if (isSummaryLoading) {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StockAccent, modifier = Modifier.size(28.dp))
            }
        } else {
            SummaryGrid(stock.summary, onSummaryItemClick)
            if (stock.health.isNotEmpty()) StockHealthCard(stock.health)
        }
    }
}

@Composable
private fun ApprovalBanner(count: Int, totalValue: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFFFF1DC))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFCD9A0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_warning),
                    contentDescription = null,
                    tint = Color(0xFF7C4A03),
                    modifier = Modifier.size(22.dp)
                )
            }
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (count > 9) "9+" else count.toString(), fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "$count indents need your approval",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = StockTitle
            )
            Spacer(Modifier.height(4.dp))
            Text("$totalValue total value", fontSize = 13.sp, color = Color(0xFFB45309))
        }
    }
}

@Composable
private fun SuggestionCard(text: String, onViewClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF3F1FF))
            .border(1.dp, Color(0xFFDDD6FE), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_sparkle),
                    contentDescription = null,
                    tint = StockAccent,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Indent suggestions", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = StockTitle)
            }
            Spacer(Modifier.height(6.dp))
            Text(text, fontSize = 11.sp, lineHeight = 15.sp, color = Color(0xFF4B5563))
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(StockAccent)
                .clickable(onClick = onViewClick)
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text("View", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
}

@Composable
private fun SummaryGrid(items: ImmutableList<StockSummaryItemUi>, onItemClick: (String) -> Unit) {
    val rows = remember(items) { items.chunked(2) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { item ->
                    SummaryTile(
                        item = item,
                        onClick = { onItemClick(item.id) },
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
                // Keep an odd last tile at half width.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SummaryTile(item: StockSummaryItemUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val toneColor = item.tone.color()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, StockBorder, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = item.label,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp,
                color = StockSubtle,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Icon(
                painter = painterResource(Res.drawable.ic_icon_next),
                contentDescription = null,
                tint = StockSubtle,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(item.value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = StockTitle, maxLines = 1)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(item.tone.icon()),
                contentDescription = null,
                tint = toneColor,
                modifier = Modifier.size(12.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(item.note, fontSize = 11.sp, color = toneColor, maxLines = 1)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StockHealthCard(buckets: ImmutableList<StockHealthBucketUi>) {
    val total = remember(buckets) { buckets.sumOf { it.skuCount }.coerceAtLeast(1) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White)
            .border(1.dp, StockBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Text("Stock Health", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StockTitle)
        Spacer(Modifier.height(4.dp))
        Text("SKU distribution by days of coverage remaining.", fontSize = 12.sp, color = StockSubtle)
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(6.dp))
        ) {
            buckets.filter { it.skuCount > 0 }.forEach { bucket ->
                Box(
                    modifier = Modifier
                        .weight(bucket.skuCount.toFloat())
                        .fillMaxHeight()
                        .background(bucket.level.color()),
                    contentAlignment = Alignment.Center
                ) {
                    // Tiny segments have no room for a label; the legend still shows the count.
                    if (bucket.skuCount * 10 >= total) {
                        Text(bucket.skuCount.toString(), fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        // Wraps so the longer labels ("Out of Stock 15") don't clip on narrow phones.
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            buckets.forEach { bucket ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(9.dp)
                            .background(bucket.level.color())
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(bucket.level.label)
                            append(" ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(bucket.skuCount.toString()) }
                        },
                        fontSize = 12.sp,
                        color = StockTitle
                    )
                }
            }
        }
    }
}
