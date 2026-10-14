package org.celimited.manager.feature.todayStats

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.collections.immutable.ImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_cart
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_order_status
import manager.composeapp.generated.resources.ic_profile_placeholder
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val SrCardBg = Color(0xFFEEEAFF)
private val SrOrange = Color(0xFFF7A23B)
internal val OnlineGreen = Color(0xFF22C55E)

/** SR tab: search, then one card per sales rep with check-in/order times and four KPI tiles. */
@Composable
internal fun SrListContent(
    query: String,
    items: ImmutableList<SrCardUi>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    onQueryChange: (String) -> Unit,
    onSrClick: (srId: Int) -> Unit,
    onLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()
    // Ask for the next page a few rows before the end; the ViewModel ignores repeats while loading.
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisible >= info.totalItemsCount - LOAD_MORE_THRESHOLD
        }
    }
    LaunchedEffect(nearEnd, items.size) {
        if (nearEnd && items.isNotEmpty()) onLoadMore()
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "search") { SearchField(query, "Search SR", onQueryChange) }
        when {
            items.isEmpty() && isLoading -> item(key = "loading") { LoadingBox() }
            items.isEmpty() -> item(key = "empty") { EmptyText("No SR found") }
            else -> {
                items(items, key = { it.srId }) { sr -> SrCard(sr, onClick = { onSrClick(sr.srId) }) }
                if (isLoadingMore) item(key = "loadingMore") { LoadingBox() }
            }
        }
    }
}

private const val LOAD_MORE_THRESHOLD = 3

@Composable
private fun SrCard(sr: SrCardUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SrCardBg)
            .clickable(onClick = onClick)
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(isOnline = sr.isOnline, size = 44)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(sr.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TitleColor)
                Spacer(Modifier.height(4.dp))
                Text(sr.location, fontSize = 13.sp, color = TitleColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(
                sr.territory,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TitleColor,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeItem(Res.drawable.ic_icon_clock, "In: ${sr.checkIn}", Green)
            TimeItem(Res.drawable.ic_icon_cart, "1st order: ${sr.firstOrder}", TitleColor)
            TimeItem(Res.drawable.ic_icon_order_status, "Last: ${sr.lastOrder}", TitleColor)
        }

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            MetricTile("Strike Rate", sr.strikeRate, Red, Modifier.weight(1f))
            MetricTile("Productive\nRate", sr.productiveRate, Green, Modifier.weight(1f))
            MetricTile("LPC", sr.lpc, Green, Modifier.weight(1f))
            MetricTile("Order\nAchievement", sr.orderAchievement, SrOrange, Modifier.weight(1f))
        }
    }
}

/** Placeholder photo with a green dot when the SR is online. Real photos come with the API (photoUrl). */
@Composable
internal fun Avatar(isOnline: Boolean, size: Int) {
    Box {
        Image(
            painter = painterResource(Res.drawable.ic_profile_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size.dp)
                .clip(CircleShape)
        )
        if (isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size((size / 4).dp)
                    .clip(CircleShape)
                    .background(OnlineGreen)
                    .border(2.dp, Color.White, CircleShape)
            )
        }
    }
}

@Composable
private fun TimeItem(icon: DrawableResource, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(14.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, color = color, maxLines = 1)
    }
}

@Composable
private fun MetricTile(label: String, value: String, barColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                color = TitleColor,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TitleColor, maxLines = 1)
        }
        // Short colored accent bar along the bottom-left, as in the design.
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(3.dp)
                .background(barColor)
        )
    }
}
