package com.cechriza.app.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class AttendanceReportEmployeeResponse(
    @SerializedName("data")
    val data: List<AttendanceReportEmployeeItem> = emptyList()
)

data class AttendanceReportEmployeeItem(
    @SerializedName("ID_Marcacion")
    val idMarcacion: Int? = null,
    @SerializedName("Ubicacion")
    val ubicacion: String? = null,
    @SerializedName("Imagen")
    val imagen: String? = null,
    @SerializedName("map_url")
    val mapUrl: String? = null,
    @SerializedName("Fecha_Hora_Marcacion")
    val fechaHoraMarcacion: String? = null,
    @SerializedName("Hora_Marcacion")
    val horaMarcacion: String? = null,
    @SerializedName("Tipo_Marcacion")
    val tipoMarcacion: String? = null,
    @SerializedName("DNI")
    val dni: String? = null,
    @SerializedName("Apellidos")
    val apellidos: String? = null,
    @SerializedName("Nombres")
    val nombres: String? = null,
    @SerializedName("Empleado_id")
    val empleadoId: Int? = null,
    @SerializedName("Departamento")
    val departamento: String? = null,
    @SerializedName("Departamento_id")
    val departamentoId: Int? = null,
    @SerializedName("Empresa")
    val empresa: String? = null,
    @SerializedName("Empresa_id")
    val empresaId: Int? = null,
    @SerializedName("Tecnico")
    val tecnico: Boolean? = null,
    @SerializedName("Horario")
    val horario: String? = null,
    @SerializedName("Fecha")
    val fecha: String? = null
)
