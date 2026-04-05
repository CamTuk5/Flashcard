package com.example.kiemtrack.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.kiemtrack.data.AppDatabase
import kotlinx.coroutines.flow.first

class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val dueCards = database.flashcardDao().getDueFlashcards(System.currentTimeMillis()).first()

        if (dueCards.isNotEmpty()) {
            val hardCount = dueCards.count { it.lastQuality == 0 }
            val mediumCount = dueCards.count { it.lastQuality == 3 }
            val easyCount = dueCards.count { it.lastQuality == 5 }

            sendCategorizedNotification(hardCount, mediumCount, easyCount)
        }
        return Result.success()
    }

    private fun sendCategorizedNotification(hard: Int, medium: Int, easy: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "flashcard_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Flashcard Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val title = when {
            hard > 0 -> "⚠️ Cần ôn tập KHẨN CẤP!"
            medium > 0 -> "📚 Đến giờ học rồi!"
            else -> "✨ Ôn tập nhẹ nhàng thôi!"
        }

        val detailText = StringBuilder("Bạn có ")
        val parts = mutableListOf<String>()
        if (hard > 0) parts.add("$hard từ KHÓ")
        if (medium > 0) parts.add("$medium từ VỪA")
        if (easy > 0) parts.add("$easy từ DỄ")
        
        detailText.append(parts.joinToString(", "))
        detailText.append(" cần xem lại ngay.")

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle(title)
            .setContentText(detailText.toString())
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
