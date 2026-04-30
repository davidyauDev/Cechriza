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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TextButton
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

private enum class HistoryMode(val label: String) {
    Historial("ALMACEN"),
    Comprobantes("GASTOS")
}

private enum class ComprobanteSource(val label: String) {
    Gastos("Otros"),
    Rrhh("Botas")
}

private data class ComprobanteEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val seguimientoComentario: String?,
    val estadoDetalleId: Int?,
    val statusCode: String?,
    val status: String,
    val date: String,
    val amount: String?,
    val areaId: Int?,
    val details: List<ComprobanteDetailItem>
)

private data class ComprobanteDetailItem(
    val id: String,
    val producto: String,
    val cantidad: Int,
    val descripcion: String,
    val rutaImagen: String,
    val urlImagen: String
)

private enum class RequestStartOption(
    val label: String,
    val description: String
) {
    Epps(
        "Solicitud Botas de Seguridad",
        "Area encargada SSOMA"
    ),
    Almacen(
        "Solicitud Almacen",
        "Para materiales o stock del almacen de la empresa."
    ),
    Gasto(
        "Solicitud de Gasto",
        "Cuando compras con tu dinero y luego la empresa te hace el reembolso."
    )
}

private data class StatusChipStyle(
    val background: Color,
    val border: Color,
    val foreground: Color
)

private fun statusChipStyle(status: RequestStatus): StatusChipStyle {
    return when (status) {
        RequestStatus.Pending -> StatusChipStyle(
            background = Color(0xFFFFF7E8),
            border = Color(0xFFF9D58A),
            foreground = Color(0xFF9A6700)
        )
        RequestStatus.Approved -> StatusChipStyle(
            background = Color(0xFFECFDF3),
            border = Color(0xFFABEFC6),
            foreground = Color(0xFF067647)
        )
        RequestStatus.Rejected -> StatusChipStyle(
            background = Color(0xFFFEF3F2),
            border = Color(0xFFFDA29B),
            foreground = Color(0xFFB42318)
        )
        RequestStatus.Reviewed -> StatusChipStyle(
            background = Color(0xFFF4F6FA),
            border = Color(0xFFD0D5DD),
            foreground = Color(0xFF344054)
        )
    }
}

private fun comprobanteStatusChipStyle(statusCode: String?, statusLabel: String): StatusChipStyle {
    val code = statusCode?.trim()?.lowercase().orEmpty()
    val label = statusLabel.trim().lowercase()

    return when {
        code == "pendiente_rrhh" || ("pendiente" in label && "rrhh" in label) -> StatusChipStyle(
            background = Color(0xFFEEF4FF),
            border = Color(0xFFB2CCFF),
            foreground = Color(0xFF1D4ED8)
        )
        "pendiente" in code || "pendiente" in label -> StatusChipStyle(
            background = Color(0xFFFFF7E8),
            border = Color(0xFFF9D58A),
            foreground = Color(0xFF9A6700)
        )
        "aprob" in code || "aprob" in label -> StatusChipStyle(
            background = Color(0xFFECFDF3),
            border = Color(0xFFABEFC6),
            foreground = Color(0xFF067647)
        )
        "rechaz" in code || "rechaz" in label -> StatusChipStyle(
            background = Color(0xFFFEF3F2),
            border = Color(0xFFFDA29B),
            foreground = Color(0xFFB42318)
        )
        else -> StatusChipStyle(
            background = Color(0xFFF4F6FA),
            border = Color(0xFFD0D5DD),
            foreground = Color(0xFF344054)
        )
    }
}

