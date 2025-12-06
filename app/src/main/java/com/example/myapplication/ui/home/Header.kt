package com.example.myapplication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BlueHeaderWithName(
    userName: String,
    modifier: Modifier = Modifier,
    onMenuClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .background(Color(0xFF0051A8)) // Azul corporativo
            .height(70.dp)
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // LEFT SIDE
            Row(verticalAlignment = Alignment.CenterVertically) {

                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Avatar circular con iniciales
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(color = Color(0xFF407BCE), shape = androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val initials = userName
                        .split(" ")
                        .take(2)
                        .joinToString("") { it.firstOrNull()?.uppercase() ?: "" }

                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Bienvenido",
                        color = Color.White,
                        fontSize = 12.sp
                    )
                    Text(
                        text = userName,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // RIGHT SIDE: Campana + punto verde
            Box(modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Notificación",
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.Center)
                )

                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Green, shape = androidx.compose.foundation.shape.CircleShape)
                        .align(Alignment.TopEnd)
                )
            }
        }
    }
}
