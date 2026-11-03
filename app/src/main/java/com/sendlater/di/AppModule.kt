package com.sendlater.di

import android.content.Context
import androidx.room.Room
import com.sendlater.data.AppDatabase
import com.sendlater.data.MessageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton
    fun provideDb(@ApplicationContext c: Context): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, "sendlater.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDao(db: AppDatabase): MessageDao = db.messageDao()
}
