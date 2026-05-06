package com.cechriza.app.ui.solicitudes.list.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBlueDark
import com.cechriza.app.ui.home.BrandBlueSoft
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandOrange
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import com.cechriza.app.ui.solicitudes.list.ComprobanteEntry
import com.cechriza.app.ui.solicitudes.list.ComprobanteSource
import com.cechriza.app.ui.solicitudes.list.HistoryMode
import com.cechriza.app.ui.solicitudes.list.RequestEntry
import com.cechriza.app.ui.solicitudes.list.RequestItemLine
import com.cechriza.app.ui.solicitudes.list.RequestStartOption
import com.cechriza.app.ui.solicitudes.list.RequestStatus
import com.cechriza.app.ui.solicitudes.list.StatusChipStyle
import java.text.SimpleDateFormat
import java.util.Locale

private val SegmentedContainerColor = Color(0xFFEEF1F5)
private val SegmentedContainerBorder = Color(0xFFDCE2EA)
private val SegmentedActiveColor = Color.White
private val SegmentedPressedOverlay = Color(0x0F101828)
private val SegmentedInactiveText = Color(0xFF667085)
private val SegmentedActiveText = Color(0xFF0F172A)

internal fun statusChipStyle(status: RequestStatus): StatusChipStyle {
    return when (status) {
        RequestStatus.Pending -> StatusChipStyle(Color(0xFFFFF7E8), Color(0xFFF9D58A), Color(0xFF9A6700))
        RequestStatus.Approved -> StatusChipStyle(Color(0xFFECFDF3), Color(0xFFABEFC6), Color(0xFF067647))
        RequestStatus.Rejected -> StatusChipStyle(Color(0xFFFEF3F2), Color(0xFFFDA29B), Color(0xFFB42318))
        RequestStatus.Reviewed -> StatusChipStyle(Color(0xFFF4F6FA), Color(0xFFD0D5DD), Color(0xFF344054))
    }
}

internal fun comprobanteStatusChipStyle(statusCode: String?, statusLabel: String): StatusChipStyle {
    val code = statusCode?.trim()?.lowercase().orEmpty()
    val label = statusLabel.trim().lowercase()
    return when {
        code == "pendiente_rrhh" || ("pendiente" in label && "rrhh" in label) -> StatusChipStyle(Color(0xFFEEF4FF), Color(0xFFB2CCFF), Color(0xFF1D4ED8))
        "pendiente" in code || "pendiente" in label -> StatusChipStyle(Color(0xFFFFF7E8), Color(0xFFF9D58A), Color(0xFF9A6700))
        "aprob" in code || "aprob" in label -> StatusChipStyle(Color(0xFFECFDF3), Color(0xFFABEFC6), Color(0xFF067647))
        "rechaz" in code || "rechaz" in label -> StatusChipStyle(Color(0xFFFEF3F2), Color(0xFFFDA29B), Color(0xFFB42318))
        else -> StatusChipStyle(Color(0xFFF4F6FA), Color(0xFFD0D5DD), Color(0xFF344054))
    }
}

private fun formatLatamDateTime(value: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return "-"

    val inputPatterns = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd"
    )

    val output = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale("es", "PE"))
    val outputDateOnly = SimpleDateFormat("dd/MM/yyyy", Locale("es", "PE"))
    for (pattern in inputPatterns) {
        try {
            val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
            val parsed = parser.parse(raw) ?: continue
            return if (pattern == "yyyy-MM-dd") outputDateOnly.format(parsed) else output.format(parsed)
        } catch (_: Exception) {
        }
    }
    return raw
}

@Composable
internal fun SolicitudModeTabs(mode: HistoryMode, onChange: (HistoryMode) -> Unit) {
    SegmentedTabs(
        options = HistoryMode.values().toList(),
        selectedOption = mode,
        labelFor = { it.label },
        onChange = onChange
    )
}

@Composable
internal fun ComprobanteSourceTabs(source: ComprobanteSource, onChange: (ComprobanteSource) -> Unit) {
    SegmentedTabs(
        options = ComprobanteSource.values().toList(),
        selectedOption = source,
        labelFor = { it.label },
        onChange = onChange
    )
}

