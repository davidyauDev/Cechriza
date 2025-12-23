package com.example.myapplication.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.google.accompanist.pager.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.remote.dto.response.EventoImagen
import androidx.compose.foundation.clickable
import androidx.compose.ui.window.Dialog

private const val TAG = "BannerCarousel"

@OptIn(ExperimentalPagerApi::class)
@Composable
fun BannerCard(
    images: List<EventoImagen>,
    title: String,
    label: String,
    description: String,
    labelColor: Color = Color(0xFFFFC107),
    date: String,
) {
    val pagerState = rememberPagerState()
    val selectedImage = images.getOrNull(pagerState.currentPage)

    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            HorizontalPager(
                count = images.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) { page ->
                SubcomposeAsyncImage(
                    model = images[page].url_imagen,
                    contentDescription = images[page].descripcion ?: title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(images.size) { index ->
                    val color = if (pagerState.currentPage == index) Color.Gray else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(6.dp)
                            .background(color, CircleShape)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(labelColor, shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = date,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = selectedImage?.descripcion.orEmpty(),
                    fontSize = 13.sp,
                    color = Color(0xFF0051A8),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

data class EventoConImagen(
    val imagen: EventoImagen,
    val eventoTitulo: String,
    val eventoDescripcion: String,
    val eventoFecha: String
)


@OptIn(ExperimentalPagerApi::class)
@Composable
fun EventosCarouselBanner(eventos: List<EventoConImagen>) {
    val pagerState = rememberPagerState()

    if (eventos.isEmpty()) return

    val current = eventos[pagerState.currentPage]
    var selectedImageUrl by remember { mutableStateOf<String?>(null) }
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column {
            HorizontalPager(
                count = eventos.size,
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) { page ->
                val img = eventos[page].imagen
                SubcomposeAsyncImage(
                    model = eventos[page].imagen.url_imagen,
                    contentDescription = eventos[page].imagen.descripcion ?: eventos[page].eventoTitulo,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clickable { selectedImageUrl = eventos[page].imagen.url_imagen } // 👈 al tocar imagen, se guarda la URL
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(eventos.size) { index ->
                    val color = if (pagerState.currentPage == index) Color.Gray else Color.LightGray
                    Box(
                        modifier = Modifier
                            .padding(2.dp)
                            .size(6.dp)
                            .background(color, CircleShape)
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFC107), shape = RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Evento",
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = current.eventoFecha,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = current.eventoTitulo,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Text(
                    text = current.imagen.descripcion.orEmpty(),
                    fontSize = 13.sp,
                    color = Color(0xFF0051A8),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = current.eventoDescripcion,
                    fontSize = 12.sp,
                    color = Color.DarkGray
                )
            }
        }
    }

    if (selectedImageUrl != null) {
        Dialog(onDismissRequest = { selectedImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = selectedImageUrl,
                    contentDescription = "Imagen ampliada",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }

}
