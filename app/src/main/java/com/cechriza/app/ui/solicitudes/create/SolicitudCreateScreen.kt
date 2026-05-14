package com.cechriza.app.ui.solicitudes.create

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.cechriza.app.data.model.solicitudes.SolicitudBaseFields
import com.cechriza.app.data.model.solicitudes.SubmitRequestResult
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBlueDark
import com.cechriza.app.ui.home.BrandBlueSoft
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandOrange
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.AppHeader
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.solicitudes.SolicitudesRemoteDataSource
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
    val isNewProduct: Boolean = false,
    val newProductName: String = "",
    val description: String = "",
    val requiresPreviousProductPhoto: Boolean = false,
    val observations: String = "",
    val photoUri: String? = null,
    val photoBitmap: Bitmap? = null
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
            val productName = if (item.isNewProduct && item.newProductName.isNotBlank()) {
                item.newProductName.trim()
            } else {
                item.description
            }
            val photoStatus = if (!item.requiresPreviousProductPhoto) {
                null
            } else if (item.hasAttachedPhoto()) {
                "Foto del producto anterior: adjunta"
            } else {
                "Foto del producto anterior: pendiente"
            }

            RequestConfirmationItem(
                quantity = quantity,
                description = productName,
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
    esPedidoCompra: Boolean
): SolicitudBaseFields {
    return SolicitudBaseFields(
        justificacion = DEFAULT_JUSTIFICACION,
        fechaNecesaria = LocalDate.now().plusDays(7).toString(),
        idDireccionEntrega = DEFAULT_ID_DIRECCION_ENTREGA_LIMA,
        esPedidoCompra = esPedidoCompra,
        ubicacion = "LIMA"
    )
}

