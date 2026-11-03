package com.sendlater.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.sendlater.Constants
import com.sendlater.data.ScheduledMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmScheduler @Inject constructor(@ApplicationContext private val ctx: Context) {
    private val am = ctx.getSystemService(AlarmManager::class.java)

    fun canScheduleExact() = Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()

    fun schedule(m: ScheduledMessage) {
        val pi = pendingIntent(m.id)
        if (canScheduleExact()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, m.triggerAt, pi)
        } else {
            // Permission not granted yet: fall back to an inexact alarm (may be a few minutes late)
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, m.triggerAt, pi)
        }
    }

    fun cancel(id: Long) = am.cancel(pendingIntent(id))

    private fun pendingIntent(id: Long): PendingIntent {
        val intent = Intent(ctx, AlarmReceiver::class.java).putExtra(Constants.EXTRA_ID, id)
        return PendingIntent.getBroadcast(
            ctx, id.toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
