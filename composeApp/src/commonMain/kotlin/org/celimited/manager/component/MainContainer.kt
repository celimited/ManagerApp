package org.celimited.manager.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.celimited.manager.feature.approval.ApprovalsRoute
import org.celimited.manager.feature.home.HomeRoute
import org.celimited.manager.feature.menu.MenuRoute
import org.celimited.manager.feature.orderStatus.OrderStatusRoute
import org.celimited.manager.feature.retailVisit.RetailVisitRoute

@Composable
fun MainContainer(
    onAttendanceCardClick: () -> Unit
) {

    var selectedTab by rememberSaveable { mutableStateOf(2) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            BottomNavigation(
                selectedIndex = selectedTab,
                onItemSelected = { selectedTab = it }
            )
        }
    ) { padding ->

        Box(Modifier.padding(padding)) {
            when (selectedTab) {
                0 -> ApprovalsRoute()
                1 -> OrderStatusRoute()
                2 -> HomeRoute(
                    onAttendanceCardClick = onAttendanceCardClick
                )
                3 -> RetailVisitRoute()
                4 -> MenuRoute()
            }
        }
    }
}