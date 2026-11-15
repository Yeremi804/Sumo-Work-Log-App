package com.resu.sumo.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.resu.sumo.MainActivity
import com.resu.sumo.R
import com.resu.sumo.ui.AppLanguage

object ShiftNotificationManager {

    private const val CHANNEL_ID = "sumo_shift_reminder_channel"
    private const val NOTIFICATION_ID = 1001

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Sumo Shift Reminders"
            val descriptionText = "Notifications reminding workers to clock out when shifts exceed 4 or 8 hours"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showShiftReminderNotification(
        context: Context,
        hoursElapsed: Int,
        location: String,
        lang: AppLanguage = AppLanguage.SPANISH
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (lang == AppLanguage.SPANISH) {
            "⏱️ Sumo: ¿Sigues en turno en $location?"
        } else {
            "⏱️ Sumo: Still Clocked In at $location?"
        }

        val text = if (lang == AppLanguage.SPANISH) {
            "Llevas $hoursElapsed horas trabajando. Toca aquí para marcar salida o registrar lonche."
        } else {
            "You have been clocked in for $hoursElapsed hours. Tap here to clock out or record lunch."
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.sumo_app_logo)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(NOTIFICATION_ID, builder.build())
            }
        } catch (e: SecurityException) {
            // Notification permission denied by user
        }
    }

    fun cancelReminderNotification(context: Context) {
        try {
            with(NotificationManagerCompat.from(context)) {
                cancel(NOTIFICATION_ID)
            }
        } catch (e: SecurityException) {
            // Ignore
        }
    }
}
