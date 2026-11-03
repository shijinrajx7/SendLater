package com.sendlater

import android.app.Application
import com.sendlater.send.Notifications
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SendLaterApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Notifications.createChannel(this)
    }
}
