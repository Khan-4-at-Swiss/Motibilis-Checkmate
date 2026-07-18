package com.example.chess

import kotlin.math.abs

enum class PieceType {
    PAWN, KNIGHT, BISHOP, ROOK, QUEEN, KING
}

enum class PieceColor {
    WHITE, BLACK;
    fun opponent(): PieceColor = if (this == WHITE) BLACK else WHITE
}

data class ChessPiece(
    val type: PieceType,
    val color: PieceColor
) {
    fun getSymbol(): String {
        return when (type) {
            PieceType.PAWN -> if (color == PieceColor.WHITE) "♙" else "♟"
            PieceType.KNIGHT -> if (color == PieceColor.WHITE) "♘" else "♞"
            PieceType.BISHOP -> if (color == PieceColor.WHITE) "♗" else "♝"
            PieceType.ROOK -> if (color == PieceColor.WHITE) "♖" else "♜"
            PieceType.QUEEN -> if (color == PieceColor.WHITE) "♕" else "♛"
            PieceType.KING -> if (color == PieceColor.WHITE) "♔" else "♚"
        }
    }
}

data class BoardPosition(val row: Int, val col: Int) {
    fun isValid(): Boolean = row in 0..7 && col in 0..7
    fun toAlgebraic(): String {
        val file = ('a' + col).toString()
        val rank = (8 - row).toString()
        return "$file$rank"
    }
}

data class ChessMove(
    val from: BoardPosition,
    val to: BoardPosition,
    val pieceMoved: ChessPiece,
    val pieceCaptured: ChessPiece? = null,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    val isDoublePawn: Boolean = false
) {
    fun toAlgebraic(isCheck: Boolean = false, isCheckmate: Boolean = false): String {
        if (isCastling) {
            return if (to.col == 6) "O-O" else "O-O-O"
        }
        val piecePrefix = when (pieceMoved.type) {
            PieceType.PAWN -> ""
            PieceType.KNIGHT -> "N"
            PieceType.BISHOP -> "B"
            PieceType.ROOK -> "R"
            PieceType.QUEEN -> "Q"
            PieceType.KING -> "K"
        }
        val captureSymbol = if (pieceCaptured != null || isEnPassant) {
            if (pieceMoved.type == PieceType.PAWN) {
                ('a' + from.col).toString() + "x"
            } else "x"
        } else ""
        
        val targetSquare = to.toAlgebraic()
        val promoText = if (promotion != null) {
            "=" + when (promotion) {
                PieceType.QUEEN -> "Q"
                PieceType.ROOK -> "R"
                PieceType.BISHOP -> "B"
                PieceType.KNIGHT -> "N"
                else -> ""
            }
        } else ""

        val suffix = if (isCheckmate) "#" else if (isCheck) "+" else ""
        return "$piecePrefix$captureSymbol$targetSquare$promoText$suffix"
    }
}

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
)

data class ChessGameState(
    val board: Array<Array<ChessPiece?>> = Array(8) { Array(8) { null } },
    val activeColor: PieceColor = PieceColor.WHITE,
    val castlingRights: CastlingRights = CastlingRights(),
    val enPassantFile: Int? = null, // Col of pawn that just double-stepped
    val moveHistory: List<ChessMove> = emptyList(),
    val isCheck: Boolean = false,
    val isCheckmate: Boolean = false,
    val isStalemate: Boolean = false,
    val halfmoveClock: Int = 0,
    val fullmoveNumber: Int = 1
) {
    fun getPiece(pos: BoardPosition): ChessPiece? = if (pos.isValid()) board[pos.row][pos.col] else null

    fun copyBoard(): Array<Array<ChessPiece?>> {
        return Array(8) { r -> Array(8) { c -> board[r][c] } }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ChessGameState
        if (!board.contentDeepEquals(other.board)) return false
        if (activeColor != other.activeColor) return false
        if (castlingRights != other.castlingRights) return false
        if (enPassantFile != other.enPassantFile) return false
        if (moveHistory != other.moveHistory) return false
        if (isCheck != other.isCheck) return false
        if (isCheckmate != other.isCheckmate) return false
        if (isStalemate != other.isStalemate) return false
        return true
    }

    override fun hashCode(): Int {
        var result = board.contentDeepHashCode()
        result = 31 * result + activeColor.hashCode()
        result = 31 * result + castlingRights.hashCode()
        result = 31 * result + (enPassantFile ?: -1)
        result = 31 * result + moveHistory.hashCode()
        result = 31 * result + isCheck.hashCode()
        result = 31 * result + isCheckmate.hashCode()
        result = 31 * result + isStalemate.hashCode()
        return result
    }
}

