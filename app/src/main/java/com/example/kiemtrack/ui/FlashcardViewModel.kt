package com.example.kiemtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiemtrack.data.AppDatabase
import com.example.kiemtrack.model.Flashcard
import com.example.kiemtrack.srs.SM2Logic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).flashcardDao()
    
    val allCards: Flow<List<Flashcard>> = dao.getAllFlashcards()
    
    fun addFlashcard(front: String, back: String) {
        viewModelScope.launch {
            dao.insertFlashcard(Flashcard(front = front, back = back))
        }
    }

    fun updateFlashcardQuality(flashcard: Flashcard, quality: Int) {
        viewModelScope.launch {
            val updatedCard = SM2Logic.calculateNextReview(flashcard, quality)
            dao.updateFlashcard(updatedCard)
        }
    }

    fun getDueCards(currentTime: Long): Flow<List<Flashcard>> {
        return dao.getDueFlashcards(currentTime)
    }
}
