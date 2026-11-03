package com.sendlater.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sendlater.Constants
import com.sendlater.data.MessageRepository
import com.sendlater.data.SendMode
import com.sendlater.data.SettingsStore
import com.sendlater.data.Status
import com.sendlater.di.ReceiverEntryPoint
import com.sendlater.send.Notifications
import com.sendlater.send.WhatsApp
import com.sendlater.send.WhatsAppSenderService
import com.sendlater.send.isAccessibilityServiceEnabled
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Constants.EXTRA_ID, -1L)
        if (id < 0) return
        val appCtx = context.applicationContext
        val ep = EntryPointAccessors.fromApplication(appCtx, ReceiverEntryPoint::class.java)
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { handle(appCtx, id, ep.repo(), ep.settings()) } finally { result.finish() }
        }
    }

    private suspend fun handle(ctx: Context, id: Long, repo: MessageRepository, settings: SettingsStore) {
        val m = repo.get(id) ?: return
        if (m.status != Status.PENDING) return

        if (WhatsApp.installedPackage(ctx) == null) {
            repo.markFailed(id, "WhatsApp isn't installed.")
            Notifications.failure(ctx, m, "WhatsApp isn't installed.")
            return
        }

        val auto = settings.mode.value == SendMode.AUTO
        if (!auto) {
            Notifications.sendReminder(ctx, m)
            return
        }

        // Our process was probably just started by the alarm; give Android a moment to reconnect the service.
        var service = WhatsAppSenderService.instance
        if (service == null && isAccessibilityServiceEnabled(ctx)) {
            var waited = 0
            while (service == null && waited < 8000) {
                delay(250); waited += 250
                service = WhatsAppSenderService.instance
            }
        }

        val svc = service
        if (svc == null) {
            // Service isn't running: fall back to a tap-to-send notification
            Notifications.sendReminder(ctx, m)
            return
        }
        val ok = withContext(Dispatchers.Main) { svc.begin(m) }
        if (!ok) {
            repo.markFailed(id, "Couldn't open WhatsApp (check 'Display over other apps' in Settings).")
            Notifications.failure(ctx, m, "Couldn't open WhatsApp (check the Setup checklist in Settings).")
        }
    }
}
