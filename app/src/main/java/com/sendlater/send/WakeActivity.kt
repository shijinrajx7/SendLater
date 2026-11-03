package com.sendlater.send

import android.app.KeyguardManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.core.app.NotificationManagerCompat
import com.sendlater.Constants
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Invisible activity that turns the screen on over the lock screen and asks Android to dismiss a
 * swipe-only lock. With a PIN/pattern/fingerprint lock Android keeps it locked: the reminder
 * notification posted by the service is the fallback in that case.
 */
@AndroidEntryPoint
class WakeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val id = intent.getLongExtra(Constants.EXTRA_ID, -1L)
        val km = getSystemService(KeyguardManager::class.java)

        if (!km.isKeyguardLocked) {
            proceed(id)
            return
        }
        km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() { proceed(id) }
            override fun onDismissError() { finish() }
            override fun onDismissCancelled() { finish() }
        })
    }

    private fun proceed(id: Long) {
        if (id >= 0) {
            NotificationManagerCompat.from(this).cancel(id.toInt())
            WhatsAppSenderService.instance?.launchAndSendById(id)
        }
        finish()
    }
}
