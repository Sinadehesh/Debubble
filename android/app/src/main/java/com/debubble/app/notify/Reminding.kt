package com.debubble.app.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.debubble.app.MainActivity
import com.debubble.app.R
import com.debubble.app.engine.Reminder
import com.debubble.app.engine.Reminders
import com.debubble.app.widget.DayWidget
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reminders, on the phone: alarms that wake [ReminderReceiver], and the notification it
 * posts. What to remind and when is decided in [Reminders]; this only carries it out.
 *
 * Inexact alarms, on purpose. An exact one needs a permission the user has to grant in
 * settings, and nobody needs "wash up" at 19:00:00 rather than a minute or two after.
 */
object Reminding {

    const val CHANNEL = "reminders"
    const val ACTION_FIRE = "com.debubble.app.notify.FIRE"
    const val ACTION_SNOOZE = "com.debubble.app.notify.SNOOZE"
    const val EXTRA_LINK = "link"

    /** Whether notifications can be shown at all: the permission on 13+, and the app's switch. */
    fun canNotify(context: Context): Boolean {
        val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return granted && NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /** Whether the system permission dialog is the thing standing in the way. */
    fun needsPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "The times you picked for the things you mean to do."
            }
        )
    }

    /** Set every reminder's next alarm. Setting one that is already set replaces it. */
    fun scheduleAll(context: Context, reminders: List<Reminder>, now: LocalDateTime = LocalDateTime.now()) {
        reminders.forEach { schedule(context, it, now) }
    }

    fun schedule(context: Context, r: Reminder, now: LocalDateTime = LocalDateTime.now()) {
        val at = Reminders.next(r, now) ?: return
        val millis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runCatching {
            context.getSystemService(AlarmManager::class.java)
                ?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, fire(context, r.link))
        }
    }

    fun cancel(context: Context, link: String) {
        runCatching { context.getSystemService(AlarmManager::class.java)?.cancel(fire(context, link)) }
        runCatching { NotificationManagerCompat.from(context).cancel(link.hashCode()) }
    }

    /** Take down a notification that is showing for [link], if one is: it has been done. */
    fun dismiss(context: Context, link: String) {
        runCatching { NotificationManagerCompat.from(context).cancel(link.hashCode()) }
        runCatching { NotificationManagerCompat.from(context).cancel("$link#later".hashCode()) }
    }

    /** The notification itself. Tapping it opens the thing; "In 30 min" puts it off once. */
    fun post(context: Context, r: Reminder) {
        if (!canNotify(context)) return
        ensureChannel(context)
        val open = PendingIntent.getActivity(
            context,
            ("open:" + r.link).hashCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(DayWidget.EXTRA_OPEN_CHALLENGE, Reminders.base(r.link))
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snooze = PendingIntent.getBroadcast(
            context,
            ("snooze:" + r.link).hashCode(),
            Intent(context, ReminderReceiver::class.java)
                .setAction(ACTION_SNOOZE)
                .putExtra(EXTRA_LINK, r.link),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notify)
            .setColor(ContextCompat.getColor(context, R.color.ink_heart))
            .setContentTitle(r.title)
            .setContentText(r.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.text))
            .setLargeIcon(DayWidget.iconFor(context, iconOf(Reminders.base(r.link))))
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(0, "In ${Reminders.SNOOZE_MINUTES} min", snooze)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(r.link.hashCode(), notification) }
    }

    private fun iconOf(link: String): String = when {
        link.startsWith("care:") -> "icon_" + link.removePrefix("care:").lowercase()
        link.startsWith("mission:") -> "icon_goal"
        else -> "icon_out"
    }

    private fun fire(context: Context, link: String): PendingIntent = PendingIntent.getBroadcast(
        context,
        ("fire:" + link).hashCode(),
        Intent(context, ReminderReceiver::class.java)
            .setAction(ACTION_FIRE)
            .putExtra(EXTRA_LINK, link),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}
