package com.example.myapplication.data.remote.dto.response

data class EventosHoyResponse(
    val success: Boolean,
    val data: List<Evento>
)

data class Evento(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val fecha_inicio: String,
    val fecha_fin: String,
    val estado: String,
    val created_at: String,
    val updated_at: String,
    val imagenes: List<EventoImagen> = emptyList()
)

data class EventoImagen(
    val id: Int,
    val evento_id: Int,
    val url_imagen: String,
    val descripcion: String?,
    val orden: Int?,
    val autor: String?,
    val created_at: String?,
    val updated_at: String?
)