object ChessEngine {
    
    fun createInitialState(): ChessGameState {
        val board = Array(8) { Array<ChessPiece?>(8) { null } }
        
        // Setup Rooks
        board[0][0] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        board[0][7] = ChessPiece(PieceType.ROOK, PieceColor.BLACK)
        board[7][0] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        board[7][7] = ChessPiece(PieceType.ROOK, PieceColor.WHITE)
        
        // Setup Knights
        board[0][1] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        board[0][6] = ChessPiece(PieceType.KNIGHT, PieceColor.BLACK)
        board[7][1] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        board[7][6] = ChessPiece(PieceType.KNIGHT, PieceColor.WHITE)
        
        // Setup Bishops
        board[0][2] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        board[0][5] = ChessPiece(PieceType.BISHOP, PieceColor.BLACK)
        board[7][2] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        board[7][5] = ChessPiece(PieceType.BISHOP, PieceColor.WHITE)
        
        // Queens
        board[0][3] = ChessPiece(PieceType.QUEEN, PieceColor.BLACK)
        board[7][3] = ChessPiece(PieceType.QUEEN, PieceColor.WHITE)
        
        // Kings
        board[0][4] = ChessPiece(PieceType.KING, PieceColor.BLACK)
        board[7][4] = ChessPiece(PieceType.KING, PieceColor.WHITE)
        
        // Pawns
        for (col in 0..7) {
            board[1][col] = ChessPiece(PieceType.PAWN, PieceColor.BLACK)
            board[6][col] = ChessPiece(PieceType.PAWN, PieceColor.WHITE)
        }
        
        return ChessGameState(board = board)
    }

