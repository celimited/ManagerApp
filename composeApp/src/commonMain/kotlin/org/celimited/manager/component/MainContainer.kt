package org.celimited.manager.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch
import org.celimited.manager.feature.approval.ApprovalsRoute
import org.celimited.manager.feature.home.HomeRoute
import org.celimited.manager.feature.menu.MenuRoute
import org.celimited.manager.feature.orderStatus.OrderStatusRoute
import org.celimited.manager.feature.retailVisit.RetailVisitRoute

@Composable
fun MainContainer(
    onAISalesReportClick: () -> Unit,
    onAttendanceCardClick: () -> Unit,
    onTodayStatsClick: () -> Unit,
    onStockStatusClick: () -> Unit,
    onLogout: () -> Unit
) {

    var selectedTab by rememberSaveable { mutableStateOf(2) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showUnderDevelopment: () -> Unit = {
        scope.launch { snackbarHostState.showUnderDevelopment() }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            BottomNavigation(
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it },
                onLockedItemClick = showUnderDevelopment
            )
        }
    ) { padding ->

        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ApprovalsRoute()
                1 -> OrderStatusRoute()
                2 -> HomeRoute(
                    onAISalesReportClick = onAISalesReportClick,
                    onAttendanceCardClick = onAttendanceCardClick,
                    onTodayStatsClick = onTodayStatsClick,
                    onStockStatusClick = onStockStatusClick,
                    onFeatureLocked = showUnderDevelopment,
                    onLogout = onLogout
                )
                3 -> RetailVisitRoute()
                4 -> MenuRoute()
            }
        }
    }
}
