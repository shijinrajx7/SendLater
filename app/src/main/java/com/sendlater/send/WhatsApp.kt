package com.sendlater.send

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.sendlater.data.ScheduledMessage

object WhatsApp {
    val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

    fun installedPackage(ctx: Context): String? = packages.firstOrNull {
        try { ctx.packageManager.getPackageInfo(it, 0); true } catch (e: Exception) { false }
    }

    /** wa.me deep link that opens the chat with the text pre-filled. */
    fun intentFor(ctx: Context, m: ScheduledMessage): Intent? {
        val pkg = installedPackage(ctx) ?: return null
        val digits = m.phone.filter { it.isDigit() }
        val uri = Uri.parse("https://wa.me/$digits?text=${Uri.encode(m.text)}")
        return Intent(Intent.ACTION_VIEW, uri)
            .setPackage(pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

fun isAccessibilityServiceEnabled(ctx: Context): Boolean {
    val enabled = Settings.Secure.getString(
        ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val cn = ComponentName(ctx, WhatsAppSenderService::class.java)
    return enabled.split(':').any {
        it.equals(cn.flattenToString(), true) || it.equals(cn.flattenToShortString(), true)
    }
}
