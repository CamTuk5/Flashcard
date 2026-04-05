package com.example.kiemtrack.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = "",
    val front: String = "",
    val back: String = "",
    // SM-2 fields
    val interval: Int = 0,
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewDate: Long = 0L, // Thời gian cuối cùng học thẻ này
    val courseId: String = ""
)
