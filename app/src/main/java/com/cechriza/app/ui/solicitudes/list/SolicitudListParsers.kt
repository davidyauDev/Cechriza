package com.cechriza.app.ui.solicitudes.list

import com.google.gson.JsonElement
import com.google.gson.JsonObject

internal fun extractTipoSolicitud(obj: JsonObject?): String? {
    if (obj == null) return null
    val tipoSolicitud = obj.get("tipo_solicitud") ?: return null
    if (tipoSolicitud.isJsonNull) return null

    return when {
        tipoSolicitud.isJsonPrimitive -> tipoSolicitud.asNonBlankStringOrNull()
        tipoSolicitud.isJsonObject -> {
            val typeObject = tipoSolicitud.asJsonObject
            typeObject.get("descripcion").asNonBlankStringOrNull()
                ?: typeObject.get("nombre").asNonBlankStringOrNull()
                ?: typeObject.get("tipo").asNonBlankStringOrNull()
        }
        else -> null
    }
}

internal fun parseRequestEntries(root: JsonElement?): List<RequestEntry> {
    val dataArray = root
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.getAsJsonArray("data")
        ?: return emptyList()

    return dataArray.mapNotNull { element ->
        if (!element.isJsonObject) return@mapNotNull null
        val obj = element.asJsonObject

        val idSolicitud = obj.get("id_solicitud").asIntOrNull() ?: return@mapNotNull null
        val statusDescription = obj
            .getObjectOrNull("estado")
            ?.get("descripcion")
            .asNonBlankStringOrNull()
            ?: RequestStatus.Reviewed.label
        val estadoId = obj
            .getObjectOrNull("estado")
            ?.get("id_estado")
            .asIntOrNull()
        val estadoGeneralId = obj.get("id_estado_general").asIntOrNull() ?: estadoId
        val status = resolveRequestStatus(statusDescription)
        val requester = obj.get("solicitante").asNonBlankStringOrNull()
            ?: buildStaffName(obj.getObjectOrNull("staff"))
            ?: "Solicitante"
        val tipoSolicitud = extractTipoSolicitud(obj) ?: "Solicitud"
        val allowSubirActa = obj.get("subir_acta").asBooleanOrFalse() || estadoGeneralId == 41
        val actaRrhhUrl = obj.get("acta_rrhh_url").asNonBlankStringOrNull()
        val detallesArray = obj.getAsJsonArray("detalles")
        val items = detallesArray
            ?.mapNotNull { detailElement ->
                if (!detailElement.isJsonObject) return@mapNotNull null
                val detail = detailElement.asJsonObject
                val detailStatusDescription = detail.get("estado").asNonBlankStringOrNull()
                    ?: statusDescription
                val detailStatus = resolveRequestStatus(detailStatusDescription)
                val approvedAt = detail.get("fecha_atencion").asNonBlankStringOrNull()
                    ?: detail.get("fecha_cierre").asNonBlankStringOrNull()
                    ?: "--"

                RequestItemLine(
                    id = "#${detail.get("id_detalle_solicitud").asIntOrZero()}",
                    product = detail.get("producto").asNonBlankStringOrNull() ?: "Producto",
                    area = detail.get("area").asNonBlankStringOrNull(),
                    requested = detail.get("solicitado").asIntOrZero(),
                    approved = detail.get("aprobado").asIntOrNull(),
                    motivo = detail.get("motivo").asNonBlankStringOrNull(),
                    status = detailStatus,
                    statusDescription = detailStatusDescription,
                    approvedAt = approvedAt,
                    subirActa = allowSubirActa
                )
            }
            .orEmpty()

        RequestEntry(
            solicitudId = idSolicitud,
            estadoGeneralId = estadoGeneralId,
            qrToken = obj.get("qr_token").asNonBlankStringOrNull(),
            empresaAgencia = obj.get("empresa_agencia").asNonBlankStringOrNull(),
            numeroOrden = obj.get("numero_orden").asNonBlankStringOrNull(),
            codOrden = obj.get("cod_orden").asNonBlankStringOrNull(),
            id = "#$idSolicitud",
            requester = requester,
            email = "--",
            title = "Solicitud #$idSolicitud",
            category = tipoSolicitud,
            time = obj.get("fecha_registro").asNonBlankStringOrNull() ?: "--",
            status = status,
            statusDescription = statusDescription,
            subirActa = allowSubirActa,
            actaRrhhUrl = actaRrhhUrl,
            items = items
        )
    }
}

