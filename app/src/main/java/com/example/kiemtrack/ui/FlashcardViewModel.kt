package com.example.kiemtrack.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.kiemtrack.data.AppDatabase
import com.example.kiemtrack.model.Flashcard
import com.example.kiemtrack.srs.SM2Logic
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class FlashcardViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).flashcardDao()
    private val firestore = FirebaseFirestore.getInstance()
    
    val allCards: Flow<List<Flashcard>> = dao.getAllFlashcards()

    init {
        fetchFromFirestore()
    }

    private fun fetchFromFirestore() {
        // Lấy dữ liệu từ Firestore về khi khởi tạo để đồng bộ máy mới
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
    
    fun addFlashcard(front: String, back: String, courseId: String = "Chung") {
        viewModelScope.launch {
            val card = Flashcard(front = front, back = back, courseId = courseId)
            val id = dao.insertFlashcard(card)
            
            // ĐẨY LÊN BACKEND (FIRESTORE)
            val remoteCard = card.copy(id = id)
            firestore.collection("flashcards").document(id.toString()).set(remoteCard)
        }
    }

    fun updateFlashcardQuality(flashcard: Flashcard, quality: Int) {
        viewModelScope.launch {
            val updatedCard = SM2Logic.calculateNextReview(flashcard, quality)
            dao.updateFlashcard(updatedCard)
            
            // CẬP NHẬT TIẾN ĐỘ LÊN BACKEND
            firestore.collection("flashcards").document(flashcard.id.toString()).set(updatedCard)
        }
    }

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            dao.deleteFlashcard(flashcard)
            // XOÁ TRÊN BACKEND
            firestore.collection("flashcards").document(flashcard.id.toString()).delete()
        }
    }

    fun deleteCourse(courseId: String) {
        viewModelScope.launch {
            dao.deleteCardsByCourse(courseId)
            // Logic xoá toàn bộ collection trên backend (có thể triển khai thêm tuỳ nhu cầu)
        }
    }

    fun getDueCards(currentTime: Long): Flow<List<Flashcard>> {
        return dao.getDueFlashcards(currentTime)
    }

    fun getDueCardsByCourse(courseId: String, currentTime: Long): Flow<List<Flashcard>> {
        return if (courseId == "All" || courseId == "Chung") {
            dao.getDueFlashcards(currentTime)
        } else {
            dao.getDueFlashcardsByCourse(courseId, currentTime)
        }
    }
    
    fun getAllCourses(): Flow<List<String>> {
        return dao.getAllCourseIds()
    }
}
