package com.sendlater.send

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationManagerCompat
import com.sendlater.Constants
import com.sendlater.MainActivity
import com.sendlater.data.MessageRepository
import com.sendlater.data.ScheduledMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AUTO MODE.
 * begin() -> (wake + unlock if needed) -> launchAndSend() opens the wa.me chat ->
 * we look for WhatsApp's Send button (on every accessibility event AND every 500 ms) -> click it ->
 * mark SENT -> re-lock the phone if we woke it, otherwise return to SendLater.
 * If Send isn't found within TIMEOUT_MS the message is marked FAILED with a reason.
 */
@AndroidEntryPoint
class WhatsAppSenderService : AccessibilityService() {

    @Inject lateinit var repo: MessageRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val handler = Handler(Looper.getMainLooper())

    private var armedId: Long? = null   // message currently being sent
    private var clicking = false        // guards against double clicks
    private var relock = false          // we woke a sleeping phone -> lock it again afterwards

    companion object {
        @Volatile var instance: WhatsAppSenderService? = null
        private const val TIMEOUT_MS = 25_000L
        private const val CLICK_DELAY_MS = 700L // let WhatsApp finish pre-filling the text
        private const val POLL_MS = 500L
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        instance = null
        return super.onUnbind(intent)
    }

    /** Entry point. Wakes the screen first if the phone is asleep or locked. Main thread only. */
    fun begin(m: ScheduledMessage): Boolean {
        val km = getSystemService(KeyguardManager::class.java)
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isInteractive || km.isKeyguardLocked) {
            return try {
                relock = true
                wakeScreen()
                // Safety net: if the phone has a PIN/pattern we can't unlock it ourselves.
                // Tapping this notification after you unlock still sends automatically.
                Notifications.sendReminder(this, m)
                startActivity(
                    Intent(this, WakeActivity::class.java)
                        .putExtra(Constants.EXTRA_ID, m.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                true
            } catch (e: Exception) {
                false
            }
        }
        relock = false
        return launchAndSend(m)
    }

    @Suppress("DEPRECATION")
    private fun wakeScreen() {
        val pm = getSystemService(PowerManager::class.java)
        val wl = pm.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP or PowerManager.ON_AFTER_RELEASE,
            "SendLater:wake"
        )
        wl.acquire(40_000L)
    }

    /** Opens the chat and starts hunting for the Send button. Returns false if WhatsApp can't be opened. */
    fun launchAndSend(m: ScheduledMessage): Boolean {
        val intent = WhatsApp.intentFor(this, m) ?: return false
        armedId = m.id
        clicking = false
        NotificationManagerCompat.from(this).cancel(m.id.toInt())
        return try {
            startActivity(intent)
            handler.removeCallbacks(timeout)
            handler.postDelayed(timeout, TIMEOUT_MS)
            handler.removeCallbacks(poll)
            handler.postDelayed(poll, POLL_MS)
            true
        } catch (e: Exception) {
            armedId = null
            false
        }
    }

    /** Used by WakeActivity once the screen is awake and unlocked. */
    fun launchAndSendById(id: Long) {
        scope.launch {
            val m = repo.get(id) ?: return@launch
            if (!launchAndSend(m)) {
                repo.markFailed(id, "Couldn't open WhatsApp.")
                Notifications.failure(this@WhatsAppSenderService, m, "Couldn't open WhatsApp.")
            }
        }
    }

    private val poll = object : Runnable {
        override fun run() {
            if (armedId == null) return
            trySend()
            handler.postDelayed(this, POLL_MS)
        }
    }

    private val timeout = Runnable {
        val id = armedId ?: return@Runnable
        armedId = null
        handler.removeCallbacks(poll)
        val why = "Couldn't find WhatsApp's Send button in time. Is the number on WhatsApp, and was the phone unlocked?"
        scope.launch {
            val m = repo.get(id)
            repo.markFailed(id, why)
            if (m != null) Notifications.failure(this@WhatsAppSenderService, m, why)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (armedId == null) return
        if (event?.packageName?.toString() !in WhatsApp.packages) return
        trySend()
    }

    private fun trySend() {
        val id = armedId ?: return
        if (clicking) return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() !in WhatsApp.packages) return
        if (findSendButton(root) == null) return

        clicking = true
        handler.postDelayed({
            val fresh = rootInActiveWindow?.let { findSendButton(it) }
            if (fresh != null && clickNode(fresh)) finish(id) else clicking = false
        }, CLICK_DELAY_MS)
    }

    /** WhatsApp's send button: by view id, then by its "Send" label. Only exists once text is in the box. */
    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val pkg = root.packageName?.toString() ?: return null
        root.findAccessibilityNodeInfosByViewId("$pkg:id/send").firstOrNull()?.let { return it }
        return root.findAccessibilityNodeInfosByText("Send").firstOrNull {
            it.contentDescription?.toString().equals("Send", ignoreCase = true)
        }
    }

    /** Click the node, or the nearest clickable parent. */
    private fun clickNode(n: AccessibilityNodeInfo): Boolean {
        var cur: AccessibilityNodeInfo? = n
        var hops = 0
        while (cur != null && hops < 4) {
            if (cur.isClickable && cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
            cur = cur.parent
            hops++
        }
        return false
    }

    private fun finish(id: Long) {
        handler.removeCallbacks(timeout)
        handler.removeCallbacks(poll)
        armedId = null
        clicking = false
        scope.launch { repo.markSent(id) }
        val lockAgain = relock
        relock = false
        handler.postDelayed({
            if (lockAgain && Build.VERSION.SDK_INT >= 28) {
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                startActivity(
                    Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(Constants.EXTRA_SKIP_INTRO, true)
                )
            }
        }, 1_200)
    }

    override fun onInterrupt() {}
}