internal fun resolveRequestStatus(statusText: String?): RequestStatus {
    val normalized = statusText?.lowercase().orEmpty()
    return when {
        "pendiente" in normalized -> RequestStatus.Pending
        "aprob" in normalized || "atencion" in normalized || "atención" in normalized -> RequestStatus.Approved
        "rechaz" in normalized -> RequestStatus.Rejected
        else -> RequestStatus.Reviewed
    }
}

internal fun buildStaffName(staff: JsonObject?): String? {
    if (staff == null) return null
    val firstName = staff.get("firstname").asNonBlankStringOrNull().orEmpty()
    val lastName = staff.get("lastname").asNonBlankStringOrNull().orEmpty()
    return listOf(firstName, lastName)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .ifBlank { null }
}

internal fun JsonElement?.asIntOrNull(): Int? {
    if (this == null || isJsonNull || !isJsonPrimitive) return null
    return runCatching { asInt }.getOrNull()
}

internal fun JsonElement?.asIntOrZero(): Int {
    return asIntOrNull() ?: 0
}

internal fun JsonElement?.asNonBlankStringOrNull(): String? {
    if (this == null || isJsonNull || !isJsonPrimitive) return null
    return runCatching { asString.trim() }.getOrNull()?.takeIf { it.isNotBlank() }
}

internal fun JsonElement?.asBooleanOrFalse(): Boolean {
    if (this == null || isJsonNull || !isJsonPrimitive) return false
    val primitive = asJsonPrimitive
    return when {
        primitive.isBoolean -> primitive.asBoolean
        primitive.isString -> primitive.asString.equals("true", ignoreCase = true)
        primitive.isNumber -> runCatching { primitive.asInt != 0 }.getOrDefault(false)
        else -> false
    }
}

internal fun JsonObject.getObjectOrNull(key: String): JsonObject? {
    val value = get(key) ?: return null
    return if (value.isJsonObject) value.asJsonObject else null
}

internal fun parseCourierTrackingInfo(root: JsonElement?): CourierTrackingInfo? {
    val rootObject = root?.takeIf { it.isJsonObject }?.asJsonObject ?: return null
    val tracking = rootObject.getObjectOrNull("tracking")
    val solicitud = rootObject.getObjectOrNull("solicitud")

    val agencia = tracking?.get("agencia").asNonBlankStringOrNull()
        ?: solicitud?.get("empresa_agencia").asNonBlankStringOrNull()
        ?: return null

    return CourierTrackingInfo(
        agencia = agencia,
        ticket = tracking?.get("ticket").asNonBlankStringOrNull(),
        numero = tracking?.get("numero").asNonBlankStringOrNull()
            ?: solicitud?.get("numero_orden").asNonBlankStringOrNull(),
        codigo = tracking?.get("codigo").asNonBlankStringOrNull()
            ?: solicitud?.get("cod_orden").asNonBlankStringOrNull(),
        estadoGeneral = tracking?.get("estado_general").asIntOrNull(),
        estadoNombre = tracking?.get("estado_nombre").asNonBlankStringOrNull(),
        estadoActual = tracking?.get("estado_actual").asNonBlankStringOrNull(),
        fecha = tracking?.get("fecha").asNonBlankStringOrNull(),
        comprobanteUrl = tracking?.get("comprobante_url").asNonBlankStringOrNull()
            ?: solicitud?.get("comprobante_url").asNonBlankStringOrNull(),
        fallbackUrl = tracking?.get("fallback_url").asNonBlankStringOrNull(),
        comentario = tracking?.get("comentario").asNonBlankStringOrNull()
            ?: solicitud?.get("comentario_adicional").asNonBlankStringOrNull(),
        detalleManual = tracking?.get("detalle_manual").asNonBlankStringOrNull()
            ?: tracking?.get("detalle").asNonBlankStringOrNull()
    )
}

