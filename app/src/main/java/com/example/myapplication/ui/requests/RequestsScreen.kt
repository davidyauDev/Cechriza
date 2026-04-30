package com.example.myapplication.ui.requests

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.ui.home.BrandBlue
import com.example.myapplication.ui.home.BrandBlueDark
import com.example.myapplication.ui.home.BrandBlueSoft
import com.example.myapplication.ui.home.BrandBorder
import com.example.myapplication.ui.home.BrandOrange
import com.example.myapplication.ui.home.BrandSurface
import com.example.myapplication.ui.home.BrandText
import com.example.myapplication.ui.home.BrandMuted
import com.example.myapplication.ui.home.AppHeader
import com.example.myapplication.data.preferences.SessionManager
import com.example.myapplication.data.remote.network.RetrofitClient
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val MAX_DROPDOWN_OPTIONS = 80
private const val SOLICITUD_LOG_TAG = "SolicitudCompleta"
private const val DEFAULT_JUSTIFICACION = "Pedido interno"
private const val DEFAULT_ID_DIRECCION_ENTREGA_LIMA = "5"
private const val DEFAULT_ID_DIRECCION_ENTREGA_PROVINCIA = "6"
private const val EPP_AREA_ID = 11
private const val ALMACEN_SOLICITUD_GENERAL_AREA_ID = 1

private val ScreenBackground = BrandSurface
private val HeaderBackground = BrandBlueSoft
private val HeaderBorder = BrandBorder
private val CardBorder = BrandBorder
private val TitleColor = BrandText
private val BodyColor = BrandMuted
private val AccentColor = BrandBlue
private val AccentSoft = BrandBlueSoft
private val DangerColor = Color(0xFFB42318)
private val DangerSoft = Color(0xFFFEE4E2)
private val SectionOptionsHeader = Color(0xFFF4F1FF)

private enum class RequestTab(
    val label: String,
    val accent: Color,
    val areaId: Int,
    val inventoryResponsable: String,
    val submitCategoryKey: String
) {
    Materials("Insumos/Materiales", BrandBlue, 7, "LOGISTICA", "insumos"),
    Tools("Cilbradores / herramientas", BrandOrange, 12, "SSGG", "ssgg"),
    Epp("EPP", BrandBlueDark, 11, "SSOMA", "rrhh")
}

private enum class DeliveryZone(
    val label: String,
    val idDireccionEntrega: String,
    val ubicacionValue: String
) {
    Lima("Lima", DEFAULT_ID_DIRECCION_ENTREGA_LIMA, "LIMA"),
    Provincia("Provincia", DEFAULT_ID_DIRECCION_ENTREGA_PROVINCIA, "PROVINCIA")
}

private data class InventoryOption(
    val inventoryId: Int? = null,
    val areaId: Int? = null,
    val label: String,
    val requiresPreviousProductPhoto: Boolean = false
)

private val eppBootOptions = listOf(
    InventoryOption(195, EPP_AREA_ID, "BOTAS DE SEGURIDAD NEGRO TALLA 38", true),
    InventoryOption(196, EPP_AREA_ID, "BOTAS DE SEGURIDAD NEGRO TALLA 40", true),
    InventoryOption(197, EPP_AREA_ID, "BOTAS DE SEGURIDAD NEGRO TALLA 41", true),
    InventoryOption(198, EPP_AREA_ID, "BOTAS DE SEGURIDAD NEGRO TALLA 42", true),
    InventoryOption(199, EPP_AREA_ID, "BOTAS DE SEGURIDAD NEGRO TALLA 43", true)
)
private val hiddenEppBootIdsForAlmacen = setOf(195, 196, 197, 198, 199)

private fun defaultOptions(vararg labels: String): List<InventoryOption> {
    return labels.map { InventoryOption(label = it, requiresPreviousProductPhoto = false) }
}

private data class RequestSectionTemplate(
    val tab: RequestTab,
    val title: String,
    val options: List<InventoryOption>
)

@Stable
private class RequestSectionState(template: RequestSectionTemplate) {
    val tab: RequestTab = template.tab
    val title: String = template.title
    val options: List<InventoryOption> = template.options
    val items = mutableStateListOf<MaterialItemForm>()
}

@Stable
private data class MaterialItemForm(
    val id: String = UUID.randomUUID().toString(),
    val quantity: String = "",
    val selectedInventoryId: Int? = null,
    val selectedAreaId: Int? = null,
    val description: String = "",
    val requiresPreviousProductPhoto: Boolean = false,
    val observations: String = "",
    val photoUri: String? = null,
    val photoBitmap: Bitmap? = null
)

private data class SubmitRequestResult(
    val success: Boolean,
    val message: String
)

private data class SolicitudBaseFields(
    val justificacion: String,
    val fechaNecesaria: String,
    val idDireccionEntrega: String,
    val esPedidoCompra: Boolean,
    val ubicacion: String
)

private data class RequestConfirmationItem(
    val quantity: String,
    val description: String,
    val observations: String?,
    val photoStatus: String?
)

private data class RequestConfirmationSection(
    val title: String,
    val items: List<RequestConfirmationItem>
)

private fun buildRequestConfirmationSections(
    sections: List<RequestSectionState>
): List<RequestConfirmationSection> {
    return sections.mapNotNull { section ->
        val summaryItems = section.items.map { item ->
            val quantity = item.quantity.toIntOrNull()?.toString() ?: item.quantity.ifBlank { "0" }
            val photoStatus = if (!item.requiresPreviousProductPhoto) {
                null
            } else if (item.hasAttachedPhoto()) {
                "Foto del producto anterior: adjunta"
            } else {
                "Foto del producto anterior: pendiente"
            }

            RequestConfirmationItem(
                quantity = quantity,
                description = item.description,
                observations = item.observations.trim().takeIf { it.isNotBlank() },
                photoStatus = photoStatus
            )
        }

        if (summaryItems.isEmpty()) {
            null
        } else {
            RequestConfirmationSection(
                title = section.title,
                items = summaryItems
            )
        }
    }
}

private fun defaultSolicitudBaseFields(
    deliveryZone: DeliveryZone,
    esPedidoCompra: Boolean
): SolicitudBaseFields {
    return SolicitudBaseFields(
        justificacion = DEFAULT_JUSTIFICACION,
        fechaNecesaria = LocalDate.now().plusDays(7).toString(),
        idDireccionEntrega = deliveryZone.idDireccionEntrega,
        esPedidoCompra = esPedidoCompra,
        ubicacion = deliveryZone.ubicacionValue
    )
}

