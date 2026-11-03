package com.sendlater.di

import com.sendlater.data.MessageRepository
import com.sendlater.data.SettingsStore
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Lets BroadcastReceivers reach Hilt objects (Kotlin can't use @AndroidEntryPoint on receivers cleanly). */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ReceiverEntryPoint {
    fun repo(): MessageRepository
    fun settings(): SettingsStore
}
