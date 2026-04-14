package com.example.myapplication.ui.home

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.data.preferences.SessionManager
import com.example.myapplication.data.remote.network.RetrofitClient
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TICKET_BASE_URL = "https://osticket.cechriza.com/system/formulario_ticket?id="

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
fun RoutesScreen(navController: NavHostController, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var routes by remember { mutableStateOf<List<RemoteRoute>>(emptyList()) }
    var totalRutas by remember { mutableStateOf<Int?>(null) }

    val todayLabel = remember {
        SimpleDateFormat("EEEE dd MMM yyyy", Locale("es")).format(Date())
    }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val empCode = SessionManager.empCode ?: ""
            val token = SessionManager.token ?: ""
            val api = RetrofitClient.apiWithToken { token }
            val resp = withContext(Dispatchers.IO) { api.getRutasDia(empCode) }
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
                AppHeader(
                    title = "Rutas del día",
                    subtitle = totalRutas?.let { "$it rutas disponibles" } ?: "Resumen de recorridos",
                    showBackButton = true,
                    onBackClick = { navController.popBackStack("main", false) },
                    showNotificationButton = true,
                    onNotificationClick = { navController.navigate("notifications") }
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
                                text = todayLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            when {
                                isLoading -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, BrandBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            CircularProgressIndicator(color = BrandBlue)
                                            Text(
                                                text = "Cargando rutas...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = BrandText
                                            )
                                        }
                                    }
                                }

                                !errorMessage.isNullOrEmpty() -> {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, BrandBorder)
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
                                        shape = RoundedCornerShape(22.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, BrandBorder)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(20.dp),
                                            horizontalAlignment = Alignment.Start,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Sin rutas para mostrar",
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
    val isOpen = !route.estado.equals("cerrado", ignoreCase = true)
    val accent = if (isOpen) BrandBlue else BrandOrange
    val accentSoft = if (isOpen) BrandBlueSoft else BrandOrangeSoft

    Surface(
        onClick = onOpenTicket,
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = route.number,
                        style = MaterialTheme.typography.titleMedium,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = route.agencia,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentSoft,
                    border = BorderStroke(1.dp, accentSoft.copy(alpha = 0.9f))
                ) {
                    Text(
                        text = route.estado.ifBlank { "Sin estado" },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            HorizontalDivider(color = BrandBorder.copy(alpha = 0.7f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailBadge(
                    badge = "F",
                    text = route.fecha.ifBlank { "Sin fecha" },
                    modifier = Modifier.weight(1f)
                )
                DetailBadge(
                    badge = "H",
                    text = route.hora.ifBlank { "Sin hora" },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DetailBadge(
                    badge = "C",
                    text = route.cliente.ifBlank { "Sin cliente" },
                    modifier = Modifier.weight(1f)
                )
                DetailBadge(
                    badge = "E",
                    text = route.equipo.ifBlank { "Sin equipo" },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = "Serie: ${route.serie.ifBlank { "N/D" }}",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )

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

            Text(
                text = "Toca para abrir el ticket",
                style = MaterialTheme.typography.labelSmall,
                color = BrandBlueDark
            )
        }
    }
}

@Composable
private fun DetailBadge(
    badge: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = BrandSurface,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .background(BrandBlueSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = badge,
                    color = BrandBlueDark,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.labelSmall
                )
            }
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
