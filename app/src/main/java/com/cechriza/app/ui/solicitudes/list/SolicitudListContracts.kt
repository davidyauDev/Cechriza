package com.cechriza.app.ui.solicitudes.list

import androidx.compose.ui.graphics.Color
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBlueDark
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandOrange

internal enum class RequestStatus(val label: String, val color: Color) {
    Pending("Pendiente", BrandOrange),
    Approved("Aprobada", BrandBlue),
    Rejected("Rechazada", BrandBlueDark),
    Reviewed("Revisada", BrandMuted)
}

internal enum class HistoryMode(val label: String) {
    Historial("ALMACEN"),
    Comprobantes("GASTOS")
}

internal enum class ComprobanteSource(val label: String) {
    Gastos("Otros"),
    Rrhh("Botas")
}

internal data class ComprobanteEntry(
    val id: String,
    val title: String,
    val subtitle: String,
    val seguimientoComentario: String?,
    val estadoDetalleId: Int?,
    val statusCode: String?,
    val status: String,
    val date: String,
    val amount: String?,
    val areaId: Int?,
    val details: List<ComprobanteDetailItem>
)

internal data class ComprobanteDetailItem(
    val id: String,
    val producto: String,
    val cantidad: Int,
    val descripcion: String,
    val rutaImagen: String,
    val urlImagen: String
)

internal enum class RequestStartOption(
    val label: String,
    val description: String
) {
    Epps(
        "Solicitud Botas de Seguridad",
        "Area encargada SSOMA"
    ),
    Almacen(
        "Solicitud Almacen",
        "Para materiales o stock del almacen de la empresa."
    ),
    Gasto(
        "Solicitud de Gasto",
        "Cuando compras con tu dinero y luego la empresa te hace el reembolso."
    )
}

internal data class StatusChipStyle(
    val background: Color,
    val border: Color,
    val foreground: Color
)

internal data class RequestItemLine(
    val id: String,
    val product: String,
    val requested: Int,
    val status: RequestStatus,
    val statusDescription: String,
    val approvedAt: String
)

internal data class RequestEntry(
    val solicitudId: Int,
    val id: String,
    val requester: String,
    val email: String,
    val title: String,
    val category: String,
    val time: String,
    val status: RequestStatus,
    val statusDescription: String,
    val items: List<RequestItemLine>
)
