package com.example.myapplication.ui.notifications

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.myapplication.data.preferences.SessionManager
import com.example.myapplication.data.remote.network.RetrofitClient
import com.example.myapplication.ui.home.AppHeader
import com.example.myapplication.ui.home.BrandBlue
import com.example.myapplication.ui.home.BrandBlueDark
import com.example.myapplication.ui.home.BrandBlueSoft
import com.example.myapplication.ui.home.BrandBorder
import com.example.myapplication.ui.home.BrandMuted
import com.example.myapplication.ui.home.BrandOrange
import com.example.myapplication.ui.home.BrandSurface
import com.example.myapplication.ui.home.BrandText
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

private enum class RequestStatus(val label: String, val color: Color) {
    Pending("Pendiente", BrandOrange),
    Approved("Aprobada", BrandBlue),
    Rejected("Rechazada", BrandBlueDark),
    Reviewed("Revisada", BrandMuted)
}

private data class RequestItemLine(
    val id: String,
    val product: String,
    val area: String,
    val requested: Int,
    val approved: Int,
    val attended: Int,
    val status: RequestStatus,
    val statusDescription: String,
    val reason: String,
    val stock: Int,
    val registeredAt: String
)

private data class RequestEntry(
    val solicitudId: Int,
    val id: String,
    val requester: String,
    val email: String,
    val title: String,
    val category: String,
    val time: String,
    val justification: String,
    val status: RequestStatus,
    val statusDescription: String,
    val items: List<RequestItemLine>
)

private const val HARDCODED_SOLICITANTE_USER_ID = 14

private fun parseRequestEntries(root: JsonElement?): List<RequestEntry> {
    val dataArray = root
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.getAsJsonArray("data")
        ?: return emptyList()

    return dataArray.mapNotNull { element ->
        if (!element.isJsonObject) return@mapNotNull null
        val obj = element.asJsonObject

        val idSolicitud = obj.get("id_solicitud").asIntOrNull() ?: return@mapNotNull null
        val statusDescription = obj
            .getObjectOrNull("estado")
            ?.get("descripcion")
            .asNonBlankStringOrNull()
            ?: RequestStatus.Reviewed.label
        val status = resolveRequestStatus(statusDescription)
        val requester = obj.get("solicitante").asNonBlankStringOrNull()
            ?: buildStaffName(obj.getObjectOrNull("staff"))
            ?: "Solicitante"

        RequestEntry(
            solicitudId = idSolicitud,
            id = "#$idSolicitud",
            requester = requester,
            email = "--",
            title = "Solicitud #$idSolicitud",
            category = "Solicitud",
            time = obj.get("fecha_registro").asNonBlankStringOrNull() ?: "--",
            justification = obj.get("justificacion").asNonBlankStringOrNull() ?: "--",
            status = status,
            statusDescription = statusDescription,
            items = emptyList()
        )
    }
}

private fun resolveRequestStatus(obj: JsonObject): RequestStatus {
    val statusText = obj
        .getObjectOrNull("estado")
        ?.get("descripcion")
        .asNonBlankStringOrNull()
        ?.lowercase()
        .orEmpty()

    return when {
        "pendiente" in statusText -> RequestStatus.Pending
        "aprob" in statusText || "atencion" in statusText || "atención" in statusText -> RequestStatus.Approved
        "rechaz" in statusText -> RequestStatus.Rejected
        else -> RequestStatus.Reviewed
    }
}

private fun resolveRequestStatus(statusText: String?): RequestStatus {
    val normalized = statusText?.lowercase().orEmpty()
    return when {
        "pendiente" in normalized -> RequestStatus.Pending
        "aprob" in normalized || "atencion" in normalized || "atención" in normalized -> RequestStatus.Approved
        "rechaz" in normalized -> RequestStatus.Rejected
        else -> RequestStatus.Reviewed
    }
}

private fun buildStaffName(staff: JsonObject?): String? {
    if (staff == null) return null
    val firstName = staff.get("firstname").asNonBlankStringOrNull().orEmpty()
    val lastName = staff.get("lastname").asNonBlankStringOrNull().orEmpty()
    return listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { null }
}

private fun JsonElement?.asIntOrNull(): Int? {
    if (this == null || isJsonNull || !isJsonPrimitive) return null
    return runCatching { asInt }.getOrNull()
}

private fun JsonElement?.asIntOrZero(): Int {
    return asIntOrNull() ?: 0
}

