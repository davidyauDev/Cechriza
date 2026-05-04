package com.cechriza.app.ui.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cechriza.app.data.local.model.NavItem

private val BottomBarBackground = Color.White
private val BottomBarBorder = Color(0xFFE6E6E6)
private val SelectedIcon = Color(0xFF111111)
private val SelectedLabel = Color(0xFF111111)
private val UnselectedIcon = Color(0xFFB0B0B0)
private val UnselectedLabel = Color(0xFFB0B0B0)

@Composable
fun BottomNavBar(
    navItemList: List<NavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BottomBarBackground,
        border = BorderStroke(1.dp, BottomBarBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItemList.forEachIndexed { index, navItem ->
                val isSelected = index == selectedIndex

                NavItemChip(
                    label = navItem.label,
                    icon = navItem.icon,
                    selected = isSelected,
                    onClick = { onItemSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun NavItemChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) SelectedIcon else UnselectedIcon,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) SelectedLabel else UnselectedLabel,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
