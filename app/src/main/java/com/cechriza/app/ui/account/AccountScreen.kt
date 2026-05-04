package com.cechriza.app.ui.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.ui.home.AppHeader
import com.cechriza.app.ui.home.BrandBlueDark
import com.cechriza.app.ui.home.BrandBlueSoft
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandOrange
import com.cechriza.app.ui.home.BrandOrangeSoft
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import com.cechriza.app.ui.user.UserViewModel

@Composable
fun AccountScreen(
    modifier: Modifier = Modifier,
    onNotificationsClick: () -> Unit = {},
    onLogoutClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val userViewModel: UserViewModel = viewModel()
    val userName by userViewModel.userName.collectAsState()
    val userEmail by userViewModel.userEmail.collectAsState()
    val empCode by userViewModel.userEmpCode.collectAsState()
    val userId by userViewModel.userId.collectAsState()

    val safeName = userName.ifBlank { "Usuario" }
    val initials = safeName
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifBlank { "U" }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandSurface)
    ) {
        AppHeader(
            title = "Account",
            subtitle = "Perfil de usuario",
            showNotificationButton = true,
            onNotificationClick = onNotificationsClick
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                border = BorderStroke(1.dp, BrandBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = BrandBlueSoft,
                        border = BorderStroke(1.dp, BrandBorder),
                        modifier = Modifier.size(72.dp)
                    ) {
                        BoxCentered(initials)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = safeName,
                            color = BrandText,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = userEmail.ifBlank { "correo@empresa.com" },
                            color = BrandMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    HorizontalDivider(color = BrandBorder)

                    ProfileRow(label = "Codigo", value = empCode.ifBlank { "--" })
                    ProfileRow(label = "Usuario", value = "#$userId")
                    ProfileRow(label = "Estado", value = "Activo")
                }
            }

            Surface(
                onClick = {
                    SessionManager.clear(context)
                    userViewModel.clearUser()
                    onLogoutClick()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = BrandOrangeSoft,
                border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cerrar sesión",
                        color = BrandText,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Salir de la cuenta actual y volver al inicio.",
                        color = BrandMuted
                    )

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.25f))
                    ) {
                        Text(
                            text = "Cerrar sesión",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            color = BrandOrange,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = BrandMuted
        )
        Text(
            text = value,
            color = BrandBlueDark,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun BoxCentered(text: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = BrandBlueDark,
            fontWeight = FontWeight.Bold
        )
    }
}