private fun JsonElement?.asNonBlankStringOrNull(): String? {
    if (this == null || isJsonNull || !isJsonPrimitive) return null
    return runCatching { asString.trim() }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun JsonObject.getObjectOrNull(key: String): JsonObject? {
    val value = get(key) ?: return null
    return if (value.isJsonObject) value.asJsonObject else null
}

private fun parseRequestDetailEntry(root: JsonElement?, fallback: RequestEntry): RequestEntry? {
    val data = root
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.getObjectOrNull("data")
        ?: return null

    val solicitudObj = data.getObjectOrNull("solicitud")
    val detallesArray = data.getAsJsonArray("detalles")

    val solicitudId = solicitudObj?.get("id_solicitud").asIntOrNull() ?: fallback.solicitudId
    val requester = solicitudObj?.get("solicitante").asNonBlankStringOrNull()
        ?: buildStaffName(solicitudObj?.getObjectOrNull("staff"))
        ?: fallback.requester
    val email = solicitudObj
        ?.getObjectOrNull("staff")
        ?.get("email")
        .asNonBlankStringOrNull()
        ?: fallback.email
    val statusDescription = solicitudObj?.get("estado").asNonBlankStringOrNull()
        ?: fallback.statusDescription
    val status = resolveRequestStatus(statusDescription)

    val items = detallesArray
        ?.mapNotNull { detailElement ->
            if (!detailElement.isJsonObject) return@mapNotNull null
            val detail = detailElement.asJsonObject
            val detailStatusDescription = detail.get("estado").asNonBlankStringOrNull()
                ?: statusDescription
            val detailStatus = resolveRequestStatus(detailStatusDescription)

            RequestItemLine(
                id = "#${detail.get("id_detalle_solicitud").asIntOrZero()}",
                product = detail.get("producto").asNonBlankStringOrNull() ?: "Producto",
                area = detail.get("area").asNonBlankStringOrNull() ?: "--",
                requested = detail.get("solicitado").asIntOrZero(),
                approved = detail.get("aprobado").asIntOrZero(),
                attended = detail.get("cantidad_atendida").asIntOrZero(),
                status = detailStatus,
                statusDescription = detailStatusDescription,
                reason = detail.get("motivo").asNonBlankStringOrNull()
                    ?: detail.get("observacion_atencion").asNonBlankStringOrNull()
                    ?: "--",
                stock = detail.get("stock_actual").asIntOrZero(),
                registeredAt = detail.get("fecha_registro").asNonBlankStringOrNull() ?: "--"
            )
        }
        .orEmpty()

    return fallback.copy(
        solicitudId = solicitudId,
        id = "#$solicitudId",
        requester = requester,
        email = email,
        title = "Solicitud #$solicitudId",
        category = "Solicitud",
        time = solicitudObj?.get("fecha_registro").asNonBlankStringOrNull() ?: fallback.time,
        justification = solicitudObj?.get("justificacion").asNonBlankStringOrNull() ?: fallback.justification,
        status = status,
        statusDescription = statusDescription,
        items = items
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onAddRequestClick: () -> Unit = {}
) {
    var requests by remember { mutableStateOf<List<RequestEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isDetailLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var detailEntry by remember { mutableStateOf<RequestEntry?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun openDetail(entry: RequestEntry) {
        scope.launch {
            if (isDetailLoading) return@launch
            selectedId = entry.id
            isDetailLoading = true
            try {
                val tokenProvider = { SessionManager.token }
                val api = RetrofitClient.apiWithToken(tokenProvider)
                val response = withContext(Dispatchers.IO) {
                    api.getSolicitudById(entry.solicitudId)
                }

                if (response.isSuccessful) {
                    val detailed = withContext(Dispatchers.Default) {
                        parseRequestDetailEntry(response.body(), entry)
                    }
                    if (detailed != null) {
                        detailEntry = detailed
                    } else {
                        detailEntry = entry
                        snackbarHostState.showSnackbar("No se pudo procesar el detalle de la solicitud.")
                    }
                } else if (response.code() == 404) {
                    detailEntry = null
                    snackbarHostState.showSnackbar("Solicitud no encontrada.")
                } else {
                    detailEntry = null
                    snackbarHostState.showSnackbar("Error al consultar detalle (${response.code()}).")
                }
            } catch (_: Exception) {
                detailEntry = null
                snackbarHostState.showSnackbar("Sin conexion para consultar detalle.")
            } finally {
                isDetailLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val tokenProvider = { SessionManager.token }
            val api = RetrofitClient.apiWithToken(tokenProvider)
            val response = withContext(Dispatchers.IO) {
                api.getSolicitudes(HARDCODED_SOLICITANTE_USER_ID)
            }

            if (response.isSuccessful) {
                requests = withContext(Dispatchers.Default) {
                    parseRequestEntries(response.body())
                }
                selectedId = requests.firstOrNull()?.id
            } else {
                errorMessage = "No se pudo cargar solicitudes (${response.code()})"
            }
        } catch (_: Exception) {
            errorMessage = "Sin conexion para consultar solicitudes"
        } finally {
            isLoading = false
        }
    }

    val pendingCount = requests.count { it.status == RequestStatus.Pending }
    val approvedCount = requests.count { it.status == RequestStatus.Approved }
    val rejectedCount = requests.count { it.status == RequestStatus.Rejected }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BrandSurface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppHeader(
                title = "Historial",
                subtitle = "Solicitudes y detalle",
                showBackButton = showBackButton,
                onBackClick = {
                    if (showBackButton) {
                        navController.popBackStack()
                    }
                },
                showNotificationButton = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddRequestClick,
                containerColor = BrandBlue,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Nueva solicitud"
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = BrandBlueSoft,
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BrandBorder)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = BrandBlue,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Estados solicitud",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandText
                                )
                                Text(
                                    text = "Resumen rapido",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandMuted
                                )
                            }
                            Text(
                                text = requests.size.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                color = BrandBlueDark,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MetricPill("Pend.", pendingCount, BrandOrange, Modifier.weight(1f))
                            MetricPill("Aprob.", approvedCount, BrandBlue, Modifier.weight(1f))
                            MetricPill("Rech.", rejectedCount, BrandBlueDark, Modifier.weight(1f))
                        }
                    }
                }
            }

            when {
                isLoading -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BrandBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.width(20.dp),
                                    strokeWidth = 2.dp,
                                    color = BrandBlue
                                )
                                Text(
                                    text = "Cargando solicitudes...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BrandText
                                )
                            }
                        }
                    }
                }

                !errorMessage.isNullOrBlank() -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BrandBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No se pudo cargar el historial",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrandText,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandMuted
                                )
                            }
                        }
                    }
                }

                requests.isEmpty() -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, BrandBorder)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Sin solicitudes",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = BrandText,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Aun no hay registros para este usuario.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandMuted
                                )
                            }
                        }
                    }
                }

                else -> {
                    items(requests, key = { it.id }) { entry ->
                        RequestListCard(
                            entry = entry,
                            selected = entry.id == selectedId,
                            onClick = {
                                openDetail(entry)
                            }
                        )
                    }
                }
            }
        }

        detailEntry?.let { entry ->
            ModalBottomSheet(
                onDismissRequest = { detailEntry = null },
                sheetState = sheetState
            ) {
                RequestDetailPanel(
                    entry = entry,
                    onClose = { detailEntry = null }
                )
            }
        }

        if (isDetailLoading) {
            DetailLoadingSwal()
        }
    }
}

