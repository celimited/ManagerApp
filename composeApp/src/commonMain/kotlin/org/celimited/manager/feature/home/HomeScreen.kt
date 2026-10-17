package org.celimited.manager.feature.home

import androidx.compose.foundation.Image
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
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
import androidx.compose.runtime.LaunchedEffect
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
import manager.composeapp.generated.resources.ic_icon_box
import manager.composeapp.generated.resources.ic_icon_truck
import manager.composeapp.generated.resources.ic_icon_keyboard_next
import manager.composeapp.generated.resources.ic_icon_location
import manager.composeapp.generated.resources.ic_icon_logout
import manager.composeapp.generated.resources.ic_icon_next
import manager.composeapp.generated.resources.ic_icon_notification
import manager.composeapp.generated.resources.ic_icon_pending
import manager.composeapp.generated.resources.ic_icon_trending_down
import manager.composeapp.generated.resources.ic_icon_trending_up
import manager.composeapp.generated.resources.ic_profile_placeholder
import manager.composeapp.generated.resources.ic_icon_attendance
import manager.composeapp.generated.resources.ic_icon_cart
import manager.composeapp.generated.resources.ic_icon_peoples
import org.celimited.manager.component.ErrorDialog
import org.celimited.manager.component.LockedFeature
import org.celimited.manager.component.PopupCard
import org.celimited.manager.component.PopupTitle
import org.celimited.manager.component.PrimaryButton
import org.celimited.manager.core.common.Feature
import org.celimited.manager.core.common.location.rememberLocationPermissionRequester
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun HomeRoute(
    onAISalesReportClick: () -> Unit,
    onAttendanceCardClick: () -> Unit,
    onTodayStatsClick: () -> Unit,
    onStockStatusClick: () -> Unit,
    onFeatureLocked: () -> Unit,
    onLogout: () -> Unit,
    viewModel: HomeViewModel = koinViewModel()
){
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestLocationPermission = rememberLocationPermissionRequester(
        onResult = viewModel::onLocationPermissionResult
    )

    LaunchedEffect(Unit) {
        viewModel.onScreenShown()
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                HomeUiEffect.NavigateToAttendance -> onAttendanceCardClick()
                HomeUiEffect.RequestLocationPermission -> requestLocationPermission()
                HomeUiEffect.NavigateToTodayStats -> onTodayStatsClick()
                HomeUiEffect.NavigateToDistributorList -> onStockStatusClick()
                HomeUiEffect.ShowUnderDevelopment -> onFeatureLocked()
                HomeUiEffect.NavigateToLogin -> onLogout()
            }
        }
    }

    AttendancePopupHost(
        popup = uiState.attendancePopup,
        onDistributorSelected = viewModel::onDistributorSelected,
        onLateReasonSelected = viewModel::onLateReasonSelected,
        onRemarksChanged = viewModel::onRemarksChanged,
        onSubmit = viewModel::onSubmitAttendance,
        onDismiss = viewModel::onAttendancePopupDismissed,
        onGeofenceWarningConfirmed = viewModel::onGeofenceWarningConfirmed,
        onGeofenceWarningDismissed = viewModel::onGeofenceWarningDismissed
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->

        HomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onAttendanceCardClick = viewModel::onAttendanceCardClick,
            onGiveAttendanceClick = viewModel::onGiveAttendanceClick,
            onAISalesReportClick = onAISalesReportClick,
            onTodayStatsClick = viewModel::onTodayStatsClick,
            onStockStatusClick = viewModel::onStockStatusClick,
            onLockedFeatureClick = viewModel::onLockedFeatureClick,
            onLogoutClick = viewModel::onLogoutClick,
            userName = uiState.userName,
            userRole = uiState.userRole,
            attendanceCard = uiState.attendanceCard
        )
    }

    if (uiState.showLogoutConfirm) {
        LogoutConfirmDialog(
            onConfirm = viewModel::onLogoutConfirmed,
            onDismiss = viewModel::onLogoutDismissed
        )
    }

    // Composed after the check-in popup so it stacks on top of it.
    uiState.errorDialog?.let { error ->
        ErrorDialog(message = error.message, onClose = viewModel::onDismissError)
    }
}

