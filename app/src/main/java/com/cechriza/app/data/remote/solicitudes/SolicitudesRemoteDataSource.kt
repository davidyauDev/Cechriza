package com.cechriza.app.data.remote.solicitudes

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.cechriza.app.BuildConfig
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.network.ApiService
import com.cechriza.app.data.remote.network.RetrofitClient
import com.google.gson.JsonElement
import okhttp3.MediaType.Companion.toMediaType
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
    private const val COURIER_SHALOM_URL = "$OSTICKET_BASE_URL/system/vista/ajax/feature/pedidos/api/courier_shalom.php"
    private const val COURIER_OLVA_URL = "$OSTICKET_BASE_URL/system/vista/ajax/feature/pedidos/api/courier_olva.php"
    private const val COURIER_OTROS_URL = "$OSTICKET_BASE_URL/system/vista/ajax/feature/pedidos/api/courier_otros.php"

    private fun api(): ApiService {
        return RetrofitClient.apiWithToken { SessionManager.token }
    }

    private fun publicApi(): ApiService {
        return RetrofitClient.apiWithoutToken
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

    suspend fun descargarActaRrhh(
        solicitudId: Int
    ): Response<ResponseBody> {
        return api().descargarActaRrhh(solicitudId)
    }

    suspend fun descargarCompromiso(
        solicitudId: Int,
        userId: Int
    ): Response<ResponseBody> {
        return api().descargarCompromiso(
            url = DESCARGAR_COMPROMISO_URL,
            solicitudId = solicitudId,
            userId = userId
        )
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

    suspend fun uploadActaRrhh(
        solicitudId: Int,
        actaPdfFile: File,
        comentario: String? = null
    ): Response<JsonElement> {
        val plainText = "text/plain".toMediaTypeOrNull()
        val fileBody = actaPdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("acta_rrhh", actaPdfFile.name, fileBody)
        val comentarioPart = comentario
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.toRequestBody(plainText)
        return api().uploadActaRrhh(
            solicitudId = solicitudId,
            actaRrhh = filePart,
            comentario = comentarioPart
        )
    }

    suspend fun registrarSolicitudGastoComprobante(
        context: Context,
        solicitudGastoId: Int,
        tipo: String,
        numero: String,
        monto: String,
        archivoUri: Uri
    ): Response<JsonElement> {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(archivoUri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo leer el archivo seleccionado.")
        val maxBytes = 10L * 1024L * 1024L
        if (bytes.isEmpty()) {
            throw IllegalArgumentException("El archivo seleccionado esta vacio.")
        }
        if (bytes.size > maxBytes) {
            throw IllegalArgumentException("El archivo supera el maximo de 10MB.")
        }

        val mimeType = (resolver.getType(archivoUri) ?: inferMimeTypeFromName(resolveDisplayName(context, archivoUri)))
            ?.lowercase()
            ?: throw IllegalArgumentException("No se pudo identificar el tipo del archivo.")
        val allowedMimeTypes = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "application/pdf"
        )
        if (mimeType !in allowedMimeTypes) {
            throw IllegalArgumentException("El archivo debe ser JPG, JPEG, PNG, WEBP o PDF.")
        }

        val displayName = resolveDisplayName(context, archivoUri)
            ?.takeIf { it.isNotBlank() }
            ?: "comprobante.${defaultExtensionForMimeType(mimeType)}"

        val plainText = "text/plain".toMediaTypeOrNull()
        val archivoBody = bytes.toRequestBody(mimeType.toMediaType())
        val archivoPart = MultipartBody.Part.createFormData("archivo", displayName, archivoBody)

        return api().registrarSolicitudGastoComprobante(
            solicitudGastoId = solicitudGastoId.toString().toRequestBody(plainText),
            tipo = tipo.toRequestBody(plainText),
            numero = numero.toRequestBody(plainText),
            monto = monto.toRequestBody(plainText),
            archivo = archivoPart
        )
    }

    suspend fun consultarCourierTracking(
        solicitudId: Int,
        empresaAgencia: String
    ): Response<JsonElement> {
        val normalizedAgency = empresaAgencia.trim().lowercase()
        val endpointUrl = when {
            "shalom" in normalizedAgency || "shalon" in normalizedAgency -> COURIER_SHALOM_URL
            "olva" in normalizedAgency -> COURIER_OLVA_URL
            else -> COURIER_OTROS_URL
        }
        val apiKey = BuildConfig.COURIER_MOBILE_API_KEY
        return publicApi().consultarCourierTracking(
            url = endpointUrl,
            apiKey = apiKey,
            authorization = "Bearer $apiKey",
            body = mapOf("id_solicitud" to solicitudId)
        )
    }

    fun authenticatedApi(): ApiService = api()
}

private fun resolveDisplayName(context: Context, uri: Uri): String? {
    val resolver = context.contentResolver
    resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0) return cursor.getString(index)
        }
    }
    return uri.lastPathSegment?.substringAfterLast('/')
}

private fun inferMimeTypeFromName(fileName: String?): String? {
    val extension = fileName?.substringAfterLast('.', "")?.lowercase().orEmpty()
    return when (extension) {
        "jpg", "jpeg" -> "image/jpeg"
        "png" -> "image/png"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        else -> null
    }
}

private fun defaultExtensionForMimeType(mimeType: String): String {
    return when (mimeType) {
        "image/jpeg", "image/jpg" -> "jpg"
        "image/png" -> "png"
        "image/webp" -> "webp"
        "application/pdf" -> "pdf"
        else -> "bin"
    }
}
