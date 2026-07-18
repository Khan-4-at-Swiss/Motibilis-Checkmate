package com.example.chess

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.random.Random

object ChessAI {

    // Piece Material Values (centipawns)
    private const val VALUE_PAWN = 100
    private const val VALUE_KNIGHT = 320
    private const val VALUE_BISHOP = 330
    private const val VALUE_ROOK = 500
    private const val VALUE_QUEEN = 900
    private const val VALUE_KING = 20000

    // Piece-Square positional tables. Indexed by [row][col] from White's perspective.
    // For Black's perspective, we flip the rows.

    private val PAWN_TABLE = arrayOf(
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0),
        intArrayOf( 50,  50,  50,  50,  50,  50,  50,  50),
        intArrayOf( 10,  10,  20,  30,  30,  20,  10,  10),
        intArrayOf(  5,   5,  10,  25,  25,  10,   5,   5),
        intArrayOf(  0,   0,   0,  20,  20,   0,   0,   0),
        intArrayOf(  5,  -5, -10,   0,   0, -10,  -5,   5),
        intArrayOf(  5,  10,  10, -20, -20,  10,  10,   5),
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0)
    )

    private val KNIGHT_TABLE = arrayOf(
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50),
        intArrayOf(-40, -20,   0,   0,   0,   0, -20, -40),
        intArrayOf(-30,   0,  10,  15,  15,  10,   0, -30),
        intArrayOf(-30,   5,  15,  20,  20,  15,   5, -30),
        intArrayOf(-30,   0,  15,  20,  20,  15,   0, -30),
        intArrayOf(-30,   5,  10,  15,  15,  10,   5, -30),
        intArrayOf(-40, -20,   0,   5,   5,   0, -20, -40),
        intArrayOf(-50, -40, -30, -30, -30, -30, -40, -50)
    )

    private val BISHOP_TABLE = arrayOf(
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20),
        intArrayOf(-10,   0,   0,   0,   0,   0,   0, -10),
        intArrayOf(-10,   0,   5,  10,  10,   5,   0, -10),
        intArrayOf(-10,   5,   5,  10,  10,   5,   5, -10),
        intArrayOf(-10,   0,  10,  10,  10,  10,   0, -10),
        intArrayOf(-10,  10,  10,  10,  10,  10,  10, -10),
        intArrayOf(-10,   5,   0,   0,   0,   0,   5, -10),
        intArrayOf(-20, -10, -10, -10, -10, -10, -10, -20)
    )

    private val ROOK_TABLE = arrayOf(
        intArrayOf(  0,   0,   0,   0,   0,   0,   0,   0),
        intArrayOf(  5,  10,  10,  10,  10,  10,  10,   5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf( -5,   0,   0,   0,   0,   0,   0,  -5),
        intArrayOf(  0,   0,   0,   5,   5,   0,   0,   0)
    )

    private val QUEEN_TABLE = arrayOf(
        intArrayOf(-20, -10, -10,  -5,  -5, -10, -10, -20),
        intArrayOf(-10,   0,   0,   0,   0,   0,   0, -10),
        intArrayOf(-10,   0,   5,   5,   5,   5,   0, -10),
        intArrayOf( -5,   0,   5,   5,   5,   5,   0,  -5),
        intArrayOf(  0,   0,   5,   5,   5,   5,   0,  -5),
        intArrayOf(-10,   5,   5,   5,   5,   5,   0, -10),
        intArrayOf(-10,   0,   5,   0,   0,   5,   0, -10),
        intArrayOf(-20, -10, -10,  -5,  -5, -10, -10, -20)
    )

    private val KING_MIDDLE_GAME_TABLE = arrayOf(
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-30, -40, -40, -50, -50, -40, -40, -30),
        intArrayOf(-20, -30, -30, -40, -40, -30, -30, -20),
        intArrayOf(-10, -20, -20, -20, -20, -20, -20, -10),
        intArrayOf( 20,  20,   0,   0,   0,   0,  20,  20),
        intArrayOf( 20,  30,  10,   0,   0,  10,  30,  20)
    )

    // Evaluate current state. Positive value favors White, negative favors Black.
    fun evaluateBoard(state: ChessGameState): Int {
        var score = 0
        
        // Handle Game Over terminal states
        if (state.isCheckmate) {
            return if (state.activeColor == PieceColor.WHITE) -100000 else 100000
        }
        if (state.isStalemate) return 0

        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c] ?: continue
                val isWhite = piece.color == PieceColor.WHITE
                val multiplier = if (isWhite) 1 else -1
                
                // 1. Material Evaluation
                val materialScore = when (piece.type) {
                    PieceType.PAWN -> VALUE_PAWN
                    PieceType.KNIGHT -> VALUE_KNIGHT
                    PieceType.BISHOP -> VALUE_BISHOP
                    PieceType.ROOK -> VALUE_ROOK
                    PieceType.QUEEN -> VALUE_QUEEN
                    PieceType.KING -> VALUE_KING
                }
                score += materialScore * multiplier

                // 2. Positional Evaluation
                val tableRow = if (isWhite) r else 7 - r
                val tableCol = if (isWhite) c else 7 - c
                
                val positionBonus = when (piece.type) {
                    PieceType.PAWN -> PAWN_TABLE[tableRow][tableCol]
                    PieceType.KNIGHT -> KNIGHT_TABLE[tableRow][tableCol]
                    PieceType.BISHOP -> BISHOP_TABLE[tableRow][tableCol]
                    PieceType.ROOK -> ROOK_TABLE[tableRow][tableCol]
                    PieceType.QUEEN -> QUEEN_TABLE[tableRow][tableCol]
                    PieceType.KING -> KING_MIDDLE_GAME_TABLE[tableRow][tableCol]
                }
                score += positionBonus * multiplier
            }
        }
        
        return score
    }

    // Get best move asynchronously in a background thread
    suspend fun getBestMove(state: ChessGameState, maxDepth: Int): ChessMove? = withContext(Dispatchers.Default) {
        val legalMoves = ChessEngine.getAllLegalMoves(state, state.activeColor)
        if (legalMoves.isEmpty()) return@withContext null
        
        // Shuffle moves to add unpredictability for equally good paths
        val shuffledMoves = legalMoves.shuffled(Random(System.currentTimeMillis()))
        
        var bestMove: ChessMove? = null
        val color = state.activeColor
        
        if (color == PieceColor.WHITE) {
            var highestVal = Int.MIN_VALUE
            for (move in shuffledMoves) {
                val nextState = ChessEngine.makeMoveSimulated(state, move)
                val evaluation = minimax(nextState, maxDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, false, maxDepth)
                if (evaluation > highestVal) {
                    highestVal = evaluation
                    bestMove = move
                }
            }
        } else {
            var lowestVal = Int.MAX_VALUE
            for (move in shuffledMoves) {
                val nextState = ChessEngine.makeMoveSimulated(state, move)
                val evaluation = minimax(nextState, maxDepth - 1, Int.MIN_VALUE, Int.MAX_VALUE, true, maxDepth)
                if (evaluation < lowestVal) {
                    lowestVal = evaluation
                    bestMove = move
                }
            }
        }
        
        bestMove
    }

    // Minimax search with Alpha-Beta pruning
    private fun minimax(
        state: ChessGameState,
        depth: Int,
        alpha: Int,
        beta: Int,
        isMaximizing: Boolean,
        maxDepth: Int
    ): Int {
        if (depth == 0) {
            return evaluateBoard(state)
        }

        val legalMoves = ChessEngine.getAllLegalMoves(state, state.activeColor)
        if (legalMoves.isEmpty()) {
            val inCheck = ChessEngine.isKingInCheck(state, state.activeColor)
            return if (inCheck) {
                if (state.activeColor == PieceColor.WHITE) {
                    -100000 + (maxDepth - depth)
                } else {
                    100000 - (maxDepth - depth)
                }
            } else {
                0
            }
        }

        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = Int.MIN_VALUE
            for (move in legalMoves) {
                val nextState = ChessEngine.makeMoveSimulated(state, move)
                val eval = minimax(nextState, depth - 1, currentAlpha, currentBeta, false, maxDepth)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break // Pruning
            }
            return maxEval
        } else {
            var minEval = Int.MAX_VALUE
            for (move in legalMoves) {
                val nextState = ChessEngine.makeMoveSimulated(state, move)
                val eval = minimax(nextState, depth - 1, currentAlpha, currentBeta, true, maxDepth)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= currentAlpha) break // Pruning
            }
            return minEval
        }
    }
}
