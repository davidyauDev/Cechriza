package com.example.myapplication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.example.myapplication.data.preferences.SessionManager
import com.example.myapplication.data.remote.network.RetrofitClient
import com.google.accompanist.pager.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log

private const val TAG = "BannerCarousel"

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BannerCarousel() {
    val pagerState = rememberPagerState()

    var images by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    fun normalizeUrl(url: String): String {
        return url
            .replace("http://localhost", "http://10.0.2.2")
            .replace("http://127.0.0.1", "http://10.0.2.2")
    }

    LaunchedEffect(Unit) {

        // Auto-slide
        launch {
            while (true) {
                delay(4000)
                if (images.isNotEmpty()) {
                    val next = (pagerState.currentPage + 1) % images.size
                    pagerState.animateScrollToPage(next)
                }
            }
        }

        try {
            isLoading = true
            errorMsg = null

            val tokenProvider = { SessionManager.token }
            val api = RetrofitClient.apiWithToken(tokenProvider)

            val response = try {
                api.getEventosHoy()
            } catch (e: Exception) {
                Log.e(TAG, "Error llamando eventos/hoy: ${e.message}")
                errorMsg = "Error de conexión al obtener eventos"
                null
            }

            if (response != null && response.isSuccessful) {
                val body = response.body()
                if (body != null && body.success) {

                    // ⛔️ ERROR AQUÍ ANTES: body.data YA NO ES LISTA → ES OBJETO
                    val urls = body.data.events.flatMap { evento ->
                        evento.imagenes.mapNotNull { it.url_imagen }
                    }.map { normalizeUrl(it) }

                    images = urls

                    if (images.isEmpty()) {
                        errorMsg = "No hay imágenes para mostrar"
                    }

                } else {
                    errorMsg = "Respuesta inválida del servidor"
                }
            } else {
                errorMsg = "Error al obtener eventos: ${response?.code() ?: "?"}"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error general: ${e.message}")
            errorMsg = e.localizedMessage ?: "Error inesperado"
            images = emptyList()
        } finally {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // LOADING
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // SI NO HAY IMÁGENES
        else if (images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = errorMsg ?: "No hay banners disponibles", color = Color.Gray)
            }
        }

        // SI HAY IMÁGENES
        else {
            HorizontalPager(
                count = images.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) { page ->

                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                ) {

                    val url = images.getOrNull(page)
                    val token = SessionManager.token

                    if (url != null) {
                        val builder = ImageRequest.Builder(LocalContext.current)
                            .data(url)

                        if (!token.isNullOrEmpty()) {
                            builder.addHeader("Authorization", "Bearer $token")
                        }

                        val model = builder.build()

                        SubcomposeAsyncImage(
                            model = model,
                            contentDescription = "Banner $page",
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            loading = {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            },
                            error = {
                                Log.w(TAG, "Coil failed to load $url")
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "Error cargando imagen", color = Color.Red)
                                }
                            },
                            success = {
                                SubcomposeAsyncImageContent()
                            }
                        )

                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "No image", color = Color.Gray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {

                val pageCount = images.size

                for (i in 0 until pageCount) {
                    val isSelected = pagerState.currentPage == i

                    Box(
                        modifier = Modifier
                            .size(if (isSelected) 10.dp else 8.dp)
                            .padding(4.dp)
                            .background(
                                color = if (isSelected) Color.Red else Color.LightGray,
                                shape = CircleShape
                            )
                    )
                }
            }

            if (errorMsg != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMsg ?: "",
                    color = Color.Red,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
