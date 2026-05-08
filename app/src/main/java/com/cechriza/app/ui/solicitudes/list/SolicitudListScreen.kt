package com.cechriza.app.ui.solicitudes.list

import android.content.Intent
import android.content.ActivityNotFoundException
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.solicitudes.SolicitudesRemoteDataSource
import com.google.gson.JsonElement
import com.google.gson.JsonParser
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
import com.cechriza.app.ui.solicitudes.list.components.RequestTypeSheet
import com.cechriza.app.ui.solicitudes.list.components.SolicitudListViewTabs
import com.cechriza.app.ui.solicitudes.list.components.SolicitudModeTabs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File

private const val SOLICITUD_LIST_OPEN_BOTAS_TAB_KEY = "solicitud_list_open_botas_tab"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolicitudListScreen(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    showBackButton: Boolean = true,
    onAddRequestClick: (String) -> Unit = {}
) {
    var viewMode by remember { mutableStateOf(SolicitudListViewMode.Todo) }
    var mode by remember { mutableStateOf(HistoryMode.Historial) }
    var comprobanteSource by remember { mutableStateOf(ComprobanteSource.Gastos) }
    var allComprobantes by remember { mutableStateOf<List<ComprobanteEntry>>(emptyList()) }
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
    var isUploadingActa by remember { mutableStateOf(false) }
    var uploadDialogTitle by remember { mutableStateOf<String?>(null) }
    var uploadDialogMessage by remember { mutableStateOf<String?>(null) }
    var showRequestTypeSheet by remember { mutableStateOf(false) }
    val sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
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

    LaunchedEffect(mode, comprobanteSource, viewMode, reloadComprobantesTick) {
        if (mode != HistoryMode.Comprobantes && viewMode != SolicitudListViewMode.Todo) return@LaunchedEffect
        val staffId = SessionManager.staffId ?: SessionManager.userId
        if (staffId == null || staffId <= 0) {
            allComprobantes = emptyList()
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
                allComprobantes = parsed
                comprobantes = when (comprobanteSource) {
                    ComprobanteSource.Rrhh -> parsed.filter { it.areaId == 11 }
                    ComprobanteSource.Gastos -> parsed.filter { it.areaId != 11 }
                }
            } else {
                allComprobantes = emptyList()
                comprobantes = emptyList()
                comprobantesError = "No se pudo cargar ${comprobanteSource.label} (${response.code()})"
            }
        } catch (_: Exception) {
            allComprobantes = emptyList()
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
        bottomBar = {
            Button(
                onClick = { showRequestTypeSheet = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandBlue,
                    contentColor = Color.White
                )
            ) {
                Text("Nueva solicitud")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("Vista", color = Color(0xFF344054)) }
            item { SolicitudListViewTabs(mode = viewMode, onChange = { viewMode = it }) }
            item {
                Text(
                    text = if (viewMode == SolicitudListViewMode.Todo) {
                        "Se muestra todo junto con etiquetas por tipo."
                    } else {
                        "Filtra por categoria usando las pestanas de Almacen y Gastos."
                    },
                    color = Color(0xFF667085)
                )
            }
            if (viewMode == SolicitudListViewMode.Tabs) {
                item { SolicitudModeTabs(mode = mode, onChange = { mode = it }) }
            }

            if (viewMode == SolicitudListViewMode.Tabs && uiState.showComprobanteTabs) {
                item { ComprobanteSourceTabs(source = uiState.source, onChange = { comprobanteSource = it }) }
                item { ReloadRow(onReload = { reloadComprobantesTick += 1 }) }

                when {
                    uiState.isLoadingComprobantes -> item { LoadingCard("Cargando ${uiState.source.label.lowercase()}...") }
                    !uiState.comprobantesError.isNullOrBlank() -> item { MessageCard("No se pudo cargar comprobantes", uiState.comprobantesError.orEmpty()) }
                    uiState.comprobantes.isEmpty() -> item { MessageCard("Sin comprobantes", "No hay registros para este usuario.") }
                    else -> {
                        items(uiState.comprobantes, key = { "${it.id}-${it.date}-${it.title}" }) { entry ->
                            ComprobanteListCard(
                                entry = entry,
                                onClick = { detailComprobanteEntry = entry },
                                typeBadge = if (entry.areaId == 11) "Botas" else "Otros"
                            )
                        }
                    }
                }
            } else if (viewMode == SolicitudListViewMode.Tabs) {
                item { ReloadRow(onReload = { reloadRequestsTick += 1 }) }
                when {
                    uiState.isLoadingRequests -> item { LoadingCard("Cargando solicitudes...") }
                    !uiState.requestsError.isNullOrBlank() -> item { MessageCard("No se pudo cargar el historial", uiState.requestsError.orEmpty()) }
                    uiState.requests.isEmpty() -> item { MessageCard("Sin solicitudes", "Aun no hay registros para este usuario.") }
                    else -> {
                        itemsIndexed(uiState.requests, key = { index, entry -> "${entry.id}-$index" }) { _, entry ->
                            RequestListCard(
                                entry = entry,
                                selected = entry.id == selectedId,
                                onClick = {
                                    selectedId = entry.id
                                    detailEntry = entry
                                },
                                typeBadge = mapRequestCategoryBadge(entry.category),
                                onDownloadActaClick = {
                                    scope.launch {
                                        downloadActaForEntry(
                                            entry = entry,
                                            context = context,
                                            snackbarHostState = snackbarHostState
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                item { ReloadRow(onReload = { reloadRequestsTick += 1; reloadComprobantesTick += 1 }) }
                item { Text("Solicitudes de Almacen", color = Color(0xFF344054)) }
                when {
                    uiState.isLoadingRequests -> item { LoadingCard("Cargando solicitudes...") }
                    !uiState.requestsError.isNullOrBlank() -> item { MessageCard("No se pudo cargar solicitudes", uiState.requestsError.orEmpty()) }
                    uiState.requests.isEmpty() -> item { MessageCard("Sin solicitudes", "Aun no hay registros para este usuario.") }
                    else -> {
                        itemsIndexed(uiState.requests, key = { index, entry -> "${entry.id}-$index" }) { _, entry ->
                            RequestListCard(
                                entry = entry,
                                selected = entry.id == selectedId,
                                onClick = {
                                    selectedId = entry.id
                                    detailEntry = entry
                                },
                                typeBadge = mapRequestCategoryBadge(entry.category),
                                onDownloadActaClick = {
                                    scope.launch {
                                        downloadActaForEntry(
                                            entry = entry,
                                            context = context,
                                            snackbarHostState = snackbarHostState
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item { Text("Comprobantes de Gasto y Botas", color = Color(0xFF344054)) }
                when {
                    uiState.isLoadingComprobantes -> item { LoadingCard("Cargando comprobantes...") }
                    !uiState.comprobantesError.isNullOrBlank() -> item { MessageCard("No se pudo cargar comprobantes", uiState.comprobantesError.orEmpty()) }
                    allComprobantes.isEmpty() -> item { MessageCard("Sin comprobantes", "No hay registros para este usuario.") }
                    else -> {
                        items(allComprobantes, key = { "${it.id}-${it.date}-${it.title}" }) { entry ->
                            ComprobanteListCard(
                                entry = entry,
                                onClick = { detailComprobanteEntry = entry },
                                typeBadge = if (entry.areaId == 11) "Botas" else "Gasto"
                            )
                        }
                    }
                }
            }
        }

        detailEntry?.takeIf { mode == HistoryMode.Historial || viewMode == SolicitudListViewMode.Todo }?.let { entry ->
            ModalBottomSheet(onDismissRequest = { detailEntry = null }, sheetState = sheetState) {
                RequestDetailPanel(
                    entry = entry,
                    onClose = { detailEntry = null },
                    isUploadingActa = isUploadingActa,
                    onUploadActaClick = { solicitudId, selectedFileUri ->
                        val userId = SessionManager.staffId ?: SessionManager.userId
                        if (userId == null || userId <= 0) {
                            scope.launch { snackbarHostState.showSnackbar("No se encontro id de usuario") }
                        } else scope.launch {
                            val sourceUri = runCatching { Uri.parse(selectedFileUri) }.getOrNull()
                            if (sourceUri == null) {
                                snackbarHostState.showSnackbar("Archivo invalido")
                                return@launch
                            }
                            isUploadingActa = true
                            snackbarHostState.showSnackbar("Preparando archivo...")
                            val tempPdf = withContext(Dispatchers.IO) {
                                copyUriToTempPdf(context = context, uri = sourceUri, solicitudId = solicitudId)
                            }
                            if (tempPdf == null) {
                                isUploadingActa = false
                                snackbarHostState.showSnackbar("No se pudo leer el archivo")
                                return@launch
                            }
                            snackbarHostState.showSnackbar("Subiendo acta...")
                            val uploadResponse = withContext(Dispatchers.IO) {
                                runCatching {
                                    SolicitudesRemoteDataSource.subirActaFirmada(
                                        solicitudId = solicitudId,
                                        userId = userId,
                                        actaPdfFile = tempPdf
                                    )
                                }.getOrNull()
                            }
                            val uploadBody = uploadResponse?.body()
                            val uploadSuccess = uploadResponse?.isSuccessful == true &&
                                uploadBody?.jsonBoolean("success") == true
                            val uploadMessage = uploadBody?.jsonString("message")
                            if (uploadSuccess) {
                                uploadDialogTitle = "Acta subida"
                                uploadDialogMessage = uploadMessage ?: "Acta subida correctamente."
                                reloadRequestsTick += 1
                            } else {
                                val errorMessage = uploadMessage
                                    ?: runCatching { uploadResponse?.errorBody()?.string() }.getOrNull()
                                        ?.let(::extractMessageFromJsonString)
                                    ?: "No se pudo subir el acta"
                                uploadDialogTitle = "No se pudo subir"
                                uploadDialogMessage = errorMessage
                            }
                            isUploadingActa = false
                        }
                    }
                )
            }
        }

        detailComprobanteEntry?.takeIf { mode == HistoryMode.Comprobantes || viewMode == SolicitudListViewMode.Todo }?.let { entry ->
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

        if (showRequestTypeSheet) {
            RequestTypeSheet(
                onDismiss = { showRequestTypeSheet = false },
                onSelect = { option ->
                    showRequestTypeSheet = false
                    val preset = when (option) {
                        RequestStartOption.Epps -> "epp"
                        RequestStartOption.Almacen -> "almacen"
                        RequestStartOption.Gasto -> "gasto"
                    }
                    onAddRequestClick(preset)
                }
            )
        }

        if (!uploadDialogTitle.isNullOrBlank() && !uploadDialogMessage.isNullOrBlank()) {
            AlertDialog(
                onDismissRequest = {
                    uploadDialogTitle = null
                    uploadDialogMessage = null
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            uploadDialogTitle = null
                            uploadDialogMessage = null
                        }
                    ) {
                        Text("Entendido")
                    }
                },
                title = { Text(uploadDialogTitle.orEmpty()) },
                text = { Text(uploadDialogMessage.orEmpty()) }
            )
        }
    }
}

private fun mapRequestCategoryBadge(category: String): String? {
    val normalized = category.trim().lowercase()
    return when {
        "almacen" in normalized -> "Almacen"
        "gasto" in normalized -> "Gasto"
        "bota" in normalized || "epp" in normalized || "ssoma" in normalized -> "Botas"
        else -> null
    }
}

private suspend fun downloadActaForEntry(
    entry: RequestEntry,
    context: android.content.Context,
    snackbarHostState: SnackbarHostState
) {
    val userId = SessionManager.staffId ?: SessionManager.userId
    if (userId == null || userId <= 0) {
        snackbarHostState.showSnackbar("No se encontro user_id de sesion")
        return
    }

    snackbarHostState.showSnackbar("Descargando acta...")

    val result = withContext(Dispatchers.IO) {
        runCatching {
            SolicitudesRemoteDataSource.descargarCompromiso(
                solicitudId = entry.solicitudId,
                userId = userId
            )
        }
    }
    val response = result.getOrNull()
    if (response == null) {
        snackbarHostState.showSnackbar("No se pudo descargar el acta (sin respuesta del servidor)")
        return
    }

    val contentType = response.headers()["Content-Type"].orEmpty().lowercase()
    val contentDisposition = response.headers()["Content-Disposition"].orEmpty()
    val body = response.body()

    if (response.isSuccessful && contentType.contains("application/pdf") && body != null) {
        val fileName = extractFilenameFromContentDisposition(contentDisposition)
            ?: "compromiso_${entry.solicitudId}.pdf"
        val savedFile = withContext(Dispatchers.IO) {
            savePdfFromResponse(
                responseBody = body,
                fileName = fileName,
                targetDir = context.getExternalFilesDir(null) ?: context.filesDir
            )
        }
        if (savedFile == null) {
            snackbarHostState.showSnackbar("No se pudo guardar el PDF")
        } else {
            val opened = openPdfFile(context, savedFile)
            if (!opened) {
                snackbarHostState.showSnackbar("PDF descargado, pero no hay una app para abrirlo")
            }
        }
        return
    }

    val bodyMessage = runCatching { body?.string() }.getOrNull()
        ?.let(::extractMessageFromJsonString)
    val errorMessage = bodyMessage
        ?: runCatching { response.errorBody()?.string() }.getOrNull()
            ?.let(::extractMessageFromJsonString)
        ?: "No se pudo descargar el acta"
    snackbarHostState.showSnackbar(errorMessage)
}

private fun savePdfFromResponse(responseBody: ResponseBody, fileName: String, targetDir: File): File? {
    return runCatching {
        if (!targetDir.exists()) targetDir.mkdirs()
        val outFile = File(targetDir, fileName)
        responseBody.byteStream().use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        }
        outFile
    }.getOrNull()
}

private fun extractFilenameFromContentDisposition(contentDisposition: String): String? {
    if (contentDisposition.isBlank()) return null
    val idx = contentDisposition.lowercase().indexOf("filename=")
    if (idx < 0) return null
    return contentDisposition.substring(idx + "filename=".length)
        .trim()
        .trim('"')
        .takeIf { it.isNotBlank() }
}

private fun extractMessageFromJsonString(raw: String): String? {
    if (raw.isBlank()) return null
    return runCatching {
        val obj = JsonParser().parse(raw).asJsonObject
        obj.get("message")?.asString?.trim()?.takeIf { it.isNotBlank() }
    }.getOrNull()
}

private fun JsonElement.jsonString(key: String): String? {
    if (!isJsonObject) return null
    val value = asJsonObject.get(key) ?: return null
    if (!value.isJsonPrimitive) return null
    return runCatching { value.asString.trim() }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun JsonElement.jsonBoolean(key: String): Boolean? {
    if (!isJsonObject) return null
    val value = asJsonObject.get(key) ?: return null
    if (!value.isJsonPrimitive) return null
    val primitive = value.asJsonPrimitive
    return when {
        primitive.isBoolean -> primitive.asBoolean
        primitive.isString -> primitive.asString.equals("true", ignoreCase = true)
        primitive.isNumber -> runCatching { primitive.asInt != 0 }.getOrDefault(false)
        else -> null
    }
}

private fun copyUriToTempPdf(context: android.content.Context, uri: Uri, solicitudId: Int): File? {
    return runCatching {
        val tempDir = File(context.cacheDir, "actas").apply { mkdirs() }
        val target = File(tempDir, "acta_${solicitudId}_${System.currentTimeMillis()}.pdf")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target
    }.getOrNull()
}


private fun openPdfFile(context: android.content.Context, file: File): Boolean {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val canHandle = intent.resolveActivity(context.packageManager) != null
    if (!canHandle) return false

    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }
}
