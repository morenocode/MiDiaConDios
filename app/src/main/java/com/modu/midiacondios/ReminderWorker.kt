package com.modu.midiacondios

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

private const val CHANNEL_ID = "daily_devotional"
private const val UNIQUE_WORK_NAME = "daily_devotional_reminder"

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val devotional = DevotionalRepository.today()
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Devocional diario",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Recordatorio para tu momento diario con Dios"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_today)
            .setContentTitle("Tu momento con Dios te espera 🙏")
            .setContentText("Lectura de hoy: ${devotional.reference}")
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "Dedica unos minutos a tu devocional de hoy. Lectura: ${devotional.reference}."
                )
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(1001, notification)
        return Result.success()
    }
}

fun scheduleDailyReminder(context: Context) {
    val now = ZonedDateTime.now()
    var next = now.withHour(8).withMinute(0).withSecond(0).withNano(0)
    if (!next.isAfter(now)) next = next.plusDays(1)
    val initialDelay = Duration.between(now, next).toMillis()

    val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
        .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        UNIQUE_WORK_NAME,
        ExistingPeriodicWorkPolicy.UPDATE,
        request
    )
}

fun cancelDailyReminder(context: Context) {
    WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
}