@Composable
fun RequestsScreen(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    initialPreset: String? = null
) {
    val context = LocalContext.current
    val materialsSection = remember {
        RequestSectionState(
            RequestSectionTemplate(
                tab = RequestTab.Materials,
                title = "Insumos / Materiales",
                options = defaultOptions(
                    "Cemento",
                    "Arena",
                    "Tornilleria",
                    "Cable",
                    "Pintura",
                    "Otros"
                )
            )
        )
    }
    val toolsSection = remember {
        RequestSectionState(
            RequestSectionTemplate(
                tab = RequestTab.Tools,
                title = "Calibradores / Herramientas",
                options = defaultOptions(
                    "Flexometro",
                    "Nivel",
                    "Taladro",
                    "Llave",
                    "Destornillador",
                    "Otros"
                )
            )
        )
    }
    val eppSection = remember {
        RequestSectionState(
            RequestSectionTemplate(
                tab = RequestTab.Epp,
                title = "EPP",
                options = eppBootOptions
            )
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var submitAttempted by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var selectedDeliveryZone by remember { mutableStateOf(DeliveryZone.Lima) }
    var isPurchaseRequest by remember { mutableStateOf(false) }
    var expandedItemId by remember { mutableStateOf<String?>(null) }
    val presetKey = remember(initialPreset) { initialPreset?.trim()?.lowercase() }
    val isEppOnlyFlow = remember(presetKey) {
        presetKey in setOf("epp", "epps", "botas")
    }
    val isAlmacenFlow = remember(presetKey) { presetKey == "almacen" }
    val isGastoFlow = remember(presetKey) { presetKey == "gasto" }
    val initialTab = remember(presetKey) {
        when (presetKey) {
            "epp", "epps", "botas" -> RequestTab.Epp
            "almacen" -> RequestTab.Materials
            "herramientas" -> RequestTab.Tools
            else -> RequestTab.Materials
        }
    }
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    val optionsByArea = remember { mutableStateMapOf<Int, List<InventoryOption>>() }
    val optionsErrorByArea = remember { mutableStateMapOf<Int, String>() }
    var loadingAreaId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(isEppOnlyFlow, selectedTab) {
        if (isEppOnlyFlow && !isPurchaseRequest) {
            isPurchaseRequest = true
        }
        if (isEppOnlyFlow && selectedTab == RequestTab.Epp && eppSection.items.isEmpty()) {
            eppSection.items.add(MaterialItemForm(quantity = "1"))
        } else if (isEppOnlyFlow && selectedTab == RequestTab.Epp && eppSection.items.isNotEmpty()) {
            eppSection.items.indices.forEach { index ->
                val current = eppSection.items[index]
                if (current.quantity != "1") {
                    eppSection.items[index] = current.copy(quantity = "1")
                }
            }
        }
    }

    LaunchedEffect(selectedTab) {
        if (isEppOnlyFlow && selectedTab == RequestTab.Epp) return@LaunchedEffect
        val targetAreaId = selectedTab.areaId
        val targetResponsable = selectedTab.inventoryResponsable
        if (optionsByArea.containsKey(targetAreaId)) return@LaunchedEffect

        loadingAreaId = targetAreaId
        optionsErrorByArea.remove(targetAreaId)

        try {
            val tokenProvider = { SessionManager.token }
            val api = RetrofitClient.apiWithToken(tokenProvider)
            val response = withContext(Dispatchers.IO) {
                api.getInventarioProductos(targetResponsable)
            }

            if (response.isSuccessful) {
                val remoteOptions = withContext(Dispatchers.Default) {
                    extractInventoryOptionsForArea(
                        root = response.body(),
                        targetAreaId = targetAreaId,
                        excludedProductIds = if (selectedTab == RequestTab.Epp && isAlmacenFlow) {
                            hiddenEppBootIdsForAlmacen
                        } else {
                            emptySet()
                        }
                    )
                }
                if (remoteOptions.isNotEmpty()) {
                    optionsByArea[targetAreaId] = remoteOptions
                } else {
                    optionsErrorByArea[targetAreaId] =
                        "Sin items remotos con ID para $targetResponsable. No se podra enviar hasta recargar."
                }
            } else {
                optionsErrorByArea[targetAreaId] =
                    "No se pudo cargar inventario de $targetResponsable (${response.code()}). No se podra enviar hasta recargar."
            }
        } catch (_: Exception) {
            optionsErrorByArea[targetAreaId] =
                "Sin conexion al inventario de $targetResponsable. No se podra enviar hasta recargar."
        } finally {
            if (loadingAreaId == targetAreaId) {
                loadingAreaId = null
            }
        }
    }

    val allSections = listOf(materialsSection, toolsSection, eppSection)
    val totalItems = allSections.sumOf { it.items.size }
    val activeSections = when (selectedTab) {
        RequestTab.Materials -> listOf(materialsSection)
        RequestTab.Tools -> listOf(toolsSection)
        RequestTab.Epp -> listOf(eppSection)
    }
    val allItems = allSections.flatMap { it.items }
    val canSubmit = totalItems > 0 && allItems.all {
        it.quantity.toIntOrNull()?.let { value -> value > 0 } == true &&
            it.selectedInventoryId != null &&
            it.description.isNotBlank() &&
            (isGastoFlow || !it.requiresPreviousProductPhoto || it.hasAttachedPhoto())
    }
    val confirmationSections = buildRequestConfirmationSections(allSections)
    val currentAreaError = optionsErrorByArea[selectedTab.areaId]
    val submitConfirmedRequest: () -> Unit = {
        scope.launch {
            if (isSubmitting) return@launch
            isSubmitting = true
            showConfirmDialog = false
            val solicitanteUserId = SessionManager.staffId
            if (solicitanteUserId == null || solicitanteUserId <= 0) {
                isSubmitting = false
                snackbarHostState.showSnackbar("No se encontro staff_id de sesion. Vuelve a iniciar sesion.")
                return@launch
            }
            val tokenProvider = { SessionManager.token }
            val api = RetrofitClient.apiWithToken(tokenProvider)
            val baseFields = defaultSolicitudBaseFields(
                deliveryZone = selectedDeliveryZone,
                esPedidoCompra = isPurchaseRequest
            )
            val result = withContext(Dispatchers.IO) {
                if (isEppOnlyFlow) {
                    submitSolicitudGastoGeneralRequest(
                        api = api,
                        sections = listOf(eppSection),
                        solicitanteUserId = solicitanteUserId,
                        context = context,
                        areaId = EPP_AREA_ID,
                        motivo = "Solicitud EPP - Botas de seguridad",
                        fallbackPrefix = "bota",
                        defaultErrorMessage = "No hay botas validas para registrar.",
                        backendErrorMessage = "No se pudo registrar la solicitud de EPP",
                        backendSuccessMessage = "Solicitud de EPP registrada correctamente."
                    )
                } else if (isGastoFlow) {
                    submitSolicitudGastoGeneralRequest(
                        api = api,
                        sections = allSections,
                        solicitanteUserId = solicitanteUserId,
                        context = context,
                        areaId = ALMACEN_SOLICITUD_GENERAL_AREA_ID,
                        motivo = "Solicitud general de almacen",
                        fallbackPrefix = "general",
                        defaultErrorMessage = "No hay items validos para registrar en almacen.",
                        backendErrorMessage = "No se pudo registrar la solicitud de almacen",
                        backendSuccessMessage = "Solicitud de almacen registrada correctamente."
                    )
                } else {
                    submitCompleteRequest(
                        api = api,
                        sections = allSections,
                        baseFields = baseFields,
                        solicitanteUserId = solicitanteUserId,
                        context = context
                    )
                }
            }
            isSubmitting = false
            if (result.success) {
                materialsSection.items.clear()
                toolsSection.items.clear()
                eppSection.items.clear()
                expandedItemId = null
                submitAttempted = false
                onRegisterSuccess()
            } else {
                snackbarHostState.showSnackbar(result.message)
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ScreenBackground,
        topBar = {
            AppHeader(
                title = "Solicitudes",
                showBackButton = true,
                showNotificationButton = true,
                onBackClick = onHomeClick,
                onNotificationClick = onNotificationsClick,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    val requestTabs = remember {
                        if (isEppOnlyFlow) listOf(RequestTab.Epp) else listOf(RequestTab.Materials, RequestTab.Tools, RequestTab.Epp)
                    }
                    val selectedTabIndex = requestTabs.indexOf(selectedTab).coerceAtLeast(0)
                    TabRow(
                        selectedTabIndex = selectedTabIndex,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        containerColor = Color.White,
                        divider = {
                            HorizontalDivider(color = BrandBorder)
                        }
                    ) {
                        requestTabs.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                selectedContentColor = tab.accent,
                                unselectedContentColor = BrandMuted,
                                text = {
                                    Text(
                                        text = tab.label,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            )
                        }
                    }
                }

                if (loadingAreaId == selectedTab.areaId) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .width(18.dp)
                                    .height(18.dp),
                                strokeWidth = 2.dp,
                                color = AccentColor
                            )
                            Text(
                                text = "Cargando items desde inventario...",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                        }
                    }
                }

                if (!currentAreaError.isNullOrBlank()) {
                    item {
                        Text(
                            text = currentAreaError.orEmpty(),
                            modifier = Modifier.padding(horizontal = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandMuted
                        )
                    }
                }

                activeSections.forEach { section ->
                    item {
                        val sectionOptions = if (section.tab == RequestTab.Epp && isEppOnlyFlow) {
                            section.options
                        } else {
                            val baseOptions = optionsByArea[section.tab.areaId].orEmpty().ifEmpty { section.options }
                            if (section.tab == RequestTab.Epp && isAlmacenFlow) {
                                baseOptions.filterNot { option ->
                                    option.inventoryId != null && option.inventoryId in hiddenEppBootIdsForAlmacen
                                }
                            } else {
                                baseOptions
                            }
                        }
                        RequestForm(
                            section = section,
                            options = sectionOptions,
                            submitAttempted = submitAttempted,
                            expandedItemId = expandedItemId,
                            onExpandedItemIdChange = { expandedItemId = it },
                            onAddItem = { section.items.add(MaterialItemForm()) },
                            onDeleteItem = { index ->
                                if (index in section.items.indices) {
                                    val removedId = section.items[index].id
                                    section.items.removeAt(index)
                                    if (expandedItemId == removedId) {
                                        expandedItemId = null
                                    }
                                }
                            },
                            onQuantityChange = { index, value ->
                                if (index in section.items.indices) {
                                    section.items[index] = section.items[index].copy(quantity = value)
                                }
                            },
                            onDescriptionChange = { index, value ->
                                if (index in section.items.indices) {
                                    section.items[index] = section.items[index].copy(
                                        selectedInventoryId = value.inventoryId,
                                        selectedAreaId = value.areaId,
                                        description = value.label,
                                        requiresPreviousProductPhoto = value.requiresPreviousProductPhoto
                                    )
                                }
                            },
                            onObservationsChange = { index, value ->
                                if (index in section.items.indices) {
                                    section.items[index] = section.items[index].copy(observations = value)
                                }
                            },
                            onPhotoChange = { index, uri, bitmap ->
                                if (index in section.items.indices) {
                                    section.items[index] = section.items[index].copy(
                                        photoUri = uri,
                                        photoBitmap = bitmap
                                    )
                                }
                            },
                            onSubmit = {
                                submitAttempted = true
                                if (!canSubmit) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Completa los campos requeridos")
                                    }
                                } else {
                                    showConfirmDialog = true
                                }
                            },
                            enabledSubmit = totalItems > 0 && !isSubmitting,
                            isSubmitting = isSubmitting,
                            isGastoFlow = isGastoFlow,
                            singleItemFlow = isEppOnlyFlow && selectedTab == RequestTab.Epp
                        )
                    }
                }
            }

            if (isSubmitting) {
                RegistrationLoadingOverlay()
            }

            if (showConfirmDialog) {
                RequestConfirmationDialog(
                    sections = confirmationSections,
                    totalItems = totalItems,
                    selectedDeliveryZone = selectedDeliveryZone,
                    onDeliveryZoneChange = { selectedDeliveryZone = it },
                    isPurchaseRequest = isPurchaseRequest,
                    onPurchaseRequestChange = { isPurchaseRequest = it },
                    showPurchaseOption = false,
                    onDismiss = { showConfirmDialog = false },
                    onConfirm = submitConfirmedRequest
                )
            }
        }
    }
}

