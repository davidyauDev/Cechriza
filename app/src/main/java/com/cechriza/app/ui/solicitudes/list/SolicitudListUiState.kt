package com.cechriza.app.ui.solicitudes.list

internal data class SolicitudListUiState(
    val mode: HistoryMode,
    val source: ComprobanteSource,
    val requests: List<RequestEntry>,
    val comprobantes: List<ComprobanteEntry>,
    val isLoadingRequests: Boolean,
    val isLoadingComprobantes: Boolean,
    val requestsError: String?,
    val comprobantesError: String?
) {
    val showComprobanteTabs: Boolean get() = mode == HistoryMode.Comprobantes
    val showRequestList: Boolean get() = mode == HistoryMode.Historial
}
