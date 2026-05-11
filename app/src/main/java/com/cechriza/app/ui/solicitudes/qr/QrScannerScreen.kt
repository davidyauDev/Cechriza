package com.cechriza.app.ui.solicitudes.qr

import android.Manifest
import android.content.pm.PackageManager
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.cechriza.app.data.preferences.SessionManager
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

private const val QR_DELIVERY_SCAN_URL = "https://osticket.cechriza.com/system/back/qr_entrega_mobile_scan.php"
private const val QR_DELIVERY_CLOSE_URL = "https://osticket.cechriza.com/system/back/qr_entrega_mobile_close.php"
private const val SOLICITUD_LIST_REFRESH_REQUESTS_KEY = "solicitud_list_refresh_requests_key"

@Composable
fun QrScannerScreen(
    navController: NavController,
    solicitudId: Int,
    qrToken: String?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isClosing by remember { mutableStateOf(false) }
    var responseDialogMessage by remember { mutableStateOf<String?>(null) }
    var responseDialogSuccess by remember { mutableStateOf<Boolean?>(null) }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        if (!hasCameraPermission) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Se requiere permiso de camara para escanear QR.")
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("Dar permiso")
                }
            }
            return@Surface
        }

        val previewView = remember { PreviewView(context) }
        val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
        val barcodeScanner = remember { BarcodeScanning.getClient() }
        val scanHandled = remember { AtomicBoolean(false) }

        DisposableEffect(lifecycleOwner, hasCameraPermission) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val listener = Runnable {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().apply {
                    surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(analyzerExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    if (scanHandled.get()) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            val value = barcodes.firstOrNull()?.rawValue
                            if (!value.isNullOrBlank() && scanHandled.compareAndSet(false, true)) {
                                cameraError = null
                                scannedValue = value
                                val token = qrToken?.trim().orEmpty()
                                if (token.isBlank()) {
                                    responseDialogMessage = "No hay qr_token para esta solicitud."
                                    return@addOnSuccessListener
                                }
                                val requestUrl = buildScanUrl(scannedRawValue = value.trim(), qrToken = token)
                                scope.launch {
                                    isSubmitting = true
                                    val response = submitQrScan(
                                        requestUrl = requestUrl
                                    )
                                    isSubmitting = false
                                    responseDialogSuccess = response.success
                                    responseDialogMessage = response.message
                                }
                            }
                        }
                        .addOnFailureListener {
                            cameraError = "No se pudo procesar el codigo QR."
                        }
                        .addOnCompleteListener {
                            imageProxy.close()
                        }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    analysis
                )
            }
            cameraProviderFuture.addListener(listener, ContextCompat.getMainExecutor(context))

            onDispose {
                runCatching { cameraProviderFuture.get().unbindAll() }
                runCatching { barcodeScanner.close() }
                analyzerExecutor.shutdown()
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(240.dp)
                    .border(
                        width = 3.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(20.dp)
                    )
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color.White.copy(alpha = 0.92f),
                tonalElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Escaner QR - Solicitud #$solicitudId",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Enfoca el codigo dentro del recuadro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475467)
                    )
                }
            }

            cameraError?.let { message ->
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFFEF3F2)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFB42318),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (isSubmitting) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Validando QR con servidor...")
                    }
                }
            }
        }
    }

    responseDialogMessage?.let { message ->
        val isSuccess = responseDialogSuccess == true
        AlertDialog(
            onDismissRequest = {
                responseDialogMessage = null
                responseDialogSuccess = null
                scannedValue = null
            },
            confirmButton = {
                TextButton(
                    enabled = !isClosing,
                    onClick = {
                        if (isSuccess) {
                            val token = qrToken?.trim().orEmpty()
                            val userId = SessionManager.staffId ?: SessionManager.userId
                            if (token.isBlank() || userId == null || userId <= 0) {
                                responseDialogSuccess = false
                                responseDialogMessage = "No se pudo confirmar la solicitud. Verifica tu sesion."
                                return@TextButton
                            }
                            scope.launch {
                                isClosing = true
                                val closeResponse = closeSolicitud(
                                    token = token,
                                    solicitudId = solicitudId,
                                    userId = userId
                                )
                                isClosing = false
                                if (closeResponse.success) {
                                    navController.previousBackStackEntry
                                        ?.savedStateHandle
                                        ?.set(SOLICITUD_LIST_REFRESH_REQUESTS_KEY, System.currentTimeMillis())
                                    navController.popBackStack()
                                } else {
                                    responseDialogSuccess = false
                                    responseDialogMessage = closeResponse.message
                                    scannedValue = null
                                }
                            }
                        } else {
                            responseDialogMessage = null
                            responseDialogSuccess = null
                            scannedValue = null
                        }
                    }
                ) {
                    Text(
                        if (isSuccess) {
                            if (isClosing) "Confirmando..." else "Confirmar solicitud"
                        } else {
                            "Cerrar"
                        }
                    )
                }
            },
            title = {
                Text(
                    if (isSuccess) {
                        if (isClosing) "Confirmando entrega" else "Solicitud valida"
                    } else {
                        "No se pudo validar"
                    }
                )
            },
            text = { Text(message) }
        )
    }
}

