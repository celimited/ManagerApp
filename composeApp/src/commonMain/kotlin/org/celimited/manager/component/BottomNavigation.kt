package org.celimited.manager.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import manager.composeapp.generated.resources.Res
import manager.composeapp.generated.resources.ic_icon_approval
import manager.composeapp.generated.resources.ic_icon_home
import manager.composeapp.generated.resources.ic_icon_menu
import manager.composeapp.generated.resources.ic_icon_order_status
import manager.composeapp.generated.resources.ic_icon_retail_visit
import org.celimited.manager.navigation.Screen
import org.jetbrains.compose.resources.painterResource

@Composable
fun BottomNavigation(
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {

    val items = listOf(
        "Approval",
        "OrderStatus",
        "Home",
        "RetailVisit",
        "Menu"
    )

    NavigationBar {

        items.forEachIndexed { index, label ->

            val isSelected = selectedIndex == index
            val tintColor = if (isSelected) Color(0xFF582FFF) else Color.Gray

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    Icon(
                        painter = painterResource(
                            when (index) {
                                0 -> Res.drawable.ic_icon_approval
                                1 -> Res.drawable.ic_icon_order_status
                                2 -> Res.drawable.ic_icon_home
                                3 -> Res.drawable.ic_icon_retail_visit
                                4 -> Res.drawable.ic_icon_menu
                                else -> Res.drawable.ic_icon_home
                            }
                        ),
                        contentDescription = null,
                        tint = tintColor,
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        color = tintColor
                    )
                }
            )
        }
    }
}