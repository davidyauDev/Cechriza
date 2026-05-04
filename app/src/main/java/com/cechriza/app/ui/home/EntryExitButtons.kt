package com.cechriza.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cechriza.app.data.local.entity.AttendanceType

@Composable
fun EntryExitButtons(
    onEntry: () -> Unit,
    onExit: () -> Unit,
    isBusy: Boolean = false,
    activeType: AttendanceType = AttendanceType.ENTRADA
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Marcar asistencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandText
            )

            Text(
                text = "Elige Entrada o Salida para registrar",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    label = "ENTRADA",
                    icon = Icons.Default.CameraAlt,
                    accent = BrandBlue,
                    container = Color.White,
                    contentColor = BrandBlueDark,
                    isBusy = isBusy && activeType == AttendanceType.ENTRADA,
                    onClick = onEntry
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    label = "SALIDA",
                    icon = Icons.Default.CameraAlt,
                    accent = BrandOrangeSoft,
                    container = Color.White,
                    contentColor = Color(0xFF9A4A10),
                    isBusy = isBusy && activeType == AttendanceType.SALIDA,
                    onClick = onExit
                )
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    container: Color,
    contentColor: Color,
    isBusy: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(96.dp),
        shape = RoundedCornerShape(22.dp),
        color = container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(accent.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = contentColor,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = label,
                color = contentColor,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}
