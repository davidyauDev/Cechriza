package com.cechriza.app.data.remote.dto.request

data class MemoryMatchScoreRequest(
    val user_id: Int,
    val user_name: String,
    val moves: Int,
    val elapsed_seconds: Int,
    val matched_pairs: Int
)
