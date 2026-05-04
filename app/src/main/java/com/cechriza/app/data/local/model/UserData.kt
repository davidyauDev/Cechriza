package com.cechriza.app.data.local.model

data class UserData(
    val id: Int,
    val staffId: Int? = null,
    val name: String,
    val email: String,
    val roles: List<String> = emptyList(),
    val empCode: String? = null
)
