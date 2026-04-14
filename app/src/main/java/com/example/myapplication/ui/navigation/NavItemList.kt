package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import com.example.myapplication.data.local.model.NavItem

object NavItemList {
    val navItemList = listOf(
        NavItem("Inicio", Icons.Default.Home),
        NavItem("Asist.", Icons.Default.Person),
        NavItem("Sol.", Icons.Default.Menu),
        NavItem("Account", Icons.Default.AccountCircle)
    )
}
