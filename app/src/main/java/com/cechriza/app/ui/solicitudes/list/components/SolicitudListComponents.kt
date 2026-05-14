package com.cechriza.app.ui.solicitudes.list.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
import com.cechriza.app.ui.solicitudes.list.CourierTrackingInfo
import com.cechriza.app.ui.solicitudes.list.HistoryMode
import com.cechriza.app.ui.solicitudes.list.RequestEntry
import com.cechriza.app.ui.solicitudes.list.RequestItemLine
import com.cechriza.app.ui.solicitudes.list.RequestStartOption
import com.cechriza.app.ui.solicitudes.list.RequestStatus
import com.cechriza.app.ui.solicitudes.list.SolicitudListViewMode
import com.cechriza.app.ui.solicitudes.list.StatusChipStyle
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SegmentedContainerColor = Color(0xFFEEF1F5)
private val SegmentedContainerBorder = Color(0xFFDCE2EA)
private val SegmentedActiveColor = Color.White
private val SegmentedPressedOverlay = Color(0x0F101828)
private val SegmentedInactiveText = Color(0xFF667085)
private val SegmentedActiveText = Color(0xFF0F172A)
private val LatamLocale = Locale("es", "PE")
private val InputDateTimeFormatters = listOf(
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS")
)
private val InputDateOnlyFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
private val OutputDateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", LatamLocale)
private val OutputDateOnlyFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", LatamLocale)

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
    for (formatter in InputDateTimeFormatters) {
        val formatted = runCatching {
            LocalDateTime.parse(raw, formatter).format(OutputDateTimeFormatter)
        }.getOrNull()
        if (formatted != null) return formatted
    }
    return runCatching {
        LocalDate.parse(raw, InputDateOnlyFormatter).format(OutputDateOnlyFormatter)
    }.getOrElse { raw }
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
internal fun SolicitudListViewTabs(mode: SolicitudListViewMode, onChange: (SolicitudListViewMode) -> Unit) {
    SegmentedTabs(
        options = SolicitudListViewMode.values().toList(),
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
internal fun LoadingSkeletonList(items: Int = 3) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(items) {
            SkeletonCard()
        }
    }
}

