package com.sendlater.send

import android.content.Context
import com.sendlater.data.ScheduledMessage

object SendActions {
    /** "Send now" button. Returns an error message, or null if it started fine. */
    fun sendNow(ctx: Context, m: ScheduledMessage, auto: Boolean): String? {
        if (WhatsApp.installedPackage(ctx) == null) return "WhatsApp isn't installed"
        if (auto) {
            val svc = WhatsAppSenderService.instance
                ?: return "Turn on the accessibility service first (Settings > Setup checklist)"
            return if (svc.begin(m)) null else "Couldn't open WhatsApp"
        }
        val intent = WhatsApp.intentFor(ctx, m) ?: return "Couldn't open WhatsApp"
        ctx.startActivity(intent)
        return null
    }
}