@Composable
fun HomeScreen(
    modifier: Modifier,
    onAttendanceCardClick: () -> Unit,
    onGiveAttendanceClick: () -> Unit,
    onAISalesReportClick: () -> Unit,
    onTodayStatsClick: () -> Unit,
    onStockStatusClick: () -> Unit,
    onLockedFeatureClick: () -> Unit,
    onLogoutClick: () -> Unit,
    userName: String,
    userRole: String,
    attendanceCard: AttendanceCardUi
) {

    TopBackground()

    Column (
        modifier = modifier
            .fillMaxSize()
    ){
        TopProfile(
            name = userName,
            role = userRole,
            onLockedFeatureClick = onLockedFeatureClick,
            onLogoutClick = onLogoutClick
        )
        PendingApprovalsSection()

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
        ) {
            AISalesReportSection(
                onAISalesReportClick = onAISalesReportClick
            )
            MyAttendanceSection(
                status = attendanceCard,
                onAttendanceCardClick = onAttendanceCardClick,
                onGiveAttendanceClick = onGiveAttendanceClick
            )
            WorkPlanSection(onLockedFeatureClick = onLockedFeatureClick)
            TodayStatsSection(onArrowClick = onTodayStatsClick)
            MTDPerformanceSection(onLockedFeatureClick = onLockedFeatureClick)
            StockStatusSection(onArrowClick = onStockStatusClick)
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
fun TopProfile(
    name: String,
    role: String,
    onLockedFeatureClick: () -> Unit,
    onLogoutClick: () -> Unit
){
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
                text = name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 18.sp
            )

            Text(
                text = role,
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

        LockedFeature(
            locked = Feature.Notifications.isLocked,
            onLockedClick = onLockedFeatureClick,
            shape = CircleShape
        ) {
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

        Spacer(modifier = Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable(onClick = onLogoutClick),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_icon_logout),
                contentDescription = "Logout",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PopupCard(onDismissRequest = onDismiss) {
        PopupTitle(text = "Log out", color = Color(0xFF1F1059))
        Spacer(Modifier.height(12.dp))
        Text(text = "Are you sure you want to log out?", fontSize = 13.sp, color = Color(0xFF1E1E1E))
        Spacer(Modifier.height(20.dp))
        PrimaryButton(text = "Logout", onClick = onConfirm, fontSize = 14.sp, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        PrimaryButton(
            text = "Cancel",
            onClick = onDismiss,
            fontSize = 14.sp,
            containerColor = Color(0xFFEEEAFF),
            contentColor = Color(0xFF582FFF),
            modifier = Modifier.fillMaxWidth()
        )
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
fun AISalesReportSection(
    onAISalesReportClick: () -> Unit
) {
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
            .clickable { onAISalesReportClick() }
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
                        text = "AI Business Assistant",
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

private val AttendanceTitleColor = Color(0xFF1F1059)
private val AttendanceOrange = Color(0xFFF07B14)
private val AttendanceRed = Color(0xFFE53935)
private val AttendanceLavender = Color(0xFF8B7BE8)
private val PillGreen = Color(0xFFCFF7D3)
private val PillPeach = Color(0xFFFFCB99)

/** Message line, its color and the status pill (null = none) for each card state. */
private data class AttendanceCardContent(
    val message: String,
    val messageColor: Color,
    val pillText: String? = null,
    val pillColor: Color = Color.Transparent
)

private fun AttendanceCardUi.content(): AttendanceCardContent = when (this) {
    AttendanceCardUi.Unknown ->
        AttendanceCardContent("Tap to see today's attendance", Color.DarkGray)
    AttendanceCardUi.NotGiven ->
        AttendanceCardContent("You haven't given attendance yet !", AttendanceOrange)
    AttendanceCardUi.TimePassed ->
        AttendanceCardContent("Attendance time has passed !", AttendanceRed)
    is AttendanceCardUi.CheckedIn -> when {
        time == null -> AttendanceCardContent("You have checked in today", Color.Black, "Present", PillGreen)
        isLate == true -> AttendanceCardContent("You Checked in Late    $time", Color.Black, "Late", PillPeach)
        else -> AttendanceCardContent("Checked in at    $time", Color.Black, "On time", PillGreen)
    }
    AttendanceCardUi.LeavePending ->
        AttendanceCardContent("Your leave approval is pending", AttendanceLavender, "Pending", PillPeach)
    AttendanceCardUi.OnLeave ->
        AttendanceCardContent("You are on leave", AttendanceLavender, "On Leave", PillPeach)
}

@Composable
fun MyAttendanceSection (
    status: AttendanceCardUi,
    onAttendanceCardClick: () -> Unit,
    onGiveAttendanceClick: () -> Unit
) {
    val content = remember(status) { status.content() }

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
                    text = "Attendance",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AttendanceTitleColor
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
                    .fillMaxWidth()
                    .padding(top = 15.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = content.message,
                        color = content.messageColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 14.sp
                    )

                    Text(
                        text = "You Have 4 leave plan this month",
                        color = AttendanceOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 12.sp,
                        modifier = Modifier.padding(top = 5.dp)
                    )
                }

                // Absent is everyone's starting state, so it always offers check-in (late after the deadline).
                if (status == AttendanceCardUi.NotGiven || status == AttendanceCardUi.TimePassed) {
                    Button(
                        onClick = onGiveAttendanceClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF582FFF)),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(
                            text = "Give attendance",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else if (content.pillText != null) {
                    AttendanceStatusPill(text = content.pillText, color = content.pillColor)
                }
            }
        }
    }
}

/** Status only — not clickable. */
@Composable
private fun AttendanceStatusPill(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 12.sp
        )
    }
}

@Composable
fun WorkPlanSection(onLockedFeatureClick: () -> Unit) {
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

    LockedFeature(
        locked = Feature.WorkPlan.isLocked,
        onLockedClick = onLockedFeatureClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 18.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = " Today's Stats",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D1B69)
            )
            Spacer(modifier = Modifier.weight(1f))
            SectionArrowButton(
                contentDescription = "Open Today's Stats",
                onClick = onArrowClick
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
    onArrowClick: () -> Unit = {},
    onLockedFeatureClick: () -> Unit
) {

    LockedFeature(
        locked = Feature.MtdPerformance.isLocked,
        onLockedClick = onLockedFeatureClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 28.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column (
            modifier = Modifier
                .fillMaxWidth()
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

@Composable
fun StockStatusSection(
    onArrowClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 28.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Stock Status",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2D1B69)
            )
            Spacer(modifier = Modifier.weight(1f))
            SectionArrowButton(
                contentDescription = "Open Stock Status",
                onClick = onArrowClick
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dummy data — replace with API-driven values later.
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StockStatusCard(
                icon = Res.drawable.ic_icon_truck,
                title = "Transit Invoices",
                pendingCount = 4,
                distributorCount = 4,
                modifier = Modifier.weight(1f)
            )
            StockStatusCard(
                icon = Res.drawable.ic_icon_box,
                title = "Indent",
                pendingCount = 3,
                distributorCount = 3,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/** Circular arrow button for section headers — same look as the TopBar back button. */
@Composable
private fun SectionArrowButton(
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_icon_next),
            contentDescription = contentDescription,
            tint = Color(0xFF2D1B69),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Composable
private fun StockStatusCard(
    icon: DrawableResource,
    title: String,
    pendingCount: Int,
    distributorCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFFFFDD2))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = Color(0xFF3A3A3A),
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Pending $pendingCount",
                fontSize = 12.sp,
                color = Color(0xFFF7931E)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            fontSize = 18.sp,
            color = Color.Black,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "$distributorCount Distributor",
            fontSize = 12.sp,
            color = Color.Black
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

@Composable
@Preview(showBackground = true, showSystemUi = true)
private fun HomeScreenPreview() {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.systemBars
    ) { padding ->
        HomeScreen(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onAttendanceCardClick = {},
            onGiveAttendanceClick = {},
            onAISalesReportClick = {},
            onTodayStatsClick = {},
            onStockStatusClick = {},
            onLockedFeatureClick = {},
            onLogoutClick = {},
            userName = "John Doe",
            userRole = "Area Manager",
            attendanceCard = AttendanceCardUi.NotGiven
        )
    }
}