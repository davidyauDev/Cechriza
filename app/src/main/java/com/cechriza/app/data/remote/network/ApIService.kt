package com.cechriza.app.data.remote.network

import com.cechriza.app.data.remote.dto.request.LoginRequest
import com.cechriza.app.data.remote.dto.request.MemoryMatchScoreRequest
import com.cechriza.app.data.remote.dto.response.AttendanceResponse
import com.cechriza.app.data.remote.dto.response.EventosHoyResponse
import com.cechriza.app.data.remote.dto.response.LoginResponseTotal
import com.google.gson.JsonElement
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponseTotal>

    @Multipart
    @POST("attendances")
    suspend fun sendAttendance(
        @Part("user_id") userId: RequestBody,
        @Part("timestamp") timestamp: RequestBody,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("notes") notes: RequestBody,
        @Part("device_model") deviceModel: RequestBody,
        @Part("battery_percentage") batteryPercentage: RequestBody,
        @Part("signal_strength") signalStrength: RequestBody,
        @Part("network_type") networkType: RequestBody,
        @Part("address") address: RequestBody,
        @Part empCode: MultipartBody.Part,
        @Part("is_internet_available") isInternetAvailable: RequestBody,
        @Part("type") type: RequestBody,
        @Part("client_id") clientId: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<AttendanceResponse>

    @GET("technicians/rutas-dia")
    suspend fun getRutasDia(
        @Query("emp_code") empCode: String,
        @Query("fecha") fecha: String? = null
    ): Response<JsonElement>

    @GET("eventos/hoy")
    suspend fun getEventosHoy(): Response<EventosHoyResponse>

    @GET("inventario/productos")
    suspend fun getInventarioProductos(
        @Query("tipo_responsable") tipoResponsable: String
    ): Response<JsonElement>

    @GET("solicitudes")
    suspend fun getSolicitudes(
        @Query("id_usuario_solicitante") userId: Int
    ): Response<JsonElement>

    @GET("solicitudes-gasto/comprobantes")
    suspend fun getSolicitudesGastoComprobantes(
        @Query("staff_id") staffId: Int
    ): Response<JsonElement>

    @GET("rrhh/solicitudes-compra")
    suspend fun getRrhhSolicitudesCompra(
        @Query("staff_id") staffId: Int
    ): Response<JsonElement>

    @GET("solicitudes/{id}")
    suspend fun getSolicitudById(
        @Path("id") solicitudId: Int
    ): Response<JsonElement>

    @POST("memory-match/scores")
    suspend fun submitMemoryMatchScore(
        @Body request: MemoryMatchScoreRequest
    ): Response<JsonElement>

    @GET("memory-match/leaderboard")
    suspend fun getMemoryMatchLeaderboard(
        @Query("limit") limit: Int = 10
    ): Response<JsonElement>

    @GET("memory-match/my-score/{userId}")
    suspend fun getMemoryMatchMyScore(
        @Path("userId") userId: Int
    ): Response<JsonElement>

    @POST("solicitudes/registrar-completa")
    suspend fun registrarSolicitudCompleta(
        @Body multipartBody: MultipartBody
    ): Response<JsonElement>

    @Multipart
    @POST("solicitudes-gasto")
    suspend fun registrarSolicitudGasto(
        @Part parts: List<MultipartBody.Part>
    ): Response<JsonElement>

    @GET
    suspend fun descargarCompromiso(
        @Url url: String,
        @Query("id_solicitud") solicitudId: Int,
        @Query("id_usuario") userId: Int
    ): Response<ResponseBody>

    @Multipart
    @POST
    suspend fun subirActaFirmada(
        @Url url: String,
        @Part("id_solicitud") solicitudId: RequestBody,
        @Part("id_usuario") userId: RequestBody,
        @Part actaPdf: MultipartBody.Part
    ): Response<JsonElement>


}
