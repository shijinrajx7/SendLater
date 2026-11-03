package com.sendlater.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Repeat { NONE, DAILY, WEEKLY, MONTHLY }
enum class Status { PENDING, SENT, FAILED }

@Entity(tableName = "scheduled_messages")
data class ScheduledMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val contactName: String,
    val phone: String,          // digits only, with country code
    val text: String,
    val triggerAt: Long,        // epoch millis
    val repeat: Repeat = Repeat.NONE,
    val status: Status = Status.PENDING,
    val note: String = ""       // why it failed (shown on the card)
)
