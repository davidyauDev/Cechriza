package com.cechriza.app.ui.Attendance

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri
import com.cechriza.app.data.local.entity.AttendanceType
import com.cechriza.app.ui.home.AppHeader
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBlueSoft
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandOrange
import com.cechriza.app.ui.home.BrandOrangeSoft
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import com.cechriza.app.ui.home.RoundedTopContainer
import com.cechriza.app.ui.user.UserViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RegistroAsistencia(
    val tipo: AttendanceType,
    val hora: String,
    val ubicacion: String,
    val fecha: String,
    val dayKey: String,
    val lat: Double,
    val lon: Double,
    val synced: Boolean
)

private enum class DayFilter { ALL, CUSTOM }

@Composable
fun AttendanceScreen(
    attendanceViewModel: AttendanceViewModel,
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val userViewModel: UserViewModel = viewModel()
    val userName by userViewModel.userName.collectAsState()
    val fullName = remember(userName) {
        userName.trim().takeIf { it.isNotBlank() } ?: "Usuario"
    }

    val todayRange = remember {
        val cal = Calendar.getInstance()
        cal.time = Date()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        val end = cal.timeInMillis - 1
        Pair(start, end)
    }

    var selectedFilter by remember { mutableStateOf(DayFilter.CUSTOM) }
    var customRange by remember { mutableStateOf<Pair<Long, Long>?>(todayRange) }
    var selectedDateLabel by remember {
        mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale("es")).format(Date(todayRange.first)))
    }

    fun rangeForFilter(filter: DayFilter): Pair<Long, Long>? {
        return when (filter) {
            DayFilter.ALL -> null
            DayFilter.CUSTOM -> customRange
        }
    }

    val selectedRange = rangeForFilter(selectedFilter)
    val attendanceListLiveData = selectedRange?.let { (start, end) ->
        attendanceViewModel.getAttendancesBetween(start, end)
    } ?: attendanceViewModel.getAllAttendances()
    val allAttendances by attendanceListLiveData.observeAsState(initial = emptyList())

    val context = LocalContext.current
    val sdfDate = SimpleDateFormat("dd MMM yyyy", Locale("es"))
    val sdfDayKey = SimpleDateFormat("yyyyMMdd", Locale.US)
    val sdfTime = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun openDatePicker() {
        val c = Calendar.getInstance()
        val dialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val cal = Calendar.getInstance()
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 1)
                val end = cal.timeInMillis - 1
                customRange = Pair(start, end)
                selectedDateLabel = sdfDate.format(Date(start))
                selectedFilter = DayFilter.CUSTOM
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        )
        dialog.show()
    }

    val registrosUi = allAttendances
        .sortedByDescending { it.timestamp }
        .map { attendance ->
            val date = Date(attendance.timestamp)
            RegistroAsistencia(
                tipo = attendance.type,
                hora = sdfTime.format(date),
                ubicacion = attendance.address?.takeIf { it.isNotBlank() }
                    ?: "Lat: ${attendance.latitude}, Lon: ${attendance.longitude}",
                fecha = sdfDate.format(date),
                dayKey = sdfDayKey.format(date),
                lat = attendance.latitude,
                lon = attendance.longitude,
                synced = attendance.synced
            )
        }

    val headerText = when (selectedFilter) {
        DayFilter.ALL -> "Todas las asistencias"
        DayFilter.CUSTOM -> "Asistencias del $selectedDateLabel"
    }

    Scaffold { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BrandSurface)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                AppHeader(
                    title = "Asistencias",
                    subtitle = fullName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .zIndex(1f),
                    showBackButton = true,
                    showNotificationButton = true,
                    onBackClick = onHomeClick,
                    onNotificationClick = onNotificationsClick
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    RoundedTopContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BrandBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Filtro",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BrandMuted
                                            )
                                            Text(
                                                text = selectedDateLabel.ifBlank { "Todo el historial" },
                                                style = MaterialTheme.typography.titleMedium,
                                                color = BrandText,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        FilterChip(
                                            selected = selectedFilter == DayFilter.ALL,
                                            onClick = {
                                                if (selectedFilter == DayFilter.ALL) {
                                                    selectedFilter = DayFilter.CUSTOM
                                                    customRange = todayRange
                                                    selectedDateLabel = sdfDate.format(Date(todayRange.first))
                                                } else {
                                                    selectedFilter = DayFilter.ALL
                                                    customRange = null
                                                    selectedDateLabel = ""
                                                }
                                            },
                                            label = { Text(if (selectedFilter == DayFilter.ALL) "Ver hoy" else "Todas") }
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { openDatePicker() },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = "Calendario",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(text = selectedDateLabel.ifBlank { "Elegir fecha" })
                                        }

                                        if (selectedFilter == DayFilter.CUSTOM) {
                                            TextButton(onClick = {
                                                customRange = todayRange
                                                selectedDateLabel = sdfDate.format(Date(todayRange.first))
                                                selectedFilter = DayFilter.CUSTOM
                                            }) {
                                                Text("Hoy")
                                            }
                                        }
                                    }
                                }
                            }

                            Text(
                                text = "$headerText (${registrosUi.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = BrandText,
                                fontWeight = FontWeight.SemiBold
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                if (registrosUi.isEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, BrandBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "No hay registros",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = BrandText,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Cuando existan marcaciones, apareceran aqui con el mismo estilo del home.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BrandMuted,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 20.dp)
                                    ) {
                                        groupedForLazy(registrosUi)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun LazyListScope.groupedForLazy(items: List<RegistroAsistencia>) {
    val grouped = items.groupBy { it.dayKey }.toSortedMap(compareByDescending { it })

    grouped.forEach { (_, list) ->
        val fecha = list.first().fecha

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = BrandSurface,
                border = BorderStroke(1.dp, BrandBorder)
            ) {
                Text(
                    text = fecha,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(list) { registro ->
            RegistroAsistenciaCard(registro)
        }
    }
}

@Composable
fun RegistroAsistenciaCard(registro: RegistroAsistencia) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val isEntrada = registro.tipo == AttendanceType.ENTRADA
    val accent = if (isEntrada) BrandBlue else BrandOrange
    val accentSoft = if (isEntrada) BrandBlueSoft else BrandOrangeSoft
    val label = if (isEntrada) "Entrada" else "Salida"
    val statusLabel = if (registro.synced) "Sincronizado" else "Pendiente"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(accentSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEntrada) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accent
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${registro.fecha} - ${registro.hora}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentSoft,
                    border = BorderStroke(1.dp, accentSoft.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = registro.ubicacion,
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = BrandBorder.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    val uri = "geo:${registro.lat},${registro.lon}?q=${registro.lat},${registro.lon}(${Uri.encode(registro.ubicacion)})".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Abrir mapa",
                        tint = BrandBlue
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Mapa")
                }

                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(registro.ubicacion))
                    Toast.makeText(context, "Ubicacion copiada", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copiar")
                }
            }
        }
    }
}
