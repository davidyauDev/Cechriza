package com.cechriza.app.ui.solicitudes.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.solicitudes.SolicitudesRemoteDataSource
import com.cechriza.app.ui.home.AppHeader
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.solicitudes.list.components.ComprobanteDetailPanel
import com.cechriza.app.ui.solicitudes.list.components.ComprobanteListCard
import com.cechriza.app.ui.solicitudes.list.components.ComprobanteSourceTabs
import com.cechriza.app.ui.solicitudes.list.components.LoadingCard
import com.cechriza.app.ui.solicitudes.list.components.MessageCard
import com.cechriza.app.ui.solicitudes.list.components.ReloadRow
import com.cechriza.app.ui.solicitudes.list.components.RequestDetailPanel
import com.cechriza.app.ui.solicitudes.list.components.RequestListCard
import com.cechriza.app.ui.solicitudes.list.components.RequestTypeDialog
import com.cechriza.app.ui.solicitudes.list.components.SolicitudModeTabs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SOLICITUD_LIST_OPEN_BOTAS_TAB_KEY = "solicitud_list_open_botas_tab"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudListScreen(
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
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var detailEntry by remember { mutableStateOf<RequestEntry?>(null) }
    var detailComprobanteEntry by remember { mutableStateOf<ComprobanteEntry?>(null) }
    var showRequestTypeDialog by remember { mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val openBotasTabSignal = navBackStackEntry
        ?.savedStateHandle
        ?.get<Long>(SOLICITUD_LIST_OPEN_BOTAS_TAB_KEY)
    val uiState = SolicitudListUiState(
        mode = mode,
        source = comprobanteSource,
        requests = requests,
        comprobantes = comprobantes,
        isLoadingRequests = isLoading,
        isLoadingComprobantes = isLoadingComprobantes,
        requestsError = errorMessage,
        comprobantesError = comprobantesError
    )

    LaunchedEffect(openBotasTabSignal) {
        if (openBotasTabSignal == null) return@LaunchedEffect
        mode = HistoryMode.Comprobantes
        comprobanteSource = ComprobanteSource.Rrhh
        navBackStackEntry
            ?.savedStateHandle
            ?.remove<Long>(SOLICITUD_LIST_OPEN_BOTAS_TAB_KEY)
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
            val response = withContext(Dispatchers.IO) {
                SolicitudesRemoteDataSource.getSolicitudes(solicitanteUserId)
            }
            if (response.isSuccessful) {
                requests = withContext(Dispatchers.Default) { parseRequestEntries(response.body()) }
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
            val response = withContext(Dispatchers.IO) {
                SolicitudesRemoteDataSource.getSolicitudesGastoComprobantes(staffId)
            }
            if (response.isSuccessful) {
                val parsed = withContext(Dispatchers.Default) { parseComprobantesEntries(response.body()) }
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
                onBackClick = { if (showBackButton) navController.popBackStack() },
                showNotificationButton = false
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showRequestTypeDialog = true },
                containerColor = BrandBlue,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
            ) { Icon(imageVector = Icons.Default.Add, contentDescription = "Nueva solicitud") }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SolicitudModeTabs(mode = mode, onChange = { mode = it }) }

            if (uiState.showComprobanteTabs) {
                item { ComprobanteSourceTabs(source = uiState.source, onChange = { comprobanteSource = it }) }
                item { ReloadRow(onReload = { reloadComprobantesTick += 1 }) }

                when {
                    uiState.isLoadingComprobantes -> item { LoadingCard("Cargando ${uiState.source.label.lowercase()}...") }
                    !uiState.comprobantesError.isNullOrBlank() -> item { MessageCard("No se pudo cargar comprobantes", uiState.comprobantesError.orEmpty()) }
                    uiState.comprobantes.isEmpty() -> item { MessageCard("Sin comprobantes", "No hay registros para este usuario.") }
                    else -> {
                        items(uiState.comprobantes, key = { "${it.id}-${it.date}-${it.title}" }) { entry ->
                            ComprobanteListCard(entry = entry, onClick = { detailComprobanteEntry = entry })
                        }
                    }
                }
            } else {
                item { ReloadRow(onReload = { reloadRequestsTick += 1 }) }
                when {
                    uiState.isLoadingRequests -> item { LoadingCard("Cargando solicitudes...") }
                    !uiState.requestsError.isNullOrBlank() -> item { MessageCard("No se pudo cargar el historial", uiState.requestsError.orEmpty()) }
                    uiState.requests.isEmpty() -> item { MessageCard("Sin solicitudes", "Aun no hay registros para este usuario.") }
                    else -> {
                        itemsIndexed(uiState.requests, key = { index, entry -> "${entry.id}-$index" }) { _, entry ->
                            RequestListCard(entry = entry, selected = entry.id == selectedId, onClick = {
                                selectedId = entry.id
                                detailEntry = entry
                            })
                        }
                    }
                }
            }
        }

        detailEntry?.takeIf { mode == HistoryMode.Historial }?.let { entry ->
            ModalBottomSheet(onDismissRequest = { detailEntry = null }, sheetState = sheetState) {
                RequestDetailPanel(entry = entry, onClose = { detailEntry = null })
            }
        }

        detailComprobanteEntry?.takeIf { mode == HistoryMode.Comprobantes }?.let { entry ->
            ModalBottomSheet(onDismissRequest = { detailComprobanteEntry = null }, sheetState = sheetState) {
                ComprobanteDetailPanel(
                    entry = entry,
                    onClose = { detailComprobanteEntry = null },
                    onRegisterComprobanteClick = {
                        detailComprobanteEntry = null
                        val solicitudId = entry.id.filter(Char::isDigit)
                        onAddRequestClick(if (solicitudId.isNotBlank()) "comprobante:$solicitudId" else "comprobante")
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
