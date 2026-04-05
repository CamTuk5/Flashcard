package com.example.kiemtrack.data

import androidx.room.*
import com.example.kiemtrack.model.Flashcard
import kotlinx.coroutines.flow.Flow

@Dao
interface FlashcardDao {
    @Query("SELECT * FROM flashcards ORDER BY nextReviewDate ASC")
    fun getAllFlashcards(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE nextReviewDate <= :currentTime")
    fun getDueFlashcards(currentTime: Long): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard): Long

    @Update
    suspend fun updateFlashcard(flashcard: Flashcard)

    @Delete
    suspend fun deleteFlashcard(flashcard: Flashcard)

    @Query("DELETE FROM flashcards WHERE courseId = :courseId")
    suspend fun deleteCardsByCourse(courseId: String)
    
    @Query("SELECT * FROM flashcards WHERE id = :id")
    suspend fun getFlashcardById(id: Long): Flashcard?

    @Query("SELECT * FROM flashcards WHERE courseId = :courseId AND nextReviewDate <= :currentTime")
    fun getDueFlashcardsByCourse(courseId: String, currentTime: Long): Flow<List<Flashcard>>
    
    @Query("SELECT DISTINCT courseId FROM flashcards")
    fun getAllCourseIds(): Flow<List<String>>
}
