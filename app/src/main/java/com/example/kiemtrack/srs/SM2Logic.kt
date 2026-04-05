package com.example.kiemtrack.srs

import com.example.kiemtrack.model.Flashcard
import java.util.Calendar

object SM2Logic {
    /**
     * quality: 5 (Dễ), 3 (Vừa), 0 (Khó)
     */
    fun calculateNextReview(flashcard: Flashcard, quality: Int): Flashcard {
        val calendar = Calendar.getInstance()
        
        val intervalInMinutes = when (quality) {
            5 -> 20 // Dễ: 20 phút
            3 -> 10 // Vừa: 10 phút
            else -> 2 // Khó: 2 phút
        }

        calendar.add(Calendar.MINUTE, intervalInMinutes)

        return flashcard.copy(
            nextReviewDate = calendar.timeInMillis,
            lastReviewDate = System.currentTimeMillis(),
            lastQuality = quality
        )
    }
}
