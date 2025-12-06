package com.example.myapplication.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.model.NavItem

@Composable
fun BottomNavBar(
    navItemList: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 4.dp,
        contentColor = Color.Gray
    ) {
        navItemList.forEachIndexed { index, navItem ->
            val isSelected = index == selectedIndex

            NavigationBarItem(
                selected = isSelected,
                onClick = { onItemSelected(index) },
                icon = {
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) Color(0xFFDCEBFA) else Color.Transparent,
                                shape = MaterialTheme.shapes.medium
                            )
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = navItem.icon,
                            contentDescription = navItem.label,
                            tint = if (isSelected) Color(0xFF0051A8) else Color.Gray
                        )
                    }
                },
                label = {
                    Text(
                        text = navItem.label,
                        fontSize = 12.sp,
                        color = if (isSelected) Color(0xFF0051A8) else Color.Gray
                    )
                },
                alwaysShowLabel = true
            )
        }
    }
}

