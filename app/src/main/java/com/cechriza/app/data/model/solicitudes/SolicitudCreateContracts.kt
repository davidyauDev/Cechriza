package com.cechriza.app.data.model.solicitudes

internal data class SubmitRequestResult(
    val success: Boolean,
    val message: String
)

internal data class SolicitudBaseFields(
    val justificacion: String,
    val fechaNecesaria: String,
    val idDireccionEntrega: String,
    val esPedidoCompra: Boolean,
    val ubicacion: String
)
