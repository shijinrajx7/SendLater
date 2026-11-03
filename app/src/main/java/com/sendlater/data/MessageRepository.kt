package com.sendlater.data

import com.sendlater.scheduler.AlarmScheduler
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageRepository @Inject constructor(
    private val dao: MessageDao,
    private val scheduler: AlarmScheduler
) {
    val all = dao.observeAll()

    suspend fun get(id: Long) = dao.get(id)

    suspend fun save(m: ScheduledMessage): Long {
        val id = dao.upsert(m)
        scheduler.schedule(m.copy(id = id))
        return id
    }

    suspend fun delete(id: Long) {
        scheduler.cancel(id)
        dao.delete(id)
    }

    /** One-off -> SENT. Repeating -> move to next occurrence and stay PENDING. */
    suspend fun markSent(id: Long) {
        val m = dao.get(id) ?: return
        if (m.repeat == Repeat.NONE) {
            dao.upsert(m.copy(status = Status.SENT, note = ""))
        } else {
            val next = m.copy(triggerAt = nextTrigger(m), status = Status.PENDING, note = "")
            dao.upsert(next)
            scheduler.schedule(next)
        }
    }

    suspend fun markFailed(id: Long, reason: String = "") {
        val m = dao.get(id) ?: return
        dao.upsert(m.copy(status = Status.FAILED, note = reason))
    }

    /** Called after reboot / app update. */
    suspend fun rescheduleAll() {
        dao.pending().forEach { scheduler.schedule(it) }
    }

    private fun nextTrigger(m: ScheduledMessage): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = m.triggerAt }
        val now = System.currentTimeMillis()
        do {
            when (m.repeat) {
                Repeat.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                Repeat.WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                Repeat.MONTHLY -> cal.add(Calendar.MONTH, 1)
                Repeat.NONE -> return m.triggerAt
            }
        } while (cal.timeInMillis <= now)
        return cal.timeInMillis
    }
}
