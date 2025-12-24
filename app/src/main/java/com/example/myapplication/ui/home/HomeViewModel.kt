package com.example.myapplication.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.remote.dto.response.EventosHoyResponse
import com.example.myapplication.data.remote.network.EventosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}



class HomeViewModel(
    private val eventosRepository: EventosRepository
) : ViewModel() {

    private val _eventosHoy =
        MutableStateFlow<UiState<EventosHoyResponse>>(UiState.Loading)

    val eventosHoy: StateFlow<UiState<EventosHoyResponse>> = _eventosHoy

    fun loadEventosHoy() {
        viewModelScope.launch {
            _eventosHoy.value = UiState.Loading
            try {
                val response = eventosRepository.getEventosHoy()
                if (response.isSuccessful && response.body() != null) {
                    _eventosHoy.value = UiState.Success(response.body()!!)
                } else {
                    _eventosHoy.value = UiState.Error("Error del servidor")
                }
            } catch (e: Exception) {
                _eventosHoy.value = UiState.Error("Sin conexión a internet")
            }
        }
    }
}
