package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "game_history")
data class GameHistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mode: String,              // "AI", "PASS_AND_PLAY", "WIFI"
    val opponentName: String,
    val playerColor: String,       // "WHITE", "BLACK"
    val result: String,            // "WIN", "LOSS", "DRAW"
    val reason: String,            // "CHECKMATE", "STALEMATE", "RESIGNATION", "TIMEOUT", "DRAW_AGREED"
    val movesText: String,         // Comma-separated list of algebraic moves (e.g. "e4, e5, Nf3, Nc6")
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "puzzle_state")
data class PuzzleState(
    @PrimaryKey val puzzleId: String,
    val solved: Boolean,
    val attempts: Int,
    val solvedAt: Long = System.currentTimeMillis()
)

@Dao
interface ChessDao {
    @Insert
    suspend fun insertGameHistory(entry: GameHistoryEntry)

    @Query("SELECT * FROM game_history ORDER BY timestamp DESC")
    suspend fun getAllGameHistory(): List<GameHistoryEntry>

    @Query("DELETE FROM game_history")
    suspend fun clearHistory()

    @Query("DELETE FROM puzzle_state")
    suspend fun clearPuzzleStates()

    @Query("SELECT * FROM puzzle_state")
    suspend fun getAllPuzzleStates(): List<PuzzleState>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun savePuzzleState(state: PuzzleState)

    @Query("SELECT * FROM puzzle_state WHERE puzzleId = :puzzleId")
    suspend fun getPuzzleState(puzzleId: String): PuzzleState?
}