private suspend fun submitQrScan(
    requestUrl: String
): QrScanResponse = withContext(Dispatchers.IO) {
    val request = Request.Builder()
        .url(requestUrl)
        .get()
        .build()
    return@withContext runCatching {
        OkHttpClient().newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@use QrScanResponse(
                    success = false,
                    message = extractBackendMessage(body)
                        ?: "No se pudo validar la solicitud. Intenta nuevamente."
                )
            }
            if (body.isBlank()) {
                return@use QrScanResponse(success = true, message = "Solicitud validada correctamente.")
            }
            val json = runCatching { JSONObject(body) }.getOrNull()
            if (json != null) {
                val success = json.optBoolean("success", false)
                val message = json.optString("message").ifBlank {
                    if (success) "Solicitud validada correctamente." else "No se pudo validar la solicitud."
                }
                QrScanResponse(success = success, message = message)
            } else {
                QrScanResponse(success = false, message = body)
            }
        }
    }.getOrElse { ex ->
        QrScanResponse(success = false, message = "No se pudo validar el QR. Revisa tu conexion e intenta otra vez.")
    }
}

private fun buildScanUrl(scannedRawValue: String, qrToken: String): String {
    val encodedToken = URLEncoder.encode(qrToken, StandardCharsets.UTF_8.toString())
    val isUrl = scannedRawValue.startsWith("http://", ignoreCase = true) ||
        scannedRawValue.startsWith("https://", ignoreCase = true)
    return if (isUrl) {
        val separator = if (scannedRawValue.contains("?")) "&" else "?"
        "$scannedRawValue${separator}token=$encodedToken"
    } else {
        "$QR_DELIVERY_SCAN_URL?token=$encodedToken"
    }
}

private suspend fun closeSolicitud(
    token: String,
    solicitudId: Int,
    userId: Int
): QrScanResponse = withContext(Dispatchers.IO) {
    val payload = """
        {
          "token": "$token",
          "id_solicitud": $solicitudId,
          "id_usuario": $userId
        }
    """.trimIndent()
    val request = Request.Builder()
        .url(QR_DELIVERY_CLOSE_URL)
        .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
        .build()

    return@withContext runCatching {
        OkHttpClient().newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val parsed = runCatching { JSONObject(body) }.getOrNull()
            val success = parsed?.optBoolean("success", false) == true && response.isSuccessful
            val backendMessage = extractBackendMessage(body)
            if (success) {
                QrScanResponse(
                    success = true,
                    message = backendMessage ?: "Solicitud confirmada correctamente."
                )
            } else {
                QrScanResponse(
                    success = false,
                    message = backendMessage ?: "No se pudo confirmar la solicitud."
                )
            }
        }
    }.getOrElse {
        QrScanResponse(success = false, message = "No se pudo confirmar la solicitud. Intenta nuevamente.")
    }
}

private fun extractBackendMessage(raw: String): String? {
    if (raw.isBlank()) return null
    val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null
    return json.optString("message").trim().takeIf { it.isNotBlank() }
}

private data class QrScanResponse(
    val success: Boolean,
    val message: String
)
