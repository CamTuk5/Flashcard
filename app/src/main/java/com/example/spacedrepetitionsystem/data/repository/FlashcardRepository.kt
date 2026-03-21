package com.example.spacedrepetitionsystem.data.repository

import com.example.spacedrepetitionsystem.data.SM2Logic
import com.example.spacedrepetitionsystem.data.local.FlashcardDao
import com.example.spacedrepetitionsystem.data.model.Flashcard
import com.example.spacedrepetitionsystem.data.remote.ApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class FlashcardRepository @Inject constructor(
    private val flashcardDao: FlashcardDao,
    private val apiService: ApiService
) {
    fun getAllCards(): Flow<List<Flashcard>> = flashcardDao.getAllCards()

    fun getCardsToReview(currentTime: Long): Flow<List<Flashcard>> = 
        flashcardDao.getCardsToReview(currentTime)

    suspend fun insertCard(card: Flashcard) = flashcardDao.insertCard(card)

    suspend fun reviewCard(card: Flashcard, quality: Int) {
        val updatedCard = SM2Logic.calculateNextReview(card, quality)
        flashcardDao.updateCard(updatedCard)
    }

    suspend fun syncWithBackend() {
        try {
            val localCards = flashcardDao.getAllCards().first()
            apiService.syncFlashcards(localCards)
            
            val remoteCards = apiService.getFlashcards()
            remoteCards.forEach { card ->
                flashcardDao.insertCard(card)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Giả lập qua ngày mới bằng cách trừ 24h vào ngày hẹn của tất cả các thẻ
    suspend fun simulateNewDay() {
        flashcardDao.simulateNewDay()
    }

    suspend fun deleteCard(card: Flashcard) = flashcardDao.deleteCard(card)
}