    fun parseFen(fen: String): ChessGameState {
        val board = Array(8) { Array<ChessPiece?>(8) { null } }
        val parts = fen.trim().split("\\s+".toRegex())
        if (parts.isEmpty()) return createInitialState()

        // 1. Board state
        val rows = parts[0].split("/")
        for (r in 0..7) {
            if (r >= rows.size) break
            var c = 0
            val rowStr = rows[r]
            var i = 0
            while (i < rowStr.length) {
                val char = rowStr[i]
                if (char.isDigit()) {
                    c += char.toString().toInt()
                } else {
                    val color = if (char.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
                    val type = when (char.lowercaseChar()) {
                        'p' -> PieceType.PAWN
                        'n' -> PieceType.KNIGHT
                        'b' -> PieceType.BISHOP
                        'r' -> PieceType.ROOK
                        'q' -> PieceType.QUEEN
                        'k' -> PieceType.KING
                        else -> PieceType.PAWN
                    }
                    if (c < 8) {
                        board[r][c] = ChessPiece(type, color)
                    }
                    c++
                }
                i++
            }
        }

        // 2. Active player
        val activeColor = if (parts.size > 1 && parts[1].lowercase() == "b") PieceColor.BLACK else PieceColor.WHITE

        // 3. Castling rights
        var wK = false; var wQ = false; var bK = false; var bQ = false
        if (parts.size > 2) {
            val castling = parts[2]
            if (castling.contains("K")) wK = true
            if (castling.contains("Q")) wQ = true
            if (castling.contains("k")) bK = true
            if (castling.contains("q")) bQ = true
        } else {
            wK = true; wQ = true; bK = true; bQ = true
        }

        // 4. En passant file
        var epFile: Int? = null
        if (parts.size > 3 && parts[3] != "-") {
            val epStr = parts[3]
            if (epStr.length >= 2) {
                epFile = epStr[0] - 'a'
            }
        }

        // 5. Clocks
        val halfmove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
        val fullmove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

        val state = ChessGameState(
            board = board,
            activeColor = activeColor,
            castlingRights = CastlingRights(wK, wQ, bK, bQ),
            enPassantFile = epFile,
            halfmoveClock = halfmove,
            fullmoveNumber = fullmove
        )

        // Compute check and mates for Loaded Puzzle State
        val hasMoves = hasAnyLegalMoves(state, activeColor)
        val inCheck = isKingInCheck(state, activeColor)

        return state.copy(
            isCheck = inCheck,
            isCheckmate = inCheck && !hasMoves,
            isStalemate = !inCheck && !hasMoves
        )
    }

    // Generate pseudo-legal moves (not checking if King gets attacked)
    fun getPseudoLegalMoves(state: ChessGameState, from: BoardPosition): List<ChessMove> {
        val piece = state.getPiece(from) ?: return emptyList()
        if (piece.color != state.activeColor) return emptyList()
        
        val moves = mutableListOf<ChessMove>()
        val row = from.row
        val col = from.col
        val color = piece.color
        val opponent = color.opponent()

        when (piece.type) {
            PieceType.PAWN -> {
                val dir = if (color == PieceColor.WHITE) -1 else 1
                val startRow = if (color == PieceColor.WHITE) 6 else 1
                val promoRow = if (color == PieceColor.WHITE) 0 else 7

                // 1. One step forward
                val nextPos = BoardPosition(row + dir, col)
                if (nextPos.isValid() && state.getPiece(nextPos) == null) {
                    if (nextPos.row == promoRow) {
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                            moves.add(ChessMove(from, nextPos, piece, promotion = it))
                        }
                    } else {
                        moves.add(ChessMove(from, nextPos, piece))
                    }

                    // 2. Double step from initial row
                    val doublePos = BoardPosition(row + 2 * dir, col)
                    if (row == startRow && doublePos.isValid() && state.getPiece(doublePos) == null) {
                        moves.add(ChessMove(from, doublePos, piece, isDoublePawn = true))
                    }
                }

                // 3. Diagonal captures
                val captureCols = listOf(col - 1, col + 1)
                for (c in captureCols) {
                    val target = BoardPosition(row + dir, c)
                    if (target.isValid()) {
                        val destPiece = state.getPiece(target)
                        if (destPiece != null && destPiece.color == opponent) {
                            if (target.row == promoRow) {
                                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach {
                                    moves.add(ChessMove(from, target, piece, destPiece, promotion = it))
                                }
                            } else {
                                moves.add(ChessMove(from, target, piece, destPiece))
                            }
                        }

                        // En Passant capture
                        if (state.enPassantFile == c && row == (if (color == PieceColor.WHITE) 3 else 4)) {
                            val epCapturedPawnPos = BoardPosition(row, c)
                            val epCapturedPawn = state.getPiece(epCapturedPawnPos)
                            if (epCapturedPawn != null && epCapturedPawn.color == opponent && epCapturedPawn.type == PieceType.PAWN) {
                                moves.add(ChessMove(from, target, piece, epCapturedPawn, isEnPassant = true))
                            }
                        }
                    }
                }
            }

            PieceType.KNIGHT -> {
                val offsets = listOf(
                    Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
                    Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
                )
                for (o in offsets) {
                    val target = BoardPosition(row + o.first, col + o.second)
                    if (target.isValid()) {
                        val destPiece = state.getPiece(target)
                        if (destPiece == null) {
                            moves.add(ChessMove(from, target, piece))
                        } else if (destPiece.color == opponent) {
                            moves.add(ChessMove(from, target, piece, destPiece))
                        }
                    }
                }
            }

            PieceType.BISHOP -> {
                addSlidingMoves(state, from, piece, listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1)), moves)
            }

