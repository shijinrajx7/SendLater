package com.sendlater.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sendlater.di.ReceiverEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are wiped on reboot, so re-register every pending message. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = EntryPointAccessors
            .fromApplication(context.applicationContext, ReceiverEntryPoint::class.java).repo()
        val result = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try { repo.rescheduleAll() } finally { result.finish() }
        }
    }
}
