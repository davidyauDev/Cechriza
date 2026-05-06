package com.cechriza.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cechriza.app.ui.Attendance.AttendanceViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LastMarkText(viewModel: AttendanceViewModel) {
    val lastAttendanceState = viewModel.getLastAttendance().observeAsState()
    val lastAttendance = lastAttendanceState.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Ultima marca",
            style = MaterialTheme.typography.titleMedium.copy(
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            ),
            color = BrandText
        )

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f)),
            tonalElevation = 0.dp,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (lastAttendance != null) {
                val date = Date(lastAttendance.timestamp)
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                val formattedDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                val type = lastAttendance.type.name.uppercase()

                val (accent, label, icon) = when (type) {
                    "ENTRADA" -> Triple(BrandBlue, "Entrada", Icons.Default.KeyboardArrowUp)
                    "SALIDA" -> Triple(BrandOrange, "Salida", Icons.Default.KeyboardArrowDown)
                    else -> Triple(
                        BrandMuted,
                        type.lowercase().replaceFirstChar { it.uppercase() },
                        Icons.Default.KeyboardArrowDown
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(accent.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Hoy · $label",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = BrandText
                            )
                            Text(
                                text = formattedDate,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                        }

                        Text(
                            text = formattedTime,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandText
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Circle,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Registro completado correctamente",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Sin marcaciones todavia",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tu ultima asistencia aparecera aqui cuando registres entrada o salida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
            }
        }
    }
}
