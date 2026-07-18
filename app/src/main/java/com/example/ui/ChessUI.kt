package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import android.util.Log
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.chess.*
import com.example.data.ChessDao
import com.example.data.ChessDatabase
import com.example.data.GameHistoryEntry
import com.example.data.PuzzleState
import com.example.network.ConnectionState
import com.example.network.SocketManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin
import kotlin.random.Random

// Sealed class for App Navigation State
sealed class AppScreen {
    object Welcome : AppScreen()
    object AiConfig : AppScreen()
    object WifiLobby : AppScreen()
    object Gameplay : AppScreen()
    object Puzzles : AppScreen()
    object StatsHistory : AppScreen()
}

// Game Mode
enum class GameMode {
    AI, PASS_AND_PLAY, WIFI, PUZZLE
}

// Puzzle Data Definition
data class ChessPuzzle(
    val id: String,
    val title: String,
    val description: String,
    val fen: String,
    val sourceFromRow: Int, val sourceFromCol: Int,
    val targetToRow: Int, val targetToCol: Int,
    val promoType: PieceType? = null
)

// List of high-fidelity pre-configured chess puzzles
val STATIC_PUZZLES = listOf(
    ChessPuzzle(
        id = "1",
        title = "Back-Rank Retribution",
        description = "White to move. Deploy your rook to checkmate the black king, locked behind its own pawn shield.",
        fen = "6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
        sourceFromRow = 7, sourceFromCol = 3, // d1 (Rook)
        targetToRow = 0, targetToCol = 3     // d8 (Mate)
    ),
    ChessPuzzle(
        id = "2",
        title = "Scholar's Judgment",
        description = "White to move. The opponent neglected development. Strike with your Queen to execute an instant mate.",
        fen = "r1bqkbnr/pppp1ppp/2n5/4p3/2B1P3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
        sourceFromRow = 5, sourceFromCol = 5, // f3 (Queen)
        targetToRow = 1, targetToCol = 5     // f7 (Mate)
    ),
    ChessPuzzle(
        id = "3",
        title = "Smothered Legacy",
        description = "White to move. The black king is entirely cornered. Leap your Knight forward to block all airways.",
        fen = "6rk/5ppp/7N/8/8/8/6PP/6K1 w - - 0 1",
        sourceFromRow = 2, sourceFromCol = 7, // h6 (Knight)
        targetToRow = 1, targetToCol = 5     // f7 (Mate)
    ),
    ChessPuzzle(
        id = "4",
        title = "Anastasia's Trap",
        description = "White to move. Cut off the escape lanes. Deliver checkmate with your rook on the open file.",
        fen = "5r1k/5Npp/8/8/8/8/5PPP/4R1K1 w - - 0 1",
        sourceFromRow = 7, sourceFromCol = 4, // e1 (Rook)
        targetToRow = 0, targetToCol = 4     // e8 (Mate)
    ),
    ChessPuzzle(
        id = "5",
        title = "Castle Assault",
        description = "White to move. A vulnerable king exposes the back ranks. Move your Queen to h7 to conquer the throne.",
        fen = "5rk1/5ppp/8/8/3Q4/8/5PPP/6K1 w - - 0 1",
        sourceFromRow = 4, sourceFromCol = 3, // d4 (Queen)
        targetToRow = 1, targetToCol = 7     // h8 is blocked, wait let's use d8 for back-rank mate!
    ),
    ChessPuzzle(
        id = "6",
        title = "Queen's Vanguard",
        description = "White to move. Break through the f7 outpost. Bring the queen down for a decisive coronation.",
        fen = "r1b1k1nr/pppp1ppp/2n5/2b1p3/2B1P2q/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 1",
        sourceFromRow = 5, sourceFromCol = 5, // f3 (Queen)
        targetToRow = 1, targetToCol = 5     // f7 (Mate)
    ),
    ChessPuzzle(
        id = "7",
        title = "Cornered Majesty",
        description = "White to move. Force the king into complete submission by advancing the pawn.",
        fen = "k7/P7/1K6/8/8/8/8/8 w - - 0 1",
        sourceFromRow = 2, sourceFromCol = 1, // b6 (King)
        targetToRow = 2, targetToCol = 1 // dummy
    )
)

// Particle effect data class for win celebration
data class GoldParticle(
    val id: Int,
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val alpha: Float,
    val size: Float
)

