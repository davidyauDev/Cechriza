package com.cechriza.app.ui.camera

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.cechriza.app.data.local.database.AttendanceDatabase
import com.cechriza.app.data.local.database.LocationDatabase
import com.cechriza.app.data.local.entity.AttendanceType
import com.cechriza.app.data.preferences.UserPreferences
import com.cechriza.app.data.repository.AttendanceRepository
import com.cechriza.app.ui.home.LocationResult
import com.cechriza.app.ui.home.awaitLocationForAttendanceImproved
import com.cechriza.app.ui.home.saveBitmapToFile
import com.google.android.gms.location.LocationServices
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@androidx.camera.core.ExperimentalGetImage
@Composable
fun CameraScreen(
    navController: NavController,
    attendanceType: AttendanceType
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    val attendanceDao = remember { AttendanceDatabase.getDatabase(context).attendanceDao() }
    val attendanceRepository = remember {
        AttendanceRepository(
            userPreferences = UserPreferences(context),
            context = context,
            dao = attendanceDao
        )
    }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationDao = remember { LocationDatabase.getDatabase(context).locationDao() }

    AttendanceCameraView(
        onCaptureImage = { bitmap ->
            saveBitmapToFile(context, bitmap)
            coroutineScope.launch {
                isLoading = true
                try {
                    var loc: android.location.Location? = null
                    var result = awaitLocationForAttendanceImproved(fusedLocationClient, context, locationDao, 10000L)
                    if (result is LocationResult.Success) {
                        loc = result.location
                    } else {
                        delay(1000L)
                        result = awaitLocationForAttendanceImproved(fusedLocationClient, context, locationDao, 5000L)
                        if (result is LocationResult.Success) loc = result.location
                    }

                    if (loc != null) {
                        val saveResult = attendanceRepository.saveAttendance(
                            latitude = loc.latitude,
                            longitude = loc.longitude,
                            type = attendanceType,
                            photo = bitmap
                        )
                        if (saveResult.isSuccess) {
                            Toast.makeText(context, "Asistencia registrada", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            Toast.makeText(context, "No se pudo registrar asistencia", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "Ubicacion no disponible", Toast.LENGTH_SHORT).show()
                    }
                } catch (ex: Exception) {
                    Log.e("CameraScreen", "Error en captura", ex)
                    Toast.makeText(context, "Error inesperado", Toast.LENGTH_LONG).show()
                } finally {
                    isLoading = false
                }
            }
        },
        onClose = { navController.popBackStack() },
        isLoading = isLoading
    )
}

@androidx.camera.core.ExperimentalGetImage
@Composable
fun AttendanceCameraView(
    onCaptureImage: (Bitmap) -> Unit,
    onClose: () -> Unit,
    isLoading: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { androidx.camera.view.PreviewView(context) }
    val executor = ContextCompat.getMainExecutor(context)

    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var faceDetected by remember { mutableStateOf(false) }
    var consecutiveDetectedFrames by remember { mutableStateOf(0) }
    var consecutiveLostFrames by remember { mutableStateOf(0) }

    val detectorOptions = remember {
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .setMinFaceSize(0.05f)
            .build()
    }
    val faceDetector: FaceDetector = remember { FaceDetection.getClient(detectorOptions) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also { p -> p.setSurfaceProvider(previewView.surfaceProvider) }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(executor) { imageProxy: ImageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        faceDetector.process(inputImage)
                            .addOnSuccessListener { faces: List<Face> ->
                                val detectedNow = if (faces.isNotEmpty()) {
                                    val f = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                                    val wRatio = (f?.boundingBox?.width()?.toFloat() ?: 0f) / imageProxy.width.toFloat()
                                    val hRatio = (f?.boundingBox?.height()?.toFloat() ?: 0f) / imageProxy.height.toFloat()
                                    wRatio >= 0.09f && hRatio >= 0.09f
                                } else false

                                if (detectedNow) {
                                    consecutiveDetectedFrames += 1
                                    consecutiveLostFrames = 0
                                    if (consecutiveDetectedFrames >= 2) faceDetected = true
                                } else {
                                    consecutiveDetectedFrames = 0
                                    consecutiveLostFrames += 1
                                    if (consecutiveLostFrames >= 3) faceDetected = false
                                }
                            }
                            .addOnCompleteListener { imageProxy.close() }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    imageAnalysis
                )
            }, executor)
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(140.dp)
                .background(Brush.verticalGradient(0f to Color.Black.copy(alpha = 0.45f), 1f to Color.Transparent))
                .zIndex(2f)
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(190.dp)
                .background(Brush.verticalGradient(0f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.55f)))
                .zIndex(2f)
        )

        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .size(44.dp)
                .background(Color.Black.copy(alpha = 0.45f), CircleShape)
                .zIndex(6f)
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
        }

        val frameColor by animateColorAsState(
            targetValue = if (faceDetected) Color(0xFF22C55E) else Color.White.copy(alpha = 0.82f),
            animationSpec = tween(220), label = "frame_color"
        )
        val frameScale by animateFloatAsState(
            targetValue = if (faceDetected) 1.02f else 1f,
            animationSpec = tween(220), label = "frame_scale"
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.68f)
                .aspectRatio(0.75f)
                .scale(frameScale)
                .border(2.dp, frameColor, RoundedCornerShape(999.dp))
                .zIndex(4f)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 132.dp)
                .zIndex(5f)
        ) {
            val hintBg by animateColorAsState(
                targetValue = if (faceDetected) Color(0xFF16A34A) else Color.Black.copy(alpha = 0.52f),
                animationSpec = tween(220), label = "hint_bg"
            )
            Surface(shape = RoundedCornerShape(12.dp), color = hintBg) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (faceDetected) Icons.Default.CheckCircle else Icons.Default.Face,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = if (faceDetected) "Rostro alineado. Captura lista." else "Centra tu rostro dentro del ovalo",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        val captureEnabled = faceDetected && !isLoading
        Button(
            onClick = {
                val bitmap = previewView.bitmap
                if (bitmap != null) capturedBitmap = bitmap
                else Toast.makeText(context, "No se pudo capturar imagen", Toast.LENGTH_SHORT).show()
            },
            enabled = captureEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp)
                .zIndex(5f)
        ) {
            Text("Capturar")
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .zIndex(8f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        if (capturedBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .zIndex(9f),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    bitmap = capturedBitmap!!.asImageBitmap(),
                    contentDescription = "Vista previa",
                    modifier = Modifier.fillMaxSize()
                )
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(onClick = { capturedBitmap = null }) { Text("Cancelar") }
                    Button(onClick = {
                        onCaptureImage(capturedBitmap!!)
                        capturedBitmap = null
                    }) { Text("Aceptar") }
                }
            }
        }
    }
}
