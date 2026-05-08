package com.cechriza.app.data.remote.solicitudes

import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.network.ApiService
import com.cechriza.app.data.remote.network.RetrofitClient
import com.google.gson.JsonElement
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File

internal object SolicitudesRemoteDataSource {
    private const val OSTICKET_BASE_URL = "https://osticket.cechriza.com"
    private const val DESCARGAR_COMPROMISO_URL = "$OSTICKET_BASE_URL/system/vista/ajax/feature/pedidos/api/descargar_compromiso.php"
    private const val SUBIR_ACTA_FIRMADA_URL = "$OSTICKET_BASE_URL/system/vista/ajax/feature/pedidos/api/subir_acta_firmada.php"

    private fun api(): ApiService {
        return RetrofitClient.apiWithToken { SessionManager.token }
    }

    suspend fun getSolicitudes(solicitanteUserId: Int): Response<JsonElement> {
        return api().getSolicitudes(solicitanteUserId)
    }

    suspend fun getSolicitudesGastoComprobantes(staffId: Int): Response<JsonElement> {
        return api().getSolicitudesGastoComprobantes(staffId)
    }

    suspend fun getInventarioProductos(responsable: String): Response<JsonElement> {
        return api().getInventarioProductos(responsable)
    }

    suspend fun descargarCompromiso(
        solicitudId: Int,
        userId: Int
    ): Response<ResponseBody> {
        return api().descargarCompromiso(DESCARGAR_COMPROMISO_URL, solicitudId, userId)
    }

    suspend fun subirActaFirmada(
        solicitudId: Int,
        userId: Int,
        actaPdfFile: File
    ): Response<JsonElement> {
        val plainText = "text/plain".toMediaTypeOrNull()
        val solicitudPart = solicitudId.toString().toRequestBody(plainText)
        val userPart = userId.toString().toRequestBody(plainText)
        val fileBody = actaPdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("acta_pdf", actaPdfFile.name, fileBody)
        return api().subirActaFirmada(SUBIR_ACTA_FIRMADA_URL, solicitudPart, userPart, filePart)
    }

    fun authenticatedApi(): ApiService = api()
}
