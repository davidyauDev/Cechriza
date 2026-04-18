package com.example.myapplication.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val HeaderBackground = BrandBlue
private val HeaderTitleColor = Color.White
private val HeaderSubtitleColor = Color(0xFFDCE7FF)
private val HeaderButtonTint = BrandBlueDark
private val HeaderButtonBackground = Color.White
private val HeaderButtonBorder = Color.White.copy(alpha = 0.58f)
private val HeaderNotificationTint = BrandBlueDark
private val HeaderNotificationBackground = Color.White
private val HeaderNotificationBorder = Color.White.copy(alpha = 0.58f)

@Composable
fun AppHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBackButton: Boolean = false,
    showMenuButton: Boolean = false,
    showNotificationButton: Boolean = false,
    onBackClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HeaderBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderActionSlot(
                visible = showBackButton || showMenuButton,
                onClick = if (showBackButton) onBackClick else onMenuClick,
                icon = if (showBackButton) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Menu,
                contentDescription = if (showBackButton) "Atras" else "Menu",
                tint = HeaderButtonTint,
                backgroundColor = HeaderButtonBackground,
                borderColor = HeaderButtonBorder
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = HeaderTitleColor,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = HeaderSubtitleColor,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }

            HeaderActionSlot(
                visible = showNotificationButton,
                onClick = onNotificationClick,
                icon = Icons.Default.Notifications,
                contentDescription = "Notificaciones",
                tint = HeaderNotificationTint,
                backgroundColor = HeaderNotificationBackground,
                borderColor = HeaderNotificationBorder
            )
        }
    }
}

@Composable
private fun HeaderActionSlot(
    visible: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    backgroundColor: Color,
    borderColor: Color
) {
    if (!visible) {
        Box(modifier = Modifier.size(40.dp))
        return
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.size(40.dp),
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint
            )
        }
    }
}

@Composable
fun BlueHeaderWithName(
    userName: String,
    modifier: Modifier = Modifier,
    showNotificationButton: Boolean = false,
    onMenuClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(HeaderBackground)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HeaderActionSlot(
                visible = true,
                onClick = onMenuClick,
                icon = Icons.Default.Menu,
                contentDescription = "Menu",
                tint = HeaderButtonTint,
                backgroundColor = HeaderButtonBackground,
                borderColor = HeaderButtonBorder
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Welcome,",
                    color = HeaderSubtitleColor,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1
                )
                Text(
                    text = userName,
                    color = HeaderTitleColor,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            HeaderActionSlot(
                visible = showNotificationButton,
                onClick = onNotificationClick,
                icon = Icons.Default.Notifications,
                contentDescription = "Notificaciones",
                tint = HeaderNotificationTint,
                backgroundColor = HeaderNotificationBackground,
                borderColor = HeaderNotificationBorder
            )
        }
    }
}
