package com.sendlater.send

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sendlater.Constants
import com.sendlater.MainActivity
import com.sendlater.data.ScheduledMessage

object Notifications {
    private const val CHANNEL = "scheduled_messages"

    fun createChannel(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Scheduled messages", NotificationManager.IMPORTANCE_HIGH)
            ctx.getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    /** Safe mode: tapping this opens WhatsApp with the message ready to send. */
    @SuppressLint("MissingPermission")
    fun sendReminder(ctx: Context, m: ScheduledMessage) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val intent = Intent(ctx, MainActivity::class.java)
            .putExtra(Constants.EXTRA_SEND_ID, m.id)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            ctx, m.id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val who = m.contactName.ifBlank { m.phone }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentTitle("Time to message $who")
            .setContentText(m.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(m.text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(m.id.toInt(), n)
    }

    @SuppressLint("MissingPermission")
    fun failure(ctx: Context, m: ScheduledMessage, reason: String) {
        val nm = NotificationManagerCompat.from(ctx)
        if (!nm.areNotificationsEnabled()) return
        val pi = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val who = m.contactName.ifBlank { m.phone }
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle("Message to $who failed")
            .setContentText(reason)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(m.id.toInt() + 1_000_000, n)
    }
}
