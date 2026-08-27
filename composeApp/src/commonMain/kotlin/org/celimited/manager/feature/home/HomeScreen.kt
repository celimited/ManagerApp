package org.celimited.manager.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_app_logo
import manager.composeapp.generated.resources.ic_icon_ai_sales
import manager.composeapp.generated.resources.ic_icon_bag
import manager.composeapp.generated.resources.ic_icon_keyboard_next
import manager.composeapp.generated.resources.ic_icon_location
import manager.composeapp.generated.resources.ic_icon_next
import manager.composeapp.generated.resources.ic_icon_notification
import manager.composeapp.generated.resources.ic_icon_pending
import manager.composeapp.generated.resources.ic_icon_trending_down
import manager.composeapp.generated.resources.ic_icon_trending_up
import manager.composeapp.generated.resources.ic_profile_placeholder
import manager.composeapp.generated.resources.ic_icon_attendance
import manager.composeapp.generated.resources.ic_icon_cart
import manager.composeapp.generated.resources.ic_icon_peoples
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeRoute(
    onAttendanceCardClick: () -> Unit
){
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->

        HomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onAttendanceCardClick
        )
    }
}

@Composable
fun HomeScreen(modifier: Modifier, onAttendanceCardClick: () -> Unit) {

    TopBackground()

    Column (
        modifier = modifier
            .fillMaxSize()
    ){
        TopProfile()
        PendingApprovalsSection()

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            AISalesReportSection()
            MyAttendanceSection(
                onAttendanceCardClick = onAttendanceCardClick
            )
            WorkPlanSection()
            TodayStatsSection()
            MTDPerformanceSection()
        }
    }
}

@Composable
fun TopBackground(){
    val statusBarHeight = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp + statusBarHeight)
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF1F1059),
                        Color(0xFF4322BF)
                    )
                ),
                shape = RoundedCornerShape(
                    bottomStart = 25.dp,
                    bottomEnd = 25.dp
                )
            )
    )
}

@Composable
fun TopProfile(){
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(top = 24.dp, start = 12.dp, end = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ){
        Image(
            painter = painterResource(Res.drawable.ic_profile_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(56.dp)
                .clip(CircleShape)
        )

        Column (
            modifier = Modifier.padding(start = 12.dp),
        ){

            Text(
                text = "Alex",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )

            Text(
                text = "Regional State Manager",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp
            )

            Text(
                text = "Area",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 14.sp
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(Res.drawable.ic_icon_notification),
                    contentDescription = "Notification",
                    modifier = Modifier.size(22.dp)
                )
            }

            var count: Int = 8

            if (count > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                        .padding(horizontal = 5.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (count > 9) "9+" else count.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PendingApprovalsSection () {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 18.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFCBBFFF),
                shape = RoundedCornerShape(12)
            )
            .clip(RoundedCornerShape(12))
            .background( Color(0xFFFFFDD2))
    ) {
        Row (
            modifier = Modifier.fillMaxWidth()
                .padding(start = 10.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Image(
                painter = painterResource(Res.drawable.ic_icon_pending),
                contentDescription = null,
                alignment = Alignment.Center,
            )

            Column (
                modifier = Modifier
                    .padding(start = 20.dp, top = 15.dp, bottom = 15.dp)
            ){
                Text(
                    text = "Pending approvals",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 16.sp,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                )

                Text(
                    text = "leave(4), stock(8), Indent Approval pending",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            Image(
                painter = painterResource(Res.drawable.ic_icon_next),
                contentDescription = null,
                modifier = Modifier.padding(end = 5.dp)
            )
        }
    }
}

@Composable
fun AISalesReportSection() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 18.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF020105),
                        Color(0xFF4323BF)
                    )
                )
            )
            .clickable { }
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_ai_sales),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title + subtitle
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "AI Sales Reporting",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .height(22.dp).width(40.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NEW",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Ask questions, get instant charts & insights",
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Trailing chevron
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_keyboard_next),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

@Composable
fun MyAttendanceSection (
    onAttendanceCardClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 18.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFCBBFFF),
                shape = RoundedCornerShape(12)
            )
            .clip(RoundedCornerShape(12))
            .background( Color(0xFFFFFFFF))
            .clickable{
                onAttendanceCardClick()
            }
    ) {

        Column (
            modifier = Modifier.padding(12.dp)
        ) {
            Row {
                Text(
                    text = "My Attendance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.weight(1f))

                Image(
                    painter = painterResource(Res.drawable.ic_icon_next),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 5.dp)
                )

            }

            Row (
                modifier = Modifier
                    .padding(top = 15.dp)
            ) {

                Column {
                    Text(
                        text = "Checked in at     9:12",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "You Have 4 leave plan this month",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }

            }
        }
    }
}

