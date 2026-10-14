package org.celimited.manager.feature.distributorList

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_location
import manager.composeapp.generated.resources.ic_icon_peoples
import manager.composeapp.generated.resources.ic_icon_search
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.component.TopBar
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

private val TitleColor = Color(0xFF1F1059)
private val Accent = Color(0xFF582FFF)
private val PanelBg = Color(0xFFF3F1FF)
private val FieldBorder = Color(0xFFE0DCEF)
private val SubtleText = Color(0xFF6B7280)

@Composable
fun DistributorListRoute(
    onBackClick: () -> Unit,
    onSessionExpired: () -> Unit,
    onDistributorClick: (distributorId: Int) -> Unit,
    viewModel: DistributorListViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                DistributorListUiEffect.SessionExpired -> onSessionExpired()
                is DistributorListUiEffect.NavigateToDetail -> onDistributorClick(effect.distributorId)
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "Distributor List",
                onBackClick = onBackClick
            )
        }
    ) { padding ->
        DistributorListScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            distributors = uiState.distributors,
            query = uiState.query,
            isRefreshing = uiState.isRefreshing,
            isInitialLoading = uiState.isInitialLoading,
            onQueryChange = viewModel::onQueryChange,
            onRefresh = viewModel::onRefresh,
            onDistributorClick = viewModel::onDistributorClick
        )
    }

    uiState.errorDialog?.let { error ->
        ErrorDialog(message = error.message, onClose = viewModel::onDismissError, onRetry = viewModel::onRetry)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DistributorListScreen(
    modifier: Modifier,
    distributors: ImmutableList<DistributorItemUi>,
    query: String,
    isRefreshing: Boolean,
    isInitialLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onDistributorClick: (distributorId: Int) -> Unit
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = modifier
    ) {
        // LazyColumn fills the box so pulling works even when the list is empty.
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp)
                .padding(top = 8.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item(key = "header") { PanelHeader() }
            item(key = "search") { SearchField(query, onQueryChange) }

            when {
                isInitialLoading -> item(key = "loading") {
                    PanelFooter {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                        }
                    }
                }
                distributors.isEmpty() -> item(key = "empty") {
                    PanelFooter {
                        Text(
                            text = if (query.isBlank()) "No distributors found" else "No match for \"$query\"",
                            fontSize = 13.sp,
                            color = SubtleText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        )
                    }
                }
                else -> {
                    items(distributors, key = { it.id }) { distributor ->
                        PanelRow {
                            DistributorCard(distributor, onClick = { onDistributorClick(distributor.id) })
                        }
                    }
                    item(key = "bottom") { PanelFooter { Spacer(Modifier.height(4.dp)) } }
                }
            }
        }
    }
}

// The design is one lavender panel; in a LazyColumn it's built from a top, middle rows and a bottom piece.

@Composable
private fun PanelHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(PanelBg)
            .padding(horizontal = 12.dp)
            .padding(top = 16.dp, bottom = 12.dp),
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
                painter = painterResource(Res.drawable.ic_icon_peoples),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Select Distributor", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TitleColor)
            Spacer(Modifier.height(2.dp))
            Text("Choose from available distributors", fontSize = 11.sp, color = SubtleText)
        }
    }
}

// Hint and typed text share one style so they sit on the same baseline.
private val SearchTextStyle = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, color = TitleColor)

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    PanelRow {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = SearchTextStyle,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White)
                        .border(1.dp, FieldBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_icon_search),
                        contentDescription = null,
                        tint = SubtleText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text("Search by name or ID...", style = SearchTextStyle.copy(color = SubtleText))
                        }
                        inner()
                    }
                }
            }
        )
    }
}

@Composable
private fun PanelRow(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PanelBg)
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        content()
    }
}

@Composable
private fun PanelFooter(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .background(PanelBg)
            .padding(horizontal = 12.dp)
            .padding(bottom = 12.dp)
    ) {
        content()
    }
}

@Composable
private fun DistributorCard(distributor: DistributorItemUi, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = distributor.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111827),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(4.dp))
        Text("ID: ${distributor.code}", fontSize = 12.sp, color = SubtleText)
        if (distributor.address.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_location),
                    contentDescription = null,
                    tint = Color(0xFF374151),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = distributor.address,
                    fontSize = 12.sp,
                    color = Color(0xFF374151),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
