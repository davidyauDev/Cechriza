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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LastMarkText(viewModel: AttendanceViewModel) {
    val attendancesTodayState = viewModel.getAttendancesOfToday().observeAsState(initial = emptyList())
    val localLastAttendance = attendancesTodayState.value.maxByOrNull { it.timestamp }
    val reportState by viewModel.reportUiState.collectAsState()
    val apiDate = rememberTodayApiDate()

    LaunchedEffect(apiDate) {
        viewModel.loadAttendanceReport(dates = listOf(apiDate))
    }

    val remoteLastMark = reportState.records
        .filter { !it.fechaHoraMarcacion.isNullOrBlank() || !it.horaMarcacion.isNullOrBlank() || !it.fecha.isNullOrBlank() }
        .maxByOrNull {
            parseRemoteAttendanceDate(it.fechaHoraMarcacion ?: "${it.fecha.orEmpty()} ${it.horaMarcacion.orEmpty()}")?.time
                ?: Long.MIN_VALUE
        }

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
            if (remoteLastMark != null || localLastAttendance != null) {
                val parsedRemoteDate = remoteLastMark?.let {
                    parseRemoteAttendanceDate(it.fechaHoraMarcacion)
                        ?: parseRemoteAttendanceDate("${it.fecha.orEmpty()} ${it.horaMarcacion.orEmpty()}")
                }
                val localDate = localLastAttendance?.let { Date(it.timestamp) }
                val date = parsedRemoteDate ?: localDate ?: Date()
                val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
                val formattedDate = SimpleDateFormat("dd MMM", Locale.getDefault()).format(date)
                val type = when {
                    remoteLastMark != null -> when (remoteLastMark.tipoMarcacion?.trim()) {
                        "0" -> "ENTRADA"
                        "1" -> "SALIDA"
                        else -> "ENTRADA"
                    }
                    else -> localLastAttendance?.type?.name?.uppercase().orEmpty()
                }

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
                            text = if (remoteLastMark != null) "Registro del servidor" else "Registro completado correctamente",
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
                        text = "Sin marcaciones hoy",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "La asistencia de hoy aparecera aqui cuando registres entrada o salida.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
            }
        }
    }
}

private fun rememberTodayApiDate(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return formatter.format(Date())
}

private fun parseRemoteAttendanceDate(value: String?): Date? {
    if (value.isNullOrBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd HH:mm",
        "yyyy-MM-dd"
    )
    patterns.forEach { pattern ->
        try {
            return SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(value)
        } catch (_: ParseException) {
        } catch (_: Exception) {
        }
    }
    return null
}