@Composable
private fun RegistrationLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, HeaderBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    color = AccentColor,
                    strokeWidth = 3.dp
                )
                Text(
                    text = "Registrando solicitud...",
                    style = MaterialTheme.typography.titleSmall,
                    color = TitleColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Espera un momento, estamos enviando los datos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = BodyColor,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun RequestConfirmationDialog(
    sections: List<RequestConfirmationSection>,
    totalItems: Int,
    selectedDeliveryZone: DeliveryZone,
    onDeliveryZoneChange: (DeliveryZone) -> Unit,
    isPurchaseRequest: Boolean,
    onPurchaseRequestChange: (Boolean) -> Unit,
    showPurchaseOption: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Confirmar solicitud",
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Revisa el detalle antes de registrar ($totalItems item${if (totalItems == 1) "" else "s"}).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BodyColor
                )
                Text(
                    text = "Datos del pedido",
                    style = MaterialTheme.typography.titleSmall,
                    color = TitleColor,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Entrega",
                    style = MaterialTheme.typography.bodySmall,
                    color = BodyColor
                )
                TabRow(
                    selectedTabIndex = if (selectedDeliveryZone == DeliveryZone.Lima) 0 else 1,
                    containerColor = Color.White,
                    divider = { HorizontalDivider(color = BrandBorder) }
                ) {
                    Tab(
                        selected = selectedDeliveryZone == DeliveryZone.Lima,
                        onClick = { onDeliveryZoneChange(DeliveryZone.Lima) },
                        selectedContentColor = AccentColor,
                        unselectedContentColor = BrandMuted,
                        text = { Text(DeliveryZone.Lima.label) }
                    )
                    Tab(
                        selected = selectedDeliveryZone == DeliveryZone.Provincia,
                        onClick = { onDeliveryZoneChange(DeliveryZone.Provincia) },
                        selectedContentColor = AccentColor,
                        unselectedContentColor = BrandMuted,
                        text = { Text(DeliveryZone.Provincia.label) }
                    )
                }

                if (showPurchaseOption) {
                    Text(
                        text = "Solicitud de compra",
                        style = MaterialTheme.typography.bodySmall,
                        color = BodyColor
                    )
                    TabRow(
                        selectedTabIndex = if (isPurchaseRequest) 1 else 0,
                        containerColor = Color.White,
                        divider = { HorizontalDivider(color = BrandBorder) }
                    ) {
                        Tab(
                            selected = !isPurchaseRequest,
                            onClick = { onPurchaseRequestChange(false) },
                            selectedContentColor = AccentColor,
                            unselectedContentColor = BrandMuted,
                            text = { Text("No") }
                        )
                        Tab(
                            selected = isPurchaseRequest,
                            onClick = { onPurchaseRequestChange(true) },
                            selectedContentColor = AccentColor,
                            unselectedContentColor = BrandMuted,
                            text = { Text("Si") }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    sections.forEachIndexed { sectionIndex, section ->
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = TitleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        section.items.forEachIndexed { itemIndex, item ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "${itemIndex + 1}. ${item.description} x ${item.quantity}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TitleColor
                                )
                                item.observations?.let {
                                    Text(
                                        text = "Obs: $it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BodyColor
                                    )
                                }
                                item.photoStatus?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = BodyColor
                                    )
                                }
                            }
                        }
                        if (sectionIndex < sections.lastIndex) {
                            HorizontalDivider(color = HeaderBorder)
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Volver")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirmar y registrar")
            }
        }
    )
}

