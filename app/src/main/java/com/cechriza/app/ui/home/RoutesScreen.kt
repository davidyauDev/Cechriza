package com.cechriza.app.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.network.RetrofitClient
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val TICKET_BASE_URL = "https://osticket.cechriza.com/system/formulario_ticket?id="

private enum class RouteDayTab(val title: String, val dayOffset: Int) {
    Today("Hoy", 0),
    Tomorrow("Manana", 1)
}

data class RemoteRoute(
    val ticketId: Int?,
    val number: String,
    val fecha: String,
    val hora: String,
    val agencia: String,
    val equipo: String,
    val serie: String,
    val topic: String,
    val estado: String,
    val cliente: String,
    val subject: String
)

@Composable
fun RoutesScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dayTabs = remember { RouteDayTab.values().toList() }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var refreshTrigger by remember { mutableIntStateOf(0) }
    val dayOffset = dayTabs[selectedTabIndex].dayOffset
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var routes by remember { mutableStateOf<List<RemoteRoute>>(emptyList()) }
    var totalRutas by remember { mutableStateOf<Int?>(null) }

    val targetDate = remember(dayOffset) {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, dayOffset) }.time
    }
    val dayLabel = remember(targetDate) {
        SimpleDateFormat("EEEE dd MMM yyyy", Locale("es")).format(targetDate)
    }
    val requestDate = remember(targetDate) {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(targetDate)
    }
    val headerTitle = "Rutas"
    val loadingText = if (dayOffset == 1) "Cargando rutas de manana..." else "Cargando rutas..."
    val emptyText = if (dayOffset == 1) "Sin rutas para manana" else "Sin rutas para mostrar"
    val subtitleText = totalRutas?.let { "$it rutas · ${dayTabs[selectedTabIndex].title}" } ?: "Resumen operativo"

    LaunchedEffect(dayOffset, refreshTrigger) {
        isLoading = true
        errorMessage = null
        routes = emptyList()
        totalRutas = null
        try {
            val empCode = SessionManager.empCode ?: ""
            val token = SessionManager.token ?: ""
            val api = RetrofitClient.apiWithToken { token }
            val resp = withContext(Dispatchers.IO) { api.getRutasDia(empCode, requestDate) }
            if (resp.isSuccessful) {
                val body: JsonElement? = resp.body()
                val parsed = mutableListOf<RemoteRoute>()
                var metaTotal: Int? = null
                if (body != null && body.isJsonObject) {
                    val obj = body.asJsonObject
                    val dataObj = obj.getAsJsonObject("data")
                    val rutasArray = dataObj?.getAsJsonArray("rutas")

                    if (rutasArray != null) {
                        for (el in rutasArray) {
                            try {
                                val o = el.asJsonObject
                                parsed.add(
                                    RemoteRoute(
                                        ticketId = if (o.has("ticket_id")) o.get("ticket_id").asInt else null,
                                        number = o.get("number")?.asString ?: "",
                                        fecha = o.get("fecha_programada_formateada")?.asString ?: "",
                                        hora = o.get("fecha_programada")?.asString?.substringAfter(" ") ?: "",
                                        agencia = o.get("agencia")?.asString ?: "",
                                        equipo = o.get("equipo")?.asString ?: "",
                                        serie = o.get("serie")?.asString ?: "",
                                        topic = o.get("topic")?.asString ?: "",
                                        estado = o.get("estado")?.asString ?: "",
                                        cliente = o.get("cliente")?.asString ?: "",
                                        subject = o.get("subject")?.asString ?: ""
                                    )
                                )
                            } catch (_: Exception) {
                            }
                        }
                    }

                    val metaObj = dataObj?.getAsJsonObject("meta")
                    if (metaObj?.has("total_rutas") == true) {
                        metaTotal = metaObj.get("total_rutas").asInt
                    }
                }

                routes = parsed
                totalRutas = metaTotal ?: parsed.size
                isLoading = false
            } else {
                errorMessage = "Error de red: ${resp.code()}"
                isLoading = false
            }
        } catch (_: Exception) {
            errorMessage = "Error desconocido"
            isLoading = false
        }
    }

    Scaffold { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(BrandSurface)
                .padding(padding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                RoutesTopBar(
                    title = headerTitle,
                    subtitle = subtitleText,
                    onRefresh = { refreshTrigger += 1 },
                    refreshing = isLoading
                )

                Box(modifier = Modifier.fillMaxSize()) {
                    RoundedTopContainer {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = dayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            RoutesSegmentedTabs(
                                tabs = dayTabs,
                                selectedIndex = selectedTabIndex,
                                onSelect = { selectedTabIndex = it }
                            )

                            RouteStatsRow(routes = routes)

                            when {
                                isLoading -> {
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
                                            CircularProgressIndicator(color = BrandBlue)
                                            Text(
                                                text = loadingText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = BrandText
                                            )
                                        }
                                    }
                                }

                                !errorMessage.isNullOrEmpty() -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.Start,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "No fue posible cargar las rutas",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = BrandText,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = errorMessage ?: "Error",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BrandMuted
                                            )
                                        }
                                    }
                                }

                                routes.isEmpty() -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.Start,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = emptyText,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = BrandText,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Prueba más tarde.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = BrandMuted
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    LazyColumn(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(12.dp),
                                        contentPadding = PaddingValues(bottom = 20.dp)
                                    ) {
                                        items(routes) { route ->
                                            RouteCard(
                                                route = route,
                                                onOpenTicket = {
                                                    route.ticketId?.let { id ->
                                                        val intent = Intent(
                                                            Intent.ACTION_VIEW,
                                                            Uri.parse("$TICKET_BASE_URL$id")
                                                        )
                                                        context.startActivity(intent)
                                                    }
                                                }
                                            )
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
}

@Composable
private fun RouteCard(
    route: RemoteRoute,
    onOpenTicket: () -> Unit
) {
    val (statusColor, statusBg) = routeStatusColors(route.estado)

    Surface(
        onClick = onOpenTicket,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.65f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.number,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = route.cliente.ifBlank { route.agencia.ifBlank { "Cliente no disponible" } },
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusBg,
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.25f))
                ) {
                    Text(
                        text = route.estado.ifBlank { "Programado" },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledMetaChip(
                    icon = Icons.Default.CalendarToday,
                    text = route.fecha.ifBlank { "Sin fecha" },
                    modifier = Modifier.weight(1f)
                )
                FilledMetaChip(
                    icon = Icons.Default.Schedule,
                    text = route.hora.ifBlank { "Sin hora" },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = route.topic.ifBlank { "Servicio sin detalle" },
                style = MaterialTheme.typography.bodyMedium,
                color = BrandText,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = route.subject.ifBlank { "Sin asunto" },
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledMetaChip(
                    icon = Icons.Default.LocationOn,
                    text = route.agencia.ifBlank { "Ubicacion no disponible" },
                    modifier = Modifier.weight(1f)
                )
                FilledMetaChip(
                    icon = Icons.Default.Schedule,
                    text = "Serie ${route.serie.ifBlank { "N/D" }}",
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = BrandBorder.copy(alpha = 0.55f))

            Text(
                text = "Abrir ticket",
                style = MaterialTheme.typography.labelMedium,
                color = BrandBlue,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FilledMetaChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = BrandSurface
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = BrandMuted, modifier = Modifier.size(14.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = BrandText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RoutesTopBar(
    title: String,
    subtitle: String,
    onRefresh: () -> Unit,
    refreshing: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.65f)),
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, color = BrandText, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = BrandMuted)
            }
            IconButton(onClick = onRefresh, enabled = !refreshing) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = BrandBlue)
                } else {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refrescar", tint = BrandText)
                }
            }
        }
    }
}

