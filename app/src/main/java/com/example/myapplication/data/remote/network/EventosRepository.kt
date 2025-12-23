package com.example.myapplication.data.remote.network

import com.example.myapplication.data.remote.dto.response.EventosHoyResponse
import retrofit2.Response
import javax.inject.Inject

class EventosRepository @Inject constructor(
    private val apiService: ApiService
) {
    suspend fun getEventosHoy(): Response<EventosHoyResponse> {
        return apiService.getEventosHoy()
    }
}