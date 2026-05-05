package com.cechriza.app.data.remote.solicitudes

import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.network.ApiService
import com.cechriza.app.data.remote.network.RetrofitClient
import com.google.gson.JsonElement
import retrofit2.Response

internal object SolicitudesRemoteDataSource {

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

    fun authenticatedApi(): ApiService = api()
}
