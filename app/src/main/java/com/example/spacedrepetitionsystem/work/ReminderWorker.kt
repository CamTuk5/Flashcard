package com.example.spacedrepetitionsystem.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.spacedrepetitionsystem.data.repository.FlashcardRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: FlashcardRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val dueCards = repository.getCardsToReview(System.currentTimeMillis()).first()
        
        if (dueCards.isNotEmpty()) {
            showNotification(dueCards.size)
        }
        
        return Result.success()
    }

    private fun showNotification(count: Int) {
        val channelId = "srs_reminders"
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Study Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Time to Study!")
            .setContentText("You have $count cards due for review.")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
