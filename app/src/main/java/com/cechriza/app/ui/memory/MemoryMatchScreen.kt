package com.cechriza.app.ui.memory

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.cechriza.app.R
import com.cechriza.app.data.preferences.SessionManager
import com.cechriza.app.data.preferences.UserPreferences
import com.cechriza.app.data.remote.dto.request.MemoryMatchScoreRequest
import com.cechriza.app.data.remote.memory.MemoryMatchLeaderboardEntry
import com.cechriza.app.data.remote.memory.MemoryMatchMyScoreResult
import com.cechriza.app.data.remote.memory.MemoryMatchRemoteDataSource
import com.cechriza.app.ui.home.BrandBlue
import com.cechriza.app.ui.home.BrandBlueDark
import com.cechriza.app.ui.home.BrandBlueSoft
import com.cechriza.app.ui.home.BrandBorder
import com.cechriza.app.ui.home.BrandMuted
import com.cechriza.app.ui.home.BrandOrange
import com.cechriza.app.ui.home.BrandSurface
import com.cechriza.app.ui.home.BrandText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

private data class MemoryCard(
    val id: Int,
    val pairKey: Int,
    val imageRes: Int,
    val isFaceUp: Boolean = false,
    val isMatched: Boolean = false
)

private val memoryImages = listOf(
    R.drawable.memory_machine_1,
    R.drawable.memory_machine_2,
    R.drawable.memory_machine_3,
    R.drawable.memory_machine_4,
    R.drawable.memory_machine_5,
    R.drawable.memory_machine_6
)

