package com.example.spacedrepetitionsystem.data.local

import androidx.room.*
import com.example.spacedrepetitionsystem.data.model.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY nextReviewDate ASC")
    fun getAllCards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentTime ORDER BY nextReviewDate ASC")
    fun getCardsToReview(currentTime: Long): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: Flashcard)

    @Update
    suspend fun updateCard(card: Flashcard)

    @Delete
    suspend fun deleteCard(card: Flashcard)

    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getCardById(id: Int): Flashcard?

    // Hàm hack: Trừ 1 ngày (24h) cho tất cả các thẻ để giả lập ngày mới
    @Query("UPDATE flashcards SET nextReviewDate = nextReviewDate - 86400000")
    suspend fun simulateNewDay()
}