            PieceType.ROOK -> {
                addSlidingMoves(state, from, piece, listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)), moves)
            }

            PieceType.QUEEN -> {
                val directions = listOf(
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                )
                addSlidingMoves(state, from, piece, directions, moves)
            }

            PieceType.KING -> {
                val directions = listOf(
                    Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1),
                    Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1)
                )
                for (d in directions) {
                    val target = BoardPosition(row + d.first, col + d.second)
                    if (target.isValid()) {
                        val destPiece = state.getPiece(target)
                        if (destPiece == null) {
                            moves.add(ChessMove(from, target, piece))
                        } else if (destPiece.color == opponent) {
                            moves.add(ChessMove(from, target, piece, destPiece))
                        }
                    }
                }

                // Castling
                if (!state.isCheck) {
                    if (color == PieceColor.WHITE && row == 7 && col == 4) {
                        // Kingside Castling
                        if (state.castlingRights.whiteKingside &&
                            state.getPiece(BoardPosition(7, 5)) == null &&
                            state.getPiece(BoardPosition(7, 6)) == null &&
                            !isSquareAttacked(state, BoardPosition(7, 5), opponent) &&
                            !isSquareAttacked(state, BoardPosition(7, 6), opponent)
                        ) {
                            moves.add(ChessMove(from, BoardPosition(7, 6), piece, isCastling = true))
                        }
                        // Queenside Castling
                        if (state.castlingRights.whiteQueenside &&
                            state.getPiece(BoardPosition(7, 3)) == null &&
                            state.getPiece(BoardPosition(7, 2)) == null &&
                            state.getPiece(BoardPosition(7, 1)) == null &&
                            !isSquareAttacked(state, BoardPosition(7, 3), opponent) &&
                            !isSquareAttacked(state, BoardPosition(7, 2), opponent)
                        ) {
                            moves.add(ChessMove(from, BoardPosition(7, 2), piece, isCastling = true))
                        }
                    } else if (color == PieceColor.BLACK && row == 0 && col == 4) {
                        // Kingside Castling
                        if (state.castlingRights.blackKingside &&
                            state.getPiece(BoardPosition(0, 5)) == null &&
                            state.getPiece(BoardPosition(0, 6)) == null &&
                            !isSquareAttacked(state, BoardPosition(0, 5), opponent) &&
                            !isSquareAttacked(state, BoardPosition(0, 6), opponent)
                        ) {
                            moves.add(ChessMove(from, BoardPosition(0, 6), piece, isCastling = true))
                        }
                        // Queenside Castling
                        if (state.castlingRights.blackQueenside &&
                            state.getPiece(BoardPosition(0, 3)) == null &&
                            state.getPiece(BoardPosition(0, 2)) == null &&
                            state.getPiece(BoardPosition(0, 1)) == null &&
                            !isSquareAttacked(state, BoardPosition(0, 3), opponent) &&
                            !isSquareAttacked(state, BoardPosition(0, 2), opponent)
                        ) {
                            moves.add(ChessMove(from, BoardPosition(0, 2), piece, isCastling = true))
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun addSlidingMoves(
        state: ChessGameState,
        from: BoardPosition,
        piece: ChessPiece,
        directions: List<Pair<Int, Int>>,
        outMoves: MutableList<ChessMove>
    ) {
        val opponent = piece.color.opponent()
        for (d in directions) {
            var step = 1
            while (true) {
                val target = BoardPosition(from.row + d.first * step, from.col + d.second * step)
                if (!target.isValid()) break
                val destPiece = state.getPiece(target)
                if (destPiece == null) {
                    outMoves.add(ChessMove(from, target, piece))
                } else {
                    if (destPiece.color == opponent) {
                        outMoves.add(ChessMove(from, target, piece, destPiece))
                    }
                    break // Blocked
                }
                step++
            }
        }
    }

    // Verify if a specific square is under attack by ANY piece of attackerColor
    fun isSquareAttacked(state: ChessGameState, pos: BoardPosition, attackerColor: PieceColor): Boolean {
        // Run simple scans in all directions to see if an attacker can strike this square
        // 1. Check Rook / Queen attacks along ranks and files
        val orthogonalDirs = listOf(Pair(-1, 0), Pair(1, 0), Pair(0, -1), Pair(0, 1))
        for (d in orthogonalDirs) {
            var step = 1
            while (true) {
                val t = BoardPosition(pos.row + d.first * step, pos.col + d.second * step)
                if (!t.isValid()) break
                val piece = state.getPiece(t)
                if (piece != null) {
                    if (piece.color == attackerColor && (piece.type == PieceType.ROOK || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // Blocked
                }
                step++
            }
        }

        // 2. Check Bishop / Queen attacks along diagonals
        val diagonalDirs = listOf(Pair(-1, -1), Pair(-1, 1), Pair(1, -1), Pair(1, 1))
        for (d in diagonalDirs) {
            var step = 1
            while (true) {
                val t = BoardPosition(pos.row + d.first * step, pos.col + d.second * step)
                if (!t.isValid()) break
                val piece = state.getPiece(t)
                if (piece != null) {
                    if (piece.color == attackerColor && (piece.type == PieceType.BISHOP || piece.type == PieceType.QUEEN)) {
                        return true
                    }
                    break // Blocked
                }
                step++
            }
        }

        // 3. Check Knight attacks
        val knightOffsets = listOf(
            Pair(-2, -1), Pair(-2, 1), Pair(-1, -2), Pair(-1, 2),
            Pair(1, -2), Pair(1, 2), Pair(2, -1), Pair(2, 1)
        )
        for (o in knightOffsets) {
            val t = BoardPosition(pos.row + o.first, pos.col + o.second)
            if (t.isValid()) {
                val piece = state.getPiece(t)
                if (piece != null && piece.color == attackerColor && piece.type == PieceType.KNIGHT) {
                    return true
                }
            }
        }

        // 4. Check Pawn attacks (relative to the target square's row)
        val pawnDir = if (attackerColor == PieceColor.WHITE) 1 else -1 // If white attacks pos, pawn must be in row + 1 (lower on screen)
        val pawnCols = listOf(pos.col - 1, pos.col + 1)
        for (c in pawnCols) {
            val t = BoardPosition(pos.row + pawnDir, c)
            if (t.isValid()) {
                val piece = state.getPiece(t)
                if (piece != null && piece.color == attackerColor && piece.type == PieceType.PAWN) {
                    return true
                }
            }
        }

        // 5. Check King attacks (adjacent squares)
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val t = BoardPosition(pos.row + dr, pos.col + dc)
                if (t.isValid()) {
                    val piece = state.getPiece(t)
                    if (piece != null && piece.color == attackerColor && piece.type == PieceType.KING) {
                        return true
                    }
                }
            }
        }

        return false
    }

    fun findKing(state: ChessGameState, color: PieceColor): BoardPosition? {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c]
                if (piece != null && piece.type == PieceType.KING && piece.color == color) {
                    return BoardPosition(r, c)
                }
            }
        }
        return null
    }

    fun isKingInCheck(state: ChessGameState, color: PieceColor): Boolean {
        val kingPos = findKing(state, color) ?: return false
        return isSquareAttacked(state, kingPos, color.opponent())
    }

    // Returns only legal moves for a piece (that don't put or keep King in check)
    fun getLegalMoves(state: ChessGameState, from: BoardPosition): List<ChessMove> {
        val pseudo = getPseudoLegalMoves(state, from)
        return pseudo.filter { move ->
            val nextState = makeMoveSimulated(state, move)
            !isKingInCheck(nextState, state.activeColor)
        }
    }

    fun getAllLegalMoves(state: ChessGameState, color: PieceColor): List<ChessMove> {
        val allMoves = mutableListOf<ChessMove>()
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c]
                if (piece != null && piece.color == color) {
                    allMoves.addAll(getLegalMoves(state, BoardPosition(r, c)))
                }
            }
        }
        return allMoves
    }

    fun hasAnyLegalMoves(state: ChessGameState, color: PieceColor): Boolean {
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = state.board[r][c]
                if (piece != null && piece.color == color) {
                    if (getLegalMoves(state, BoardPosition(r, c)).isNotEmpty()) {
                        return true
                    }
                }
            }
        }
        return false
    }

    // Play a move and return the new actual game state
    fun makeMove(state: ChessGameState, move: ChessMove): ChessGameState {
        val nextBoard = state.copyBoard()
        val piece = move.pieceMoved
        
        // 1. Move piece on the board
        nextBoard[move.from.row][move.from.col] = null
        val finalPiece = if (move.promotion != null) {
            ChessPiece(move.promotion, piece.color)
        } else {
            piece
        }
        nextBoard[move.to.row][move.to.col] = finalPiece

        // 2. Handle Castling Extra moves
        if (move.isCastling) {
            val r = move.to.row
            if (move.to.col == 6) { // Kingside
                val rook = nextBoard[r][7]
                nextBoard[r][7] = null
                nextBoard[r][5] = rook
            } else if (move.to.col == 2) { // Queenside
                val rook = nextBoard[r][0]
                nextBoard[r][0] = null
                nextBoard[r][3] = rook
            }
        }

        // 3. Handle En Passant extra capture
        if (move.isEnPassant) {
            // Captured pawn is on adjacent col, same row as 'from'
            nextBoard[move.from.row][move.to.col] = null
        }

        // 4. Update Castling Rights
        var wK = state.castlingRights.whiteKingside
        var wQ = state.castlingRights.whiteQueenside
        var bK = state.castlingRights.blackKingside
        var bQ = state.castlingRights.blackQueenside

        if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) {
                wK = false; wQ = false
            } else {
                bK = false; bQ = false
            }
        }
        if (piece.type == PieceType.ROOK) {
            if (piece.color == PieceColor.WHITE) {
                if (move.from.row == 7 && move.from.col == 7) wK = false
                if (move.from.row == 7 && move.from.col == 0) wQ = false
            } else {
                if (move.from.row == 0 && move.from.col == 7) bK = false
                if (move.from.row == 0 && move.from.col == 0) bQ = false
            }
        }
        // Also if opponent's rooks are captured, they lose castling rights on that rook side!
        if (move.pieceCaptured != null && move.pieceCaptured.type == PieceType.ROOK) {
            if (move.to.row == 0 && move.to.col == 0) bQ = false
            if (move.to.row == 0 && move.to.col == 7) bK = false
            if (move.to.row == 7 && move.to.col == 0) wQ = false
            if (move.to.row == 7 && move.to.col == 7) wK = false
        }

        // 5. En Passant File
        val nextEnPassantFile = if (move.isDoublePawn) move.to.col else null

        // 6. Halfmove and Fullmove count
        val isCaptureOrPawn = move.pieceCaptured != null || piece.type == PieceType.PAWN
        val nextHalfmoveClock = if (isCaptureOrPawn) 0 else state.halfmoveClock + 1
        val nextFullmoveNumber = if (state.activeColor == PieceColor.BLACK) state.fullmoveNumber + 1 else state.fullmoveNumber

        val nextColor = state.activeColor.opponent()

        // Assemble temporary state to evaluate checks
        val tempState = ChessGameState(
            board = nextBoard,
            activeColor = nextColor,
            castlingRights = CastlingRights(wK, wQ, bK, bQ),
            enPassantFile = nextEnPassantFile,
            moveHistory = state.moveHistory + move,
            halfmoveClock = nextHalfmoveClock,
            fullmoveNumber = nextFullmoveNumber
        )

        val opponentHasMoves = hasAnyLegalMoves(tempState, nextColor)
        val opponentInCheck = isKingInCheck(tempState, nextColor)

        return tempState.copy(
            isCheck = opponentInCheck,
            isCheckmate = opponentInCheck && !opponentHasMoves,
            isStalemate = !opponentInCheck && !opponentHasMoves
        )
    }

    // Light simulation function (no recursion, keeps lightweight)
    fun makeMoveSimulated(state: ChessGameState, move: ChessMove): ChessGameState {
        val nextBoard = state.copyBoard()
        nextBoard[move.from.row][move.from.col] = null
        nextBoard[move.to.row][move.to.col] = move.pieceMoved
        if (move.isCastling) {
            val r = move.to.row
            if (move.to.col == 6) {
                nextBoard[r][5] = nextBoard[r][7]; nextBoard[r][7] = null
            } else if (move.to.col == 2) {
                nextBoard[r][3] = nextBoard[r][0]; nextBoard[r][0] = null
            }
        }
        if (move.isEnPassant) {
            nextBoard[move.from.row][move.to.col] = null
        }
        return ChessGameState(board = nextBoard, activeColor = state.activeColor.opponent())
    }
}
