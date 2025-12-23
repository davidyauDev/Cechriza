package com.example.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.response.EventosHoyResponse
import com.example.myapplication.data.remote.network.EventosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val eventosRepository: EventosRepository
) : ViewModel() {

    private val _eventosHoy = MutableStateFlow<EventosHoyResponse?>(null)
    val eventosHoy: StateFlow<EventosHoyResponse?> = _eventosHoy

    fun loadEventosHoy() {
        viewModelScope.launch {
            val response = eventosRepository.getEventosHoy()
            _eventosHoy.value = if (response.isSuccessful) response.body() else null
        }
    }
}