@Composable
internal fun ReloadRow(onReload: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SegmentedContainerBorder)
        ) {
            IconButton(
                onClick = onReload,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Recargar",
                    tint = SegmentedInactiveText
                )
            }
        }
    }
}

@Composable
private fun <T> SegmentedTabs(
    options: List<T>,
    selectedOption: T,
    labelFor: (T) -> String,
    onChange: (T) -> Unit,
    enabled: Boolean = true
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = SegmentedContainerColor,
        border = BorderStroke(1.dp, SegmentedContainerBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                val isActive = option == selectedOption
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()

                Surface(
                    onClick = { if (enabled) onChange(option) },
                    enabled = enabled,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        !enabled -> Color.Transparent
                        isPressed -> SegmentedPressedOverlay
                        isActive -> SegmentedActiveColor
                        else -> Color.Transparent
                    },
                    border = null,
                    shadowElevation = if (isActive) 1.dp else 0.dp,
                    interactionSource = interactionSource
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = labelFor(option),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Medium,
                            color = when {
                                !enabled -> SegmentedInactiveText.copy(alpha = 0.45f)
                                isActive -> SegmentedActiveText
                                else -> SegmentedInactiveText
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun LoadingCard(text: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, BrandBorder)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp, color = BrandBlue)
            Text(text = text, style = MaterialTheme.typography.bodyMedium, color = BrandText)
        }
    }
}

@Composable
internal fun MessageCard(title: String, message: String) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, BrandBorder)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleSmall, color = BrandText, fontWeight = FontWeight.SemiBold)
            Text(text = message, style = MaterialTheme.typography.bodySmall, color = BrandMuted)
        }
    }
}

@Composable
internal fun ComprobanteListCard(entry: ComprobanteEntry, onClick: () -> Unit) {
    val chipStyle = comprobanteStatusChipStyle(entry.statusCode, entry.status)
    val firstDetail = entry.details.firstOrNull()
    val extraDetailsCount = (entry.details.size - 1).coerceAtLeast(0)
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, BrandBorder)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = entry.title, style = MaterialTheme.typography.bodyLarge, color = BrandText, fontWeight = FontWeight.Medium)
            if (firstDetail != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Producto: ${firstDetail.producto}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Cant: ${firstDetail.cantidad}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted
                        )
                        if (extraDetailsCount > 0) {
                            Text(
                                text = "+$extraDetailsCount item(s) más",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            entry.seguimientoComentario?.let { Text(text = it, style = MaterialTheme.typography.bodySmall, color = BrandMuted) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = chipStyle.background, border = BorderStroke(1.dp, chipStyle.border)) {
                    Text(text = entry.status, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = chipStyle.foreground, fontWeight = FontWeight.SemiBold)
                }
                Text(text = formatLatamDateTime(entry.date), style = MaterialTheme.typography.bodySmall, color = BrandMuted)
            }
        }
    }
}

