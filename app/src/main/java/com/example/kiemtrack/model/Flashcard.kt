package com.example.kiemtrack.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "flashcards")
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val front: String,
    val back: String,
    // SM-2 fields
    val interval: Int = 0, // Days
    val repetitions: Int = 0,
    val easeFactor: Float = 2.5f,
    val nextReviewDate: Long = System.currentTimeMillis()
)