@Composable
private fun DetailLoadingSwal() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.22f)
        ) {}

        Surface(
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandBorder),
            shadowElevation = 10.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(28.dp),
                    strokeWidth = 3.dp,
                    color = BrandBlue
                )
                Text(
                    text = "Cargando items",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Estamos consultando el detalle...",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }
        }
    }
}

@Composable
private fun MetricPill(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = Color.White,
        border = BorderStroke(1.dp, color.copy(alpha = 0.18f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = BrandMuted,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = color,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun RequestListCard(
    entry: RequestEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) BrandBlueSoft else Color.White,
        border = BorderStroke(1.dp, if (selected) BrandBlue.copy(alpha = 0.22f) else BrandBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.id,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = entry.requester,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = entry.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }

                StatusChip(
                    status = entry.status,
                    labelOverride = entry.statusDescription
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = entry.time,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
                Text(
                    text = entry.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = "${entry.items.size} items",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )

            androidx.compose.material3.Button(
                onClick = onClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .height(40.dp),
                shape = RoundedCornerShape(14.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Ver items")
            }
        }
    }
}

@Composable
private fun RequestDetailPanel(
    entry: RequestEntry,
    onClose: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detalle de solicitud",
                            style = MaterialTheme.typography.titleMedium,
                            color = BrandText,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Items asociados a la solicitud seleccionada",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandMuted
                        )
                    }
                    StatusChip(
                        status = entry.status,
                        labelOverride = entry.statusDescription
                    )
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = BrandSurface,
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Solicitante",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = entry.requester,
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandText,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = entry.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandMuted
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = BrandSurface,
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Registro",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = entry.time,
                            style = MaterialTheme.typography.titleSmall,
                            color = BrandText,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = BrandSurface,
                    border = BorderStroke(1.dp, BrandBorder)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Justificacion",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = entry.justification,
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandText
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
            }

            items(entry.items, key = { it.id }) { item ->
                RequestItemCard(item = item)
            }

            item {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BrandBorder),
                    colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(contentColor = BrandMuted)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
private fun RequestItemCard(item: RequestItemLine) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = BrandSurface,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = BrandBlue,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = item.product,
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                StatusChip(
                    status = item.status,
                    labelOverride = item.statusDescription
                )
            }

            Text(
                text = item.area,
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallMetric("Solic.", item.requested.toString(), Modifier.weight(1f))
                SmallMetric("Aprob.", item.approved.toString(), Modifier.weight(1f))
                SmallMetric("Atend.", item.attended.toString(), Modifier.weight(1f))
            }

            Text(
                text = item.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = BrandText
            )
            Text(
                text = item.registeredAt,
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )
        }
    }
}

@Composable
private fun SmallMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = BrandMuted,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                color = BrandText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun StatusChip(status: RequestStatus, labelOverride: String? = null) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = status.color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, status.color.copy(alpha = 0.20f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when (status) {
                    RequestStatus.Pending -> Icons.Default.Info
                    RequestStatus.Approved -> Icons.Default.CheckCircle
                    RequestStatus.Rejected -> Icons.Default.Close
                    RequestStatus.Reviewed -> Icons.Default.Info
                },
                contentDescription = null,
                tint = status.color
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelOverride?.takeIf { it.isNotBlank() } ?: status.label,
                style = MaterialTheme.typography.labelSmall,
                color = status.color,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
