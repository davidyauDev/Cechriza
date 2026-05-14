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
    Historial("Economato"),
    Comprobantes("Solicitud de compras")
}

internal enum class ComprobanteSource(val label: String) {
    Gastos("Solicitud de compras"),
    Rrhh("Botas de Seguridad")
}

internal enum class SolicitudListViewMode(val label: String) {
    Todo("Todo"),
    Tabs("Por categorias")
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
    val comprobante: UploadedComprobante?,
    val details: List<ComprobanteDetailItem>
)

internal data class UploadedComprobante(
    val id: Int?,
    val tipo: String?,
    val numero: String?,
    val monto: String?,
    val archivoUrl: String?
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
        "Botas de Seguridad",
        "Area encargada SSOMA"
    ),
    Almacen(
        "Insumos, Herramientas, Calibradores, EPPS",
        "Para materiales o stock del economato de la empresa."
    ),
    Gasto(
        "Compras",
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
    val area: String?,
    val requested: Int,
    val approved: Int?,
    val motivo: String?,
    val status: RequestStatus,
    val statusDescription: String,
    val approvedAt: String,
    val subirActa: Boolean
)

internal data class RequestEntry(
    val solicitudId: Int,
    val estadoGeneralId: Int?,
    val qrToken: String?,
    val empresaAgencia: String?,
    val numeroOrden: String?,
    val codOrden: String?,
    val id: String,
    val requester: String,
    val email: String,
    val title: String,
    val category: String,
    val time: String,
    val status: RequestStatus,
    val statusDescription: String,
    val subirActa: Boolean,
    val actaRrhhUrl: String?,
    val items: List<RequestItemLine>
)

internal data class CourierTrackingInfo(
    val agencia: String,
    val ticket: String?,
    val numero: String?,
    val codigo: String?,
    val estadoGeneral: Int?,
    val estadoNombre: String?,
    val estadoActual: String?,
    val fecha: String?,
    val comprobanteUrl: String?,
    val fallbackUrl: String?,
    val comentario: String?,
    val detalleManual: String?
)
