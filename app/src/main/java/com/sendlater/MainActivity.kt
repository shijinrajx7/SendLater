package com.sendlater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.sendlater.data.MessageRepository
import com.sendlater.data.SendMode
import com.sendlater.data.SettingsStore
import com.sendlater.data.Status
import com.sendlater.data.ThemeMode
import com.sendlater.send.WhatsApp
import com.sendlater.send.WhatsAppSenderService
import com.sendlater.ui.AppNav
import com.sendlater.ui.intro.IntroScreen
import com.sendlater.ui.intro.IntroState
import com.sendlater.ui.theme.SendLaterTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repo: MessageRepository
    @Inject lateinit var settings: SettingsStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val skipIntro = intent?.hasExtra(Constants.EXTRA_SEND_ID) == true ||
            intent?.getBooleanExtra(Constants.EXTRA_SKIP_INTRO, false) == true
        handleSendIntent(intent)
        setContent {
            val theme by settings.theme.collectAsState()
            val maker by settings.maker.collectAsState()
            var showIntro by remember { mutableStateOf(!skipIntro && !IntroState.shown) }
            val dark = when (theme) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            SendLaterTheme(dark) {
                if (showIntro) {
                    IntroScreen(maker) {
                        IntroState.shown = true
                        showIntro = false
                    }
                } else {
                    AppNav(onReplayIntro = { showIntro = true })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleSendIntent(intent)
    }

    /** Reminder notification tapped -> send that message (automatically in auto mode). */
    private fun handleSendIntent(i: Intent?) {
        val id = i?.getLongExtra(Constants.EXTRA_SEND_ID, -1L) ?: -1L
        if (id < 0) return
        i?.removeExtra(Constants.EXTRA_SEND_ID)
        lifecycleScope.launch {
            val m = repo.get(id) ?: return@launch
            if (m.status != Status.PENDING) return@launch
            NotificationManagerCompat.from(this@MainActivity).cancel(id.toInt())

            val service = WhatsAppSenderService.instance
            if (settings.mode.value == SendMode.AUTO && service != null) {
                if (!service.begin(m)) repo.markFailed(id, "Couldn't open WhatsApp.")
                return@launch
            }
            val waIntent = WhatsApp.intentFor(this@MainActivity, m)
            if (waIntent == null) {
                repo.markFailed(id, "WhatsApp isn't installed.")
                return@launch
            }
            startActivity(waIntent)
            repo.markSent(id) // safe mode: you still tap Send inside WhatsApp
        }
    }
}
