package com.cechriza.app.data.remote.dto.request

data class AttendanceReportEmployeeRequest(
    val empleado_id: Int,
    val fecha_inicio: String? = null,
    val fecha_fin: String? = null,
    val fechas: List<String>? = null
)
