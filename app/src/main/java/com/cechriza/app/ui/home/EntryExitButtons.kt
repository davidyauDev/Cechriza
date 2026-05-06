package com.cechriza.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Marcar asistencia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = BrandText
            )

            Text(
                text = "Selecciona una accion para registrar tu jornada",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    label = "Entrada",
                    icon = Icons.Default.Login,
                    accent = BrandBlue,
                    container = BrandBlue,
                    contentColor = Color.White,
                    isBusy = isBusy && activeType == AttendanceType.ENTRADA,
                    onClick = onEntry
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    label = "Salida",
                    icon = Icons.Default.Logout,
                    accent = Color(0xFF0F172A),
                    container = Color(0xFF0F172A),
                    contentColor = Color.White,
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
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Surface(
        onClick = onClick,
        enabled = !isBusy,
        interactionSource = interactionSource,
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (pressed) container.copy(alpha = 0.9f) else container,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.15f)),
        shadowElevation = if (pressed) 0.dp else 1.dp
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = if (contentColor == Color.White) 0.18f else 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = Color.White,
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
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall
            )

            Spacer(modifier = Modifier.height(1.dp))
        }
    }
}
