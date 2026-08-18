package com.tasker.android.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.tasker.android.MainActivity
import com.tasker.android.R
import kotlin.math.abs

class NoteReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getStringExtra("note_id") ?: return
        val noteTitle = intent.getStringExtra("note_title") ?: "Note Reminder"
        val noteContent = intent.getStringExtra("note_content") ?: ""
        val offset = intent.getIntExtra("reminder_offset", 0)

        val channelId = context.getString(R.string.channel_reminders_id)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "notes/$noteId")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.hashCode(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val timingText = when (offset) {
            0 -> "Now"
            5 -> "In 5 minutes"
            10 -> "In 10 minutes"
            15 -> "In 15 minutes"
            30 -> "In 30 minutes"
            60 -> "In 1 hour"
            1440 -> "In 1 day"
            else -> "$offset mins before"
        }

        val excerpt = noteContent.replace(Regex("[#*_`>-]"), " ").trim().take(100)
        val contentText = if (excerpt.isNotEmpty()) "$timingText • $excerpt" else timingText

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("🔔 Note Reminder: $noteTitle")
            .setContentText(contentText)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        nm.notify(abs(noteId.hashCode() * 31 + offset), notification)
    }
}