@Composable
fun WorkPlanSection() {
    // Dummy data — replace with API-driven list (ViewModel/StateFlow) later.
    val days = remember {
        listOf(
            WorkPlanDay(8, "Sun", "Branch office", "123 main street, Anywhere"),
            WorkPlanDay(9, "Mon", "Head office", "45 north avenue, Anywhere"),
            WorkPlanDay(10, "Tue", "Branch office", "123 main street, Anywhere"),
            WorkPlanDay(11, "Wed", "Warehouse", "9 industrial road, Anywhere"),
            WorkPlanDay(12, "Thu", "Branch office", "123 main street, Anywhere"),
            WorkPlanDay(13, "Fri", "Branch office", "123 main street, Anywhere"),
            WorkPlanDay(14, "Sat", "Head office", "45 north avenue, Anywhere")
        )
    }

    var selectedDay by remember { mutableStateOf(days.first()) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 18.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFCBBFFF),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
        // removed outer .clickable — no reason for the whole card to be tappable
        // when individual dates and the button already have actions
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Work Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D1B69)
                )
                Spacer(modifier = Modifier.weight(1f))
                Image(
                    painter = painterResource(Res.drawable.ic_icon_next),
                    contentDescription = null,
                    modifier = Modifier.padding(end = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Date row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(days, key = { it.date }) { day ->
                    val isSelected = day == selectedDay
                    Column(
                        modifier = Modifier
                            .width(42.dp).height(50.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) Color(0xFF4B2AC4) else Color.Transparent
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color.Transparent else Color(0xFFE0DCEF),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedDay = day }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = day.date.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else Color(0xFF2D1B69),
                            lineHeight = 18.sp
                        )
                        Text(
                            text = day.dayShort,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.White.copy(alpha = 0.85f) else Color.Gray,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reporting place
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_bag),
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Reporting place : ",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = selectedDay.reportingPlace,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Visit route
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(Res.drawable.ic_icon_location),
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Visit Route : ",
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = selectedDay.visitRoute,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View Visit Details button
            Button(
                onClick = { /* navigate to visit details for selectedDay */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4B2AC4))
            ) {
                Text(
                    text = "View Visit Details",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

data class TodayStat(
    val icon: DrawableResource,
    val label: String,
    val value: String
)

@Composable
fun TodayStatsSection(
    onArrowClick: () -> Unit = {}
) {

    val stats = remember {
        listOf(
            TodayStat(Res.drawable.ic_icon_attendance, "Attendance", "12%"),
            TodayStat(Res.drawable.ic_icon_cart, "Order started", "30%"),
            TodayStat(Res.drawable.ic_icon_cart, "Order", "30%"),
            TodayStat(Res.drawable.ic_icon_peoples, "Idle SR", "35%")
        )
    }

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = " Today's Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D1B69)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(Res.drawable.ic_icon_next),
                contentDescription = null,
                tint = Color(0xFF2D1B69),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onArrowClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(stats) { stat ->
                TodayStatCard(stat)
            }
        }
    }
}

@Composable
private fun TodayStatCard(stat: TodayStat) {
    Box(
        modifier = Modifier
            .width(110.dp)
            .border(
                width = 1.dp,
                color = Color(0xFFE0DCEF),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp)
    ) {
        Column {
            Icon(
                painter = painterResource(stat.icon),
                contentDescription = null,
                tint = Color(0xFF2D1B69),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stat.label,
                fontSize = 12.sp,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stat.value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B2AC4)
            )
        }
    }
}


data class WorkPlanDay(
    val date: Int,
    val dayShort: String,
    val reportingPlace: String,
    val visitRoute: String
)

@Composable
fun MTDPerformanceSection(
    data: MTDPerformanceData = MTDPerformanceData(
        mtdAchievedPercent = 30,
        ytdAchievedPercent = 25,
        tillDateTargetPercent = 12,
        mtdGrowthPercent = 10,
        ytdGrowthPercent = 10,
        m2mGrowthPercent = 10,
        ytdGrowthIsPositive = false
    ),
    onArrowClick: () -> Unit = {}
) {

    Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "MTD Performance",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D1B69)
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(Res.drawable.ic_icon_next),
                contentDescription = null,
                tint = Color(0xFF2D1B69),
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onArrowClick() }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = Color(0xFFE0DCEF),
                    shape = RoundedCornerShape(12.dp)
                )
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {

                MetricProgressRow(
                    label = "MTD Achieved",
                    percent = data.mtdAchievedPercent,
                    progressColor = Color(0xFFFF7A00)
                )
                Spacer(modifier = Modifier.height(12.dp))

                MetricProgressRow(
                    label = "YTD Achieved",
                    percent = data.ytdAchievedPercent,
                    progressColor = Color(0xFFFFB800)
                )
                Spacer(modifier = Modifier.height(12.dp))

                MetricProgressRow(
                    label = "Till Date Target",
                    percent = data.tillDateTargetPercent,
                    progressColor = Color(0xFFFF4D6D)
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Bottom pill badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GrowthPill(
                        label = "MTD",
                        percent = data.mtdGrowthPercent,
                        isPositive = true,
                        backgroundColor = Color(0xFFE3F7E9),
                        contentColor = Color(0xFF1E9E4A)
                    )
                    GrowthPill(
                        label = "YTD",
                        percent = data.ytdGrowthPercent,
                        isPositive = data.ytdGrowthIsPositive,
                        backgroundColor = Color(0xFFFFF9DB),
                        contentColor = Color(0xFF9E7C1E)
                    )
                    GrowthPill(
                        label = "M2M",
                        percent = data.m2mGrowthPercent,
                        isPositive = true,
                        backgroundColor = Color(0xFFE3F7E9),
                        contentColor = Color(0xFF1E9E4A)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricProgressRow(
    label: String,
    percent: Int,
    progressColor: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 13.sp, color = Color.DarkGray)
            Text(
                text = "$percent%",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { percent / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(50)),
            color = progressColor,
            trackColor = Color(0xFFEDEDED),
        )
    }
}

@Composable
private fun GrowthPill(
    label: String,
    percent: Int,
    isPositive: Boolean,
    backgroundColor: Color,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(backgroundColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            painter = if (isPositive) painterResource(Res.drawable.ic_icon_trending_up) else painterResource(Res.drawable.ic_icon_trending_down),
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label $percent%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

data class MTDPerformanceData(
    val mtdAchievedPercent: Int,
    val ytdAchievedPercent: Int,
    val tillDateTargetPercent: Int,
    val mtdGrowthPercent: Int,
    val ytdGrowthPercent: Int,
    val m2mGrowthPercent: Int,
    val ytdGrowthIsPositive: Boolean = false // controls arrow direction on the YTD pill
)