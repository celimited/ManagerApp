package org.celimited.manager.feature.teamAttendance

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Brush
import org.celimited.manager.component.TopBar
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_clock
import manager.composeapp.generated.resources.ic_icon_keyboard_next
import manager.composeapp.generated.resources.ic_icon_location
import manager.composeapp.generated.resources.ic_icon_search
import androidx.compose.material3.CircularProgressIndicator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.collections.immutable.ImmutableList
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.model.attendance.TeamMemberStatus
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun TeamAttendanceRoute(
    onBackClick: () -> Unit,
    onSessionExpired: () -> Unit,
    viewModel: TeamAttendanceViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.onScreenOpened()
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                TeamAttendanceUiEffect.SessionExpired -> onSessionExpired()
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "Team Attendance",
                onBackClick = { onBackClick() }
            )
        }
    ) { padding ->
        TeamAttendanceScreen(
            modifier = Modifier
                .padding(padding),
            levels = uiState.levels,
            selectedLevelKey = uiState.selectedLevelKey,
            isLoadingLevels = uiState.isLoadingLevels,
            summary = uiState.summary,
            managers = uiState.managers,
            isLoadingSummary = uiState.isLoadingSummary,
            onLevelSelected = viewModel::onLevelSelected
        )
    }

    uiState.errorDialog?.let { error ->
        ErrorDialog(
            message = error.message,
            onClose = viewModel::onDismissError,
            onRetry = if (error.canRetry) viewModel::onRetry else null
        )
    }
}

@Composable
fun TeamAttendanceScreen(
    modifier: Modifier = Modifier,
    levels: ImmutableList<HierarchyLevelTabUi>,
    selectedLevelKey: String?,
    isLoadingLevels: Boolean,
    summary: TeamSummaryUi,
    managers: ImmutableList<ManagerItemUi>,
    isLoadingSummary: Boolean,
    onLevelSelected: (String) -> Unit
) {
    val listTitle = remember(levels, selectedLevelKey) {
        val levelName = levels.firstOrNull { it.key == selectedLevelKey }?.name
        if (levelName == null) "Manager List" else "$levelName Manager List"
    }

    Column(
        modifier = modifier
    ) {
        TeamSearchBar()
        TeamFilterTabs(
            tabs = levels,
            selectedKey = selectedLevelKey,
            isLoading = isLoadingLevels,
            onTabSelected = onLevelSelected
        )
        TeamAttendanceCards(summary = summary)
        ManagerListUI(
            title = listTitle,
            managers = managers,
            // Levels loading also means no summary yet — show one spinner for both.
            isLoading = isLoadingSummary || isLoadingLevels
        )
    }
}

@Composable
fun TeamSearchBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, start = 10.dp, end = 10.dp)
            .border(width = 1.dp, color = Color(0xFFCBBFFF), shape = RoundedCornerShape(size = 20.dp))
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_icon_search),
            contentDescription = "Search",
            tint = Color(0xFF9CA3AF),
            modifier = Modifier
                .padding(start = 16.dp)
                .size(20.dp)
        )
        Text(
            text = "Search by name, location, designation",
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF),
            modifier = Modifier
                .padding(start = 12.dp, end = 16.dp)
        )
    }
}