@Composable
private fun RequestForm(
    section: RequestSectionState,
    options: List<InventoryOption>,
    submitAttempted: Boolean,
    expandedItemId: String?,
    onExpandedItemIdChange: (String?) -> Unit,
    onAddItem: () -> Unit,
    onDeleteItem: (Int) -> Unit,
    onQuantityChange: (Int, String) -> Unit,
    onDescriptionChange: (Int, InventoryOption) -> Unit,
    onObservationsChange: (Int, String) -> Unit,
    onPhotoChange: (Int, String?, Bitmap?) -> Unit,
    onSubmit: () -> Unit,
    enabledSubmit: Boolean,
    isSubmitting: Boolean,
    isGastoFlow: Boolean = false,
    singleItemFlow: Boolean = false
) {
    var pendingPhotoIndex by remember { mutableStateOf<Int?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        val index = pendingPhotoIndex ?: return@rememberLauncherForActivityResult
        onPhotoChange(index, uri?.toString(), null)
        pendingPhotoIndex = null
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        val index = pendingPhotoIndex ?: return@rememberLauncherForActivityResult
        onPhotoChange(index, null, bitmap)
        pendingPhotoIndex = null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (section.items.isEmpty()) {
            Column(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (!singleItemFlow) {
                    FilledTonalButton(
                        onClick = onAddItem,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentSoft,
                            contentColor = AccentColor
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Agregar")
                    }
                } else {
                    Text(
                        text = "Preparando formulario de botas...",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                section.items.forEachIndexed { index, item ->
                    MaterialItemCard(
                        itemNumber = index + 1,
                        item = item,
                        options = options,
                        expanded = expandedItemId == item.id,
                        onExpandedChange = { isExpanded ->
                            onExpandedItemIdChange(if (isExpanded) item.id else null)
                        },
                        onDelete = { onDeleteItem(index) },
                        onAdd = onAddItem,
                        onQuantityChange = { value -> onQuantityChange(index, value) },
                        onDescriptionChange = { value -> onDescriptionChange(index, value) },
                        onObservationsChange = { value -> onObservationsChange(index, value) },
                        onGalleryPhotoClick = {
                            pendingPhotoIndex = index
                            galleryLauncher.launch("image/*")
                        },
                        onCameraPhotoClick = {
                            pendingPhotoIndex = index
                            cameraLauncher.launch(null)
                        },
                        isGastoFlow = isGastoFlow,
                        showValidation = submitAttempted,
                        showAddButton = !singleItemFlow,
                        showDeleteButton = !singleItemFlow,
                        lockQuantity = singleItemFlow
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = enabledSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentColor,
                contentColor = Color.White,
                disabledContainerColor = Color(0xFFE4E7EC),
                disabledContentColor = Color(0xFF98A2B3)
            )
        ) {
            Text(if (isSubmitting) "Enviando..." else "Guardar")
        }
    }
}

private suspend fun submitCompleteRequest(
    api: com.example.myapplication.data.remote.network.ApiService,
    sections: List<RequestSectionState>,
    baseFields: SolicitudBaseFields,
    solicitanteUserId: Int,
    context: android.content.Context
): SubmitRequestResult {
    val multipartParts = mutableListOf<MultipartBody.Part>()
    sections.forEach { section ->
        appendSectionParts(
            section = section,
            context = context,
            multipartParts = multipartParts
        )
    }

    val hasValidProducts = multipartParts.any {
        it.headers?.toString()?.contains("id_producto_") == true
    }
    if (!hasValidProducts) {
        return SubmitRequestResult(
            success = false,
            message = "No hay productos validos para enviar."
        )
    }

    return try {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("id_usuario_solicitante", solicitanteUserId.toString())
            .addFormDataPart("es_pedido_compra", if (baseFields.esPedidoCompra) "1" else "0")
            .addFormDataPart("ubicacion", baseFields.ubicacion)
            .addFormDataPart("justificacion", baseFields.justificacion)
            .addFormDataPart("fecha_necesaria", baseFields.fechaNecesaria)
            .addFormDataPart("id_direccion_entrega", baseFields.idDireccionEntrega)

        multipartParts.forEach { requestBody.addPart(it) }
        logSolicitudPayload(
            solicitanteUserId = solicitanteUserId,
            baseFields = baseFields,
            multipartParts = multipartParts
        )

        val response = api.registrarSolicitudCompleta(
            multipartBody = requestBody.build()
        )

        if (!response.isSuccessful) {
            val errorBody = response.errorBody()?.string()
            val backendMessage = extractBackendMessage(errorBody)
            return SubmitRequestResult(
                success = false,
                message = backendMessage ?: "No se pudo registrar la solicitud (${response.code()})."
            )
        }

        val bodyObj = response.body()?.asJsonObject
        val backendSuccess = bodyObj?.get("success").asBooleanOrNull() ?: true
        val backendMessage = bodyObj?.get("message").asNonBlankStringOrNull()
            ?: "Solicitud registrada correctamente."
        val ticket = bodyObj?.get("ticket").asNonBlankStringOrNull()

        if (!backendSuccess) {
            return SubmitRequestResult(
                success = false,
                message = backendMessage
            )
        }

        SubmitRequestResult(
            success = true,
            message = if (ticket.isNullOrBlank()) backendMessage else "$backendMessage Ticket: $ticket"
        )
    } catch (e: Exception) {
        SubmitRequestResult(
            success = false,
            message = "Error enviando solicitud: ${e.message.orEmpty()}"
        )
    }
}

private suspend fun submitSolicitudGastoGeneralRequest(
    api: com.example.myapplication.data.remote.network.ApiService,
    sections: List<RequestSectionState>,
    solicitanteUserId: Int,
    context: android.content.Context,
    areaId: Int,
    motivo: String,
    fallbackPrefix: String,
    defaultErrorMessage: String,
    backendErrorMessage: String,
    backendSuccessMessage: String
): SubmitRequestResult {
    val validItems = mutableListOf<Triple<Int, MaterialItemForm, Pair<Int, Int>>>()
    var detailIndex = 0
    sections.forEach { section ->
        section.items.forEach { item ->
            val productId = item.selectedInventoryId ?: return@forEach
            val cantidad = item.quantity.toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
            validItems += Triple(detailIndex, item, productId to cantidad)
            detailIndex += 1
        }
    }

    if (validItems.isEmpty()) {
        return SubmitRequestResult(
            success = false,
            message = defaultErrorMessage
        )
    }

    return try {
        val parts = mutableListOf<MultipartBody.Part>()
        appendSolicitudGastoBaseParts(
            parts = parts,
            solicitanteUserId = solicitanteUserId,
            areaId = areaId,
            motivo = motivo
        )

        validItems.forEach { (index, item, idCantidad) ->
            val (productId, cantidad) = idCantidad
            appendSolicitudGastoDetalleParts(
                parts = parts,
                detailIndex = index,
                idProducto = productId,
                cantidad = cantidad,
                precioEstimado = 0.0,
                precioReal = 0.0,
                descripcionAdicional = item.observations.takeIf { it.isNotBlank() }
                    ?: inferBootDescription(item.description)
            )
            val archivoPart = buildSolicitudGastoArchivoPart(
                context = context,
                item = item,
                detailIndex = index,
                fallbackPrefix = fallbackPrefix
            )
            if (item.hasAttachedPhoto()) {
                if (archivoPart == null) {
                    return SubmitRequestResult(
                        success = false,
                        message = "Adjunta una imagen o PDF valido (max 10MB) para cada detalle."
                    )
                }
                parts += archivoPart
            }
        }

        val response = api.registrarSolicitudGasto(parts)
        if (!response.isSuccessful) {
            val backendMessage = response.errorBody()?.string()?.let { extractBackendMessage(it) }
            return SubmitRequestResult(
                success = false,
                message = backendMessage ?: "$backendErrorMessage (${response.code()})."
            )
        }

        val bodyObj = response.body()?.asJsonObject
        val backendSuccess = bodyObj?.get("success").asBooleanOrNull() ?: true
        val backendMessage = bodyObj?.get("message").asNonBlankStringOrNull()
            ?: backendSuccessMessage

        if (!backendSuccess) {
            SubmitRequestResult(success = false, message = backendMessage)
        } else {
            SubmitRequestResult(success = true, message = backendMessage)
        }
    } catch (e: Exception) {
        SubmitRequestResult(
            success = false,
            message = "Error enviando solicitud: ${e.message.orEmpty()}"
        )
    }
}

private fun inferBootDescription(label: String): String {
    val normalized = label.trim()
    return when {
        normalized.contains("TALLA", ignoreCase = true) -> normalized
        normalized.isBlank() -> "Botas de seguridad"
        else -> normalized
    }
}

private fun appendSolicitudGastoBaseParts(
    parts: MutableList<MultipartBody.Part>,
    solicitanteUserId: Int,
    areaId: Int,
    motivo: String
) {
    val fechaSolicitud = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    parts += MultipartBody.Part.createFormData("staff_id", solicitanteUserId.toString())
    parts += MultipartBody.Part.createFormData("id_area", areaId.toString())
    parts += MultipartBody.Part.createFormData("motivo", motivo)
    parts += MultipartBody.Part.createFormData("monto_estimado", "0.0")
    parts += MultipartBody.Part.createFormData("monto_real", "0.0")
    parts += MultipartBody.Part.createFormData("estado", "pendiente")
    parts += MultipartBody.Part.createFormData("fecha_solicitud", fechaSolicitud)
}

private fun appendSolicitudGastoDetalleParts(
    parts: MutableList<MultipartBody.Part>,
    detailIndex: Int,
    idProducto: Int,
    cantidad: Int,
    precioEstimado: Double?,
    precioReal: Double?,
    descripcionAdicional: String?
) {
    parts += MultipartBody.Part.createFormData(
        "solicitud_gasto_detalles[$detailIndex][id_producto]",
        idProducto.toString()
    )
    parts += MultipartBody.Part.createFormData(
        "solicitud_gasto_detalles[$detailIndex][cantidad]",
        cantidad.toString()
    )
    precioEstimado?.let {
        parts += MultipartBody.Part.createFormData(
            "solicitud_gasto_detalles[$detailIndex][precio_estimado]",
            it.toString()
        )
    }
    precioReal?.let {
        parts += MultipartBody.Part.createFormData(
            "solicitud_gasto_detalles[$detailIndex][precio_real]",
            it.toString()
        )
    }
    descripcionAdicional?.takeIf { it.isNotBlank() }?.let {
        parts += MultipartBody.Part.createFormData(
            "solicitud_gasto_detalles[$detailIndex][descripcion_adicional]",
            it
        )
    }
}

private fun buildSolicitudGastoArchivoPart(
    context: android.content.Context,
    item: MaterialItemForm,
    detailIndex: Int,
    fallbackPrefix: String
): MultipartBody.Part? {
    val partName = "solicitud_gasto_detalles[$detailIndex][archivo]"
    val maxBytes = 10L * 1024L * 1024L
    val allowedMime = setOf(
        "image/jpeg",
        "image/jpg",
        "image/png",
        "image/webp",
        "application/pdf"
    )

    item.photoBitmap?.let { bitmap ->
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        val bytes = output.toByteArray()
        if (bytes.isEmpty() || bytes.size > maxBytes) return null
        val fileName = "${fallbackPrefix}_detalle_${detailIndex + 1}.jpg"
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(partName, fileName, requestBody)
    }

    val photoUri = item.photoUri ?: return null
    val uri = Uri.parse(photoUri)
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
    if (bytes.isEmpty() || bytes.size > maxBytes) return null

    val mimeTypeRaw = resolver.getType(uri)?.lowercase()
    val fileName = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
        ?: "${fallbackPrefix}_detalle_${detailIndex + 1}"
    val extension = fileName.substringAfterLast('.', "").lowercase()
    val mimeFromExt = when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        else -> null
    }
    val mimeType = mimeTypeRaw ?: mimeFromExt ?: return null
    if (mimeType !in allowedMime) return null

    val finalFileName = if ('.' in fileName) fileName else {
        val ext = when (mimeType) {
            "image/jpeg", "image/jpg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            else -> "pdf"
        }
        "$fileName.$ext"
    }
    val requestBody = bytes.toRequestBody(mimeType.toMediaType())
    return MultipartBody.Part.createFormData(partName, finalFileName, requestBody)
}

private fun appendSectionParts(
    section: RequestSectionState,
    context: android.content.Context,
    multipartParts: MutableList<MultipartBody.Part>
) {
    section.items.forEachIndexed { index, item ->
        val inventoryId = item.selectedInventoryId ?: return@forEachIndexed
        val quantityValue = item.quantity.toIntOrNull()?.takeIf { it > 0 } ?: return@forEachIndexed
        val category = section.tab.submitCategoryKey
        Log.d(
            SOLICITUD_LOG_TAG,
            "item[$index] categoria=$category id_producto=$inventoryId cantidad=$quantityValue id_area=${item.selectedAreaId} observacion='${item.observations}' fotoAdjunta=${item.hasAttachedPhoto()}"
        )

        multipartParts += MultipartBody.Part.createFormData(
            "id_producto_${category}[]",
            inventoryId.toString()
        )
        multipartParts += MultipartBody.Part.createFormData(
            "cantidad_${category}[]",
            quantityValue.toString()
        )
        multipartParts += MultipartBody.Part.createFormData(
            "observacion_${category}[]",
            item.observations
        )
        item.selectedAreaId?.let { areaId ->
            multipartParts += MultipartBody.Part.createFormData(
                "id_area[]",
                areaId.toString()
            )
        }

        multipartParts += buildPhotoPartOrEmpty(
            item = item,
            context = context,
            partName = "foto_${category}[]",
            fallbackFileName = "sol_${category}_${inventoryId}_$index.jpg"
        )
    }
}

private fun buildPhotoPartOrEmpty(
    item: MaterialItemForm,
    context: android.content.Context,
    partName: String,
    fallbackFileName: String
): MultipartBody.Part {
    item.photoBitmap?.let { bitmap ->
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)
        val bytes = output.toByteArray()
        val requestBody = bytes.toRequestBody("image/jpeg".toMediaType())
        return MultipartBody.Part.createFormData(partName, fallbackFileName, requestBody)
    }

    val photoUri = item.photoUri ?: return emptyFilePart(partName)
    val uri = Uri.parse(photoUri)
    val resolver = context.contentResolver
    val bytes = resolver.openInputStream(uri)?.use { input -> input.readBytes() }
        ?: return emptyFilePart(partName)
    val mimeType = resolver.getType(uri) ?: "image/jpeg"
    val fileName = uri.lastPathSegment
        ?.substringAfterLast('/')
        ?.takeIf { it.isNotBlank() }
        ?: fallbackFileName

    val requestBody = bytes.toRequestBody(mimeType.toMediaType())
    return MultipartBody.Part.createFormData(partName, fileName, requestBody)
}

private fun emptyFilePart(partName: String): MultipartBody.Part {
    val emptyBody = ByteArray(0).toRequestBody("application/octet-stream".toMediaType())
    return MultipartBody.Part.createFormData(partName, "", emptyBody)
}

private fun extractBackendMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return runCatching {
        val obj = JsonParser().parse(raw).asJsonObject
        obj.get("message").asNonBlankStringOrNull()
    }.getOrNull()
}

private fun logSolicitudPayload(
    solicitanteUserId: Int,
    baseFields: SolicitudBaseFields,
    multipartParts: List<MultipartBody.Part>
) {
    Log.d(SOLICITUD_LOG_TAG, "POST /api/solicitudes/registrar-completa")
    Log.d(
        SOLICITUD_LOG_TAG,
        "base id_usuario_solicitante=$solicitanteUserId es_pedido_compra=${if (baseFields.esPedidoCompra) 1 else 0} ubicacion='${baseFields.ubicacion}' justificacion='${baseFields.justificacion}' fecha_necesaria='${baseFields.fechaNecesaria}' id_direccion_entrega='${baseFields.idDireccionEntrega}'"
    )
    multipartParts.forEachIndexed { idx, part ->
        Log.d(SOLICITUD_LOG_TAG, "part[$idx] ${describeMultipartPart(part)}")
    }
}

private fun describeMultipartPart(part: MultipartBody.Part): String {
    val disposition = part.headers?.get("Content-Disposition").orEmpty()
    val name = Regex("name=\"([^\"]+)\"")
        .find(disposition)
        ?.groupValues
        ?.getOrNull(1)
        .orEmpty()
    val fileName = Regex("filename=\"([^\"]*)\"")
        .find(disposition)
        ?.groupValues
        ?.getOrNull(1)
    val contentType = part.body.contentType()?.toString().orEmpty()
    val contentLength = runCatching { part.body.contentLength() }.getOrDefault(-1L)
    return if (!fileName.isNullOrBlank()) {
        "name=$name filename=$fileName contentType=$contentType contentLength=$contentLength"
    } else {
        "name=$name contentType=$contentType contentLength=$contentLength"
    }
}

@Composable
private fun MaterialItemCard(
    itemNumber: Int,
    item: MaterialItemForm,
    options: List<InventoryOption>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onAdd: () -> Unit,
    onQuantityChange: (String) -> Unit,
    onDescriptionChange: (InventoryOption) -> Unit,
    onObservationsChange: (String) -> Unit,
    onGalleryPhotoClick: () -> Unit,
    onCameraPhotoClick: () -> Unit,
    isGastoFlow: Boolean = false,
    showValidation: Boolean,
    showAddButton: Boolean = true,
    showDeleteButton: Boolean = true,
    lockQuantity: Boolean = false
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val quantityValid = item.quantity.toIntOrNull()?.let { it > 0 } == true
    val quantityError = when {
        item.quantity.isBlank() && showValidation -> "Requerida"
        item.quantity.isNotBlank() && !quantityValid -> "Debe ser mayor a 0"
        else -> null
    }
    val descriptionError = if (
        showValidation &&
        (item.description.isBlank() || item.selectedInventoryId == null)
    ) {
        "Selecciona un item valido"
    } else {
        null
    }
    val photoRequiredError = if (
        showValidation &&
        item.requiresPreviousProductPhoto &&
        !item.hasAttachedPhoto()
    ) {
        "Debes adjuntar la foto del producto anterior"
    } else {
        null
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = AccentSoft,
                border = BorderStroke(1.dp, HeaderBorder)
            ) {
                Text(
                    text = "Item $itemNumber",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = AccentColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            if (showDeleteButton) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .width(44.dp)
                        .height(44.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DangerSoft,
                        border = BorderStroke(1.dp, Color(0xFFFECACA))
                    ) {
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(44.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar item",
                                tint = DangerColor
                            )
                        }
                    }
                }
            }
        }

        DescriptionDropdownField(
            value = item.description,
            options = options,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            onValueSelected = onDescriptionChange,
            isError = descriptionError != null,
            supportingText = descriptionError
        )

        OutlinedTextField(
            value = item.quantity,
            onValueChange = { value ->
                if (!lockQuantity) {
                    onQuantityChange(value.filter(Char::isDigit))
                }
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cantidad") },
            singleLine = true,
            enabled = !lockQuantity,
            isError = quantityError != null,
            supportingText = {
                if (lockQuantity) {
                    Text(text = "Cantidad fija: 1", color = BrandMuted)
                } else {
                    quantityError?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.clearFocus() }
            )
        )

        OutlinedTextField(
            value = item.observations,
            onValueChange = onObservationsChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Observaciones") },
            minLines = 2,
            maxLines = 3,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
            )
        )

        if (item.requiresPreviousProductPhoto) {
            Text(
                text = "Este item requiere foto del producto anterior.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted
            )

            PhotoAttachmentSection(
                photoUri = item.photoUri,
                photoBitmap = item.photoBitmap,
                onGalleryClick = onGalleryPhotoClick,
                onCameraClick = onCameraPhotoClick,
                isRequired = !isGastoFlow,
                requiredError = photoRequiredError
            )
        }

        if (showAddButton || showDeleteButton) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (showAddButton) {
                    FilledTonalButton(
                        onClick = onAdd,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = AccentSoft,
                            contentColor = AccentColor
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    }
                }

                if (showDeleteButton) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DangerColor
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoAttachmentSection(
    photoUri: String?,
    photoBitmap: Bitmap?,
    onGalleryClick: () -> Unit,
    onCameraClick: () -> Unit,
    isRequired: Boolean,
    requiredError: String?
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = Color.White,
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    photoBitmap != null -> Image(
                        bitmap = photoBitmap.asImageBitmap(),
                        contentDescription = "Foto adjunta",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    !photoUri.isNullOrBlank() -> AsyncImage(
                        model = Uri.parse(photoUri),
                        contentDescription = "Foto adjunta",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    else -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Sin foto",
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMuted,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Adjunta una imagen o toma una foto",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandMuted
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FilledTonalButton(
                onClick = onGalleryClick,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = AccentSoft,
                    contentColor = AccentColor
                )
            ) {
                Text("Galeria")
            }

            OutlinedButton(
                onClick = onCameraClick,
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BrandBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AccentColor
                )
            ) {
                Text("Camara")
            }
        }

        if (isRequired) {
            Text(
                text = if (requiredError == null) {
                    "Foto obligatoria para este producto."
                } else {
                    requiredError
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (requiredError == null) BrandMuted else MaterialTheme.colorScheme.error
            )
        }
    }
}

