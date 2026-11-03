package com.sendlater.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {
    @Query("SELECT * FROM scheduled_messages ORDER BY triggerAt ASC")
    fun observeAll(): Flow<List<ScheduledMessage>>

    @Query("SELECT * FROM scheduled_messages WHERE id = :id")
    suspend fun get(id: Long): ScheduledMessage?

    @Query("SELECT * FROM scheduled_messages WHERE status = 'PENDING'")
    suspend fun pending(): List<ScheduledMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(m: ScheduledMessage): Long

    @Query("DELETE FROM scheduled_messages WHERE id = :id")
    suspend fun delete(id: Long)
}
