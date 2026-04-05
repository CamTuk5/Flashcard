package com.example.kiemtrack.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val remoteId: String = "",
    val front: String = "",
    val back: String = "",
    val nextReviewDate: Long = System.currentTimeMillis(),
    val lastReviewDate: Long = 0L,
    val courseId: String = "",
    val lastQuality: Int = 3 // 0: Khó, 3: Vừa, 5: Dễ
)