@Composable
fun MemoryMatchScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    BackHandler { navController.popBackStack() }
    val context = LocalContext.current
    val userPreferences = remember(context) { UserPreferences(context) }
    val userId by userPreferences.userId.collectAsState(initial = SessionManager.userId ?: 0)
    val userName by userPreferences.userName.collectAsState(initial = SessionManager.userName.orEmpty())
    val userToken by userPreferences.userToken.collectAsState(initial = SessionManager.token.orEmpty())
    val screenScope = rememberCoroutineScope()
    val resolvedUserId = userId.takeIf { it > 0 } ?: (SessionManager.userId ?: 0)
    val resolvedUserName = userName.ifBlank { SessionManager.userName.orEmpty() }
    val resolvedUserToken = userToken.ifBlank { SessionManager.token.orEmpty() }

    var deckSeed by rememberSaveable { mutableIntStateOf(0) }
    var cards by remember(deckSeed) { mutableStateOf(buildDeck(deckSeed)) }
    var firstSelectedCardId by rememberSaveable(deckSeed) { mutableStateOf<Int?>(null) }
    var inputLocked by rememberSaveable(deckSeed) { mutableStateOf(false) }
    var moves by rememberSaveable(deckSeed) { mutableIntStateOf(0) }
    var matchedPairs by rememberSaveable(deckSeed) { mutableIntStateOf(0) }
    var started by rememberSaveable(deckSeed) { mutableStateOf(false) }
    var elapsedSeconds by rememberSaveable(deckSeed) { mutableLongStateOf(0L) }
    var gameWon by rememberSaveable(deckSeed) { mutableStateOf(false) }
    var leaderboard by remember { mutableStateOf<List<MemoryMatchLeaderboardEntry>>(emptyList()) }
    var leaderboardLoading by remember { mutableStateOf(true) }
    var leaderboardError by remember { mutableStateOf<String?>(null) }
    var submittingScore by rememberSaveable(deckSeed) { mutableStateOf(false) }
    var submittedScore by rememberSaveable(deckSeed) { mutableStateOf<Int?>(null) }
    var submittedBestScore by rememberSaveable(deckSeed) { mutableStateOf<Int?>(null) }
    var submittedBestElapsedSeconds by rememberSaveable(deckSeed) { mutableStateOf<Int?>(null) }
    var submittedBestMoves by rememberSaveable(deckSeed) { mutableStateOf<Int?>(null) }
    var submittedRank by rememberSaveable(deckSeed) { mutableStateOf<Int?>(null) }
    var scoreSubmitError by rememberSaveable(deckSeed) { mutableStateOf<String?>(null) }
    var scoreSaved by rememberSaveable(deckSeed) { mutableStateOf(false) }
    var scoreSubmitAttempted by rememberSaveable(deckSeed) { mutableStateOf(false) }
    var myScore by remember { mutableStateOf<MemoryMatchMyScoreResult?>(null) }
    var debugStatus by remember { mutableStateOf("Usuario actual: --\nEstado: inicial") }

    val totalPairs = memoryImages.size

    suspend fun loadLeaderboard() {
        leaderboardLoading = true
        leaderboardError = null
        val result = MemoryMatchRemoteDataSource.getLeaderboard(limit = 10)
        result
            .onSuccess { leaderboard = it }
            .onFailure { leaderboardError = it.message ?: "No se pudo cargar el ranking." }
        leaderboardLoading = false
    }

    suspend fun loadMyScore() {
        if (resolvedUserId <= 0) return
        MemoryMatchRemoteDataSource.getMyScore(resolvedUserId)
            .onSuccess {
                myScore = it.takeIf { score -> score.userId > 0 || score.bestScore > 0 }
                debugStatus = buildString {
                    append("Usuario actual: ")
                    append(resolvedUserName.ifBlank { "--" })
                    append(" · ID ")
                    append(resolvedUserId)
                    append("\nmy-score: ")
                    append(if (it.bestScore > 0) it.bestScore else "sin score")
                    append("\nrank: ")
                    append(it.rank ?: "--")
                }
            }
            .onFailure { }
    }

    LaunchedEffect(started, gameWon, deckSeed) {
        if (!started || gameWon) return@LaunchedEffect
        while (started && !gameWon) {
            delay(1000)
            elapsedSeconds += 1
        }
    }

    LaunchedEffect(Unit) {
        loadLeaderboard()
    }

    LaunchedEffect(resolvedUserId, resolvedUserToken) {
        submittedScore = null
        submittedBestScore = null
        submittedBestElapsedSeconds = null
        submittedBestMoves = null
        submittedRank = null
        scoreSubmitError = null
        scoreSaved = false
        scoreSubmitAttempted = false
        myScore = null
        debugStatus = "Usuario actual: ${resolvedUserName.ifBlank { "--" }} - ID $resolvedUserId\nEstado: sesion cargada"
        loadMyScore()
    }

    fun restartGame() {
        deckSeed += 1
    }

    fun revealCard(card: MemoryCard) {
        if (inputLocked || card.isFaceUp || card.isMatched) return
        if (!started) started = true

        cards = cards.map {
            if (it.id == card.id) it.copy(isFaceUp = true) else it
        }

        val firstCardId = firstSelectedCardId
        if (firstCardId == null) {
            firstSelectedCardId = card.id
            return
        }

        val firstCard = cards.firstOrNull { it.id == firstCardId } ?: return
        val secondCard = cards.firstOrNull { it.id == card.id } ?: return
        moves += 1
        firstSelectedCardId = null

        if (firstCard.pairKey == secondCard.pairKey) {
            cards = cards.map {
                if (it.id == firstCard.id || it.id == secondCard.id) it.copy(isMatched = true) else it
            }
            val newMatchedPairs = matchedPairs + 1
            matchedPairs = newMatchedPairs
            if (newMatchedPairs == totalPairs) {
                gameWon = true
            }
        } else {
            inputLocked = true
        }
    }

    LaunchedEffect(cards, inputLocked) {
        if (!inputLocked) return@LaunchedEffect
        delay(750)
        cards = cards.map { current ->
            if (!current.isMatched) current.copy(isFaceUp = false) else current
        }
        inputLocked = false
    }

    LaunchedEffect(
        gameWon,
        scoreSaved,
        scoreSubmitAttempted,
        resolvedUserId,
        resolvedUserName,
        resolvedUserToken,
        moves,
        elapsedSeconds,
        deckSeed
    ) {
        if (!gameWon || scoreSaved || submittingScore || scoreSubmitAttempted) return@LaunchedEffect
        if (resolvedUserId <= 0 || resolvedUserName.isBlank() || resolvedUserToken.isBlank()) {
            scoreSubmitError = "No se pudo identificar al usuario para registrar el score."
            debugStatus = "Usuario actual: ${resolvedUserName.ifBlank { "--" }} - ID $resolvedUserId\nPOST /scores ERROR\nsesion invalida"
            return@LaunchedEffect
        }

        submittingScore = true
        scoreSubmitAttempted = true
        scoreSubmitError = null
        debugStatus = "Usuario actual: $resolvedUserName · ID $resolvedUserId\nPOST /scores enviando..."
        try {
            val result = MemoryMatchRemoteDataSource.submitScore(
                MemoryMatchScoreRequest(
                    user_id = resolvedUserId,
                    user_name = resolvedUserName,
                    moves = moves,
                    elapsed_seconds = elapsedSeconds.toInt(),
                    matched_pairs = totalPairs
                )
            )
            result
                .onSuccess { score ->
                    scoreSaved = true
                    submittedScore = score.lastGameScore.takeIf { it > 0 }
                    submittedBestScore = score.bestScore.takeIf { it > 0 }
                    submittedBestElapsedSeconds = score.bestElapsedSeconds
                    submittedBestMoves = score.bestMoves
                    submittedRank = score.rank
                    debugStatus = buildString {
                        append("Usuario actual: ")
                        append(resolvedUserName)
                        append(" - ID ")
                        append(resolvedUserId)
                        append("\nPOST /scores OK")
                        append("\nlast_game_score: ")
                        append(score.lastGameScore)
                        append("\nbest_score: ")
                        append(score.bestScore)
                        append("\nrank: ")
                        append(score.rank ?: "--")
                    }
                    Log.d("MemoryMatch", debugStatus)
                }
                .onFailure {
                    scoreSubmitError = it.message ?: "No se pudo registrar el score."
                    debugStatus = buildString {
                        append("Usuario actual: ")
                        append(resolvedUserName)
                        append(" - ID ")
                        append(resolvedUserId)
                        append("\nPOST /scores ERROR")
                        append("\n")
                        append(scoreSubmitError.orEmpty())
                    }
                    Log.e("MemoryMatch", debugStatus)
                }

            runCatching { loadLeaderboard() }
            runCatching { loadMyScore() }
        } finally {
            submittingScore = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        BrandSurface,
                        Color(0xFFF9FBFF),
                        Color.White
                    )
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = WindowInsets.safeDrawing.asPaddingValues()
        ) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                MemoryTopBar(
                    onBack = { navController.popBackStack() },
                    onRestart = { restartGame() }
                )
                Spacer(modifier = Modifier.height(18.dp))
                MemoryHero(
                    matchedPairs = matchedPairs,
                    totalPairs = totalPairs,
                    moves = moves,
                    elapsedSeconds = elapsedSeconds
                )
                Spacer(modifier = Modifier.height(18.dp))
                BoardHint(
                    gameWon = gameWon,
                    inputLocked = inputLocked,
                    matchedPairs = matchedPairs,
                    totalPairs = totalPairs
                )
                Spacer(modifier = Modifier.height(16.dp))
                MemoryBoard(
                    cards = cards,
                    onCardClick = ::revealCard
                )
                Spacer(modifier = Modifier.height(20.dp))
                if (gameWon) {
                    VictoryCard(
                        elapsedSeconds = elapsedSeconds,
                        moves = moves,
                        score = submittedScore,
                        bestScore = submittedBestScore,
                        bestElapsedSeconds = submittedBestElapsedSeconds,
                        bestMoves = submittedBestMoves,
                        rank = submittedRank,
                        isSubmitting = submittingScore,
                        submitError = scoreSubmitError,
                        onRestart = { restartGame() }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }
                MemoryDebugCard(debugStatus = debugStatus)
                Spacer(modifier = Modifier.height(12.dp))
                MemoryLeaderboardSection(
                    leaderboard = leaderboard,
                    isLoading = leaderboardLoading,
                    error = leaderboardError,
                    currentUserId = resolvedUserId,
                    myScore = myScore,
                    onReload = {
                        screenScope.launch {
                            loadLeaderboard()
                            loadMyScore()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MemoryDebugCard(
    debugStatus: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.6f))
    ) {
        Text(
            text = debugStatus,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall,
            color = BrandMuted
        )
    }
}

@Composable
private fun MemoryTopBar(
    onBack: () -> Unit,
    onRestart: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Volver",
                    tint = BrandText
                )
            }
        }

        Surface(
            onClick = onRestart,
            color = Color.White,
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reiniciar",
                    tint = BrandBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Reiniciar",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MemoryHero(
    matchedPairs: Int,
    totalPairs: Int,
    moves: Int,
    elapsedSeconds: Long
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Memory Match",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Empareja las maquinas de Cechriza lo mas rapido posible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = BrandMuted
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MemoryMetric(
                    title = "Pares",
                    value = "$matchedPairs/$totalPairs",
                    modifier = Modifier.weight(1f)
                )
                MemoryMetric(
                    title = "Movs.",
                    value = moves.toString(),
                    modifier = Modifier.weight(1f)
                )
                MemoryMetric(
                    title = "Tiempo",
                    value = formatElapsedTime(elapsedSeconds),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MemoryMetric(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = BrandSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BrandBorder.copy(alpha = 0.75f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = BrandMuted
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                color = BrandText,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BoardHint(
    gameWon: Boolean,
    inputLocked: Boolean,
    matchedPairs: Int,
    totalPairs: Int
) {
    val text = when {
        gameWon -> "Juego completo. Puedes reiniciar para volver a mezclar el tablero."
        inputLocked -> "Comparando cartas. Espera un momento para tu siguiente jugada."
        matchedPairs == 0 -> "Toca una tarjeta para empezar. El cronometro arranca con tu primer intento."
        matchedPairs < totalPairs -> "Buen ritmo. Sigue emparejando las maquinas iguales."
        else -> "Todas las parejas fueron encontradas."
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = BrandBlueSoft,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = BrandBlueDark
        )
    }
}

@Composable
private fun MemoryBoard(
    cards: List<MemoryCard>,
    onCardClick: (MemoryCard) -> Unit
) {
    val rows = remember(cards) { cards.chunked(3) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { rowCards ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCards.forEach { card ->
                    MemoryCardCell(
                        card = card,
                        onClick = { onCardClick(card) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowCards.size < 3) {
                    repeat(3 - rowCards.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryCardCell(
    card: MemoryCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "memory-card-rotation"
    )
    val frontTint by animateColorAsState(
        targetValue = if (card.isMatched) Color(0xFFEAF8F0) else Color.White,
        animationSpec = tween(durationMillis = 180),
        label = "memory-card-surface"
    )

    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .graphicsLayer { rotationY = rotation }
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Transparent)
            .clickable(enabled = !card.isMatched && !card.isFaceUp, onClick = onClick)
    ) {
        if (rotation <= 90f) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(20.dp),
                color = BrandBlue,
                shadowElevation = 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(BrandBlue, BrandBlueDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "?",
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Cechriza",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationY = 180f },
                shape = RoundedCornerShape(20.dp),
                color = frontTint,
                shadowElevation = if (card.isMatched) 2.dp else 1.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = card.imageRes),
                        contentDescription = "Tarjeta de maquina",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (card.isMatched) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp),
                            shape = RoundedCornerShape(999.dp),
                            color = Color(0xFFECFDF3)
                        ) {
                            Text(
                                text = "OK",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF027A48),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VictoryCard(
    elapsedSeconds: Long,
    moves: Int,
    score: Int?,
    bestScore: Int?,
    bestElapsedSeconds: Int?,
    bestMoves: Int?,
    rank: Int?,
    isSubmitting: Boolean,
    submitError: String?,
    onRestart: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Partida completada",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Terminaste el tablero en ${formatElapsedTime(elapsedSeconds)} con $moves movimientos.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f)
            )
            when {
                isSubmitting -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Guardando score en el ranking global...",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
                score != null -> {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White.copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Ultima partida",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = score.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Ranking",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )
                                    Text(
                                        text = rank?.let { "#$it" } ?: "--",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (bestScore != null) {
                                Text(
                                    text = buildString {
                                        append("Mejor score: ")
                                        append(bestScore)
                                        if (bestElapsedSeconds != null || bestMoves != null) {
                                            append(" - ")
                                            append(bestMoves ?: "--")
                                            append(" movs.")
                                            append(" - ")
                                            append(
                                                bestElapsedSeconds?.let { formatElapsedTime(it.toLong()) } ?: "--:--"
                                            )
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }
                submitError != null -> {
                    Text(
                        text = submitError,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFECACA)
                    )
                }
            }
            Surface(
                onClick = onRestart,
                shape = RoundedCornerShape(16.dp),
                color = BrandOrange
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Jugar de nuevo",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MemoryLeaderboardSection(
    leaderboard: List<MemoryMatchLeaderboardEntry>,
    isLoading: Boolean,
    error: String?,
    currentUserId: Int,
    myScore: MemoryMatchMyScoreResult?,
    onReload: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Ranking global",
                        style = MaterialTheme.typography.titleLarge,
                        color = BrandText,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Top 10 visible para todos los usuarios.",
                        style = MaterialTheme.typography.bodySmall,
                        color = BrandMuted
                    )
                }
                Surface(
                    onClick = onReload,
                    color = BrandSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar ranking",
                            tint = BrandBlue
                        )
                    }
                }
            }

            when {
                isLoading -> {
                    repeat(3) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BrandSurface,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(64.dp))
                        }
                    }
                }
                error != null -> {
                    Surface(
                        color = Color(0xFFFFF4F4),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = error,
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFB42318)
                        )
                    }
                }
                leaderboard.isEmpty() -> {
                    Surface(
                        color = BrandSurface,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = "Aun no hay partidas registradas en el ranking.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = BrandMuted
                        )
                    }
                }
                else -> {
                    myScore?.let { mine ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = BrandBlueSoft,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BrandBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Tu mejor marca",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = BrandBlueDark
                                )
                                Text(
                                    text = "${mine.bestScore} pts - ${mine.bestMoves ?: "--"} movs. - ${
                                        mine.bestElapsedSeconds?.let { formatElapsedTime(it.toLong()) } ?: "--:--"
                                    }",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = BrandText,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Ranking actual: ${mine.rank?.let { "#$it" } ?: "--"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandMuted
                                )
                            }
                        }
                    }
                    LeaderboardTopSummary(leaderboard = leaderboard)
                    leaderboard.forEach { entry ->
                        LeaderboardRow(
                            entry = entry,
                            highlighted = currentUserId > 0 && currentUserId == entry.userId
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LeaderboardTopSummary(
    leaderboard: List<MemoryMatchLeaderboardEntry>
) {
    val leader = leaderboard.firstOrNull() ?: return
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Lider actual",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Text(
                    text = leader.userName.ifBlank { "Usuario" },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${leader.moves} movs. - ${formatElapsedTime(leader.elapsedSeconds.toLong())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.82f)
                )
            }
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = BrandOrange
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "#1",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = leader.score.toString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: MemoryMatchLeaderboardEntry,
    highlighted: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (highlighted) BrandBlueSoft else BrandSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            if (highlighted) BrandBorder else BrandBorder.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .background(
                        if (entry.rank <= 3) BrandBlue else Color.White,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${entry.rank}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (entry.rank <= 3) Color.White else BrandText,
                    fontWeight = FontWeight.Bold
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = entry.userName.ifBlank { "Usuario" },
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandText,
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    text = "${entry.moves} movs. - ${formatElapsedTime(entry.elapsedSeconds.toLong())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = BrandMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = entry.score.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = BrandText,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (highlighted) "Tu marca" else "score",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (highlighted) BrandBlue else BrandMuted
                )
            }
        }
    }
}

private fun buildDeck(seed: Int): List<MemoryCard> {
    val random = Random(seed + 97)
    return memoryImages.flatMapIndexed { index, imageRes ->
        listOf(
            MemoryCard(id = index * 2, pairKey = index, imageRes = imageRes),
            MemoryCard(id = index * 2 + 1, pairKey = index, imageRes = imageRes)
        )
    }.shuffled(random)
}

private fun formatElapsedTime(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}


