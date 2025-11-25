package com.example.myapplication.data.remote.dto.response

data class EventosHoyResponse(
    val success: Boolean,
    val data: EventosHoyData,
    val message: String
)

data class EventosHoyData(
    val events: List<Evento>,
    val date: String
)

data class Evento(
    val id: Int,
    val titulo: String,
    val descripcion: String,
    val active: Int,
    val fecha: String,
    val created_at: String,
    val updated_at: String,
    val imagenes: List<EventoImagen>
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