@Composable
fun TeamFilterTabs(
    tabs: ImmutableList<HierarchyLevelTabUi>,
    selectedKey: String?,
    isLoading: Boolean,
    onTabSelected: (String) -> Unit
) {
    val containerModifier = Modifier
        .fillMaxWidth()
        .padding(top = 20.dp, bottom = 20.dp, start = 10.dp, end = 10.dp)
        .background(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(25.dp))
        .padding(6.dp)

    if (tabs.isEmpty()) {
        // Same pill, so the layout below doesn't jump when the levels arrive.
        Box(modifier = containerModifier.height(44.dp), contentAlignment = Alignment.Center) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color(0xFF582FFF),
                    strokeWidth = 2.dp
                )
            }
        }
        return
    }

    val selectedTab = tabs.indexOfFirst { it.key == selectedKey }.coerceAtLeast(0)

    BoxWithConstraints(modifier = containerModifier) {
        val tabWidth = maxWidth / tabs.size

        val indicatorOffset by animateDpAsState(
            targetValue = tabWidth * selectedTab,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "indicator_offset"
        )

        // Sliding background indicator
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(tabWidth)
                .height(44.dp)
                .background(color = Color(0xFF582FFF), shape = RoundedCornerShape(25.dp))
        )

        // Tab labels on top
        Row(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = selectedTab == index

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color(0xFF6B7280),
                    animationSpec = tween(durationMillis = 200),
                    label = "textColor_$index"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onTabSelected(tab.key) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tab.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TeamAttendanceCards(summary: TeamSummaryUi) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeamInfoCard(number = summary.total.toString(),   label = "Total",    bgColor = Color(0xFFD7E7FF), modifier = Modifier.weight(1f))
        TeamInfoCard(number = summary.present.toString(), label = "Present",  bgColor = Color(0xFFDCFCE7), modifier = Modifier.weight(1f))
        TeamInfoCard(number = summary.late.toString(),    label = "Late",     bgColor = Color(0xFFFEF3C6), modifier = Modifier.weight(1f))
        TeamInfoCard(number = summary.leave.toString(),   label = "On leave", bgColor = Color(0xFFFFD99F), modifier = Modifier.weight(1f))
        TeamInfoCard(number = summary.absent.toString(),  label = "Absent",   bgColor = Color(0xFFFFC7C9), modifier = Modifier.weight(1f))
    }
}

@Composable
fun TeamInfoCard(number: String, label: String, bgColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(8.dp))
    ) {
        Column(
            modifier = Modifier.padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
        ) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF111827),
            )
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = Color(0xFF374151),
            )
        }
    }
}

@Composable
fun ManagerListUI(
    title: String,
    managers: ImmutableList<ManagerItemUi>,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 24.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827)
        )

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF582FFF), modifier = Modifier.size(28.dp))
            }

            managers.isEmpty() -> Text(
                text = "No managers found",
                fontSize = 13.sp,
                color = Color(0xFF6B7280),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(24.dp)
            )

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(managers, key = { it.userId }) { manager ->
                    ManagerListItem(manager)
                }
            }
        }
    }
}

@Composable
fun ManagerListItem(manager: ManagerItemUi) {
    val (statusLabel, statusBgColor, statusTextColor) = when (manager.status) {
        TeamMemberStatus.PRESENT -> Triple("Present", Color(0xFFDCFCE7), Color(0xFF16A34A))
        TeamMemberStatus.LATE -> Triple("Late", Color(0xFFFEF3C6), Color(0xFFD97706))
        TeamMemberStatus.LEAVE -> Triple("On Leave", Color(0xFFFFD99F), Color(0xFFEA580C))
        TeamMemberStatus.ABSENT -> Triple("Absent", Color(0xFFFFC7C9), Color(0xFFDC2626))
        TeamMemberStatus.UNKNOWN -> Triple("Unknown", Color(0xFFF3F4F6), Color(0xFF6B7280))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = Color(0xFFCBBFFF), shape = RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar circle with initials
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF7C3AED), Color(0xFF582FFF))
                        ),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = manager.initials,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info section
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = manager.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )

                // The API often sends designation/location as "", so those rows are skipped then.
                if (manager.designation.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = manager.designation,
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }

                if (manager.location.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_icon_location),
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = manager.location,
                            fontSize = 12.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Status + time row
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Status chip
                    Box(
                        modifier = Modifier
                            .background(color = statusBgColor, shape = RoundedCornerShape(25.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusTextColor
                        )
                    }

                    // Time with clock icon
                    if (manager.checkInTime != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_icon_clock),
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = manager.checkInTime,
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            // Arrow icon top right
            Icon(
                painter = painterResource(Res.drawable.ic_icon_keyboard_next),
                contentDescription = null,
                tint = Color(0xFF9CA3AF),
                modifier = Modifier
                    .size(20.dp)
                    .padding(4.dp)
            )
        }
    }
}