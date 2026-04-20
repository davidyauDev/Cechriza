package com.example.myapplication.data.remote.dto.response

data class LoginResponseTotal(
    val success: Boolean,
    val data: LoginResponseData,
    val message: String
)

data class LoginResponseData(
    val access_token: String,
    val user: UserResponse
)

data class UserResponse(
    val id: Int,
    val staff_id: Int? = null,
    val name: String,
    val email: String,
    val emp_code: String,
    val role: String,
    val active: Boolean,
    val deleted_at: String?
)
