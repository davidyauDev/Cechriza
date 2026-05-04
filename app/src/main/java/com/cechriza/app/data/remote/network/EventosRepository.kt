package com.cechriza.app.data.remote.network

import com.cechriza.app.data.remote.dto.response.EventosHoyResponse
import retrofit2.Response
import javax.inject.Inject

class EventosRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getEventosHoy(): Response<EventosHoyResponse> {
        return apiService.getEventosHoy()
    }
}