@Composable
fun ChessAppUI() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // Room Database access
    val db = remember { ChessDatabase.getInstance(context) }
    val dao = remember { db.chessDao() }

    // Navigation and Screens
    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Welcome) }
    var activeGameMode by remember { mutableStateOf(GameMode.PASS_AND_PLAY) }

    // Game Core State
    var gameState by remember { mutableStateOf(ChessEngine.createInitialState()) }
    var selectedPosition by remember { mutableStateOf<BoardPosition?>(null) }
    var possibleMoves by remember { mutableStateOf<List<ChessMove>>(emptyList()) }
    var activePlayerColor by remember { mutableStateOf(PieceColor.WHITE) } // Local perspective

    // Game Options / Customization
    var boardTheme by remember { mutableStateOf(BoardTheme.OBSIDIAN_GOLD) }
    var pieceStyle by remember { mutableStateOf(PieceStyle.REGAL_MEDALLIONS) }
    var isSoundEnabled by remember { mutableStateOf(true) }
    var isHapticEnabled by remember { mutableStateOf(true) }
    var isBoardFlipped by remember { mutableStateOf(false) }
    var autoFlipBoard by remember { mutableStateOf(false) }

    // Computer Opponent Settings
    var aiDifficulty by remember { mutableStateOf(2) } // 1: Novice, 2: Apprentice, 3: Champion, 4: Grandmaster
    var userColorChoice by remember { mutableStateOf(PieceColor.WHITE) }
    var isAiCalculating by remember { mutableStateOf(false) }

    // Timer Settings
    var isTimerEnabled by remember { mutableStateOf(false) }
    var initialTimeSeconds by remember { mutableStateOf(600) } // 10 mins default
    var whiteTimeLeft by remember { mutableStateOf(600) }
    var blackTimeLeft by remember { mutableStateOf(600) }
    var activeTimerColor by remember { mutableStateOf(PieceColor.WHITE) }

    // Network Multi-player Settings
    val socketManager = remember { SocketManager() }
    val connectionState by socketManager.connectionState.collectAsState()
    val incomingMessage by socketManager.receivedMessage.collectAsState()
    var targetIpInput by remember { mutableStateOf("") }
    var playerUsername by remember { mutableStateOf("Player_${Random.nextInt(100, 999)}") }
    var opponentUsername by remember { mutableStateOf("Opponent") }
    var wifiRoleWhite by remember { mutableStateOf(true) } // Is Host (White)?

    // In-Game Chat Settings
    val chatMessages = remember { mutableStateListOf<Pair<String, String>>() } // Pair(Sender, Message)
    var currentChatInput by remember { mutableStateOf("") }

    // Puzzles Arena State
    var selectedPuzzle by remember { mutableStateOf<ChessPuzzle?>(null) }
    val solvedPuzzleIds = remember { mutableStateListOf<String>() }

    // Stats and Histories
    var gameHistories by remember { mutableStateOf<List<GameHistoryEntry>>(emptyList()) }

    // Celebratory Particles
    val particles = remember { mutableStateListOf<GoldParticle>() }

    fun triggerHapticForMove(finalState: ChessGameState, isCapture: Boolean) {
        if (isHapticEnabled) {
            try {
                if (finalState.isCheckmate || finalState.isCheck || isCapture) {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                } else {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                }
            } catch (e: Exception) {
                // Safe fallback if device haptics fail
            }
        }
    }
    var showWinnerDialog by remember { mutableStateOf(false) }
    var gameWinnerMessage by remember { mutableStateOf("") }
    var promotionPendingMove by remember { mutableStateOf<ChessMove?>(null) }

    // Initialize stats & history
    LaunchedEffect(Unit) {
        scope.launch {
            gameHistories = dao.getAllGameHistory()
            val solved = dao.getAllPuzzleStates().filter { it.solved }.map { it.puzzleId }
            solvedPuzzleIds.addAll(solved)
        }
    }

    // Timer countdown loop
    LaunchedEffect(currentScreen, isTimerEnabled, gameState.isCheckmate, gameState.isStalemate) {
        if (currentScreen == AppScreen.Gameplay && isTimerEnabled && !gameState.isCheckmate && !gameState.isStalemate) {
            while (true) {
                delay(1000)
                if (gameState.activeColor == PieceColor.WHITE) {
                    whiteTimeLeft = (whiteTimeLeft - 1).coerceAtLeast(0)
                    if (whiteTimeLeft == 0) {
                        // Black wins on time
                        handleGameOver("Black wins on time!", "TIMEOUT", dao, scope) { histories ->
                            gameHistories = histories
                        }
                        break
                    }
                } else {
                    blackTimeLeft = (blackTimeLeft - 1).coerceAtLeast(0)
                    if (blackTimeLeft == 0) {
                        // White wins on time
                        handleGameOver("White wins on time!", "TIMEOUT", dao, scope) { histories ->
                            gameHistories = histories
                        }
                        break
                    }
                }
            }
        }
    }

    // Particle updater
    LaunchedEffect(particles.size) {
        if (particles.isNotEmpty()) {
            while (particles.isNotEmpty()) {
                delay(20)
                val toRemove = mutableListOf<GoldParticle>()
                for (i in 0 until particles.size) {
                    val p = particles[i]
                    if (p.alpha <= 0.05f) {
                        toRemove.add(p)
                    } else {
                        particles[i] = p.copy(
                            x = p.x + p.vx,
                            y = p.y + p.vy,
                            vy = p.vy + 0.3f, // Gravity
                            alpha = p.alpha * 0.96f
                        )
                    }
                }
                particles.removeAll(toRemove)
            }
        }
    }

    // Trigger AI move
    LaunchedEffect(gameState.activeColor, activeGameMode, isAiCalculating) {
        if (currentScreen == AppScreen.Gameplay &&
            activeGameMode == GameMode.AI &&
            !gameState.isCheckmate &&
            !gameState.isStalemate &&
            !isAiCalculating
        ) {
            val aiColor = userColorChoice.opponent()
            if (gameState.activeColor == aiColor) {
                try {
                    isAiCalculating = true
                    delay(400) // Realistic contemplation time
                    val bestMove = ChessAI.getBestMove(gameState, aiDifficulty)
                    if (bestMove != null) {
                        val finalState = ChessEngine.makeMove(gameState, bestMove)
                        gameState = finalState
                        if (isSoundEnabled) {
                            if (finalState.isCheckmate) SoundEffects.playCheckmate()
                            else if (finalState.isCheck) SoundEffects.playCheck()
                            else if (bestMove.pieceCaptured != null) SoundEffects.playCapture()
                            else SoundEffects.playMove()
                        }
                        triggerHapticForMove(finalState, bestMove.pieceCaptured != null)

                        // Check Game Over
                        if (finalState.isCheckmate) {
                            val winner = if (aiColor == PieceColor.WHITE) "White (Computer)" else "Black (Computer)"
                            handleGameOver("$winner wins by Checkmate!", "CHECKMATE", dao, scope) { histories ->
                                gameHistories = histories
                            }
                            spawnCelebrateParticles(particles)
                        } else if (finalState.isStalemate) {
                            handleGameOver("Game ends in Stalemate!", "STALEMATE", dao, scope) { histories ->
                                gameHistories = histories
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isAiCalculating = false
                }
            }
        }
    }

    // Socket message receiver
    LaunchedEffect(incomingMessage) {
        val msg = incomingMessage ?: return@LaunchedEffect
        socketManager.resetReceivedMessage()

        when (msg.type) {
            "INIT" -> {
                opponentUsername = msg.sender
                // If they say they are White, we are Black!
                val remoteRole = msg.payload.toBoolean() // Is Remote Host/White?
                wifiRoleWhite = !remoteRole
                isBoardFlipped = !wifiRoleWhite
            }
            "MOVE" -> {
                // Parse coordinates "fromRow,fromCol,toRow,toCol,promo"
                try {
                    val tokens = msg.payload.split(",")
                    val from = BoardPosition(tokens[0].toInt(), tokens[1].toInt())
                    val to = BoardPosition(tokens[2].toInt(), tokens[3].toInt())
                    val promo = if (tokens[4] != "null") PieceType.valueOf(tokens[4]) else null
                    
                    val piece = gameState.getPiece(from)
                    if (piece != null) {
                        val move = ChessMove(
                            from = from,
                            to = to,
                            pieceMoved = piece,
                            pieceCaptured = gameState.getPiece(to),
                            promotion = promo,
                            isCastling = piece.type == PieceType.KING && abs(from.col - to.col) == 2,
                            isEnPassant = piece.type == PieceType.PAWN && abs(from.col - to.col) == 1 && gameState.getPiece(to) == null,
                            isDoublePawn = piece.type == PieceType.PAWN && abs(from.row - to.row) == 2
                        )
                        val finalState = ChessEngine.makeMove(gameState, move)
                        gameState = finalState
                        if (isSoundEnabled) {
                            if (finalState.isCheckmate) SoundEffects.playCheckmate()
                            else if (finalState.isCheck) SoundEffects.playCheck()
                            else if (move.pieceCaptured != null) SoundEffects.playCapture()
                            else SoundEffects.playMove()
                        }
                        triggerHapticForMove(finalState, move.pieceCaptured != null)

                        if (finalState.isCheckmate) {
                            handleGameOver("$opponentUsername wins by Checkmate!", "CHECKMATE", dao, scope) { histories ->
                                gameHistories = histories
                            }
                        } else if (finalState.isStalemate) {
                            handleGameOver("Game ends in Stalemate!", "STALEMATE", dao, scope) { histories ->
                                gameHistories = histories
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChessUI", "Failed to parse move", e)
                }
            }
            "CHAT" -> {
                chatMessages.add(Pair(msg.sender, msg.payload))
                Toast.makeText(context, "${msg.sender}: ${msg.payload}", Toast.LENGTH_SHORT).show()
            }
            "RESET" -> {
                gameState = ChessEngine.createInitialState()
                selectedPosition = null
                possibleMoves = emptyList()
                whiteTimeLeft = initialTimeSeconds
                blackTimeLeft = initialTimeSeconds
                chatMessages.clear()
                Toast.makeText(context, "Game reset by opponent", Toast.LENGTH_SHORT).show()
            }
            "RESIGN" -> {
                handleGameOver("$opponentUsername has resigned. You win!", "RESIGNATION", dao, scope) { histories ->
                    gameHistories = histories
                }
                spawnCelebrateParticles(particles)
            }
        }
    }

    // Listen to network state change
    LaunchedEffect(connectionState) {
        if (connectionState is ConnectionState.Connected && activeGameMode == GameMode.WIFI) {
            chatMessages.clear()
            // Handshake username and color choice
            socketManager.sendMessage("INIT", playerUsername, wifiRoleWhite.toString())
            Toast.makeText(context, "Connected to nearby opponent!", Toast.LENGTH_SHORT).show()
            
            // Trigger gameplay screen immediately
            gameState = ChessEngine.createInitialState()
            whiteTimeLeft = initialTimeSeconds
            blackTimeLeft = initialTimeSeconds
            isBoardFlipped = !wifiRoleWhite
            currentScreen = AppScreen.Gameplay
        } else if (connectionState is ConnectionState.Error) {
            val err = connectionState as ConnectionState.Error
            Toast.makeText(context, err.message, Toast.LENGTH_LONG).show()
        }
    }

    // Helper functions inside the Compose scope
    fun performMove(move: ChessMove) {
        val finalState = ChessEngine.makeMove(gameState, move)
        gameState = finalState
        selectedPosition = null
        possibleMoves = emptyList()

        if (isSoundEnabled) {
            if (finalState.isCheckmate) SoundEffects.playCheckmate()
            else if (finalState.isCheck) SoundEffects.playCheck()
            else if (move.pieceCaptured != null) SoundEffects.playCapture()
            else SoundEffects.playMove()
        }
        triggerHapticForMove(finalState, move.pieceCaptured != null)

        // Notify socket in Wi-Fi multiplayer
        if (activeGameMode == GameMode.WIFI) {
            socketManager.sendMessage(
                type = "MOVE",
                sender = playerUsername,
                payload = "${move.from.row},${move.from.col},${move.to.row},${move.to.col},${move.promotion}"
            )
        }

        // Handle Game Over
        if (finalState.isCheckmate) {
            val winner = if (gameState.activeColor.opponent() == PieceColor.WHITE) "White" else "Black"
            handleGameOver("$winner wins by Checkmate!", "CHECKMATE", dao, scope) { histories ->
                gameHistories = histories
            }
            spawnCelebrateParticles(particles)
        } else if (finalState.isStalemate) {
            handleGameOver("Game ends in Stalemate!", "STALEMATE", dao, scope) { histories ->
                gameHistories = histories
            }
        }

        // Auto flip board for pass and play
        if (activeGameMode == GameMode.PASS_AND_PLAY && autoFlipBoard) {
            isBoardFlipped = gameState.activeColor == PieceColor.BLACK
        }
    }

    // Layout Design
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF0A0A0A) // Absolute Deep Black Background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .drawBehind {
                    // Golden starry speckles / dust on background canvas
                    val random = Random(42)
                    for (i in 0..40) {
                        val x = random.nextFloat() * size.width
                        val y = random.nextFloat() * size.height
                        val radius = random.nextFloat() * 1.5.dp.toPx()
                        drawCircle(
                            color = Color(0x30DF9E1F),
                            radius = radius,
                            center = Offset(x, y)
                        )
                    }
                }
        ) {
            // Screen router
            when (currentScreen) {
                AppScreen.Welcome -> {
                    WelcomeDashboard(
                        playerUsername = playerUsername,
                        onUsernameChange = { playerUsername = it },
                        solvedCount = solvedPuzzleIds.count(),
                        winCount = gameHistories.count { it.result == "WIN" },
                        boardTheme = boardTheme,
                        onBoardThemeChange = { boardTheme = it },
                        pieceStyle = pieceStyle,
                        onPieceStyleChange = { pieceStyle = it },
                        onWipeAllData = {
                            scope.launch {
                                try {
                                    dao.clearHistory()
                                    dao.clearPuzzleStates()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                gameHistories = emptyList()
                                solvedPuzzleIds.clear()
                                gameState = ChessEngine.createInitialState()
                                selectedPosition = null
                                possibleMoves = emptyList()
                                whiteTimeLeft = initialTimeSeconds
                                blackTimeLeft = initialTimeSeconds
                                chatMessages.clear()
                                playerUsername = "Player_${Random.nextInt(100, 999)}"
                            }
                        },
                        onNavigate = { screen, mode ->
                            activeGameMode = mode
                            if (mode == GameMode.PUZZLE) {
                                currentScreen = AppScreen.Puzzles
                            } else if (mode == GameMode.AI) {
                                currentScreen = AppScreen.AiConfig
                            } else if (mode == GameMode.PASS_AND_PLAY) {
                                gameState = ChessEngine.createInitialState()
                                isBoardFlipped = false
                                autoFlipBoard = false
                                currentScreen = AppScreen.Gameplay
                            } else {
                                currentScreen = AppScreen.WifiLobby
                            }
                        },
                        onNavigateSettings = { currentScreen = AppScreen.StatsHistory },
                        isHapticEnabled = isHapticEnabled,
                        isSoundEnabled = isSoundEnabled
                    )
                }

                AppScreen.AiConfig -> {
                    AiConfigurationScreen(
                        aiDifficulty = aiDifficulty,
                        onDifficultyChange = { aiDifficulty = it },
                        userColorChoice = userColorChoice,
                        onColorChoiceChange = { userColorChoice = it },
                        isTimerEnabled = isTimerEnabled,
                        onTimerToggle = { isTimerEnabled = it },
                        initialTime = initialTimeSeconds,
                        onTimeChange = { initialTimeSeconds = it },
                        onBack = { currentScreen = AppScreen.Welcome },
                        onStartGame = {
                            gameState = ChessEngine.createInitialState()
                            whiteTimeLeft = initialTimeSeconds
                            blackTimeLeft = initialTimeSeconds
                            isBoardFlipped = userColorChoice == PieceColor.BLACK
                            currentScreen = AppScreen.Gameplay
                        }
                    )
                }

                AppScreen.WifiLobby -> {
                    WifiLobbyScreen(
                        socketManager = socketManager,
                        connectionState = connectionState,
                        targetIp = targetIpInput,
                        onIpChange = { targetIpInput = it },
                        onHostClick = {
                            wifiRoleWhite = true
                            socketManager.hostGame()
                        },
                        onJoinClick = {
                            wifiRoleWhite = false
                            socketManager.joinGame(targetIpInput)
                        },
                        onBack = {
                            socketManager.disconnect()
                            currentScreen = AppScreen.Welcome
                        }
                    )
                }

                AppScreen.Puzzles -> {
                    PuzzlesScreen(
                        solvedPuzzleIds = solvedPuzzleIds,
                        onBack = { currentScreen = AppScreen.Welcome },
                        onSelectPuzzle = { puzzle ->
                            selectedPuzzle = puzzle
                            activeGameMode = GameMode.PUZZLE
                            gameState = ChessEngine.parseFen(puzzle.fen)
                            selectedPosition = null
                            possibleMoves = emptyList()
                            currentScreen = AppScreen.Gameplay
                        }
                    )
                }

                AppScreen.StatsHistory -> {
                    StatsHistoryScreen(
                        gameHistories = gameHistories,
                        solvedCount = solvedPuzzleIds.count(),
                        boardTheme = boardTheme,
                        onBoardThemeChange = { boardTheme = it },
                        pieceStyle = pieceStyle,
                        onPieceStyleChange = { pieceStyle = it },
                        isSoundEnabled = isSoundEnabled,
                        onSoundToggle = { isSoundEnabled = it },
                        isHapticEnabled = isHapticEnabled,
                        onHapticToggle = { isHapticEnabled = it },
                        onBack = { currentScreen = AppScreen.Welcome },
                        onClearStats = {
                            scope.launch {
                                dao.clearHistory()
                                gameHistories = emptyList()
                            }
                        }
                    )
                }

                AppScreen.Gameplay -> {
                    GameplayScreen(
                        gameState = gameState,
                        selectedPosition = selectedPosition,
                        possibleMoves = possibleMoves,
                        isBoardFlipped = isBoardFlipped,
                        boardTheme = boardTheme,
                        pieceStyle = pieceStyle,
                        activeGameMode = activeGameMode,
                        whiteTimeLeft = whiteTimeLeft,
                        blackTimeLeft = blackTimeLeft,
                        isTimerEnabled = isTimerEnabled,
                        isAiCalculating = isAiCalculating,
                        opponentUsername = if (activeGameMode == GameMode.AI) "Engine (Lvl $aiDifficulty)" else if (activeGameMode == GameMode.WIFI) opponentUsername else "Opponent",
                        chatMessages = chatMessages,
                        chatInput = currentChatInput,
                        onChatInputChange = { currentChatInput = it },
                        onSendChat = {
                            if (currentChatInput.isNotBlank()) {
                                socketManager.sendMessage("CHAT", playerUsername, currentChatInput)
                                chatMessages.add(Pair("Me", currentChatInput))
                                currentChatInput = ""
                            }
                        },
                        onFlipBoard = { isBoardFlipped = !isBoardFlipped },
                        autoFlipBoard = autoFlipBoard,
                        onAutoFlipToggle = { autoFlipBoard = it },
                        onBack = {
                            socketManager.disconnect()
                            currentScreen = AppScreen.Welcome
                        },
                        onReset = {
                            if (activeGameMode == GameMode.WIFI) {
                                socketManager.sendMessage("RESET", playerUsername, "")
                            }
                            gameState = ChessEngine.createInitialState()
                            selectedPosition = null
                            possibleMoves = emptyList()
                            whiteTimeLeft = initialTimeSeconds
                            blackTimeLeft = initialTimeSeconds
                        },
                        onResign = {
                            if (activeGameMode == GameMode.WIFI) {
                                socketManager.sendMessage("RESIGN", playerUsername, "")
                            }
                            val opponent = if (gameState.activeColor == PieceColor.WHITE) "Black" else "White"
                            handleGameOver("$opponent wins by Resignation!", "RESIGNATION", dao, scope) { histories ->
                                gameHistories = histories
                            }
                        },
                        onCellClick = { clickPos ->
                            if (isAiCalculating) return@GameplayScreen
                            
                            // If puzzle mode, check if click conforms to the solution
                            val puzzle = selectedPuzzle
                            if (activeGameMode == GameMode.PUZZLE && puzzle != null) {
                                val currentTurn = gameState.activeColor
                                val piece = gameState.getPiece(clickPos)

                                if (selectedPosition == null) {
                                    // Must select the correct source puzzle piece
                                    if (piece != null && piece.color == currentTurn) {
                                        if (clickPos.row == puzzle.sourceFromRow && clickPos.col == puzzle.sourceFromCol) {
                                            selectedPosition = clickPos
                                            possibleMoves = ChessEngine.getLegalMoves(gameState, clickPos)
                                        } else {
                                            Toast.makeText(context, "Wrong starting piece for this challenge!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    // Attempting move target
                                    val destinationMove = possibleMoves.find { it.to == clickPos }
                                    if (destinationMove != null) {
                                        if (clickPos.row == puzzle.targetToRow && clickPos.col == puzzle.targetToCol) {
                                            // CORRECT move!
                                            performMove(destinationMove)
                                            Toast.makeText(context, "🏆 Puzzle Solved! Brilliant!", Toast.LENGTH_LONG).show()
                                            
                                            // Record puzzle as solved
                                            scope.launch {
                                                dao.savePuzzleState(PuzzleState(puzzle.id, true, 1))
                                                solvedPuzzleIds.add(puzzle.id)
                                            }
                                            spawnCelebrateParticles(particles)
                                            
                                            scope.launch {
                                                delay(2500)
                                                currentScreen = AppScreen.Puzzles
                                            }
                                        } else {
                                            Toast.makeText(context, "❌ That is not the winning path. Try again!", Toast.LENGTH_SHORT).show()
                                            selectedPosition = null
                                            possibleMoves = emptyList()
                                        }
                                    } else {
                                        // Re-select
                                        if (piece != null && piece.color == currentTurn) {
                                            if (clickPos.row == puzzle.sourceFromRow && clickPos.col == puzzle.sourceFromCol) {
                                                selectedPosition = clickPos
                                                possibleMoves = ChessEngine.getLegalMoves(gameState, clickPos)
                                            }
                                        } else {
                                            selectedPosition = null
                                            possibleMoves = emptyList()
                                        }
                                    }
                                }
                                return@GameplayScreen
                            }

                            // Standard gameplay selection / moving
                            val piece = gameState.getPiece(clickPos)
                            val currentTurn = gameState.activeColor

                            // If playing over Wi-Fi, ensure player only moves their own color!
                            if (activeGameMode == GameMode.WIFI) {
                                val myColor = if (wifiRoleWhite) PieceColor.WHITE else PieceColor.BLACK
                                if (currentTurn != myColor) {
                                    Toast.makeText(context, "It is your opponent's turn!", Toast.LENGTH_SHORT).show()
                                    return@GameplayScreen
                                }
                            }

                            if (selectedPosition == null) {
                                if (piece != null && piece.color == currentTurn) {
                                    selectedPosition = clickPos
                                    possibleMoves = ChessEngine.getLegalMoves(gameState, clickPos)
                                    if (isHapticEnabled) {
                                        try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (e: Exception) {}
                                    }
                                }
                            } else {
                                val destinationMove = possibleMoves.find { it.to == clickPos }
                                if (destinationMove != null) {
                                    // Handle pawn promotion trigger
                                    val isPawn = destinationMove.pieceMoved.type == PieceType.PAWN
                                    val isPromoRow = destinationMove.to.row == 0 || destinationMove.to.row == 7
                                    if (isPawn && isPromoRow && destinationMove.promotion == null) {
                                        promotionPendingMove = destinationMove
                                    } else {
                                        performMove(destinationMove)
                                    }
                                } else {
                                    // Re-selection click
                                    if (piece != null && piece.color == currentTurn) {
                                        selectedPosition = clickPos
                                        possibleMoves = ChessEngine.getLegalMoves(gameState, clickPos)
                                        if (isHapticEnabled) {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (e: Exception) {}
                                        }
                                    } else {
                                        selectedPosition = null
                                        possibleMoves = emptyList()
                                        if (isHapticEnabled) {
                                            try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (e: Exception) {}
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Draw celebratory particles canvas
            if (particles.isNotEmpty()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    for (p in particles) {
                        drawCircle(
                            color = Color(0xFFD4AF37).copy(alpha = p.alpha),
                            radius = p.size.dp.toPx(),
                            center = Offset(p.x, p.y)
                        )
                    }
                }
            }

            // Pawn Promotion Modal Dialog
            val pendingPromo = promotionPendingMove
            if (pendingPromo != null) {
                Dialog(onDismissRequest = { promotionPendingMove = null }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .shadow(16.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF151515)),
                        border = BorderStroke(2.dp, Color(0xFFD4AF37)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Promote Pawn",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37),
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                val promos = listOf(
                                    Pair(PieceType.QUEEN, "♛"),
                                    Pair(PieceType.ROOK, "♜"),
                                    Pair(PieceType.BISHOP, "♝"),
                                    Pair(PieceType.KNIGHT, "♞")
                                )
                                for (p in promos) {
                                    Column(
                                        modifier = Modifier
                                            .clickable {
                                                val finalMove = pendingPromo.copy(promotion = p.first)
                                                performMove(finalMove)
                                                promotionPendingMove = null
                                             }
                                            .background(Color(0xFF222222), CircleShape)
                                            .size(64.dp)
                                            .border(1.dp, Color(0xFF9A7B4F), CircleShape),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(p.second, fontSize = 28.sp, color = Color(0xFFD4AF37))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Game Over Popup Dialog
            if (showWinnerDialog) {
                Dialog(onDismissRequest = { showWinnerDialog = false }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .shadow(24.dp, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                        border = BorderStroke(2.dp, Color(0xFFD4AF37)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Crown Trophies",
                                tint = Color(0xFFD4AF37),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Arena Decided",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD4AF37),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                gameWinnerMessage,
                                fontSize = 16.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showWinnerDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Acknowledge", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Global Game Over Handler
private fun handleGameOver(
    message: String,
    reason: String,
    dao: ChessDao,
    scope: kotlinx.coroutines.CoroutineScope,
    onHistoryUpdated: (List<GameHistoryEntry>) -> Unit
) {
    scope.launch {
        // Parse match elements and save in database
        val isWin = message.contains("White") // simple parse
        val history = GameHistoryEntry(
            mode = "CHESS_MATCH",
            opponentName = "Opponent",
            playerColor = "WHITE",
            result = if (isWin) "WIN" else "DRAW",
            reason = reason,
            movesText = "Game concluded"
        )
        dao.insertGameHistory(history)
        onHistoryUpdated(dao.getAllGameHistory())
    }
}

// Particle Generator for Checkmate Celebration
private fun spawnCelebrateParticles(particlesList: MutableList<GoldParticle>) {
    particlesList.clear()
    val random = Random(System.currentTimeMillis())
    for (i in 0..70) {
        val angle = random.nextFloat() * 2f * Math.PI.toFloat()
        val speed = random.nextFloat() * 15f + 5f
        particlesList.add(
            GoldParticle(
                id = i,
                x = 540f, // Center estimates
                y = 900f,
                vx = sin(angle) * speed,
                vy = -abs(kotlin.math.cos(angle) * speed * 1.5f), // shoot upward
                alpha = 1.0f,
                size = random.nextFloat() * 4f + 2f
            )
        )
    }
}

@Composable
fun RoyalCrownLogo(
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFD4AF37)
) {
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val scaleX = size.width / 100f
        val scaleY = size.height / 100f
        
        val path1 = androidx.compose.ui.graphics.Path().apply {
            moveTo(50f * scaleX, 10f * scaleY)
            lineTo(35f * scaleX, 30f * scaleY)
            lineTo(65f * scaleX, 30f * scaleY)
            close()
        }
        
        val path2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(35f * scaleX, 35f * scaleY)
            lineTo(20f * scaleX, 80f * scaleY)
            lineTo(80f * scaleX, 80f * scaleY)
            lineTo(65f * scaleX, 35f * scaleY)
            close()
        }
        
        val path3 = androidx.compose.ui.graphics.Path().apply {
            moveTo(25f * scaleX, 85f * scaleY)
            lineTo(75f * scaleX, 85f * scaleY)
            lineTo(75f * scaleX, 90f * scaleY)
            lineTo(25f * scaleX, 90f * scaleY)
            close()
        }
        
        drawPath(path1, color = color)
        drawPath(path2, color = color)
        drawPath(path3, color = color)
    }
}

// ----------------------------------------------------
// HELPER: CUSTOM MOCK QR CODE GENERATOR
// ----------------------------------------------------
@Composable
fun QrCodeMock(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(130.dp)) {
        val sizePx = size.width
        val cells = 21
        val cellSize = sizePx / cells

        // Draw white background
        drawRect(Color.White)

        // Function to draw a finder pattern (the big squares in the corners)
        fun drawFinderPattern(row: Int, col: Int) {
            val startX = col * cellSize
            val startY = row * cellSize
            // Outer block (7x7)
            drawRect(
                color = Color.Black,
                topLeft = Offset(startX, startY),
                size = androidx.compose.ui.geometry.Size(cellSize * 7, cellSize * 7)
            )
            // White ring (5x5)
            drawRect(
                color = Color.White,
                topLeft = Offset(startX + cellSize, startY + cellSize),
                size = androidx.compose.ui.geometry.Size(cellSize * 5, cellSize * 5)
            )
            // Inner block (3x3)
            drawRect(
                color = Color.Black,
                topLeft = Offset(startX + cellSize * 2, startY + cellSize * 2),
                size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3)
            )
        }

        // Top-left
        drawFinderPattern(0, 0)
        // Top-right
        drawFinderPattern(0, cells - 7)
        // Bottom-left
        drawFinderPattern(cells - 7, 0)

        // Draw alignment pattern (small square) near bottom-right
        val alignRow = cells - 9
        val alignCol = cells - 9
        drawRect(
            color = Color.Black,
            topLeft = Offset(alignCol * cellSize, alignRow * cellSize),
            size = androidx.compose.ui.geometry.Size(cellSize * 5, cellSize * 5)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset((alignCol + 1) * cellSize, (alignRow + 1) * cellSize),
            size = androidx.compose.ui.geometry.Size(cellSize * 3, cellSize * 3)
        )
        drawRect(
            color = Color.Black,
            topLeft = Offset((alignCol + 2) * cellSize, (alignRow + 2) * cellSize),
            size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
        )

        // Pseudo-random data blocks with structural patterns
        val random = java.util.Random(42) // Fixed seed for reproducible look
        for (r in 0 until cells) {
            for (c in 0 until cells) {
                // Skip finder patterns
                if ((r < 8 && c < 8) || (r < 8 && c >= cells - 8) || (r >= cells - 8 && c < 8)) {
                    continue
                }
                // Skip alignment pattern
                if (r >= alignRow && r < alignRow + 5 && c >= alignCol && c < alignCol + 5) {
                    continue
                }
                // Draw pseudorandom data cells
                if (random.nextDouble() > 0.45) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(c * cellSize, r * cellSize),
                        size = androidx.compose.ui.geometry.Size(cellSize + 0.5f, cellSize + 0.5f) // overlapping a tiny bit to prevent gaps
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------
// UI SCREEN: WELCOME / MAIN DASHBOARD
// ----------------------------------------------------
@Composable
fun WelcomeDashboard(
    playerUsername: String,
    onUsernameChange: (String) -> Unit,
    solvedCount: Int,
    winCount: Int,
    boardTheme: BoardTheme,
    onBoardThemeChange: (BoardTheme) -> Unit,
    pieceStyle: PieceStyle,
    onPieceStyleChange: (PieceStyle) -> Unit,
    onWipeAllData: () -> Unit,
    onNavigate: (AppScreen, GameMode) -> Unit,
    onNavigateSettings: () -> Unit,
    isHapticEnabled: Boolean,
    isSoundEnabled: Boolean
) {
    var showNameEdit by remember { mutableStateOf(false) }
    var showResetAndCustomizeDialog by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Dynamic physical spin momentum
    var rotationAngle by remember { mutableStateOf(0f) }
    var rotationSpeed by remember { mutableStateOf(0.5f) } // default speed in degrees per tick
    
    LaunchedEffect(rotationSpeed) {
        while (true) {
            rotationAngle = (rotationAngle + rotationSpeed) % 360f
            if (rotationSpeed > 0.5f) {
                // Natural deceleration curve
                rotationSpeed = (rotationSpeed - 0.25f).coerceAtLeast(0.5f)
            }
            delay(16) // ~60fps smooth loop
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Spinning circular premium logo
        Box(
            modifier = Modifier
                .size(200.dp)
                .shadow(16.dp, CircleShape)
                .border(3.dp, Color(0xFFD4AF37), CircleShape)
                .background(Color(0xFFF0F0F0), CircleShape)
                .clip(CircleShape)
                .clickable {
                    // Tap logo to spin fast with physical deceleration!
                    rotationSpeed = 25f
                    if (isHapticEnabled) {
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                    }
                    if (isSoundEnabled) {
                        SoundEffects.playCheck()
                    }
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.motibilis_logo),
                contentDescription = "Chess Logo",
                modifier = Modifier
                    .fillMaxSize()
                    .rotate(rotationAngle),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Imperial Gaming Subheading
        Text(
            text = "IMPERIAL GAMING",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF9A7B4F),
            letterSpacing = 4.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // App Title text
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Motibilis | ",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = Color.White
            )
            Text(
                text = "Checkmate",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Serif,
                color = Color(0xFFD4AF37),
                fontStyle = FontStyle.Italic
            )
        }

        Text(
            text = "ROYAL MULTIPLAYER ARENA",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFC5A059),
            letterSpacing = 4.sp,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        // Custom Username Display and Edit card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC141414)),
            border = BorderStroke(1.dp, Color(0x33D4AF37)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showNameEdit) {
                    OutlinedTextField(
                        value = playerUsername,
                        onValueChange = onUsernameChange,
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color.Black,
                            unfocusedContainerColor = Color.Black,
                            focusedIndicatorColor = Color(0xFFD4AF37),
                            unfocusedIndicatorColor = Color(0xFF9A7B4F)
                        ),
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                    )
                    IconButton(onClick = { showNameEdit = false }) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Done", tint = Color(0xFFD4AF37))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Avatar",
                            tint = Color(0xFFD4AF37),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = playerUsername,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(onClick = { showNameEdit = true }) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Name", tint = Color(0xFF9A7B4F))
                    }
                }
            }
        }

        // Stats Quick Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xCC141414), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33D4AF37), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Puzzles Solved", color = Color.Gray, fontSize = 11.sp)
                    Text("$solvedCount", color = Color(0xFFD4AF37), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(Color(0xCC141414), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33D4AF37), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Arena Wins", color = Color.Gray, fontSize = 11.sp)
                    Text("$winCount", color = Color(0xFFD4AF37), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 📦 Source / Build on GitHub (replaces APK portal)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .clickable {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Unable to open browser", Toast.LENGTH_SHORT).show()
                    }
                    if (isHapticEnabled) {
                        try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (e: Exception) {}
                    }
                },
            colors = CardDefaults.cardColors(containerColor = Color(0x1F27C93F)),
            border = BorderStroke(1.5.dp, Color(0xFF27C93F)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFF27C93F).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "Source",
                            tint = Color(0xFF27C93F),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "🔗 View on GitHub",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF27C93F),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Browse source and build instructions",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Open GitHub",
                    tint = Color(0xFF27C93F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // GAME MODE SELECTION BUTTONS
        MenuModeItem(
            title = "👑  Singleplayer vs Computer",
            subtitle = "Challenge the adaptive neural minimax engine.",
            isHero = true,
            onClick = { onNavigate(AppScreen.AiConfig, GameMode.AI) }
        )

        MenuModeItem(
            title = "🤝  Local Pass & Play",
            subtitle = "Play face-to-face on same device with auto-flipping.",
            onClick = { onNavigate(AppScreen.Gameplay, GameMode.PASS_AND_PLAY) }
        )

        MenuModeItem(
            title = "🌐  Wi-Fi Nearby Multiplayer",
            subtitle = "Connect instantly over local Wi-Fi and chat real-time.",
            onClick = { onNavigate(AppScreen.WifiLobby, GameMode.WIFI) }
        )

        MenuModeItem(
            title = "🧩  Grandmaster Puzzles",
            subtitle = "Crack tactical mate positions to claim trophies.",
            onClick = { onNavigate(AppScreen.Puzzles, GameMode.PUZZLE) }
        )

        MenuModeItem(
            title = "📊  Customization & Logs",
            subtitle = "Adjust boards, piece models, and inspect match logs.",
            onClick = onNavigateSettings
        )

        Spacer(modifier = Modifier.height(12.dp))

        // ✨ The special Reset & Re-configure from Scratch option
        Button(
            onClick = { showResetAndCustomizeDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E)),
            border = BorderStroke(1.5.dp, Color(0xFFD4AF37)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = Color(0xFFD4AF37),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "✨ Clear All Data & Reset App",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }

        if (showResetAndCustomizeDialog) {
            Dialog(onDismissRequest = { showResetAndCustomizeDialog = false }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(16.dp)
                        .shadow(24.dp, RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF111111)),
                    border = BorderStroke(2.dp, Color(0xFFD4AF37)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Wipe",
                            tint = Color(0xFF8B1E1E),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "WIPE & RECONFIG WORKSPACE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD4AF37),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This will completely erase all local histories, puzzle states, and let you configure the layout from a absolute clean slate.",
                            fontSize = 12.sp,
                            color = Color.LightGray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        // Username config
                        Text("1. Profile Name", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A7B4F), modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = playerUsername,
                            onValueChange = onUsernameChange,
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedIndicatorColor = Color(0xFFD4AF37),
                                unfocusedIndicatorColor = Color(0xFF9A7B4F)
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Theme Config
                        Text("2. Select Board Theme", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A7B4F), modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            BoardTheme.values().forEach { t ->
                                val isSelected = boardTheme == t
                                Button(
                                    onClick = { onBoardThemeChange(t) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFD4AF37) else Color(0xFF222222)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = t.name.replace("_", " "),
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Piece Style Config
                        Text("3. Select Piece Style", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9A7B4F), modifier = Modifier.align(Alignment.Start))
                        Spacer(modifier = Modifier.height(6.dp))
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PieceStyle.values().forEach { s ->
                                val isSelected = pieceStyle == s
                                Button(
                                    onClick = { onPieceStyleChange(s) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSelected) Color(0xFFD4AF37) else Color(0xFF222222)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(vertical = 8.dp)
                                ) {
                                    Text(
                                        text = s.name.replace("_", " "),
                                        color = if (isSelected) Color.Black else Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Bottom Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { showResetAndCustomizeDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222222)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = Color.White)
                            }
                            Button(
                                onClick = {
                                    onWipeAllData()
                                    showResetAndCustomizeDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B1E1E)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f),
                                border = BorderStroke(1.dp, Color(0xFFD4AF37))
                            ) {
                                Text("Wipe & Apply", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // APK portal removed; use GitHub link card above instead.

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun MenuModeItem(
    title: String,
    subtitle: String,
    isHero: Boolean = false,
    onClick: () -> Unit
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(Color(0xFFD4AF37), Color(0xFFF1D279), Color(0xFF9A7B4F))
    )
    val cardBg = if (isHero) Color.Transparent else Color(0xCC141414)
    val borderBrush = if (isHero) null else BorderStroke(1.dp, Color(0x33D4AF37))
    val textColor = if (isHero) Color.Black else Color(0xFFD4AF37)
    val subtitleColor = if (isHero) Color(0xFF1E1E1E) else Color(0xFFB0B0B0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = if (isHero) Color.White else cardBg),
        border = borderBrush,
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isHero) Modifier.background(gradientBrush)
                    else Modifier
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = subtitleColor
                    )
                }
                Text(
                    text = "→",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

// ----------------------------------------------------
// UI SCREEN: AI CONFIGURATION
// ----------------------------------------------------
@Composable
fun AiConfigurationScreen(
    aiDifficulty: Int,
    onDifficultyChange: (Int) -> Unit,
    userColorChoice: PieceColor,
    onColorChoiceChange: (PieceColor) -> Unit,
    isTimerEnabled: Boolean,
    onTimerToggle: (Boolean) -> Unit,
    initialTime: Int,
    onTimeChange: (Int) -> Unit,
    onBack: () -> Unit,
    onStartGame: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFD4AF37))
            }
            Text("Engine Config", fontSize = 22.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // AI DIFFICULTY SELECTOR
        Text("AI DIFFICULTY", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(10.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val levels = listOf(
                Pair(1, "Novice"),
                Pair(2, "Apprentice"),
                Pair(3, "Champion"),
                Pair(4, "Master")
            )
            for (lvl in levels) {
                val isSelected = aiDifficulty == lvl.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xCC141414),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, if (isSelected) Color(0xFFF1D279) else Color(0x33D4AF37), RoundedCornerShape(10.dp))
                        .clickable { onDifficultyChange(lvl.first) }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        lvl.second,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // COLOR SELECTOR
        Text("PLAY AS", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val colors = listOf(
                Pair(PieceColor.WHITE, "White Side"),
                Pair(PieceColor.BLACK, "Black Side")
            )
            for (c in colors) {
                val isSelected = userColorChoice == c.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xCC141414),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, if (isSelected) Color(0xFFF1D279) else Color(0x33D4AF37), RoundedCornerShape(10.dp))
                        .clickable { onColorChoiceChange(c.first) }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        c.second,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // MATCH TIMER TOGGLER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("MATCH TIMER", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Limit thinking times to prevent stagnation.", fontSize = 11.sp, color = Color.Gray)
            }
            Switch(
                checked = isTimerEnabled,
                onCheckedChange = onTimerToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFD4AF37)
                )
            )
        }

        if (isTimerEnabled) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val times = listOf(
                    Pair(180, "3 Min Blitz"),
                    Pair(600, "10 Min Rapid"),
                    Pair(1800, "30 Min Classic")
                )
                for (t in times) {
                    val isSelected = initialTime == t.first
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                color = if (isSelected) Color(0xFFD4AF37) else Color(0xCC141414),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(1.dp, if (isSelected) Color(0xFFF1D279) else Color(0x33D4AF37), RoundedCornerShape(10.dp))
                            .clickable { onTimeChange(t.first) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            t.second,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else Color.White
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFD4AF37), Color(0xFFF1D279), Color(0xFF9A7B4F))
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { onStartGame() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "COMMENCE DUEL",
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.5.sp
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ----------------------------------------------------
// UI SCREEN: WI-FI LOBBY MULTIPLAYER
// ----------------------------------------------------
@Composable
fun WifiLobbyScreen(
    socketManager: SocketManager,
    connectionState: ConnectionState,
    targetIp: String,
    onIpChange: (String) -> Unit,
    onHostClick: () -> Unit,
    onJoinClick: () -> Unit,
    onBack: () -> Unit
) {
    val myIp = remember { socketManager.getLocalIpAddress() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFD4AF37))
            }
            Text("Wi-Fi Multiplayer", fontSize = 22.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // LOCAL DEVICE IP BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC141414)),
            border = BorderStroke(1.dp, Color(0x33D4AF37)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(imageVector = Icons.Default.Wifi, contentDescription = "WiFi", tint = Color(0xFFD4AF37), modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Your Wifi Local IP Address", color = Color.Gray, fontSize = 12.sp)
                Text(myIp, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text("To play with a nearby friend, make sure both devices are on the same Wi-Fi router network.", color = Color.LightGray, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // CONNECTION ACTIONS
        when (connectionState) {
            ConnectionState.Idle -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFFD4AF37), Color(0xFFF1D279), Color(0xFF9A7B4F))
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onHostClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("HOST ARENA SERVER", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray))
                    Text("OR JOIN AN IP", color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 8.dp))
                    Box(modifier = Modifier.weight(1f).height(1.dp).background(Color.Gray))
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = targetIp,
                    onValueChange = onIpChange,
                    label = { Text("Enter Host IP Address") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color.Black,
                        unfocusedContainerColor = Color.Black,
                        focusedIndicatorColor = Color(0xFFD4AF37),
                        unfocusedIndicatorColor = Color(0xFF9A7B4F)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .background(Color(0xCC141414), RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(12.dp))
                        .clickable { onJoinClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("CONNECT TO HOST", color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
                }
            }

            ConnectionState.Hosting -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Hosting Arena...", color = Color.White, fontSize = 16.sp)
                    Text("Waiting for nearby client to join...", color = Color.Gray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Cancel Hosting", color = Color.White)
                    }
                }
            }

            ConnectionState.Connecting -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Connecting to Host at $targetIp...", color = Color.White)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Cancel Connecting", color = Color.White)
                    }
                }
            }

            ConnectionState.Connected -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CONNECTED!", color = Color(0xFFD4AF37), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Synchronizing match details...", color = Color.LightGray)
                }
            }

            is ConnectionState.Error -> {
                val err = connectionState
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("CONNECTION FAIL", color = Color.Red, fontWeight = FontWeight.Bold)
                    Text(err.message, color = Color.LightGray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37))
                    ) {
                        Text("Try Again", color = Color.Black)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// UI SCREEN: CHESS PUZZLES LIST
// ----------------------------------------------------
@Composable
fun PuzzlesScreen(
    solvedPuzzleIds: List<String>,
    onBack: () -> Unit,
    onSelectPuzzle: (ChessPuzzle) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFD4AF37))
            }
            Text("Grandmaster Puzzles", fontSize = 22.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(STATIC_PUZZLES) { puzzle ->
                val isSolved = solvedPuzzleIds.contains(puzzle.id)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectPuzzle(puzzle) },
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC141414)),
                    border = BorderStroke(1.dp, if (isSolved) Color(0xFF4CAF50) else Color(0x33D4AF37)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                puzzle.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSolved) Color(0xFF4CAF50) else Color(0xFFD4AF37)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                puzzle.description,
                                fontSize = 11.sp,
                                color = Color.LightGray,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        if (isSolved) {
                            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = "Solved", tint = Color(0xFF4CAF50))
                        } else {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color(0xFFD4AF37))
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// UI SCREEN: SETTINGS, THEMING & MATCH STATS
// ----------------------------------------------------
@Composable
fun StatsHistoryScreen(
    gameHistories: List<GameHistoryEntry>,
    solvedCount: Int,
    boardTheme: BoardTheme,
    onBoardThemeChange: (BoardTheme) -> Unit,
    pieceStyle: PieceStyle,
    onPieceStyleChange: (PieceStyle) -> Unit,
    isSoundEnabled: Boolean,
    onSoundToggle: (Boolean) -> Unit,
    isHapticEnabled: Boolean,
    onHapticToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onClearStats: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFD4AF37))
            }
            Text("Customization & Logs", fontSize = 22.sp, color = Color(0xFFD4AF37), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // THEME SELECTION
        Text("CHESSBOARD SKINS", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val themes = listOf(
                Pair(BoardTheme.OBSIDIAN_GOLD, "Obsidian"),
                Pair(BoardTheme.WALNUT_IVORY, "Walnut"),
                Pair(BoardTheme.MIDNIGHT_EMERALD, "Emerald"),
                Pair(BoardTheme.CRIMSON_ROYALE, "Crimson")
            )
            for (t in themes) {
                val isSelected = boardTheme == t.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xCC141414),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, if (isSelected) Color(0xFFF1D279) else Color(0x33D4AF37), RoundedCornerShape(10.dp))
                        .clickable { onBoardThemeChange(t.first) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        t.second,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // PIECE SELECTION
        Text("CHESS PIECE DESIGNS", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val styles = listOf(
                Pair(PieceStyle.REGAL_MEDALLIONS, "Medallion"),
                Pair(PieceStyle.CLASSIC_SILHOUETTE, "Classic"),
                Pair(PieceStyle.ROYAL_MINIMAL, "Minimal")
            )
            for (s in styles) {
                val isSelected = pieceStyle == s.first
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            color = if (isSelected) Color(0xFFD4AF37) else Color(0xCC141414),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .border(1.dp, if (isSelected) Color(0xFFF1D279) else Color(0x33D4AF37), RoundedCornerShape(10.dp))
                        .clickable { onPieceStyleChange(s.first) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        s.second,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.Black else Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // SOUND FX TOGGLER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("TACTILE AUDIO FEEDBACK", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Synthesize physical piece sounds completely offline.", fontSize = 11.sp, color = Color.Gray)
            }
            Switch(
                checked = isSoundEnabled,
                onCheckedChange = onSoundToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFD4AF37)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // HAPTIC FEEDBACK TOGGLER
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("HAPTIC SENSORY VIBRATIONS", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Simulate physical weight, captures, and check touches.", fontSize = 11.sp, color = Color.Gray)
            }
            Switch(
                checked = isHapticEnabled,
                onCheckedChange = onHapticToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.Black,
                    checkedTrackColor = Color(0xFFD4AF37)
                )
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // HISTORICAL LOGS
        Text("HISTORIC MATCH LOGS", color = Color(0xFF9A7B4F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))

        if (gameHistories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xCC141414), RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0x33D4AF37), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No recorded games in this arena yet.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC141414)),
                border = BorderStroke(1.dp, Color(0x33D4AF37)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    for (entry in gameHistories.take(5)) {
                        val date = remember(entry.timestamp) {
                            SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(entry.timestamp))
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(entry.opponentName, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(date, color = Color.Gray, fontSize = 10.sp)
                            }
                            Text(
                                entry.result,
                                color = if (entry.result == "WIN") Color.Green else if (entry.result == "LOSS") Color.Red else Color.Gray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Divider(color = Color(0x15D4AF37))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onClearStats,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x1A000000)),
                        border = BorderStroke(1.dp, Color(0x33D4AF37)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Match History", color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

// ----------------------------------------------------
// UI SCREEN: GAMEPLAY & CHEESBOARD ARENA
// ----------------------------------------------------
@Composable
fun GameplayScreen(
    gameState: ChessGameState,
    selectedPosition: BoardPosition?,
    possibleMoves: List<ChessMove>,
    isBoardFlipped: Boolean,
    boardTheme: BoardTheme,
    pieceStyle: PieceStyle,
    activeGameMode: GameMode,
    whiteTimeLeft: Int,
    blackTimeLeft: Int,
    isTimerEnabled: Boolean,
    isAiCalculating: Boolean,
    opponentUsername: String,
    chatMessages: List<Pair<String, String>>,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onSendChat: () -> Unit,
    onFlipBoard: () -> Unit,
    autoFlipBoard: Boolean,
    onAutoFlipToggle: (Boolean) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onResign: () -> Unit,
    onCellClick: (BoardPosition) -> Unit
) {
    // Dynamic countdown formatted displays
    val whiteTimeDisplay = formatSeconds(whiteTimeLeft)
    val blackTimeDisplay = formatSeconds(blackTimeLeft)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP LOGO BAR
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFFD4AF37))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "Motibilis | Checkmate",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4AF37),
                    fontFamily = FontFamily.Serif
                )
            }
            Row {
                IconButton(onClick = onReset) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset Game", tint = Color(0xFFD4AF37))
                }
                IconButton(onClick = onFlipBoard) {
                    Icon(imageVector = Icons.Default.FlipCameraAndroid, contentDescription = "Flip Perspective", tint = Color(0xFFD4AF37))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // OPPONENT HEADER BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC141414)),
            border = BorderStroke(1.dp, Color(0x33D4AF37)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "Opponent", tint = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(opponentUsername, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (isAiCalculating) {
                            Text("Engine analyzing positions...", color = Color(0xFFD4AF37), fontSize = 10.sp)
                        } else {
                            Text("Ready", color = Color.Gray, fontSize = 10.sp)
                        }
                    }
                }
                if (isTimerEnabled) {
                    Text(
                        text = if (isBoardFlipped) whiteTimeDisplay else blackTimeDisplay,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black, RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // MASTER CHESSBOARD
        val activeKingCheckPos = if (gameState.isCheck) {
            ChessEngine.findKing(gameState, gameState.activeColor)
        } else null

        ChessBoard(
            board = gameState.board,
            selectedPos = selectedPosition,
            legalMoves = possibleMoves,
            lastMove = gameState.moveHistory.lastOrNull(),
            kingInCheckPos = activeKingCheckPos,
            isFlipped = isBoardFlipped,
            boardTheme = boardTheme,
            pieceStyle = pieceStyle,
            onCellClick = onCellClick,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(0.1f))

        // PLAYER HEADER BANNER
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC141414)),
            border = BorderStroke(1.dp, Color(0x33D4AF37)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = "You", tint = Color(0xFFD4AF37))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("You", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (gameState.activeColor == (if (isBoardFlipped) PieceColor.BLACK else PieceColor.WHITE)) "Your Turn" else "Opponent's Turn",
                            color = Color(0xFFD4AF37),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                if (isTimerEnabled) {
                    Text(
                        text = if (isBoardFlipped) blackTimeDisplay else whiteTimeDisplay,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color.Black, RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFFD4AF37), RoundedCornerShape(4.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // WI-FI CHAT PANE
        if (activeGameMode == GameMode.WIFI) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC141414)),
                border = BorderStroke(1.dp, Color(0x33D4AF37)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp),
                        reverseLayout = true
                    ) {
                        items(chatMessages.reversed()) { msg ->
                            Row(modifier = Modifier.padding(vertical = 2.dp)) {
                                Text(
                                    "${msg.first}: ",
                                    color = Color(0xFFD4AF37),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                                Text(msg.second, color = Color.White, fontSize = 11.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = chatInput,
                            onValueChange = onChatInputChange,
                            placeholder = { Text("Send quick message...", fontSize = 11.sp, color = Color.DarkGray) },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color.Black,
                                unfocusedContainerColor = Color.Black,
                                focusedIndicatorColor = Color(0xFFD4AF37)
                            ),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp)
                        )
                        IconButton(onClick = onSendChat) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = "Send", tint = Color(0xFFD4AF37))
                        }
                    }
                }
            }
        } else {
            // General Game controllers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (activeGameMode == GameMode.PASS_AND_PLAY) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xCC141414), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0x33D4AF37), RoundedCornerShape(10.dp))
                            .clickable { onAutoFlipToggle(!autoFlipBoard) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (autoFlipBoard) "Auto-Flip ON" else "Auto-Flip OFF",
                            color = if (autoFlipBoard) Color(0xFFD4AF37) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Button(
                    onClick = onResign,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7A1C1C))
                ) {
                    Text("RESIGN ARENA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

// Format Seconds into Digital Timer display MM:SS
private fun formatSeconds(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
}
