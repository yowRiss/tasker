package com.tasker.android.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tasker.android.data.model.Note
import java.time.Instant
import kotlin.math.abs

object NoteReminderScheduler {

    fun scheduleNoteReminders(context: Context, note: Note) {
        cancelNoteReminders(context, note.id, note.reminderOffsets)

        val reminderAtStr = note.reminderAt ?: return
        val reminderAtEpochMs = try {
            Instant.parse(reminderAtStr).toEpochMilli()
        } catch (_: Exception) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        for (offset in note.reminderOffsets) {
            val triggerAtMs = reminderAtEpochMs - (offset * 60 * 1000L)
            if (triggerAtMs <= now) continue

            val intent = Intent(context, NoteReminderReceiver::class.java).apply {
                putExtra("note_id", note.id)
                putExtra("note_title", note.title)
                putExtra("note_content", note.contentMd)
                putExtra("reminder_offset", offset)
            }

            val requestCode = getRequestCode(note.id, offset)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMs,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
                }
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMs, pendingIntent)
            }
        }
    }

    fun cancelNoteReminders(
        context: Context,
        noteId: String,
        offsets: List<Int> = listOf(0, 5, 10, 15, 30, 60, 1440)
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        for (offset in offsets) {
            val intent = Intent(context, NoteReminderReceiver::class.java)
            val requestCode = getRequestCode(noteId, offset)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun getRequestCode(noteId: String, offsetMinutes: Int): Int {
        return abs(noteId.hashCode() * 31 + offsetMinutes)
    }
}
