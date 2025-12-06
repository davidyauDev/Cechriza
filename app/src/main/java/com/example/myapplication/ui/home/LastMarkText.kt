package com.example.myapplication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.Attendance.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LastMarkText(viewModel: AttendanceViewModel) {
    val lastAttendanceState = viewModel.getLastAttendance().observeAsState()
    val lastAttendance = lastAttendanceState.value

    Column(modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        Text(
            text = "Última marca",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 4.dp)
        )

        Card(
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FB)),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (lastAttendance != null) {
                val date = Date(lastAttendance.timestamp)
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                val formattedDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                val type = lastAttendance.type.name

                val (color, icon, label) = when (type.uppercase()) {
                    "ENTRADA" -> Triple(Color(0xFF2E7D32), "↑", "ENTRADA")
                    "SALIDA" -> Triple(Color(0xFFC62828), "↓", "SALIDA")
                    else -> Triple(Color.Gray, "●", type.uppercase())
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(color.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = icon,
                            color = color,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = color
                        )
                        Text(
                            text = formattedDate,
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }

                    Text(
                        text = formattedTime,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No se ha registrado ninguna asistencia.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
