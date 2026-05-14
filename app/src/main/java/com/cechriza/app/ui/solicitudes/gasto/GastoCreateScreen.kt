package com.cechriza.app.ui.solicitudes.gasto

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.cechriza.app.ui.solicitudes.create.SolicitudCreateScreen

@Composable
fun GastoCreateScreen(
    onHomeClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    onRegisterSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    SolicitudCreateScreen(
        modifier = modifier,
        onHomeClick = onHomeClick,
        onNotificationsClick = onNotificationsClick,
        onRegisterSuccess = onRegisterSuccess,
        initialPreset = "gasto",
        screenTitle = "Solicitud de Compras"
    )
}
