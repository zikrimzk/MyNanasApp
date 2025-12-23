package com.spm.mynanasapp.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.spm.mynanasapp.R
import com.spm.mynanasapp.data.network.RetrofitClient
import com.spm.mynanasapp.utils.SessionManager

class DailyNotificationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val CHANNEL_ID = "my_nanas_notifcation_channel"

    override suspend fun doWork(): Result {
        val token = SessionManager.getToken(applicationContext) ?: return Result.failure()

        return try {
            val response = RetrofitClient.instance.getNewPosts("Bearer $token", "")
            //if (response.isSuccessful && response.body()?.status == true) {
            if (response.isSuccessful) {
                val message = response.body()?.message ?: ""
                if (message.isNotEmpty()) {
                    createNotificationChannel(applicationContext) // 1. Create channel
                    sendNotification(applicationContext, message) // 2. Call correct name
                }
                Result.success()
            } else {
                Result.failure()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Lab Notifications"
            val descriptionText = "Channel for Mobile App Dev Lab"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(context: Context, content: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Catch Up!")
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            // Permission check for Android 13+ (API 33)
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                notify(1, builder.build())
            } else {
                // If permission is not granted (e.g., on older devices or if user denied on Android 13+),
                // the notification will not be shown. On newer devices, runtime permission is needed.
            }
        }
    }
}