@Composable
private fun SkeletonCard() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val pulse by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton-alpha"
    )

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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(14.dp)
                    .alpha(pulse)
                    .background(Color(0xFFE5E7EB), RoundedCornerShape(999.dp))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(12.dp)
                    .alpha(pulse)
                    .background(Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .width(88.dp)
                        .height(22.dp)
                        .alpha(pulse)
                        .background(Color(0xFFE2E8F0), RoundedCornerShape(999.dp))
                )
                Box(
                    modifier = Modifier
                        .width(110.dp)
                        .height(12.dp)
                        .alpha(pulse)
                        .background(Color(0xFFF1F5F9), RoundedCornerShape(999.dp))
                )
            }
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
internal fun ComprobanteListCard(
    entry: ComprobanteEntry,
    onClick: () -> Unit,
    typeBadge: String? = null
) {
    val chipStyle = when (entry.estadoDetalleId) {
        15 -> StatusChipStyle(
            background = Color(0xFFECFDF3),
            border = Color(0xFFABEFC6),
            foreground = Color(0xFF067647)
        )
        7 -> StatusChipStyle(
            background = Color(0xFFFFF4E5),
            border = Color(0xFFF7B267),
            foreground = Color(0xFFB45309)
        )
        9 -> StatusChipStyle(
            background = Color(0xFFEAF2FF),
            border = Color(0xFFB8D2FF),
            foreground = Color(0xFF1D4ED8)
        )
        else -> comprobanteStatusChipStyle(entry.statusCode, entry.status)
    }
    val firstDetail = entry.details.firstOrNull()
    val extraDetailsCount = (entry.details.size - 1).coerceAtLeast(0)
    val prefix = if (entry.areaId == 11) "EPP" else "Compra"
    Surface(modifier = Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(22.dp), color = Color.White, border = BorderStroke(1.dp, BrandBorder)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "$prefix ${entry.id}",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = chipStyle.background,
                    border = BorderStroke(1.25.dp, chipStyle.border)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (entry.estadoDetalleId == 15) {
                            Icon(
                                imageVector = Icons.Default.Payments,
                                contentDescription = null,
                                tint = chipStyle.foreground,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Text(
                            text = entry.status,
                            style = MaterialTheme.typography.labelLarge,
                            color = chipStyle.foreground,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                typeBadge?.takeIf { it.isNotBlank() }?.let { badge ->
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = BrandBlueSoft,
                        border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = badge,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (entry.comprobante != null) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFECFDF3),
                            border = BorderStroke(1.dp, Color(0xFFABEFC6))
                        ) {
                            Text(
                                text = "Comprobante cargado",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF067647),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    Text(
                        text = "Fecha de creacion: ${formatLatamDateTime(entry.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
            }
        }
    }
}

@Composable
internal fun RequestListCard(
    entry: RequestEntry,
    courierTracking: CourierTrackingInfo? = null,
    isLoadingTracking: Boolean = false,
    selected: Boolean,
    onClick: () -> Unit,
    typeBadge: String? = null,
    onDownloadActaClick: (() -> Unit)? = null,
    onScanQrClick: (() -> Unit)? = null,
    isDownloadingActa: Boolean = false
) {
    val firstItem = entry.items.firstOrNull()
    val extraItemsCount = (entry.items.size - 1).coerceAtLeast(0)
    val chipLabelOverride = if (entry.estadoGeneralId == 9) {
        "Solicitud finalizada"
    } else {
        entry.statusDescription
    }
    val estadoChipOverride = when (entry.estadoGeneralId) {
        9 -> StatusChipStyle(
            background = Color(0xFFECFDF3),
            border = Color(0xFFABEFC6),
            foreground = Color(0xFF067647)
        )
        20 -> StatusChipStyle(
            background = Color(0xFFEAF2FF),
            border = Color(0xFFB8D2FF),
            foreground = Color(0xFF1D4ED8)
        )
        49 -> StatusChipStyle(
            background = Color(0xFFFFF4E5),
            border = Color(0xFFF7B267),
            foreground = Color(0xFFB45309)
        )
        27 -> StatusChipStyle(
            background = Color(0xFFF3E8FF),
            border = Color(0xFFD8B4FE),
            foreground = Color(0xFF7E22CE)
        )
        else -> null
    }
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
                    Text(
                        text = entry.title.ifBlank { "Solicitud ${entry.id}" },
                        style = MaterialTheme.typography.titleSmall,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (entry.subirActa && onDownloadActaClick != null) {
                        OutlinedButton(
                            onClick = onDownloadActaClick,
                            enabled = !isDownloadingActa,
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.25f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = BrandBlue
                            )
                        ) {
                            if (isDownloadingActa) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandBlue
                                    )
                                    Text(
                                        text = "Descargando...",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrandBlue
                                    )
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Descargar acta",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    if (onScanQrClick != null) {
                        Button(
                            onClick = onScanQrClick,
                            shape = RoundedCornerShape(999.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlueSoft,
                                contentColor = BrandBlue
                            ),
                            border = BorderStroke(1.dp, BrandBlue.copy(alpha = 0.28f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Escanear QR",
                                modifier = Modifier.padding(start = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
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
                    labelOverride = chipLabelOverride,
                    styleOverride = estadoChipOverride
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
                            text = "Cant: ${firstItem.requested} | Estado: ${firstItem.statusDescription}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted
                        )
                        firstItem.area?.takeIf { it.isNotBlank() }?.let { area ->
                            Text(
                                text = "Area responsable: $area",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandMuted
                            )
                        }
                        if (extraItemsCount > 0) {
                            Text(
                                text = "+$extraItemsCount item(s) mas",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            CourierTrackingCard(
                entry = entry,
                tracking = courierTracking,
                isLoading = isLoadingTracking,
                compact = true
            )

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
            if (entry.comprobante != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, BrandBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Comprobante cargado",
                                style = MaterialTheme.typography.labelLarge,
                                color = BrandText,
                                fontWeight = FontWeight.SemiBold
                            )
                            entry.comprobante.tipo?.let { Text("Tipo: $it", style = MaterialTheme.typography.bodySmall, color = BrandMuted) }
                            entry.comprobante.numero?.let { Text("Numero: $it", style = MaterialTheme.typography.bodySmall, color = BrandMuted) }
                            entry.comprobante.monto?.let { Text("Monto: S/ $it", style = MaterialTheme.typography.bodySmall, color = BrandMuted) }
                            entry.comprobante.archivoUrl?.let { Text("Archivo: $it", style = MaterialTheme.typography.bodySmall, color = BrandBlue) }
                        }
                    }
                }
            } else if (entry.estadoDetalleId == 9) item {
                Button(onClick = onRegisterComprobanteClick, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)) { Text("Registrar comprobante") }
            }
            item {
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BrandBorder), colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandMuted)) { Text("Cerrar") }
            }
        }
    }
}

