package com.example.myapplication.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import com.example.myapplication.data.local.model.NavItem

object NavItemList {
    val navItemList = listOf(
        NavItem("Inicio", Icons.Default.Home),
        NavItem("Asistencias", Icons.Default.Person),
        NavItem("Rutas", Icons.Default.Place)
    )
}