private fun extractInventoryOptionsForArea(
    root: JsonElement?,
    targetAreaId: Int,
    excludedProductIds: Set<Int> = emptySet()
): List<InventoryOption> {
    if (root == null || root.isJsonNull) return emptyList()

    // Fast path for payload:
    // { success, message, data: [{ id_area, producto, ... }] }
    val dataArray = root
        .takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.getAsJsonArray("data")

    if (dataArray != null) {
        fun collectOptions(filterByArea: Boolean): List<InventoryOption> {
            val options = linkedMapOf<Int, InventoryOption>()
            dataArray.forEach { item ->
                if (!item.isJsonObject) return@forEach
                val obj = item.asJsonObject
                val areaId = obj.get("id_area").asIntOrNull()
                if (filterByArea && areaId != null && areaId != targetAreaId) return@forEach

                val inventoryId = extractInventoryId(obj) ?: return@forEach
                val productId = obj.get("id_producto").asIntOrNull()
                if ((productId != null && productId in excludedProductIds) || inventoryId in excludedProductIds) {
                    return@forEach
                }
                val label = obj.get("producto").asNonBlankStringOrNull() ?: extractProductLabel(obj)
                val requiresPhoto = extractRequiresPhotoFlag(obj)
                if (!label.isNullOrBlank()) {
                    val previous = options[inventoryId]
                    options[inventoryId] = InventoryOption(
                        inventoryId = inventoryId,
                        areaId = areaId,
                        label = label,
                        requiresPreviousProductPhoto = (previous?.requiresPreviousProductPhoto == true) || requiresPhoto
                    )
                }
            }
            return options.values.toList()
        }

        // Prefer match by id_area, but if backend now groups by tipo_responsable
        // and returns another id_area, keep all returned items.
        val scoped = collectOptions(filterByArea = true)
        if (scoped.isNotEmpty()) return scoped

        val unscoped = collectOptions(filterByArea = false)
        if (unscoped.isNotEmpty()) return unscoped
    }

    // Fallback for non-standard payloads.
    return extractInventoryOptionsByArea(
        root = root,
        excludedProductIds = excludedProductIds
    )[targetAreaId].orEmpty()
}

