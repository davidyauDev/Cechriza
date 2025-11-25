package com.example.myapplication.ui.home

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.data.preferences.SessionManager
import com.google.gson.JsonElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.platform.LocalContext

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
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var routes by remember { mutableStateOf<List<RemoteRoute>>(emptyList()) }
    var totalRutas by remember { mutableStateOf<Int?>(null) }

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
                            } catch (_: Exception) {}
                        }
                    }

                    // Meta corregido
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

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Rutas del día" + (totalRutas?.let { " ($it)" } ?: ""),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        },
        modifier = modifier
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Cargando rutas...")
                }
                return@Scaffold
            }

            if (!errorMessage.isNullOrEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(errorMessage ?: "Error")
                }
                return@Scaffold
            }

            if (routes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No hay rutas para el día")
                }
                return@Scaffold
            }

            val context = LocalContext.current
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                items(routes) { r ->

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { /* acción futura */ },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(1.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {

                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                        ) {

                            // Número + estado
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = r.number,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.secondaryContainer
                                ) {
                                    Text(
                                        text = r.estado,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Fecha y hora
                            Text(
                                text = "Fecha: ${r.fecha}   Hora: ${r.hora}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(6.dp))

                            // Agencia
                            Text(
                                text = r.agencia,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(Modifier.height(4.dp))

                            // Equipo y serie
                            Text(
                                text = "Equipo: ${r.equipo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Serie: ${r.serie}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(4.dp))

                            // Servicio y cliente
                            Text(
                                text = "Servicio: ${r.topic}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Text(
                                text = "Cliente: ${r.cliente}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(12.dp))

                            // Asunto destacado
                            Text(
                                text = r.subject,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.height(16.dp))

                            // Botón profesional
                            FilledTonalButton(
                                onClick = {
                                    r.ticketId?.let { id ->
                                        val url = "http://161.132.75.22/system/formulario_ticket?id=$id"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                        context.startActivity(intent)
                                    }
                                },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("Ver detalle")
                            }
                        }
                    }
                }
            }


        }
    }
}
