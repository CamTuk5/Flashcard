package com.example.kiemtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.kiemtrack.data.AppDatabase
import com.example.kiemtrack.model.Flashcard
import com.example.kiemtrack.srs.SM2Logic
import com.example.kiemtrack.worker.ReminderWorker
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).flashcardDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val workManager = WorkManager.getInstance(application)
    
    val allCards: Flow<List<Flashcard>> = dao.getAllFlashcards()

    init {
        fetchFromFirestore()
    }

    private fun fetchFromFirestore() {
        firestore.collection("flashcards")
            .get()
            .addOnSuccessListener { result ->
                viewModelScope.launch {
                    val cards = result.toObjects(Flashcard::class.java)
                    cards.forEach { card ->
                        dao.insertFlashcard(card)
                    }
                }
            }
    }
    
    fun addFlashcard(front: String, back: String, category: String = "Chung") {
        viewModelScope.launch {
            val uniqueRemoteId = java.util.UUID.randomUUID().toString()
            val card = Flashcard(
                front = front, 
                back = back, 
                remoteId = uniqueRemoteId,
                courseId = category // Dùng trường courseId để lưu chủ đề
            )
            val id = dao.insertFlashcard(card)
            
            val remoteCard = card.copy(id = id)
            firestore.collection("flashcards").document(uniqueRemoteId).set(remoteCard)
        }
    }

    fun updateFlashcardQuality(flashcard: Flashcard, quality: Int) {
        viewModelScope.launch {
            val updatedCard = SM2Logic.calculateNextReview(flashcard, quality)
            dao.updateFlashcard(updatedCard)
            
            scheduleNotification(updatedCard)

            val docId = if (flashcard.remoteId.isNotEmpty()) flashcard.remoteId else flashcard.id.toString()
            firestore.collection("flashcards").document(docId).set(updatedCard)
        }
    }

    private fun scheduleNotification(card: Flashcard) {
        val delay = card.nextReviewDate - System.currentTimeMillis()
        if (delay > 0) {
            val workRequest = OneTimeWorkRequestBuilder<ReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("card_reminder_${card.id}")
                .build()

            workManager.enqueueUniqueWork(
                "card_reminder_${card.id}",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
        }
    }

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            dao.deleteFlashcard(flashcard)
            workManager.cancelUniqueWork("card_reminder_${flashcard.id}")
            
            val docId = if (flashcard.remoteId.isNotEmpty()) flashcard.remoteId else flashcard.id.toString()
            firestore.collection("flashcards").document(docId).delete()
        }
    }

    fun getDueCards(currentTime: Long): Flow<List<Flashcard>> {
        return dao.getDueFlashcards(currentTime)
    }
}