private data class RequestItemLine(
    val id: String,
    val product: String,
    val requested: Int,
    val status: RequestStatus,
    val statusDescription: String,
    val approvedAt: String
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

private fun extractTipoSolicitud(obj: JsonObject?): String? {
    if (obj == null) return null
    val tipoSolicitud = obj.get("tipo_solicitud") ?: return null
    if (tipoSolicitud.isJsonNull) return null

    return when {
        tipoSolicitud.isJsonPrimitive -> tipoSolicitud.asNonBlankStringOrNull()
        tipoSolicitud.isJsonObject -> {
            val typeObject = tipoSolicitud.asJsonObject
            typeObject.get("descripcion").asNonBlankStringOrNull()
                ?: typeObject.get("nombre").asNonBlankStringOrNull()
                ?: typeObject.get("tipo").asNonBlankStringOrNull()
        }
        else -> null
    }
}

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
        val tipoSolicitud = extractTipoSolicitud(obj) ?: "Solicitud"
        val detallesArray = obj.getAsJsonArray("detalles")
        val items = detallesArray
            ?.mapNotNull { detailElement ->
                if (!detailElement.isJsonObject) return@mapNotNull null
                val detail = detailElement.asJsonObject
                val detailStatusDescription = detail.get("estado").asNonBlankStringOrNull()
                    ?: statusDescription
                val detailStatus = resolveRequestStatus(detailStatusDescription)
                val approvedAt = detail.get("fecha_atencion").asNonBlankStringOrNull()
                    ?: detail.get("fecha_cierre").asNonBlankStringOrNull()
                    ?: "--"

                RequestItemLine(
                    id = "#${detail.get("id_detalle_solicitud").asIntOrZero()}",
                    product = detail.get("producto").asNonBlankStringOrNull() ?: "Producto",
                    requested = detail.get("solicitado").asIntOrZero(),
                    status = detailStatus,
                    statusDescription = detailStatusDescription,
                    approvedAt = approvedAt
                )
            }
            .orEmpty()

        RequestEntry(
            solicitudId = idSolicitud,
            id = "#$idSolicitud",
            requester = requester,
            email = "--",
            title = "Solicitud #$idSolicitud",
            category = tipoSolicitud,
            time = obj.get("fecha_registro").asNonBlankStringOrNull() ?: "--",
            justification = obj.get("justificacion").asNonBlankStringOrNull() ?: "--",
            status = status,
            statusDescription = statusDescription,
            items = items
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

private fun parseComprobantesEntries(root: JsonElement?): List<ComprobanteEntry> {
    val dataArray = root
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.getAsJsonArray("data")
        ?: return emptyList()

    return dataArray.mapIndexedNotNull { index, item ->
        if (!item.isJsonObject) return@mapIndexedNotNull null
        val obj = item.asJsonObject
        val solicitud = obj.getObjectOrNull("solicitud_gasto")

        val id = solicitud?.get("id").asIntOrNull()
            ?: obj.get("solicitud_gasto_id").asIntOrNull()
            ?: obj.get("id").asIntOrNull()
            ?: (index + 1)

        val motivo = solicitud?.get("motivo").asNonBlankStringOrNull() ?: "Solicitud"
        val solicitante = solicitud?.get("solicitante").asNonBlankStringOrNull().orEmpty()
        val area = solicitud?.get("area").asNonBlankStringOrNull().orEmpty()
        val workflow = obj.get("workflow_state").asNonBlankStringOrNull().orEmpty()
        val subtitle = listOf(solicitante, area, workflow).filter { it.isNotBlank() }.joinToString(" - ")
        val estadoDetalleObj = solicitud?.getObjectOrNull("estado_detalle")
            ?: obj.getObjectOrNull("estado_detalle")
        val estadoDetalleId = estadoDetalleObj?.get("id").asIntOrNull()
        val statusCode = estadoDetalleObj?.get("codigo").asNonBlankStringOrNull()
        val status = estadoDetalleObj?.get("nombre").asNonBlankStringOrNull()
            ?: obj.get("workflow_state").asNonBlankStringOrNull()
            ?: "pendiente"
        val date = solicitud?.get("fecha_solicitud").asNonBlankStringOrNull()
            ?: obj.get("fecha_registro").asNonBlankStringOrNull()
            ?: "--"

        val monto = obj.get("monto_real").asNonBlankStringOrNull()
            ?: obj.get("monto_estimado").asNonBlankStringOrNull()
            ?: solicitud?.get("monto_real").asNonBlankStringOrNull()
            ?: solicitud?.get("monto_estimado").asNonBlankStringOrNull()
        val areaId = solicitud?.get("id_area").asIntOrNull()
            ?: obj.get("id_area").asIntOrNull()
        val detailArray = obj.getAsJsonArray("solicitud_gasto_detalles")
        val seguimientoComentario = obj.getAsJsonArray("seguimientos_solicitud_gasto")
            ?.mapNotNull { seguimiento ->
                if (!seguimiento.isJsonObject) return@mapNotNull null
                seguimiento.asJsonObject.get("comentario").asNonBlankStringOrNull()
            }
            ?.lastOrNull()
        val details = detailArray?.mapIndexedNotNull { detailIndex, detailElement ->
            if (!detailElement.isJsonObject) return@mapIndexedNotNull null
            val detail = detailElement.asJsonObject
            val detailId = detail.get("id").asIntOrNull() ?: (detailIndex + 1)
            ComprobanteDetailItem(
                id = "#$detailId",
                producto = detail.get("producto").asNonBlankStringOrNull()
                    ?: "Producto ${detail.get("id_producto").asIntOrZero()}",
                cantidad = detail.get("cantidad").asIntOrZero(),
                descripcion = detail.get("descripcion_adicional").asNonBlankStringOrNull() ?: "--",
                rutaImagen = detail.get("ruta_imagen").asNonBlankStringOrNull() ?: "--",
                urlImagen = detail.get("url_imagen").asNonBlankStringOrNull() ?: "--"
            )
        }.orEmpty()

        ComprobanteEntry(
            id = "#$id",
            title = motivo,
            subtitle = subtitle,
            seguimientoComentario = seguimientoComentario,
            estadoDetalleId = estadoDetalleId,
            statusCode = statusCode,
            status = status,
            date = date,
            amount = monto?.let { "S/ $it" },
            areaId = areaId,
            details = details
        )
    }
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
    val tipoSolicitud = extractTipoSolicitud(solicitudObj) ?: fallback.category

    val items = detallesArray
        ?.mapNotNull { detailElement ->
            if (!detailElement.isJsonObject) return@mapNotNull null
            val detail = detailElement.asJsonObject
            val detailStatusDescription = detail.get("estado").asNonBlankStringOrNull()
                ?: statusDescription
            val detailStatus = resolveRequestStatus(detailStatusDescription)
            val approvedAt = detail.get("fecha_atencion").asNonBlankStringOrNull()
                ?: detail.get("fecha_cierre").asNonBlankStringOrNull()
                ?: "--"

            RequestItemLine(
                id = "#${detail.get("id_detalle_solicitud").asIntOrZero()}",
                product = detail.get("producto").asNonBlankStringOrNull() ?: "Producto",
                requested = detail.get("solicitado").asIntOrZero(),
                status = detailStatus,
                statusDescription = detailStatusDescription,
                approvedAt = approvedAt
            )
        }
        .orEmpty()

    return fallback.copy(
        solicitudId = solicitudId,
        id = "#$solicitudId",
        requester = requester,
        email = email,
        title = "Solicitud #$solicitudId",
        category = tipoSolicitud,
        time = solicitudObj?.get("fecha_registro").asNonBlankStringOrNull() ?: fallback.time,
        justification = solicitudObj?.get("justificacion").asNonBlankStringOrNull() ?: fallback.justification,
        status = status,
        statusDescription = statusDescription,
        items = items
    )
}

@Composable
private fun ComprobanteListCard(
    entry: ComprobanteEntry,
    onClick: () -> Unit
) {
    val chipStyle = comprobanteStatusChipStyle(entry.statusCode, entry.status)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.id,
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = chipStyle.background,
                    border = BorderStroke(1.dp, chipStyle.border)
                ) {
                    Text(
                        text = entry.status,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = chipStyle.foreground,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = entry.title,
                style = MaterialTheme.typography.bodyLarge,
                color = BrandText,
                fontWeight = FontWeight.Medium
            )

            if (entry.subtitle.isNotBlank()) {
                Text(
                    text = entry.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            entry.seguimientoComentario?.let { comentario ->
                Text(
                    text = comentario,
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            Text(
                text = entry.date,
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )

            Button(
                onClick = onClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .height(38.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Ver detalle")
            }
        }
    }
}

@Composable
private fun ComprobanteDetailPanel(
    entry: ComprobanteEntry,
    onClose: () -> Unit,
    onRegisterComprobanteClick: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Detalle de gasto ${entry.id}",
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandText,
                    fontWeight = FontWeight.Bold
                )
            }
            item {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                Text(
                    text = "${entry.subtitle.ifBlank { "Sin subtitulo" }}\nFecha: ${entry.date}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }
            item {
                Text(
                    text = "Items",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (entry.details.isEmpty()) {
                item {
                    Text(
                        text = "Sin detalle disponible.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
            } else {
                items(entry.details, key = { it.id }) { item ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = BrandSurface,
                        border = BorderStroke(1.dp, BrandBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${item.id} - ${item.producto}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Cantidad: ${item.cantidad}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                            Text(
                                text = "Descripcion: ${item.descripcion}",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                        }
                    }
                }
            }

            if (entry.estadoDetalleId == 9) {
                item {
                    Button(
                        onClick = onRegisterComprobanteClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Registrar comprobante")
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onClose,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, BrandBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandMuted)
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onAddRequestClick: (String) -> Unit = {}
) {
    var mode by remember { mutableStateOf(HistoryMode.Historial) }
    var comprobanteSource by remember { mutableStateOf(ComprobanteSource.Gastos) }
    var comprobantes by remember { mutableStateOf<List<ComprobanteEntry>>(emptyList()) }
    var isLoadingComprobantes by remember { mutableStateOf(false) }
    var comprobantesError by remember { mutableStateOf<String?>(null) }
    var reloadComprobantesTick by remember { mutableStateOf(0) }
    var reloadRequestsTick by remember { mutableStateOf(0) }
    var requests by remember { mutableStateOf<List<RequestEntry>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isDetailLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var detailEntry by remember { mutableStateOf<RequestEntry?>(null) }
    var detailComprobanteEntry by remember { mutableStateOf<ComprobanteEntry?>(null) }
    var showRequestTypeDialog by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun openDetail(entry: RequestEntry) {
        selectedId = entry.id
        detailEntry = entry
    }

    LaunchedEffect(reloadRequestsTick) {
        isLoading = true
        errorMessage = null
        val solicitanteUserId = SessionManager.staffId ?: SessionManager.userId
        if (solicitanteUserId == null || solicitanteUserId <= 0) {
            isLoading = false
            errorMessage = "No se encontro staff_id de sesion. Vuelve a iniciar sesion."
            return@LaunchedEffect
        }
        try {
            val tokenProvider = { SessionManager.token }
            val api = RetrofitClient.apiWithToken(tokenProvider)
            val response = withContext(Dispatchers.IO) {
                api.getSolicitudes(solicitanteUserId)
            }

            if (response.isSuccessful) {
                requests = withContext(Dispatchers.Default) {
                    parseRequestEntries(response.body())
                }
                selectedId = requests.firstOrNull()?.id
                detailEntry = null
            } else {
                errorMessage = "No se pudo cargar solicitudes (${response.code()})"
            }
        } catch (_: Exception) {
            errorMessage = "Sin conexion para consultar solicitudes"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(mode, comprobanteSource, reloadComprobantesTick) {
        if (mode != HistoryMode.Comprobantes) return@LaunchedEffect

        val staffId = SessionManager.staffId ?: SessionManager.userId
        if (staffId == null || staffId <= 0) {
            comprobantes = emptyList()
            comprobantesError = "No se encontro staff_id de sesion. Vuelve a iniciar sesion."
            isLoadingComprobantes = false
            return@LaunchedEffect
        }

        isLoadingComprobantes = true
        comprobantesError = null
        try {
            val api = RetrofitClient.apiWithToken { SessionManager.token }
            val response = withContext(Dispatchers.IO) {
                api.getSolicitudesGastoComprobantes(staffId)
            }
            if (response.isSuccessful) {
                val parsed = withContext(Dispatchers.Default) {
                    parseComprobantesEntries(response.body())
                }
                comprobantes = when (comprobanteSource) {
                    ComprobanteSource.Rrhh -> parsed.filter { it.areaId == 11 }
                    ComprobanteSource.Gastos -> parsed.filter { it.areaId != 11 }
                }
            } else {
                comprobantes = emptyList()
                comprobantesError = "No se pudo cargar ${comprobanteSource.label} (${response.code()})"
            }
        } catch (_: Exception) {
            comprobantes = emptyList()
            comprobantesError = "Sin conexion para consultar ${comprobanteSource.label.lowercase()}"
        } finally {
            isLoadingComprobantes = false
        }
    }

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
                onClick = { showRequestTypeDialog = true },
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryMode.values().forEach { option ->
                        OutlinedButton(
                            onClick = { mode = option },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.dp,
                                if (mode == option) BrandBlue else BrandBorder
                            )
                        ) {
                            Text(
                                text = option.label,
                                color = if (mode == option) BrandBlue else BrandMuted
                            )
                        }
                    }
                }
            }

            if (mode == HistoryMode.Comprobantes) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ComprobanteSource.values().forEach { source ->
                            OutlinedButton(
                                onClick = { comprobanteSource = source },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (comprobanteSource == source) BrandBlue else BrandBorder
                                )
                            ) {
                                Text(
                                    text = source.label,
                                    color = if (comprobanteSource == source) BrandBlue else BrandMuted
                                )
                            }
                        }
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { reloadComprobantesTick += 1 }) {
                            Text("Recargar")
                        }
                    }
                }

                when {
                    isLoadingComprobantes -> {
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
                                        text = "Cargando ${comprobanteSource.label.lowercase()}...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = BrandText
                                    )
                                }
                            }
                        }
                    }
                    !comprobantesError.isNullOrBlank() -> {
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
                                        text = "No se pudo cargar comprobantes",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BrandText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = comprobantesError.orEmpty(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandMuted
                                    )
                                }
                            }
                        }
                    }
                    comprobantes.isEmpty() -> {
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
                                        text = "Sin comprobantes",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = BrandText,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "No hay registros para este usuario.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BrandMuted
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        items(comprobantes, key = { "${it.id}-${it.date}-${it.title}" }) { entry ->
                            ComprobanteListCard(
                                entry = entry,
                                onClick = { detailComprobanteEntry = entry }
                            )
                        }
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(onClick = { reloadRequestsTick += 1 }) {
                            Text("Recargar")
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
        }

        detailEntry?.takeIf { mode == HistoryMode.Historial }?.let { entry ->
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

        detailComprobanteEntry?.takeIf { mode == HistoryMode.Comprobantes }?.let { entry ->
            ModalBottomSheet(
                onDismissRequest = { detailComprobanteEntry = null },
                sheetState = sheetState
            ) {
                ComprobanteDetailPanel(
                    entry = entry,
                    onClose = { detailComprobanteEntry = null },
                    onRegisterComprobanteClick = {
                        detailComprobanteEntry = null
                        val solicitudId = entry.id.filter(Char::isDigit)
                        onAddRequestClick(
                            if (solicitudId.isNotBlank()) "comprobante:$solicitudId" else "comprobante"
                        )
                    }
                )
            }
        }

        if (showRequestTypeDialog) {
            RequestTypeDialog(
                onDismiss = { showRequestTypeDialog = false },
                onSelect = { option ->
                    showRequestTypeDialog = false
                    val preset = when (option) {
                        RequestStartOption.Epps -> "epp"
                        RequestStartOption.Almacen -> "almacen"
                        RequestStartOption.Gasto -> "gasto"
                    }
                    onAddRequestClick(preset)
                }
            )
        }
    }
}

@Composable
private fun RequestTypeDialog(
    onDismiss: () -> Unit,
    onSelect: (RequestStartOption) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva solicitud",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Antes de continuar, selecciona el tipo de solicitud:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandText
                )

                RequestStartOption.values().forEach { option ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BrandBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(option) }
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.titleSmall,
                                color = BrandText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = option.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
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
                    text = "Tipo: ${entry.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandBlueDark,
                    fontWeight = FontWeight.SemiBold
                )
            }

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
                    }
                    StatusChip(
                        status = entry.status,
                        labelOverride = entry.statusDescription
                    )
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
                text = "Cantidad: ${item.requested}",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandText
            )
            Text(
                text = "Fecha aprobacion: ${item.approvedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )
        }
    }
}

@Composable
private fun StatusChip(status: RequestStatus, labelOverride: String? = null) {
    val style = statusChipStyle(status)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = style.background,
        border = BorderStroke(1.dp, style.border)
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
                tint = style.foreground
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = labelOverride?.takeIf { it.isNotBlank() } ?: status.label,
                style = MaterialTheme.typography.labelSmall,
                color = style.foreground,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
