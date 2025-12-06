package com.example.myapplication.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.local.entity.AttendanceType

@Suppress("DEPRECATION")
@Composable
fun EntryExitButtons(
    onEntry: () -> Unit,
    onExit: () -> Unit,
    isBusy: Boolean = false,
    activeType: AttendanceType = AttendanceType.ENTRADA
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Entrada (verde)
        Button(
            onClick = onEntry,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)), // Verde moderno
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isBusy && activeType == AttendanceType.ENTRADA) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Entrada",
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("ENTRADA", color = Color.White)
            }
        }

        // Salida (rojo)
        OutlinedButton(
            onClick = onExit,
            enabled = !isBusy,
            border = BorderStroke(1.dp, Color(0xFFB71C1C)), // Rojo oscuro
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            if (isBusy && activeType == AttendanceType.SALIDA) {
                CircularProgressIndicator(
                    color = Color(0xFFB71C1C),
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Salida",
                    tint = Color(0xFFB71C1C)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("SALIDA", color = Color(0xFFB71C1C))
            }
        }
    }
}