@Composable
internal fun RequestListCard(
    entry: RequestEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    val firstItem = entry.items.firstOrNull()
    val extraItemsCount = (entry.items.size - 1).coerceAtLeast(0)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = if (selected) BrandBlueSoft else Color.White,
        border = BorderStroke(
            1.dp,
            if (selected) BrandBlue.copy(alpha = 0.25f) else BrandBorder
        ),
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // HEADER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                }

            }

            // INFO ROW
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatusChip(
                    status = entry.status,
                    labelOverride = entry.statusDescription
                )
                Text(
                    text = formatLatamDateTime(entry.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            if (firstItem != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Producto: ${firstItem.product}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Cant: ${firstItem.requested}  ·  Estado: ${firstItem.statusDescription}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted
                        )
                        if (extraItemsCount > 0) {
                            Text(
                                text = "+$extraItemsCount item(s) más",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

        }
    }
}

@Composable
internal fun ComprobanteDetailPanel(entry: ComprobanteEntry, onClose: () -> Unit, onRegisterComprobanteClick: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, BrandBorder)) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(text = "Detalle de gasto ", style = MaterialTheme.typography.titleMedium, color = BrandText, fontWeight = FontWeight.Bold) }
            item { Text(text = entry.title, style = MaterialTheme.typography.titleSmall, color = BrandText, fontWeight = FontWeight.SemiBold) }
            item { Text(text = "${entry.subtitle.ifBlank { "Sin subtitulo" }}\nFecha: ${formatLatamDateTime(entry.date)}", style = MaterialTheme.typography.bodySmall, color = BrandMuted) }
            item { Text(text = "Items", style = MaterialTheme.typography.titleSmall, color = BrandText, fontWeight = FontWeight.SemiBold) }
            if (entry.details.isEmpty()) item { Text(text = "Sin detalle disponible.", style = MaterialTheme.typography.bodySmall, color = BrandMuted) }
            else items(entry.details, key = { it.id }) { item ->
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "${item.producto}", style = MaterialTheme.typography.bodyMedium, color = BrandText, fontWeight = FontWeight.SemiBold)
                        Text(text = "Cantidad: ${item.cantidad}", style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                        Text(text = "Descripcion: ${item.descripcion}", style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                    }
                }
            }
            if (entry.estadoDetalleId == 9) item {
                Button(onClick = onRegisterComprobanteClick, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)) { Text("Registrar comprobante") }
            }
            item {
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BrandBorder), colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandMuted)) { Text("Cerrar") }
            }
        }
    }
}

@Composable
internal fun RequestDetailPanel(entry: RequestEntry, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, BrandBorder)) {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Detalle de solicitud", style = MaterialTheme.typography.titleMedium, color = BrandText, fontWeight = FontWeight.Bold)
                    }
                    StatusChip(status = entry.status, labelOverride = entry.statusDescription)
                }
            }
            item { Text(text = "Items", style = MaterialTheme.typography.titleSmall, color = BrandText, fontWeight = FontWeight.SemiBold) }
            items(entry.items, key = { it.id }) { item -> RequestItemCard(item = item) }
            item {
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BrandBorder), colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandMuted)) { Text("Cerrar") }
            }
        }
    }
}

@Composable
internal fun RequestTypeDialog(onDismiss: () -> Unit, onSelect: (RequestStartOption) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Nueva solicitud", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "Antes de continuar, selecciona el tipo de solicitud:", style = MaterialTheme.typography.bodyMedium, color = BrandText)
                RequestStartOption.values().forEach { option ->
                    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, BrandBorder)) {
                        Column(modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = option.label, style = MaterialTheme.typography.titleSmall, color = BrandText, fontWeight = FontWeight.SemiBold)
                            Text(text = option.description, style = MaterialTheme.typography.bodySmall, color = BrandMuted)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun RequestItemCard(item: RequestItemLine) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = BrandSurface, border = BorderStroke(1.dp, BrandBorder)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.id, style = MaterialTheme.typography.labelSmall, color = BrandBlue, fontWeight = FontWeight.Bold)
                    Text(text = item.product, style = MaterialTheme.typography.titleSmall, color = BrandText, fontWeight = FontWeight.SemiBold)
                }
                StatusChip(status = item.status, labelOverride = item.statusDescription)
            }
            Text(text = "Cantidad: ${item.requested}", style = MaterialTheme.typography.bodyMedium, color = BrandText)
            Text(text = "Fecha aprobacion: ${item.approvedAt}", style = MaterialTheme.typography.bodySmall, color = BrandMuted)
        }
    }
}

@Composable
private fun StatusChip(status: RequestStatus, labelOverride: String? = null) {
    val style = statusChipStyle(status)
    Surface(shape = RoundedCornerShape(999.dp), color = style.background, border = BorderStroke(1.dp, style.border)) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(text = labelOverride?.takeIf { it.isNotBlank() } ?: status.label, style = MaterialTheme.typography.labelSmall, color = style.foreground, fontWeight = FontWeight.SemiBold)
        }
    }
}
