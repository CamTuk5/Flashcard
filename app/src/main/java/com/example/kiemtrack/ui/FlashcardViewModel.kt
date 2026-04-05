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
                        // Kiểm tra nếu thẻ chưa có trong local (dựa trên remoteId) thì mới thêm
                        // Hoặc cập nhật thẻ cũ dựa trên logic của bạn
                        dao.insertFlashcard(card)
                    }
                }
            }
    }
    
    fun addFlashcard(front: String, back: String) {
        viewModelScope.launch {
            // Tạo remoteId duy nhất bằng UUID
            val uniqueRemoteId = java.util.UUID.randomUUID().toString()
            val card = Flashcard(
                front = front, 
                back = back, 
                remoteId = uniqueRemoteId
            )
            val id = dao.insertFlashcard(card)
            
            // ĐẨY LÊN BACKEND dùng remoteId làm tên Document
            val remoteCard = card.copy(id = id)
            firestore.collection("flashcards").document(uniqueRemoteId).set(remoteCard)
        }
    }

    fun updateFlashcardQuality(flashcard: Flashcard, quality: Int) {
        viewModelScope.launch {
            val updatedCard = SM2Logic.calculateNextReview(flashcard, quality)
            dao.updateFlashcard(updatedCard)
            
            // CẬP NHẬT LÊN BACKEND dùng remoteId
            val docId = if (flashcard.remoteId.isNotEmpty()) flashcard.remoteId else flashcard.id.toString()
            firestore.collection("flashcards").document(docId).set(updatedCard)
        }
    }

    fun deleteFlashcard(flashcard: Flashcard) {
        viewModelScope.launch {
            dao.deleteFlashcard(flashcard)
            // XOÁ TRÊN BACKEND dùng remoteId
            val docId = if (flashcard.remoteId.isNotEmpty()) flashcard.remoteId else flashcard.id.toString()
            firestore.collection("flashcards").document(docId).delete()
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
}