private fun extractInventoryOptionsByArea(
    root: JsonElement?,
    excludedProductIds: Set<Int> = emptySet()
): Map<Int, List<InventoryOption>> {
    if (root == null || root.isJsonNull) return emptyMap()

    val collector = mutableMapOf<Int, LinkedHashMap<Int, InventoryOption>>()
    collectInventoryOptions(root, collector, excludedProductIds)

    return collector.mapValues { (_, value) -> value.values.toList() }
}

private fun collectInventoryOptions(
    element: JsonElement,
    collector: MutableMap<Int, LinkedHashMap<Int, InventoryOption>>,
    excludedProductIds: Set<Int>
) {
    when {
        element.isJsonArray -> {
            element.asJsonArray.forEach { child ->
                collectInventoryOptions(child, collector, excludedProductIds)
            }
        }

        element.isJsonObject -> {
            val obj = element.asJsonObject
            val areaId = obj.get("id_area").asIntOrNull()
            val inventoryId = extractInventoryId(obj)
            val productId = obj.get("id_producto").asIntOrNull()
            val label = extractProductLabel(obj)
            val requiresPhoto = extractRequiresPhotoFlag(obj)
            val isExcluded =
                (productId != null && productId in excludedProductIds) ||
                    (inventoryId != null && inventoryId in excludedProductIds)
            if (!isExcluded && areaId != null && inventoryId != null && !label.isNullOrBlank()) {
                val areaCollector = collector.getOrPut(areaId) { linkedMapOf() }
                val previous = areaCollector[inventoryId]
                areaCollector[inventoryId] = InventoryOption(
                    inventoryId = inventoryId,
                    areaId = areaId,
                    label = label,
                    requiresPreviousProductPhoto = (previous?.requiresPreviousProductPhoto == true) || requiresPhoto
                )
            }

            obj.entrySet().forEach { (_, child) ->
                collectInventoryOptions(child, collector, excludedProductIds)
            }
        }
    }
}

