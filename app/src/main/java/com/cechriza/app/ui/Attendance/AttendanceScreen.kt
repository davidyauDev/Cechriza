package com.cechriza.app.ui.Attendance

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
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
import androidx.core.net.toUri
import com.cechriza.app.data.local.entity.Attendance
import com.cechriza.app.data.local.entity.AttendanceType
import com.cechriza.app.data.remote.dto.response.AttendanceReportEmployeeItem
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBlueSoft
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandOrange
import com.cechriza.app.ui.home.BrandOrangeSoft
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class RegistroAsistencia(
    val id: String,
    val tipoLabel: String,
    val isEntrada: Boolean,
    val hora: String,
    val ubicacion: String,
    val fecha: String,
    val dayKey: String,
    val mapUrl: String?,
    val imageUrl: String?,
    val horario: String?,
    val empleado: String,
    val dni: String?,
    val departamento: String?,
    val empresa: String?,
    val tecnico: Boolean,
    val isPendingSync: Boolean = false,
    val sortTimestamp: Long = 0L
)

private enum class DayFilter { ALL, CUSTOM }

@Composable
fun AttendanceScreen(
    attendanceViewModel: AttendanceViewModel,
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {}
) {
    val reportState by attendanceViewModel.reportUiState.collectAsState()

    val todayCalendar = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val initialApiDate = remember { apiDateFormat().format(todayCalendar.time) }
    val initialLabelDate = remember { displayDateFormat().format(todayCalendar.time) }

    var selectedFilter by remember { mutableStateOf(DayFilter.CUSTOM) }
    var selectedApiDate by remember { mutableStateOf(initialApiDate) }
    var selectedDateLabel by remember { mutableStateOf(initialLabelDate) }
    val selectedRange = remember(selectedApiDate) { dayRangeMillis(selectedApiDate) }

    val localAttendances by when (selectedFilter) {
        DayFilter.ALL -> attendanceViewModel.getAllAttendances().observeAsState(initial = emptyList())
        DayFilter.CUSTOM -> attendanceViewModel
            .getAttendancesBetween(selectedRange.first, selectedRange.second)
            .observeAsState(initial = emptyList())
    }

    LaunchedEffect(selectedFilter, selectedApiDate) {
        when (selectedFilter) {
            DayFilter.ALL -> attendanceViewModel.loadAttendanceReport()
            DayFilter.CUSTOM -> attendanceViewModel.loadAttendanceReport(dates = listOf(selectedApiDate))
        }
    }

    val context = LocalContext.current

    fun openDatePicker() {
        val current = parseApiDate(selectedApiDate) ?: Date()
        val c = Calendar.getInstance().apply { time = current }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                }.time
                selectedApiDate = apiDateFormat().format(picked)
                selectedDateLabel = displayDateFormat().format(picked)
                selectedFilter = DayFilter.CUSTOM
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val registrosUi = remember(reportState.records, localAttendances) {
        val remote = reportState.records.map { it.toRegistroUi() }
        val localPending = localAttendances
            .asSequence()
            .filter { !it.synced }
            .map { it.toPendingRegistroUi() }
            .toList()
        mergeRegistros(remote, localPending)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "$headerText · ${registrosUi.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { openDatePicker() },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Calendario",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = selectedDateLabel.ifBlank { "Elegir fecha" })
                            }

                            FilterChip(
                                selected = selectedFilter == DayFilter.ALL,
                                onClick = {
                                    selectedFilter = if (selectedFilter == DayFilter.ALL) {
                                        DayFilter.CUSTOM
                                    } else {
                                        DayFilter.ALL
                                    }
                                },
                                label = { Text(if (selectedFilter == DayFilter.ALL) "Ver por fecha" else "Todas") }
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    when {
                        reportState.isLoading && registrosUi.isEmpty() -> {
                            LoadingSkeletonList()
                        }

                        !reportState.errorMessage.isNullOrBlank() && registrosUi.isEmpty() -> {
                            ErrorCard(
                                message = reportState.errorMessage.orEmpty(),
                                onRetry = {
                                    when (selectedFilter) {
                                        DayFilter.ALL -> attendanceViewModel.loadAttendanceReport()
                                        DayFilter.CUSTOM -> attendanceViewModel.loadAttendanceReport(dates = listOf(selectedApiDate))
                                    }
                                }
                            )
                        }

                        registrosUi.isEmpty() -> {
                            MessageCard(
                                title = "No hay registros",
                                message = "Cuando existan marcaciones aparecerán aquí."
                            )
                        }

                        else -> {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(bottom = 18.dp)
                            ) {
                                if (!reportState.errorMessage.isNullOrBlank()) {
                                    item {
                                        InlineWarningCard(reportState.errorMessage.orEmpty())
                                    }
                                }
                                groupedForLazy(registrosUi)
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
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent
            ) {
                Text(
                    text = fecha,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        items(list, key = { it.id }) { registro ->
            RegistroAsistenciaCard(registro)
        }
    }
}

@Composable
private fun LoadingSkeletonList(items: Int = 4) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(items) {
            SkeletonAttendanceCard()
        }
    }
}

