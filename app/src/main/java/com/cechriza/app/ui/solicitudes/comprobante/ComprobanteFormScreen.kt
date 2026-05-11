package com.cechriza.app.ui.solicitudes.comprobante

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.cechriza.app.data.remote.solicitudes.SolicitudesRemoteDataSource
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComprobanteFormScreen(
    navController: NavHostController,
    initialSolicitudId: Int?,
    onRegistered: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var solicitudId by remember { mutableStateOf(initialSolicitudId?.toString().orEmpty()) }
    var tipo by remember { mutableStateOf("") }
    var numero by remember { mutableStateOf("") }
    var monto by remember { mutableStateOf("") }
    var archivoUri by remember { mutableStateOf<Uri?>(null) }
    var archivoNombre by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        archivoUri = uri
        archivoNombre = uri?.let { resolveFileName(context, it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BrandSurface,
        topBar = {
            TopAppBar(
                title = { Text("Registrar comprobante") },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text("Volver")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Completa los datos del comprobante y adjunta el archivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandMuted
            )

            OutlinedTextField(
                value = solicitudId,
                onValueChange = { solicitudId = it.filter(Char::isDigit) },
                label = { Text("Solicitud gasto ID") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSubmitting && initialSolicitudId == null
            )

            OutlinedTextField(
                value = tipo,
                onValueChange = { tipo = it },
                label = { Text("Tipo") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSubmitting
            )

            OutlinedTextField(
                value = numero,
                onValueChange = { numero = it },
                label = { Text("Numero") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSubmitting
            )

            OutlinedTextField(
                value = monto,
                onValueChange = { monto = it },
                label = { Text("Monto") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isSubmitting
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .clickable(enabled = !isSubmitting) {
                        filePicker.launch(arrayOf("image/jpeg", "image/png", "image/webp", "application/pdf"))
                    }
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Archivo",
                    style = MaterialTheme.typography.titleSmall,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = archivoNombre ?: "Toca aqui para seleccionar JPG, PNG, WEBP o PDF.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (archivoNombre == null) BrandMuted else BrandText
                )
                Text(
                    text = "Maximo 10MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            Button(
                onClick = {
                    val solicitudIdValue = solicitudId.toIntOrNull()
                    val montoNormalizado = monto.trim().replace(',', '.')
                    val archivoSeleccionado = archivoUri
                    val validationMessage = when {
                        solicitudIdValue == null || solicitudIdValue <= 0 -> "Ingresa un solicitud_gasto_id valido."
                        tipo.trim().isBlank() -> "Ingresa el tipo del comprobante."
                        numero.trim().isBlank() -> "Ingresa el numero del comprobante."
                        montoNormalizado.toDoubleOrNull() == null -> "Ingresa un monto valido."
                        archivoSeleccionado == null -> "Selecciona un archivo."
                        else -> null
                    }
                    if (validationMessage != null) {
                        scope.launch { snackbarHostState.showSnackbar(validationMessage) }
                        return@Button
                    }
                    val solicitudIdFinal = solicitudIdValue ?: return@Button
                    val archivoFinal = archivoSeleccionado ?: return@Button

                    isSubmitting = true
                    scope.launch {
                        try {
                            val response = withContext(Dispatchers.IO) {
                                SolicitudesRemoteDataSource.registrarSolicitudGastoComprobante(
                                    context = context,
                                    solicitudGastoId = solicitudIdFinal,
                                    tipo = tipo.trim(),
                                    numero = numero.trim(),
                                    monto = montoNormalizado,
                                    archivoUri = archivoFinal
                                )
                            }
                            if (response.isSuccessful) {
                                snackbarHostState.showSnackbar(
                                    extractBackendMessage(response.body())
                                        ?: "Comprobante registrado correctamente."
                                )
                                onRegistered()
                            } else {
                                val errorMessage = response.errorBody()?.string()
                                snackbarHostState.showSnackbar(
                                    extractBackendMessage(errorMessage)
                                        ?: "No se pudo registrar el comprobante (${response.code()})."
                                )
                            }
                        } catch (error: IllegalArgumentException) {
                            snackbarHostState.showSnackbar(error.message ?: "No se pudo registrar el comprobante.")
                        } catch (_: Exception) {
                            snackbarHostState.showSnackbar("Sin conexion para registrar el comprobante.")
                        } finally {
                            isSubmitting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue, contentColor = Color.White)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("Registrar comprobante")
                }
            }

            Text(
                text = "El envio usa tu sesion actual con token bearer.",
                style = MaterialTheme.typography.bodySmall,
                color = BrandMuted,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private fun resolveFileName(context: android.content.Context, uri: Uri): String {
    return context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst() && nameIndex >= 0) {
            cursor.getString(nameIndex)
        } else {
            uri.lastPathSegment?.substringAfterLast('/')
        }
    } ?: uri.lastPathSegment?.substringAfterLast('/') ?: "archivo"
}

private fun extractBackendMessage(body: JsonElement?): String? {
    return try {
        val messageElement = body?.asJsonObject?.get("message")
        if (messageElement == null || messageElement.isJsonNull) {
            null
        } else {
            messageElement.asString.takeIf { text -> text.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
}

private fun extractBackendMessage(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    return try {
        val messageElement = JsonParser().parse(raw).asJsonObject.get("message")
        if (messageElement == null || messageElement.isJsonNull) {
            null
        } else {
            messageElement.asString.takeIf { text -> text.isNotBlank() }
        }
    } catch (_: Exception) {
        null
    }
}
