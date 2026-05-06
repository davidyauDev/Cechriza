package com.cechriza.app.data.remote.memory

import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.remote.dto.request.MemoryMatchScoreRequest
import com.cechriza.app.data.remote.network.ApiService
import com.cechriza.app.data.remote.network.RetrofitClient
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.CancellationException
import retrofit2.Response

data class MemoryMatchLeaderboardEntry(
    val rank: Int,
    val userId: Int,
    val userName: String,
    val score: Int,
    val moves: Int,
    val elapsedSeconds: Int,
    val matchedPairs: Int,
    val playedAt: String
)

data class MemoryMatchScoreResult(
    val id: Int?,
    val lastGameScore: Int,
    val bestScore: Int,
    val bestElapsedSeconds: Int?,
    val bestMoves: Int?,
    val rank: Int?,
    val playedAt: String?
)

data class MemoryMatchMyScoreResult(
    val userId: Int,
    val userName: String?,
    val bestScore: Int,
    val bestElapsedSeconds: Int?,
    val bestMoves: Int?,
    val rank: Int?
)

internal object MemoryMatchRemoteDataSource {

    private fun api(): ApiService = RetrofitClient.apiWithToken { SessionManager.token }

    suspend fun submitScore(request: MemoryMatchScoreRequest): Result<MemoryMatchScoreResult> {
        return try {
            val response = api().submitMemoryMatchScore(request)
            Result.success(parseSubmitResponse(response))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    suspend fun getLeaderboard(limit: Int = 10): Result<List<MemoryMatchLeaderboardEntry>> {
        return try {
            val response = api().getMemoryMatchLeaderboard(limit)
            Result.success(parseLeaderboardResponse(response))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    suspend fun getMyScore(userId: Int): Result<MemoryMatchMyScoreResult> {
        return try {
            val response = api().getMemoryMatchMyScore(userId)
            Result.success(parseMyScoreResponse(response))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (throwable: Throwable) {
            Result.failure(throwable)
        }
    }

    private fun parseSubmitResponse(response: Response<JsonElement>): MemoryMatchScoreResult {
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo registrar el score (${response.code()})")
        }

        val root = response.body()?.asJsonObjectOrNull()
            ?: return MemoryMatchScoreResult(
                id = null,
                lastGameScore = 0,
                bestScore = 0,
                bestElapsedSeconds = null,
                bestMoves = null,
                rank = null,
                playedAt = null
            )
        val data = root.get("data")?.asJsonObjectOrNull() ?: root

        return MemoryMatchScoreResult(
            id = data.intOrNull("id"),
            lastGameScore = data.intOrNull("last_game_score") ?: data.intOrNull("score") ?: 0,
            bestScore = data.intOrNull("best_score") ?: data.intOrNull("score") ?: 0,
            bestElapsedSeconds = data.intOrNull("best_elapsed_seconds") ?: data.intOrNull("elapsed_seconds"),
            bestMoves = data.intOrNull("best_moves") ?: data.intOrNull("moves"),
            rank = data.intOrNull("rank") ?: data.intOrNull("rank_position"),
            playedAt = data.stringOrNull("played_at")
        )
    }

    private fun parseLeaderboardResponse(response: Response<JsonElement>): List<MemoryMatchLeaderboardEntry> {
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo cargar el ranking (${response.code()})")
        }

        val root = response.body()
            ?: throw IllegalStateException("Respuesta vacia al cargar ranking")

        val entries = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> {
                val obj = root.asJsonObject
                when {
                    obj.get("data")?.asJsonObjectOrNull()?.get("leaderboard")?.isJsonArray == true ->
                        obj.get("data")!!.asJsonObject.getAsJsonArray("leaderboard")
                    obj.get("data")?.isJsonArray == true -> obj.getAsJsonArray("data")
                    obj.get("leaderboard")?.isJsonArray == true -> obj.getAsJsonArray("leaderboard")
                    else -> JsonArray()
                }
            }
            else -> JsonArray()
        }

        return entries.mapIndexedNotNull { index, element ->
            val item = element.asJsonObjectOrNull() ?: return@mapIndexedNotNull null
            MemoryMatchLeaderboardEntry(
                rank = item.intOrNull("rank")
                    ?: item.intOrNull("rank_position")
                    ?: (index + 1),
                userId = item.intOrNull("user_id") ?: 0,
                userName = item.stringOrNull("user_name").orEmpty(),
                score = item.intOrNull("best_score") ?: item.intOrNull("score") ?: 0,
                moves = item.intOrNull("best_moves") ?: item.intOrNull("moves") ?: 0,
                elapsedSeconds = item.intOrNull("best_elapsed_seconds") ?: item.intOrNull("elapsed_seconds") ?: 0,
                matchedPairs = item.intOrNull("matched_pairs") ?: 0,
                playedAt = item.stringOrNull("played_at").orEmpty()
            )
        }
    }

    private fun parseMyScoreResponse(response: Response<JsonElement>): MemoryMatchMyScoreResult {
        if (response.code() == 404) {
            return MemoryMatchMyScoreResult(
                userId = 0,
                userName = null,
                bestScore = 0,
                bestElapsedSeconds = null,
                bestMoves = null,
                rank = null
            )
        }
        if (!response.isSuccessful) {
            throw IllegalStateException("No se pudo cargar el score del usuario (${response.code()})")
        }

        val root = response.body()?.asJsonObjectOrNull()
            ?: throw IllegalStateException("Respuesta vacia al cargar score del usuario")
        val data = root.get("data")?.asJsonObjectOrNull() ?: root

        return MemoryMatchMyScoreResult(
            userId = data.intOrNull("user_id") ?: 0,
            userName = data.stringOrNull("user_name"),
            bestScore = data.intOrNull("best_score") ?: 0,
            bestElapsedSeconds = data.intOrNull("best_elapsed_seconds"),
            bestMoves = data.intOrNull("best_moves"),
            rank = data.intOrNull("rank") ?: data.intOrNull("rank_position")
        )
    }

    private fun JsonElement.asJsonObjectOrNull(): JsonObject? {
        return if (isJsonObject) asJsonObject else null
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val value = get(key) ?: return null
        return runCatching { value.asInt }.getOrNull()
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val value = get(key) ?: return null
        return runCatching { value.asString }.getOrNull()
    }
}