@Composable
fun SolicitudCreateScreen(
    modifier: Modifier = Modifier,
    onHomeClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onRegisterSuccess: () -> Unit = {},
    initialPreset: String? = null,
    screenTitle: String = "Solicitudes"
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
    var showSuccessSheet by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("Solicitud registrada correctamente.") }
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
            val response = withContext(Dispatchers.IO) {
                SolicitudesRemoteDataSource.getInventarioProductos(targetResponsable)
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
    val activeSections = if (isGastoFlow) {
        listOf(materialsSection, toolsSection, eppSection)
    } else {
        when (selectedTab) {
            RequestTab.Materials -> listOf(materialsSection)
            RequestTab.Tools -> listOf(toolsSection)
            RequestTab.Epp -> listOf(eppSection)
        }
    }

    LaunchedEffect(isGastoFlow) {
        if (!isGastoFlow) return@LaunchedEffect
        val gastoTabs = listOf(RequestTab.Materials, RequestTab.Tools, RequestTab.Epp)
        gastoTabs.forEach { tab ->
            val targetAreaId = tab.areaId
            val targetResponsable = tab.inventoryResponsable
            if (optionsByArea.containsKey(targetAreaId)) return@forEach

            loadingAreaId = targetAreaId
            optionsErrorByArea.remove(targetAreaId)

            try {
                val response = withContext(Dispatchers.IO) {
                    SolicitudesRemoteDataSource.getInventarioProductos(targetResponsable)
                }

                if (response.isSuccessful) {
                    val remoteOptions = withContext(Dispatchers.Default) {
                        extractInventoryOptionsForArea(
                            root = response.body(),
                            targetAreaId = targetAreaId,
                            excludedProductIds = if (tab == RequestTab.Epp && isAlmacenFlow) {
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
    }
    val allItems = allSections.flatMap { it.items }
    val canSubmit = totalItems > 0 && allItems.all {
        it.quantity.toIntOrNull()?.let { value -> value > 0 } == true &&
            (
                if (isGastoFlow && it.isNewProduct) {
                    it.newProductName.isNotBlank()
                } else {
                    it.selectedInventoryId != null && it.description.isNotBlank()
                }
            ) &&
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
                val api = SolicitudesRemoteDataSource.authenticatedApi()
            val baseFields = defaultSolicitudBaseFields(
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
                        motivo = "Solicitud general de economato",
                        fallbackPrefix = "general",
                        defaultErrorMessage = "No hay items validos para registrar en economato.",
                        backendErrorMessage = "No se pudo registrar la solicitud de economato",
                        backendSuccessMessage = "Solicitud de economato registrada correctamente."
                    )
                } else {
                    submitCompleteRequest(
                        api = api,
                        sections = allSections,
                        baseFields = baseFields,
                        solicitanteUserId = solicitanteUserId,
                        context = context,
                        allowNewProduct = false
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
                successMessage = result.message.ifBlank { "Solicitud registrada correctamente." }
                showSuccessSheet = true
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
                title = screenTitle,
                showBackButton = true,
                showNotificationButton = false,
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
                    val flowLabel = when {
                        isEppOnlyFlow -> "Solicitud de Botas de seguridad"
                        isGastoFlow -> "Solicitud de Compras"
                        isAlmacenFlow -> "Solicitud de Economato"
                        else -> "Nueva solicitud"
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, BrandBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = flowLabel,
                                style = MaterialTheme.typography.titleSmall,
                                color = BrandText,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Paso 2 de 3 · Completa los datos del pedido",
                                style = MaterialTheme.typography.bodySmall,
                                color = BrandMuted
                            )
                        }
                    }
                }

                if (!isGastoFlow) {
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
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = TitleColor,
                                fontWeight = FontWeight.SemiBold
                            )

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
                                            isNewProduct = false,
                                            newProductName = "",
                                            description = value.label,
                                            requiresPreviousProductPhoto = value.requiresPreviousProductPhoto
                                        )
                                    }
                                },
                                onUseNewProductChange = { index, checked ->
                                    if (index in section.items.indices) {
                                        val current = section.items[index]
                                        section.items[index] = current.copy(
                                            isNewProduct = checked,
                                            selectedInventoryId = if (checked) null else current.selectedInventoryId,
                                            selectedAreaId = if (checked) null else current.selectedAreaId,
                                            description = if (checked) "" else current.description,
                                            requiresPreviousProductPhoto = if (checked) false else current.requiresPreviousProductPhoto
                                        )
                                    }
                                },
                                onNewProductNameChange = { index, value ->
                                    if (index in section.items.indices) {
                                        section.items[index] = section.items[index].copy(newProductName = value)
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
                                onCameraLaunchError = { message ->
                                    scope.launch { snackbarHostState.showSnackbar(message) }
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
                                singleItemFlow = isEppOnlyFlow && selectedTab == RequestTab.Epp,
                                allowNewProduct = isGastoFlow
                            )
                        }
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
                    isPurchaseRequest = isPurchaseRequest,
                    onPurchaseRequestChange = { isPurchaseRequest = it },
                    showPurchaseOption = false,
                    onDismiss = { showConfirmDialog = false },
                    onConfirm = submitConfirmedRequest
                )
            }

            if (showSuccessSheet) {
                RegistrationSuccessSheet(
                    message = successMessage,
                    onClose = {
                        showSuccessSheet = false
                        onRegisterSuccess()
                    }
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
    isPurchaseRequest: Boolean,
    onPurchaseRequestChange: (Boolean) -> Unit,
    showPurchaseOption: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color.White,
            border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.88f)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Confirmar solicitud",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = TitleColor,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Revisa el resumen antes de enviarlo.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = BodyColor
                                )
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Cerrar",
                                    tint = BrandMuted
                                )
                            }
                        }
                    }

                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = BrandSurface,
                            border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.8f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Resumen",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = BrandMuted
                                    )
                                    Text(
                                        text = "$totalItems item${if (totalItems == 1) "" else "s"} listo${if (totalItems == 1) "" else "s"}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = TitleColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = AccentSoft
                                ) {
                                    Text(
                                        text = "Paso final",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    item {
                        if (showPurchaseOption) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.75f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "Solicitud de compra",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = TitleColor,
                                        fontWeight = FontWeight.SemiBold
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
                            }
                        }
                    }

                    sections.forEach { section ->
                        item {
                            RequestConfirmationSectionCard(section = section)
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.White,
                    shadowElevation = 10.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .navigationBarsPadding(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConfirm,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text(
                                text = "Confirmar solicitud",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Volver", color = BrandMuted)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationSuccessSheet(
    message: String,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFECFDF3)
            ) {
                Box(
                    modifier = Modifier
                        .width(72.dp)
                        .height(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF039855)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Registro exitoso",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TitleColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = BodyColor,
                    textAlign = TextAlign.Center
                )
            }

            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentColor,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = "Continuar",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun RequestConfirmationSectionCard(section: RequestConfirmationSection) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = section.title,
                style = MaterialTheme.typography.titleSmall,
                color = TitleColor,
                fontWeight = FontWeight.Bold
            )
            section.items.forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = BrandSurface
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = item.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TitleColor,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Cantidad: ${item.quantity}",
                            style = MaterialTheme.typography.bodySmall,
                            color = BodyColor
                        )
                        item.observations?.let {
                            Text(
                                text = "Observacion: $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = BodyColor
                            )
                        }
                        item.photoStatus?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (it.contains("pendiente", ignoreCase = true)) BrandOrange else AccentColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                if (index < section.items.lastIndex) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
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
    onUseNewProductChange: (Int, Boolean) -> Unit,
    onNewProductNameChange: (Int, String) -> Unit,
    onObservationsChange: (Int, String) -> Unit,
    onPhotoChange: (Int, String?, Bitmap?) -> Unit,
    onCameraLaunchError: (String) -> Unit,
    onSubmit: () -> Unit,
    enabledSubmit: Boolean,
    isSubmitting: Boolean,
    isGastoFlow: Boolean = false,
    singleItemFlow: Boolean = false,
    allowNewProduct: Boolean = false
) {
    val context = LocalContext.current
    var pendingPhotoIndex by remember { mutableStateOf<Int?>(null) }
    var pendingCameraPermissionIndex by remember { mutableStateOf<Int?>(null) }

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

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val index = pendingCameraPermissionIndex
        pendingCameraPermissionIndex = null
        if (!granted) {
            onCameraLaunchError("Permiso de camara denegado. Habilitalo en ajustes para tomar foto.")
            return@rememberLauncherForActivityResult
        }
        if (index == null) return@rememberLauncherForActivityResult
        pendingPhotoIndex = index
        runCatching { cameraLauncher.launch(null) }
            .onFailure {
                pendingPhotoIndex = null
                onCameraLaunchError("No se pudo abrir la camara en este dispositivo.")
            }
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
                        onUseNewProductChange = { checked -> onUseNewProductChange(index, checked) },
                        onNewProductNameChange = { value -> onNewProductNameChange(index, value) },
                        onObservationsChange = { value -> onObservationsChange(index, value) },
                        onGalleryPhotoClick = {
                            pendingPhotoIndex = index
                            galleryLauncher.launch("image/*")
                        },
                        onCameraPhotoClick = {
                            val canHandleCameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                .resolveActivity(context.packageManager) != null
                            if (!canHandleCameraIntent) {
                                onCameraLaunchError("No se encontro una app de camara disponible.")
                                return@MaterialItemCard
                            }
                            val hasCameraPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!hasCameraPermission) {
                                pendingCameraPermissionIndex = index
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                return@MaterialItemCard
                            }
                            pendingPhotoIndex = index
                            runCatching { cameraLauncher.launch(null) }
                                .onFailure {
                                    pendingPhotoIndex = null
                                    onCameraLaunchError("No se pudo abrir la camara en este dispositivo.")
                                }
                        },
                        isGastoFlow = isGastoFlow,
                        allowNewProduct = allowNewProduct,
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
    api: com.cechriza.app.data.remote.network.ApiService,
    sections: List<RequestSectionState>,
    baseFields: SolicitudBaseFields,
    solicitanteUserId: Int,
    context: android.content.Context,
    allowNewProduct: Boolean
): SubmitRequestResult {
    val multipartParts = mutableListOf<MultipartBody.Part>()
    sections.forEach { section ->
        appendSectionParts(
            section = section,
            context = context,
            multipartParts = multipartParts,
            allowNewProduct = allowNewProduct
        )
    }

    val hasValidProducts = multipartParts.any {
        val headers = it.headers?.toString().orEmpty()
        headers.contains("id_producto_") || headers.contains("nuevo_producto")
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
    api: com.cechriza.app.data.remote.network.ApiService,
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
    val validItems = mutableListOf<Pair<Int, MaterialItemForm>>()
    var detailIndex = 0
    sections.forEach { section ->
        section.items.forEach { item ->
            val cantidad = item.quantity.toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
            val hasSelectedProduct = item.selectedInventoryId != null
            val hasNewProduct = item.isNewProduct && item.newProductName.isNotBlank()
            if (!hasSelectedProduct && !hasNewProduct) return@forEach
            validItems += detailIndex to item
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

        validItems.forEach { (index, item) ->
            val cantidad = item.quantity.toIntOrNull()?.takeIf { it > 0 } ?: return@forEach
            appendSolicitudGastoDetalleParts(
                parts = parts,
                detailIndex = index,
                idProducto = item.selectedInventoryId,
                nuevoProducto = item.newProductName.takeIf { item.isNewProduct && it.isNotBlank() },
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
    idProducto: Int?,
    nuevoProducto: String?,
    cantidad: Int,
    precioEstimado: Double?,
    precioReal: Double?,
    descripcionAdicional: String?
) {
    idProducto?.let {
        parts += MultipartBody.Part.createFormData(
            "solicitud_gasto_detalles[$detailIndex][id_producto]",
            it.toString()
        )
    }
    nuevoProducto?.takeIf { it.isNotBlank() }?.let {
        parts += MultipartBody.Part.createFormData(
            "solicitud_gasto_detalles[$detailIndex][nuevo_producto]",
            it.trim()
        )
    }
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
    multipartParts: MutableList<MultipartBody.Part>,
    allowNewProduct: Boolean
) {
    section.items.forEachIndexed { index, item ->
        val quantityValue = item.quantity.toIntOrNull()?.takeIf { it > 0 } ?: return@forEachIndexed
        val category = section.tab.submitCategoryKey
        val usingNewProduct = allowNewProduct && item.isNewProduct && item.newProductName.isNotBlank()
        val inventoryId = item.selectedInventoryId

        if (!usingNewProduct && inventoryId == null) return@forEachIndexed

        Log.d(
            SOLICITUD_LOG_TAG,
            "item[$index] categoria=$category id_producto=${inventoryId ?: "null"} nuevo_producto='${item.newProductName}' cantidad=$quantityValue id_area=${item.selectedAreaId} observacion='${item.observations}' fotoAdjunta=${item.hasAttachedPhoto()}"
        )

        if (usingNewProduct) {
            multipartParts += MultipartBody.Part.createFormData(
                "nuevo_producto[]",
                item.newProductName.trim()
            )
        } else if (inventoryId != null) {
            multipartParts += MultipartBody.Part.createFormData(
                "id_producto_${category}[]",
                inventoryId.toString()
            )
        }
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
    onUseNewProductChange: (Boolean) -> Unit,
    onNewProductNameChange: (String) -> Unit,
    onObservationsChange: (String) -> Unit,
    onGalleryPhotoClick: () -> Unit,
    onCameraPhotoClick: () -> Unit,
    isGastoFlow: Boolean = false,
    allowNewProduct: Boolean = false,
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
    val descriptionError = if (showValidation) {
        if (allowNewProduct && item.isNewProduct) {
            if (item.newProductName.isBlank()) "Ingresa el nombre del producto" else null
        } else {
            if (item.description.isBlank() || item.selectedInventoryId == null) "Selecciona un item valido" else null
        }
    } else null
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

        if (allowNewProduct) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = item.isNewProduct,
                    onCheckedChange = onUseNewProductChange
                )
                Text(
                    text = "No encuentro el producto en el listado",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TitleColor
                )
            }
        }

        if (allowNewProduct && item.isNewProduct) {
            OutlinedTextField(
                value = item.newProductName,
                onValueChange = onNewProductNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre del producto") },
                placeholder = { Text("Escribe el nombre del producto") },
                singleLine = true,
                isError = descriptionError != null,
                supportingText = descriptionError?.let { { Text(text = it, color = MaterialTheme.colorScheme.error) } },
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF3F4F6),
                    unfocusedContainerColor = Color(0xFFF3F4F6),
                    disabledContainerColor = Color(0xFFF3F4F6),
                    errorContainerColor = Color(0xFFF3F4F6)
                )
            )
        } else {
            DescriptionDropdownField(
                value = item.description,
                options = options,
                expanded = expanded,
                onExpandedChange = onExpandedChange,
                onValueSelected = onDescriptionChange,
                isError = descriptionError != null,
                supportingText = descriptionError
            )
        }

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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF3F4F6),
                disabledContainerColor = Color(0xFFF3F4F6),
                errorContainerColor = Color(0xFFF3F4F6)
            ),
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
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF3F4F6),
                unfocusedContainerColor = Color(0xFFF3F4F6),
                disabledContainerColor = Color(0xFFF3F4F6),
                errorContainerColor = Color(0xFFF3F4F6)
            ),
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

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val menuWidth = maxWidth

        Box(modifier = Modifier.fillMaxWidth()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clickable { onExpandedChange(!expanded) },
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF3F4F6),
                border = BorderStroke(
                    1.dp,
                    if (isError) MaterialTheme.colorScheme.error else Color(0xFFD8DEE9)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = value.ifBlank { "Seleccionar item" },
                        color = if (value.isBlank()) Color(0xFF6B7280) else Color(0xFF111827),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    IconButton(onClick = { onExpandedChange(!expanded) }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = Color(0xFF6B7280)
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    query = ""
                    onExpandedChange(false)
                },
                modifier = Modifier
                    .width(menuWidth)
                    .background(Color.White)
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
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        )
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
                                            maxLines = 2,
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
        supportingText?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}