@Composable
internal fun RequestDetailPanel(
    entry: RequestEntry,
    courierTracking: CourierTrackingInfo? = null,
    isLoadingTracking: Boolean = false,
    onClose: () -> Unit,
    isUploadingActa: Boolean = false,
    onUploadActaClick: ((solicitudId: Int, selectedFileUri: String) -> Unit)? = null
) {
    val pickPdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val selectedUri = uri?.toString() ?: return@rememberLauncherForActivityResult
        onUploadActaClick?.invoke(entry.solicitudId, selectedUri)
    }

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
            if (!entry.empresaAgencia.isNullOrBlank()) {
                item {
                    CourierTrackingCard(
                        entry = entry,
                        tracking = courierTracking,
                        isLoading = isLoadingTracking,
                        compact = false
                    )
                }
            }
            items(entry.items, key = { it.id }) { item -> RequestItemCard(item = item) }
            if (entry.subirActa && onUploadActaClick != null) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { pickPdfLauncher.launch("application/pdf") },
                            enabled = !isUploadingActa,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBlue,
                                contentColor = Color.White
                            )
                        ) {
                            if (isUploadingActa) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color.White
                                    )
                                    Text("Subiendo acta...")
                                }
                            } else {
                                Text("Subir acta firmada")
                            }
                        }
                        if (isUploadingActa) {
                            Text(
                                text = "Estamos cargando el archivo, por favor espera.",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onClose, modifier = Modifier.fillMaxWidth().height(44.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, BrandBorder), colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandMuted)) { Text("Cerrar") }
            }
        }
    }
}

@Composable
private fun CourierTrackingCard(
    entry: RequestEntry,
    tracking: CourierTrackingInfo?,
    isLoading: Boolean,
    compact: Boolean
) {
    val uriHandler = LocalUriHandler.current
    val providerName = tracking?.agencia ?: entry.empresaAgencia ?: return
    val textStyle = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall
    val trackingUrl = tracking?.fallbackUrl?.takeIf { it.isNotBlank() } ?: defaultCourierUrl(providerName)
    val statusLabel = tracking?.estadoNombre ?: tracking?.estadoActual

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF4F7FF),
        border = BorderStroke(1.dp, Color(0xFFD6E4FF))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Empresa / Agencia: ${providerName.uppercase()}",
                style = textStyle,
                color = BrandText
            )
            (tracking?.numero ?: entry.numeroOrden)?.takeIf { it.isNotBlank() }?.let { numeroOrden ->
                Text(
                    text = "Numero de orden: $numeroOrden",
                    style = textStyle,
                    color = BrandMuted
                )
            }
            (tracking?.codigo ?: entry.codOrden)?.takeIf { it.isNotBlank() }?.let { codOrden ->
                Text(
                    text = "Codigo de orden: $codOrden",
                    style = textStyle,
                    color = BrandMuted
                )
            }
            tracking?.ticket?.takeIf { it.isNotBlank() }?.let { ticket ->
                Text(
                    text = "Ticket: $ticket",
                    style = textStyle,
                    color = BrandMuted
                )
            }
            when {
                isLoading -> {
                    Text(
                        text = "Consultando seguimiento...",
                        style = textStyle,
                        color = BrandMuted
                    )
                }
                !statusLabel.isNullOrBlank() -> {
                    Text(
                        text = "Estado: $statusLabel",
                        style = textStyle,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            tracking?.fecha?.takeIf { it.isNotBlank() }?.let { fecha ->
                Text(
                    text = "Fecha: ${formatLatamDateTime(fecha)}",
                    style = textStyle,
                    color = BrandMuted
                )
            }
            tracking?.detalleManual?.takeIf { it.isNotBlank() }?.let { detalle ->
                Text(
                    text = "Detalle: $detalle",
                    style = textStyle,
                    color = BrandMuted
                )
            }
            tracking?.comentario?.takeIf { it.isNotBlank() }?.let { comentario ->
                Text(
                    text = "Comentario: $comentario",
                    style = textStyle,
                    color = BrandMuted
                )
            }
            tracking?.comprobanteUrl?.takeIf { it.isNotBlank() }?.let { comprobanteUrl ->
                Text(
                    text = "Comprobante: $comprobanteUrl",
                    style = textStyle,
                    color = BrandBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { uriHandler.openUri(comprobanteUrl) }
                )
            }
            trackingUrl?.let { url ->
                Text(
                    text = "Seguimiento: $url",
                    style = textStyle,
                    color = BrandBlue,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { uriHandler.openUri(url) }
                )
            }
        }
    }
}