@Composable
private fun RoutesSegmentedTabs(
    tabs: List<RouteDayTab>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFEEF1F5),
        border = BorderStroke(1.dp, Color(0xFFDCE2EA))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            tabs.forEachIndexed { index, tab ->
                val selected = index == selectedIndex
                val interactionSource = remember { MutableInteractionSource() }
                val pressed by interactionSource.collectIsPressedAsState()
                Surface(
                    onClick = { onSelect(index) },
                    interactionSource = interactionSource,
                    modifier = Modifier.weight(1f).height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        selected -> Color.White
                        pressed -> Color(0x0F101828)
                        else -> Color.Transparent
                    },
                    shadowElevation = if (selected) 1.dp else 0.dp
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = tab.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (selected) BrandText else BrandMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RouteStatsRow(routes: List<RemoteRoute>) {
    val pending = routes.count { it.estado.contains("program", ignoreCase = true) || it.estado.contains("pend", ignoreCase = true) }
    val inRoute = routes.count { it.estado.contains("ruta", ignoreCase = true) || it.estado.contains("proceso", ignoreCase = true) }
    val delayed = routes.count { it.estado.contains("retras", ignoreCase = true) }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatsPill("Programadas", pending.toString(), Modifier.weight(1f))
        StatsPill("En ruta", inRoute.toString(), Modifier.weight(1f))
        StatsPill("Retrasadas", delayed.toString(), Modifier.weight(1f))
    }
}

@Composable
private fun StatsPill(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), horizontalAlignment = Alignment.Start) {
            Text(text = value, style = MaterialTheme.typography.titleSmall, color = BrandText, fontWeight = FontWeight.SemiBold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = BrandMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun routeStatusColors(statusRaw: String): Pair<Color, Color> {
    val status = statusRaw.lowercase()
    return when {
        "complet" in status || "cerrad" in status -> Color(0xFF15803D) to Color(0xFFECFDF3)
        "ruta" in status || "proceso" in status -> Color(0xFF1D4ED8) to Color(0xFFEEF4FF)
        "retras" in status -> Color(0xFFB45309) to Color(0xFFFFF7ED)
        "cancel" in status -> Color(0xFFB91C1C) to Color(0xFFFEF2F2)
        else -> Color(0xFF334155) to Color(0xFFF1F5F9)
    }
}
