package com.cechriza.app.data.repository
import com.cechriza.app.data.remote.dto.response.LoginResponseTotal
import com.cechriza.app.data.remote.dto.request.LoginRequest
import com.cechriza.app.data.remote.network.ApiService

class AuthRepository(private val api: ApiService) {

    suspend fun login(empCode: String, password: String): Result<LoginResponseTotal> {
        return try {
            val response = api.login(LoginRequest(empCode, password))
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(Exception("Respuesta vacía del servidor"))
                if (!body.success) {
                    return Result.failure(Exception(body.message))
                }
                return Result.success(body)

            } else {
                when (response.code()) {
                    401 -> Result.failure(Exception("Credenciales incorrectas"))
                    403 -> Result.failure(Exception("Acceso no autorizado"))
                    else -> Result.failure(Exception("Error ${response.code()}: ${response.message()}"))
                }
            }

        } catch (e: Exception) {
            Result.failure(Exception("Error de red: ${e.message}"))
        }
    }
}