@Composable
private fun SkeletonAttendanceCard() {
    val transition = rememberInfiniteTransition(label = "attendance-skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "attendance-skeleton-alpha"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .alpha(pulse)
                        .background(Color(0xFFE2E8F0), CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(12.dp)
                            .alpha(pulse)
                            .background(Color(0xFFE5E7EB), RoundedCornerShape(999.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(10.dp)
                            .alpha(pulse)
                            .background(Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(10.dp)
                    .alpha(pulse)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .alpha(pulse)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(10.dp)
                    .alpha(pulse)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
fun RegistroAsistenciaCard(registro: RegistroAsistencia) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val accent = if (registro.isEntrada) BrandBlue else BrandOrange
    val accentSoft = if (registro.isEntrada) {
        BrandBlueSoft.copy(alpha = 0.6f)
    } else {
        BrandOrangeSoft.copy(alpha = 0.6f)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(accentSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (registro.isEntrada) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = accent
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = registro.tipoLabel,
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
                    color = if (registro.isPendingSync) Color(0xFFFFF7ED) else Color(0xFFEEF4FF)
                ) {
                    Text(
                        text = if (registro.isPendingSync) "Pendiente sync" else if (registro.tecnico) "Tecnico" else "Empleado",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = if (registro.isPendingSync) Color(0xFFB45309) else BrandBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = registro.empleado,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandText,
                fontWeight = FontWeight.Medium
            )

            val detailLine = buildList {
                registro.dni?.takeIf { it.isNotBlank() }?.let { add("DNI: $it") }
                registro.departamento?.takeIf { it.isNotBlank() }?.let { add(it) }
                registro.empresa?.takeIf { it.isNotBlank() }?.let { add(it) }
            }.joinToString(" · ")

            if (detailLine.isNotBlank()) {
                Text(
                    text = detailLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            registro.horario?.takeIf { it.isNotBlank() }?.let { horario ->
                Text(
                    text = "Horario programado: $horario",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            Text(
                text = registro.ubicacion,
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            HorizontalDivider(color = BrandBorder.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                registro.mapUrl?.takeIf { it.isNotBlank() }?.let { mapUrl ->
                    ActionChip(
                        label = "Mapa",
                        icon = Icons.Default.LocationOn,
                        iconTint = BrandBlue,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, mapUrl.toUri()))
                        }
                    )
                }

                registro.imageUrl?.takeIf { it.isNotBlank() }?.let { imageUrl ->
                    ActionChip(
                        label = "Foto",
                        icon = Icons.Default.DateRange,
                        iconTint = BrandOrange,
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(imageUrl)))
                        }
                    )
                }

                ActionChip(
                    label = "Copiar",
                    icon = Icons.Default.ContentCopy,
                    iconTint = BrandMuted,
                    onClick = {
                        clipboard.setText(AnnotatedString(registro.ubicacion))
                        Toast.makeText(context, "Ubicacion copiada", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
private fun ActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = BrandSurface,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
            Text(label, style = MaterialTheme.typography.labelMedium, color = BrandText)
        }
    }
}

@Composable
private fun MessageCard(title: String, message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = BrandText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String, onRetry: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "No se pudo cargar la asistencia",
                style = MaterialTheme.typography.titleMedium,
                color = BrandText,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                textAlign = TextAlign.Center
            )
            OutlinedButton(onClick = onRetry) {
                Text("Reintentar")
            }
        }
    }
}

@Composable
private fun InlineWarningCard(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, Color(0xFFFDE68A))
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF92400E)
        )
    }
}

private fun mergeRegistros(
    remote: List<RegistroAsistencia>,
    localPending: List<RegistroAsistencia>
): List<RegistroAsistencia> {
    val keys = remote.map { dedupeKey(it) }.toHashSet()
    val merged = buildList {
        addAll(remote)
        localPending.forEach { local ->
            if (dedupeKey(local) !in keys) add(local)
        }
    }
    return merged.sortedByDescending { it.sortTimestamp }
}

private fun dedupeKey(item: RegistroAsistencia): String {
    return "${item.dayKey}|${item.hora}|${item.tipoLabel.lowercase()}|${item.ubicacion.trim().lowercase()}"
}

private fun AttendanceReportEmployeeItem.toRegistroUi(): RegistroAsistencia {
    val rawDate = fechaHoraMarcacion ?: fecha.orEmpty()
    val parsedDate = parseRemoteDate(rawDate)
    val displayFecha = parsedDate?.let { displayDateFormat().format(it) } ?: (fecha ?: "-")
    val dayKey = parsedDate?.let { dayKeyFormat().format(it) } ?: (fecha ?: idMarcacion.toString())
    val displayHora = horaMarcacion
        ?.takeIf { it.isNotBlank() }
        ?: parsedDate?.let { hourFormat().format(it) }
        ?: "-"
    val firstName = nombres.orEmpty().trim()
    val lastName = apellidos.orEmpty().trim()
    val fullName = listOf(firstName, lastName).filter { it.isNotBlank() }.joinToString(" ")
    val typeCode = tipoMarcacion.orEmpty().trim()
    val isEntrada = typeCode == "0"

    return RegistroAsistencia(
        id = (idMarcacion ?: 0).toString() + "-" + rawDate,
        tipoLabel = when (typeCode) {
            "0" -> "Entrada"
            "1" -> "Salida"
            else -> "Marcacion"
        },
        isEntrada = isEntrada,
        hora = displayHora,
        ubicacion = ubicacion?.takeIf { it.isNotBlank() } ?: "Sin ubicacion registrada",
        fecha = displayFecha,
        dayKey = dayKey,
        mapUrl = mapUrl?.takeIf { it.isNotBlank() },
        imageUrl = imagen?.takeIf { it.isNotBlank() },
        horario = horario?.takeIf { it.isNotBlank() },
        empleado = fullName.ifBlank { "Empleado #${empleadoId ?: "-"}" },
        dni = dni?.takeIf { it.isNotBlank() },
        departamento = departamento?.takeIf { it.isNotBlank() },
        empresa = empresa?.takeIf { it.isNotBlank() },
        tecnico = tecnico == true,
        isPendingSync = false,
        sortTimestamp = parsedDate?.time ?: 0L
    )
}

private fun Attendance.toPendingRegistroUi(): RegistroAsistencia {
    val date = Date(timestamp)
    val isEntrada = type == AttendanceType.ENTRADA
    return RegistroAsistencia(
        id = "local-$id-$timestamp",
        tipoLabel = if (isEntrada) "Entrada" else "Salida",
        isEntrada = isEntrada,
        hora = hourFormat().format(date),
        ubicacion = address?.takeIf { it.isNotBlank() } ?: "Lat: $latitude, Lon: $longitude",
        fecha = displayDateFormat().format(date),
        dayKey = dayKeyFormat().format(date),
        mapUrl = "https://www.google.com/maps?q=$latitude,$longitude",
        imageUrl = null,
        horario = null,
        empleado = "Registro local",
        dni = null,
        departamento = null,
        empresa = null,
        tecnico = true,
        isPendingSync = true,
        sortTimestamp = timestamp
    )
}

private fun parseRemoteDate(value: String?): Date? {
    if (value.isNullOrBlank()) return null
    val patterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )
    for (pattern in patterns) {
        try {
            return SimpleDateFormat(pattern, Locale.US).apply {
                isLenient = false
            }.parse(value)
        } catch (_: Exception) {
        }
    }
    return null
}

private fun parseApiDate(value: String): Date? {
    return try {
        apiDateFormat().parse(value)
    } catch (_: Exception) {
        null
    }
}

private fun dayRangeMillis(apiDate: String): Pair<Long, Long> {
    val date = parseApiDate(apiDate) ?: Date()
    val startCal = Calendar.getInstance().apply {
        time = date
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val start = startCal.timeInMillis
    val end = start + 86_400_000L - 1L
    return start to end
}

private fun apiDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.US)

private fun displayDateFormat() = SimpleDateFormat("dd MMM yyyy", Locale("es"))

private fun dayKeyFormat() = SimpleDateFormat("yyyyMMdd", Locale.US)

private fun hourFormat() = SimpleDateFormat("HH:mm", Locale.getDefault())
