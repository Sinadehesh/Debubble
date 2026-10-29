package com.debubble.app.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.debubble.app.data.Repository
import com.debubble.app.data.stillToDo
import com.debubble.app.engine.Reminder
import com.debubble.app.engine.Reminders
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * A reminder going off, or being put off, or every alarm needing setting again after the
 * phone restarted or its clock moved.
 *
 * The reminder is read back out of the saved state rather than out of the alarm's extras, so
 * a reminder removed or changed since it was set does the current thing. Nothing is posted
 * for something already done today.
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val link = intent.getStringExtra(Reminding.EXTRA_LINK)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    Reminding.ACTION_FIRE -> if (link != null) fire(app, link)
                    Reminding.ACTION_SNOOZE -> if (link != null) snooze(app, link)
                }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun fire(context: Context, link: String) {
        val repo = Repository(context)
        val today = LocalDate.now().toEpochDay()
        val state = repo.state.first().rolledTo(today)
        val r = Reminders.forLink(state.reminders, link) ?: return
        if (state.stillToDo(Reminders.base(link), today)) Reminding.post(context, r)
        if (r.daily) {
            // A minute on, so "next" is tomorrow's and not this one again.
            Reminding.schedule(context, r, LocalDateTime.now().plusMinutes(1))
        } else {
            repo.update { s -> s.copy(reminders = Reminders.remove(s.reminders, link)) }
        }
    }

    private suspend fun snooze(context: Context, link: String) {
        NotificationManagerCompat.from(context).cancel(link.hashCode())
        val repo = Repository(context)
        val now = LocalDateTime.now().plusMinutes(Reminders.SNOOZE_MINUTES.toLong())
        var set: Reminder? = null
        repo.update { s ->
            val r = Reminders.forLink(s.reminders, link) ?: return@update s
            // A daily one stays daily; the half-hour is a one-off of its own under a
            // separate link, so tomorrow's still goes off at the usual time.
            val later = r.copy(
                link = if (r.daily) "$link#later" else link,
                daily = false,
                day = now.toLocalDate().toEpochDay(),
                minute = now.hour * 60 + now.minute
            )
            set = later
            s.copy(reminders = Reminders.upsert(s.reminders, later))
        }
        set?.let { Reminding.schedule(context, it) }
    }

}

/**
 * Alarms do not survive a restart, and one set for 19:00 means something else after the
 * clock or the time zone moves. Exported because these broadcasts come from the system; it
 * only ever re-reads the saved reminders and sets them again, whoever sends it.
 */
class ReminderResetReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RESETS) return
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repo = Repository(app)
                val now = LocalDateTime.now()
                repo.update { s -> s.copy(reminders = Reminders.live(s.reminders, now)) }
                Reminding.scheduleAll(app, repo.state.first().reminders, now)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        val RESETS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED
        )
    }
}
