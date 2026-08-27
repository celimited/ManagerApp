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
import org.jetbrains.compose.resources.painterResource

@Composable
fun TeamAttendanceRoute(
    onBackClick: () -> Unit,
) {
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

            )
    }
}

@Composable
fun TeamAttendanceScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        TeamSearchBar()
        TeamFilterTabs()
        TeamAttendanceCards()
        ManagerListUI()
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
fun TeamFilterTabs() {
    val tabs = listOf("Division\n(2/20)", "Region\n(2/20)", "Area\n(2/20)", "Territory\n(2/20)")
    var selectedTab by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 20.dp, bottom = 20.dp, start = 10.dp, end = 10.dp)
            .background(color = Color(0xFFF3F4F6), shape = RoundedCornerShape(25.dp))
            .padding(6.dp)
    ) {
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
            tabs.forEachIndexed { index, title ->
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
                        ) { selectedTab = index }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
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
fun TeamAttendanceCards() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TeamInfoCard(number = "20", label = "Total",    bgColor = Color(0xFFD7E7FF), modifier = Modifier.weight(1f))
        TeamInfoCard(number = "4",  label = "Present",  bgColor = Color(0xFFDCFCE7), modifier = Modifier.weight(1f))
        TeamInfoCard(number = "2",  label = "Late",     bgColor = Color(0xFFFEF3C6), modifier = Modifier.weight(1f))
        TeamInfoCard(number = "2",  label = "On leave", bgColor = Color(0xFFFFD99F), modifier = Modifier.weight(1f))
        TeamInfoCard(number = "1",  label = "Absent",   bgColor = Color(0xFFFFC7C9), modifier = Modifier.weight(1f))
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
fun ManagerListUI () {
    val managers = listOf(
        ManagerItem("Sarah Johnson", "Sales Manager", "North / Metro / Zone A", "Present", "09:05 AM"),
        ManagerItem("Michael Chen", "Regional Lead", "East / Suburban / Zone B", "Late", "09:45 AM"),
        ManagerItem("Emily Rodriguez", "Team Lead", "West / Central / Zone C", "Present", "08:55 AM"),
        ManagerItem("James Williams", "Sales Executive", "South / Industrial / Zone D", "On Leave", null),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 10.dp, vertical = 24.dp)
    ) {
        Text(
            text = "Division Manager List",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF111827)
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(managers) { manager ->
                ManagerListItem(manager)
            }
        }
    }
}

data class ManagerItem(
    val name: String,
    val designation: String,
    val location: String,
    val status: String,
    val time: String?
)

@Composable
fun ManagerListItem(manager: ManagerItem) {
    val statusBgColor = when (manager.status) {
        "Present"  -> Color(0xFFDCFCE7)
        "Late"     -> Color(0xFFFEF3C6)
        "On Leave" -> Color(0xFFFFD99F)
        "Absent"   -> Color(0xFFFFC7C9)
        else       -> Color(0xFFF3F4F6)
    }
    val statusTextColor = when (manager.status) {
        "Present"  -> Color(0xFF16A34A)
        "Late"     -> Color(0xFFD97706)
        "On Leave" -> Color(0xFFEA580C)
        "Absent"   -> Color(0xFFDC2626)
        else       -> Color(0xFF6B7280)
    }

    // Generate initials from name
    val initials = manager.name
        .split(" ")
        .take(2)
        .joinToString("") { it.first().uppercase() }

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
                    text = initials,
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

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = manager.designation,
                    fontSize = 14.sp,
                    color = Color(0xFF6B7280)
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Location row
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
                            text = manager.status,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = statusTextColor
                        )
                    }

                    // Time with clock icon
                    if (manager.time != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Icon(
                            painter = painterResource(Res.drawable.ic_icon_clock),
                            contentDescription = null,
                            tint = Color(0xFF9CA3AF),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = manager.time,
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