private fun defaultCourierUrl(empresaAgencia: String): String? {
    val normalized = empresaAgencia.trim().lowercase()
    return when {
        "olva" in normalized -> "https://tracking.olvaexpress.pe/"
        "shalom" in normalized || "shalon" in normalized -> "https://www.shalom.pe/"
        else -> null
    }
}

@Composable
internal fun RequestTypeSheet(
    onDismiss: () -> Unit,
    onSelect: (RequestStartOption) -> Unit
) {
    var selectedOption by remember { mutableStateOf<RequestStartOption?>(null) }

    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandBorder)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Nueva solicitud",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Selecciona el tipo para continuar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandMuted
                    )

                    RequestTypeOptionCard(
                        option = RequestStartOption.Epps,
                        selected = selectedOption == RequestStartOption.Epps,
                        onClick = { selectedOption = RequestStartOption.Epps }
                    )
                    RequestTypeOptionCard(
                        option = RequestStartOption.Almacen,
                        selected = selectedOption == RequestStartOption.Almacen,
                        onClick = { selectedOption = RequestStartOption.Almacen }
                    )
                    RequestTypeOptionCard(
                        option = RequestStartOption.Gasto,
                        selected = selectedOption == RequestStartOption.Gasto,
                        onClick = { selectedOption = RequestStartOption.Gasto }
                    )

                    Button(
                        onClick = { selectedOption?.let(onSelect) },
                        enabled = selectedOption != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBlue,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Continuar")
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancelar", color = BrandMuted)
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestTypeOptionCard(
    option: RequestStartOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (selected) BrandBlueSoft else Color.White,
        border = BorderStroke(
            1.dp,
            if (selected) BrandBlue.copy(alpha = 0.35f) else BrandBorder
        ),
        tonalElevation = if (selected) 1.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
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

@Composable
private fun RequestItemCard(item: RequestItemLine) {
    val isRejected = item.status == RequestStatus.Rejected
    val isApproved = item.status == RequestStatus.Approved

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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = item.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = BrandBlue,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = item.status, labelOverride = item.statusDescription)
            }

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
                        text = "Producto: ${item.product}",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandText,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!isRejected) {
                        val quantityText = if (isApproved) {
                            val approvedQty = item.approved ?: item.requested
                            "Cant. solicitada: ${item.requested} | Cant. aprobada: $approvedQty"
                        } else {
                            "Cant. solicitada: ${item.requested}"
                        }
                        Text(
                            text = "$quantityText | Estado: ${item.statusDescription}",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted
                        )
                    }
                    item.area?.takeIf { it.isNotBlank() }?.let { area ->
                        Text(
                            text = "Area responsable: $area",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted
                        )
                    }
                    item.motivo?.takeIf { it.isNotBlank() }?.let { motivo ->
                        Text(
                            text = "Comentario: $motivo",
                            style = MaterialTheme.typography.labelSmall,
                            color = BrandMuted
                        )
                    }
                }
            }

            Text(
                text = "Fecha de Atencion: ${item.approvedAt}",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )
        }
    }
}

@Composable
private fun StatusChip(
    status: RequestStatus,
    labelOverride: String? = null,
    styleOverride: StatusChipStyle? = null
) {
    val style = styleOverride ?: statusChipStyle(status)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = style.background,
        border = BorderStroke(1.dp, style.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = labelOverride?.takeIf { it.isNotBlank() } ?: status.label,
                style = MaterialTheme.typography.labelMedium,
                color = style.foreground,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
