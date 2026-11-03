package com.sendlater.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ScheduledMessage::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun messageDao(): MessageDao
}