internal fun parseComprobantesEntries(root: JsonElement?): List<ComprobanteEntry> {
    val dataArray = root
        ?.takeIf { it.isJsonObject }
        ?.asJsonObject
        ?.getAsJsonArray("data")
        ?: return emptyList()

    return dataArray.mapIndexedNotNull { index, item ->
        if (!item.isJsonObject) return@mapIndexedNotNull null
        val obj = item.asJsonObject
        val solicitud = obj.getObjectOrNull("solicitud_gasto")

        val id = solicitud?.get("id").asIntOrNull()
            ?: obj.get("solicitud_gasto_id").asIntOrNull()
            ?: obj.get("id").asIntOrNull()
            ?: (index + 1)

        val motivo = solicitud?.get("motivo").asNonBlankStringOrNull() ?: "Solicitud"
        val solicitante = solicitud?.get("solicitante").asNonBlankStringOrNull().orEmpty()
        val area = solicitud?.get("area").asNonBlankStringOrNull().orEmpty()
        val workflow = obj.get("workflow_state").asNonBlankStringOrNull().orEmpty()
        val subtitle = listOf(solicitante, area, workflow).filter { it.isNotBlank() }.joinToString(" - ")
        val estadoDetalleObj = solicitud?.getObjectOrNull("estado_detalle")
            ?: obj.getObjectOrNull("estado_detalle")
        val estadoDetalleId = estadoDetalleObj?.get("id").asIntOrNull()
        val statusCode = estadoDetalleObj?.get("codigo").asNonBlankStringOrNull()
        val status = estadoDetalleObj?.get("nombre").asNonBlankStringOrNull()
            ?: obj.get("workflow_state").asNonBlankStringOrNull()
            ?: "pendiente"
        val date = solicitud?.get("fecha_solicitud").asNonBlankStringOrNull()
            ?: obj.get("fecha_registro").asNonBlankStringOrNull()
            ?: "--"

        val monto = obj.get("monto_real").asNonBlankStringOrNull()
            ?: obj.get("monto_estimado").asNonBlankStringOrNull()
            ?: solicitud?.get("monto_real").asNonBlankStringOrNull()
            ?: solicitud?.get("monto_estimado").asNonBlankStringOrNull()
        val areaId = solicitud?.get("id_area").asIntOrNull()
            ?: obj.get("id_area").asIntOrNull()
        val detailArray = obj.getAsJsonArray("solicitud_gasto_detalles")
        val seguimientoComentario = obj.getAsJsonArray("seguimientos_solicitud_gasto")
            ?.mapNotNull { seguimiento ->
                if (!seguimiento.isJsonObject) return@mapNotNull null
                seguimiento.asJsonObject.get("comentario").asNonBlankStringOrNull()
            }
            ?.lastOrNull()
        val details = detailArray?.mapIndexedNotNull { detailIndex, detailElement ->
            if (!detailElement.isJsonObject) return@mapIndexedNotNull null
            val detail = detailElement.asJsonObject
            val detailId = detail.get("id").asIntOrNull() ?: (detailIndex + 1)
            ComprobanteDetailItem(
                id = "#$detailId",
                producto = detail.get("producto").asNonBlankStringOrNull()
                    ?: "Producto ${detail.get("id_producto").asIntOrZero()}",
                cantidad = detail.get("cantidad").asIntOrZero(),
                descripcion = detail.get("descripcion_adicional").asNonBlankStringOrNull() ?: "--",
                rutaImagen = detail.get("ruta_imagen").asNonBlankStringOrNull() ?: "--",
                urlImagen = detail.get("url_imagen").asNonBlankStringOrNull() ?: "--"
            )
        }.orEmpty()
        val comprobanteObj = obj.getObjectOrNull("comprobante")
        val comprobante = if (comprobanteObj == null) {
            null
        } else {
            val parsed = UploadedComprobante(
                id = comprobanteObj.get("id").asIntOrNull(),
                tipo = comprobanteObj.get("tipo").asNonBlankStringOrNull(),
                numero = comprobanteObj.get("numero").asNonBlankStringOrNull(),
                monto = comprobanteObj.get("monto").asNonBlankStringOrNull(),
                archivoUrl = comprobanteObj.get("archivo_url").asNonBlankStringOrNull()
            )
            if (
                parsed.id == null &&
                parsed.tipo.isNullOrBlank() &&
                parsed.numero.isNullOrBlank() &&
                parsed.monto.isNullOrBlank() &&
                parsed.archivoUrl.isNullOrBlank()
            ) {
                null
            } else {
                parsed
            }
        }

        ComprobanteEntry(
            id = "#$id",
            title = motivo,
            subtitle = subtitle,
            seguimientoComentario = seguimientoComentario,
            estadoDetalleId = estadoDetalleId,
            statusCode = statusCode,
            status = status,
            date = date,
            amount = monto?.let { "S/ $it" },
            areaId = areaId,
            comprobante = comprobante,
            details = details
        )
    }
}