private fun extractRequiresPhotoFlag(obj: JsonObject): Boolean {
    val keys = listOf(
        "requiere_foto_producto_anterior",
        "requiere_foto",
        "requiere_imagen",
        "requires_photo",
        "requires_image",
        "foto_obligatoria",
        "imagen_obligatoria"
    )
    return keys.any { key -> obj.get(key).asBooleanOrNull() == true }
}

private fun extractInventoryId(obj: JsonObject): Int? {
    // For request payloads we must always send product id, never inventory id.
    return obj.get("id_producto").asIntOrNull()
}

private fun extractProductLabel(obj: JsonObject): String? {
    val candidateKeys = listOf(
        "nombre",
        "name",
        "descripcion",
        "description",
        "producto",
        "nombre_producto",
        "nombre_item",
        "producto_nombre",
        "item_nombre",
        "item"
    )

    for (key in candidateKeys) {
        val text = obj.get(key).asNonBlankStringOrNull()
        if (text != null) return text
    }

    return null
}

private fun JsonElement?.asIntOrNull(): Int? {
    if (this == null || isJsonNull || !isJsonPrimitive) return null
    return runCatching { asInt }.getOrNull()
}

private fun JsonElement?.asBooleanOrNull(): Boolean? {
    if (this == null || isJsonNull || !isJsonPrimitive) return null

    val primitive = asJsonPrimitive
    if (primitive.isBoolean) return primitive.asBoolean
    if (primitive.isNumber) return primitive.asInt != 0
    if (primitive.isString) {
        return when (primitive.asString.trim().lowercase()) {
            "true", "1", "si", "sí", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
    }

    return null
}

private fun JsonElement?.asNonBlankStringOrNull(): String? {
    if (this == null || isJsonNull || !isJsonPrimitive) return null
    return runCatching { asString.trim() }.getOrNull()?.takeIf { it.isNotBlank() }
}

private fun MaterialItemForm.hasAttachedPhoto(): Boolean {
    return photoBitmap != null || !photoUri.isNullOrBlank()
}

@Composable
private fun DescriptionDropdownField(
    value: String,
    options: List<InventoryOption>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onValueSelected: (InventoryOption) -> Unit,
    isError: Boolean,
    supportingText: String?
) {
    var query by remember(value) { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val filteredCandidates = remember(query, options) {
        val sequence = if (query.isBlank()) {
            options.asSequence()
        } else {
            options.asSequence().filter { it.label.contains(query, ignoreCase = true) }
        }
        sequence.take(MAX_DROPDOWN_OPTIONS + 1).toList()
    }
    val filteredOptions = filteredCandidates.take(MAX_DROPDOWN_OPTIONS)
    val hasMoreResults = filteredCandidates.size > MAX_DROPDOWN_OPTIONS

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val menuWidth = maxWidth

        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandedChange(!expanded) },
                placeholder = { Text("Seleccionar item") },
                trailingIcon = {
                    IconButton(onClick = { onExpandedChange(!expanded) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null
                        )
                    }
                },
                readOnly = true,
                singleLine = true,
                isError = isError,
                supportingText = {
                    supportingText?.let {
                        Text(text = it, color = MaterialTheme.colorScheme.error)
                    }
                },
                shape = RoundedCornerShape(18.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    query = ""
                    onExpandedChange(false)
                },
                modifier = Modifier.width(menuWidth)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Buscar") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                        ),
                        shape = RoundedCornerShape(14.dp)
                    )

                    if (filteredOptions.isEmpty()) {
                        Text(
                            text = "Sin resultados",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp),
                            color = BrandMuted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            filteredOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.label,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    onClick = {
                                        onValueSelected(option)
                                        query = ""
                                        onExpandedChange(false)
                                    }
                                )
                            }
                        }

                        if (hasMoreResults) {
                            Text(
                                text = "Mostrando primeros $MAX_DROPDOWN_OPTIONS resultados. Escribe para refinar.",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = BrandMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

