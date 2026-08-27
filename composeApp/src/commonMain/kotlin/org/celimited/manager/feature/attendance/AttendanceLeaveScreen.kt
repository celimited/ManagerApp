package org.celimited.manager.feature.attendance

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_pending
import org.celimited.manager.component.ApplyLeaveFab
import org.celimited.manager.component.TopBar
import org.jetbrains.compose.resources.painterResource
import kotlinx.datetime.*
import manager.composeapp.generated.resources.ic_icon_back
import manager.composeapp.generated.resources.ic_icon_keyboard_next
import manager.composeapp.generated.resources.ic_icon_next
import org.celimited.manager.utils.currentMonthNumber
import org.celimited.manager.utils.currentYearNumber

@Composable
fun AttendanceLeaveRoute(
    onBackClick: () -> Unit,
    teamAttendanceClick: () -> Unit
) {

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            TopBar(
                title = "Attendance & Leave",
                onBackClick = { onBackClick() }
            )
        },
        floatingActionButton = {
            ApplyLeaveFab(
                onClick = {}
            )
        }
    ) { padding ->
        AttendanceLeaveScreen(
            modifier = Modifier
                .padding(padding),
            teamAttendanceClick
        )
    }
}

@Composable
fun AttendanceLeaveScreen(
    modifier: Modifier = Modifier,
    teamAttendanceClick: () -> Unit
) {

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
    ) {
        PendingLeaveRequestSection()
        MonthlyAttendanceSummary()
        MonthlyCalenderSection()
        
        Box (
            modifier = Modifier
                .fillMaxWidth()
                .padding( start = 10.dp, end = 10.dp, top = 24.dp, bottom = 80.dp )
                .clickable {
                    teamAttendanceClick()
                }
                .background(
                    color = Color(0xFFEEEAFF),
                    shape = RoundedCornerShape(12.dp)),
        ){

            Row (
                modifier = Modifier.padding(
                    horizontal = 10.dp, vertical = 10.dp
                ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "View Your Team Attendance",
                    fontSize = 16.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F1059),
                )

                Spacer(modifier = Modifier.weight(1F))

                Image(
                    painter = painterResource(Res.drawable.ic_icon_next),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
fun PendingLeaveRequestSection() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 20.dp)
            .background(color = Color(0xFFFFFDD2), shape = RoundedCornerShape(12.dp)),
        verticalAlignment = Alignment.CenterVertically
        ) {

        Image(
            painter = painterResource(Res.drawable.ic_icon_pending),
            contentDescription = null,
            modifier = Modifier
                .padding(horizontal = 16.dp)
        )

        Column(
            modifier = Modifier
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "3 Leave Requests Pending",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )

            Text(
                text = "Review from Approvals page",
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                color = Color(0xFF4A5565),
                modifier = Modifier.padding(top = 5.dp)
            )
        }

        Spacer(
            modifier = Modifier
                .weight(1F)
        )

        Text(
            text = "View Requests",
            fontSize = 12.sp,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.padding(end = 24.dp)
        )
    }
}

