package com.example.spacedrepetitionsystem.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val front: String,
    val back: String,
    val difficulty: Double = 2.5,
    val interval: Int = 0,
    val repetitions: Int = 0,
    val nextReviewDate: Long = 0, // Đặt là 0 để thẻ mới luôn xuất hiện trong danh sách học ngay lập tức
    val lastUpdated: Long = System.currentTimeMillis()
)
