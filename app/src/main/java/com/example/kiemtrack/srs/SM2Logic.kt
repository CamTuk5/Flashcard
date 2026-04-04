package com.example.kiemtrack.srs

import com.example.kiemtrack.model.Flashcard
import java.util.Calendar
import java.util.Date

object SM2Logic {
    fun calculateNextReview(flashcard: Flashcard, quality: Int): Flashcard {
        var interval: Int
        var repetitions: Int
        var easeFactor: Float

        if (quality >= 3) {
            if (flashcard.repetitions == 0) {
                interval = 1
                repetitions = 1
            } else if (flashcard.repetitions == 1) {
                interval = 6
                repetitions = 2
            } else {
                interval = (flashcard.interval * flashcard.easeFactor).toInt()
                repetitions = flashcard.repetitions + 1
            }
            easeFactor = flashcard.easeFactor + (0.1f - (5 - quality) * (0.08f + (5 - quality) * 0.02f))
        } else {
            repetitions = 0
            interval = 1
            easeFactor = flashcard.easeFactor
        }

        if (easeFactor < 1.3f) easeFactor = 1.3f

        val calendar = Calendar.getInstance()
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, interval)
        
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        return flashcard.copy(
            interval = interval,
            repetitions = repetitions,
            easeFactor = easeFactor,
            nextReviewDate = calendar.timeInMillis,
            lastReviewDate = System.currentTimeMillis() // Cập nhật ngày học xong
        )
    }
}