@Composable
fun MonthlyAttendanceSummary () {
    Column(
        modifier = Modifier
            .padding(horizontal = 10.dp, vertical = 20.dp)
    ) {
        Text(
            text = "Monthly attendance summary",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFF1F1059),
            lineHeight = 32.sp,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress Bar
            Box(
                modifier = Modifier.size(110.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    val strokeWidth = 10.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val centerX = size.width / 2
                    val centerY = size.height / 2

                    // Background track
                    drawArc(
                        color = Color(0xFFE0E0E0),
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc (75%)
                    drawArc(
                        color = Color(0xFF582FFF),
                        startAngle = -90f,
                        sweepAngle = 360f * 0.75f,
                        useCenter = false,
                        topLeft = Offset(centerX - radius, centerY - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "75%",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF582FFF)
                    )
                    Text(
                        text = "Attendance",
                        fontSize = 10.sp,
                        color = Color(0xFF4A5565)
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // 4 Section Grid
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttendanceCard(number = "12", label = "Present", bgColor = Color(0xFFCDFFD0))
                    AttendanceCard(number = "2", label = "Late", bgColor = Color(0xFFFFC3C4))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AttendanceCard(number = "1", label = "Leave", bgColor = Color(0xFFFFE8D4))
                    AttendanceCard(number = "1", label = "Absent", bgColor = Color(0xFFF2F2F7))
                }
            }
        }
    }
}

@Composable
fun AttendanceCard(number: String, label: String, bgColor: Color) {
    Box(
        modifier = Modifier
            .size(width = 120.dp, height = 70.dp)
            .background(color = bgColor, shape = RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.TopStart
    ) {
        Column(
            modifier = Modifier.padding(top = 12.dp, start = 12.dp),
            horizontalAlignment = Alignment.Start) {
            Text(
                text = number,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F1059)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color(0xFF1F1059)
            )
        }
    }
}

enum class AttendanceStatus { PRESENT, LATE, LEAVE, NONE }

@Composable
fun MonthlyCalenderSection(
    initialMonth: Int = currentMonthNumber(),
    initialYear: Int = currentYearNumber()
) {
    val attendanceData = mapOf(
        3 to AttendanceStatus.PRESENT,
        4 to AttendanceStatus.PRESENT,
        5 to AttendanceStatus.PRESENT,
        8 to AttendanceStatus.LATE,
        9 to AttendanceStatus.PRESENT,
        10 to AttendanceStatus.PRESENT,
        11 to AttendanceStatus.LATE,
        12 to AttendanceStatus.PRESENT,
        15 to AttendanceStatus.LEAVE,
        16 to AttendanceStatus.LEAVE,
        17 to AttendanceStatus.PRESENT,
        18 to AttendanceStatus.PRESENT,
    )

    var currentMonth by remember { mutableIntStateOf(initialMonth) }
    var currentYear by remember { mutableIntStateOf(initialYear) }

    val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    fun navigatePrev() {
        if (currentMonth == 1) { currentMonth = 12; currentYear-- } else currentMonth--
    }

    fun navigateNext() {
        if (currentMonth == 12) { currentMonth = 1; currentYear++ } else currentMonth++
    }

    val firstDate = LocalDate(currentYear, currentMonth, 1)
    val firstDayOffset = (firstDate.dayOfWeek.isoDayNumber % 7)
    val totalDays = firstDate.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
    val prevMonthLastDay = firstDate.minus(1, DateTimeUnit.DAY).dayOfMonth

    val calendarDays = buildList {
        for (i in firstDayOffset - 1 downTo 0) add(Triple(prevMonthLastDay - i, false, false))
        for (day in 1..totalDays) add(Triple(day, true, false))
        val remaining = 42 - size
        for (i in 1..remaining) add(Triple(i, false, true))
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
    ) {
        // Cap max width for large screens (tablets, desktops)
        val boxWidth = minOf(maxWidth, 500.dp)

        // Dynamically calculate cell width based on available space
        val horizontalPadding = 8.dp
        val cellGap = 8.dp
        val cellWidth = (boxWidth - (horizontalPadding * 2) - (cellGap * 6)) / 7
        val cellHeight = cellWidth * 0.72f // maintain aspect ratio

        Box(
            modifier = Modifier
                .width(boxWidth)
                .wrapContentHeight()
                .align(Alignment.TopCenter)
                .border(width = 1.dp, color = Color(0xFFCBBFFF), shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
        ) {
            Column {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFFAD46FF), Color(0xFF582FFF))
                            )
                        )
                        .padding(vertical = 12.dp, horizontal = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { navigatePrev() }
                            .align(Alignment.CenterStart),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_icon_back),
                            contentDescription = "Previous",
                            tint = Color(0xFF582FFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "${monthNames[currentMonth - 1]} $currentYear",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .clickable { navigateNext() }
                            .align(Alignment.CenterEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_icon_keyboard_next),
                            contentDescription = "Next",
                            tint = Color(0xFF582FFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Day name headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(cellGap)
                ) {
                    dayNames.forEach { day ->
                        val isFriSat = day == "Fri" || day == "Sat"
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isFriSat) Color(0xFF582FFF) else Color(0xFF1E2939),
                            modifier = Modifier.width(cellWidth),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Calendar grid
                val weeks = calendarDays.chunked(7)
                Column(
                    modifier = Modifier.padding(horizontal = horizontalPadding),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    weeks.forEach { week ->
                        Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                            week.forEachIndexed { index, (day, isCurrentMonth, _) ->
                                val isWeekend = index == 5 || index == 6
                                val status = if (isCurrentMonth) attendanceData[day] else null

                                val (bgColor, textColor) = when {
                                    !isCurrentMonth -> Pair(Color.Transparent, Color(0x6699A1AF))
                                    status == AttendanceStatus.PRESENT -> Pair(Color(0xFFCDFFD0), Color(0xFF364153))
                                    status == AttendanceStatus.LATE -> Pair(Color(0xFFFFC8CA), Color(0xFF364153))
                                    status == AttendanceStatus.LEAVE -> Pair(Color(0xFFFF902F), Color(0xFFFFFFFF))
                                    isWeekend -> Pair(Color(0xFFEEEAFF), Color(0xFF1E2939))
                                    else -> Pair(Color.Transparent, Color(0xFF1E2939))
                                }

                                Box(
                                    modifier = Modifier
                                        .width(cellWidth)
                                        .height(cellHeight)
                                        .background(color = bgColor, shape = RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = (cellWidth.value * 0.26f).sp,
                                        fontWeight = if (isCurrentMonth) FontWeight.Medium else FontWeight.Normal,
                                        color = textColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Legend
                Row(
                    modifier = Modifier.padding(start = 12.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(color = Color(0xFF4CAF50), label = "Present")
                    LegendItem(color = Color(0xFFFFC3C4), label = "Late")
                    LegendItem(color = Color(0xFFFF902F), label = "Leave")
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color = color, shape = CircleShape)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color(0xFF364153)
        )
    }
}