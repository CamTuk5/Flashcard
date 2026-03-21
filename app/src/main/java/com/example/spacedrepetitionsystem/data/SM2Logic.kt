package com.example.spacedrepetitionsystem.data

import com.example.spacedrepetitionsystem.data.model.Flashcard
import kotlin.math.max

object SM2Logic {
    fun calculateNextReview(card: Flashcard, quality: Int): Flashcard {
        val newRepetitions: Int
        val newInterval: Int // đơn vị: ngày (cho SM2 chuẩn)
        val nextReviewDate: Long
        val newDifficulty: Double

        when (quality) {
            0 -> { // Làm lại (Quên hoàn toàn) - Hiện lại sau 5 giây
                newRepetitions = 0
                nextReviewDate = System.currentTimeMillis() + 5 * 1000L
                newInterval = 0
                newDifficulty = card.difficulty
            }
            3 -> { // Tạm được (Nhớ mang máng) - Hiện lại sau 30 giây
                newRepetitions = card.repetitions
                nextReviewDate = System.currentTimeMillis() + 30 * 1000L
                newInterval = 0
                newDifficulty = card.difficulty
            }
            5 -> { // Rất tốt (Thuộc rồi)
                if (card.repetitions == 0) {
                    // Thẻ mới hoặc vừa bị quên, cho xem lại sau 1 phút
                    newRepetitions = 1
                    nextReviewDate = System.currentTimeMillis() + 60 * 1000L
                    newInterval = 1
                    newDifficulty = card.difficulty
                } else {
                    // Đã qua giai đoạn học, áp dụng SM2 chuẩn để dời sang các ngày sau
                    newRepetitions = card.repetitions + 1
                    newInterval = when (newRepetitions) {
                        2 -> 6
                        else -> max(1, (card.interval * card.difficulty).toInt())
                    }
                    newDifficulty = max(1.3, card.difficulty + (0.1 - (5 - quality) * (0.08 + (5 - quality) * 0.02)))
                    nextReviewDate = System.currentTimeMillis() + (newInterval * 24L * 60L * 60L * 1000L)
                }
            }
            else -> {
                newRepetitions = card.repetitions
                nextReviewDate = System.currentTimeMillis() + 60 * 1000L
                newInterval = 0
                newDifficulty = card.difficulty
            }
        }

        return card.copy(
            repetitions = newRepetitions,
            interval = newInterval,
            difficulty = newDifficulty,
            nextReviewDate = nextReviewDate,
            lastUpdated = System.currentTimeMillis()
        )
    }
}
