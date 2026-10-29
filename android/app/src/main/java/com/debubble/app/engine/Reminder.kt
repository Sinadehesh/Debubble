package com.debubble.app.engine

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.TextStyle
import java.util.Locale

/**
 * A nudge at a time you picked, for one of the things you mean to do.
 *
 * Every task already has a "when" — "when I get out of bed", "when I put my shoes on" — and
 * that event cue is what eventually makes a habit run by itself (Wood & Neal, 2007). But a
 * plan only works if it is remembered at the moment it names, and remembering to do
 * something later is exactly what fails most often, most of all in the first weeks before
 * the cue has done its work. Reminders help people repeat things reliably (Fenerty et al.,
 * 2012); the cue is what makes the repetition stick (Stawarz, Cox & Blandford, 2015). So the
 * app has both, and a reminder never replaces the cue.
 *
 * It sits beside Done on every task because that is where the decision happens: "not now"
 * is the moment to say "then when", not a blank where the task quietly drops out of the day.
 *
 * Pure: the clock is passed in, so every label and choice here is tested.
 */
@Serializable
data class Reminder(
    /**
     * What it opens, in the same links the quests and the widget use: "care:BODY",
     * "tier:SOCIAL:4", "mission:7", or [Reminders.OUTSIDE] for whatever today's mission is.
     */
    val link: String,
    /** The thing, in the notification's title: "Wash up". */
    val title: String,
    /** Its smallest version, in the notification's text. */
    val text: String,
    /** Minutes after local midnight. */
    val minute: Int,
    /** Every day at [minute], or once on [day]. */
    val daily: Boolean = false,
    /** The epoch day of a one-off. Ignored when [daily]. */
    val day: Long = 0L
)

/** One of the quick times offered: "This evening", today, at 19:00. */
data class ReminderChoice(val label: String, val day: Long, val minute: Int)

object Reminders {

    /** A daily reminder for whichever mission outside is today's. */
    const val OUTSIDE = "outside"

    /** "Later" on the notification: half an hour, long enough to finish what you were doing. */
    const val SNOOZE_MINUTES = 30

    private const val DAY = 24 * 60

    /** A slot is only offered with at least this much of a gap before it. */
    private const val LEAD = 15

    /** The day's usual moments, in order. Named, because "this evening" is how people plan. */
    private val SLOTS = listOf(
        "Lunchtime" to 12 * 60 + 30,
        "This afternoon" to 15 * 60,
        "This evening" to 19 * 60,
        "Before bed" to 21 * 60 + 30
    )
    private const val MORNING = 8 * 60

    fun clock(minute: Int, h24: Boolean = true): String {
        val m = ((minute % DAY) + DAY) % DAY
        val h = m / 60
        val mm = m % 60
        if (h24) return "%02d:%02d".format(h, mm)
        val h12 = if (h % 12 == 0) 12 else h % 12
        return "%d:%02d %s".format(h12, mm, if (h < 12) "am" else "pm")
    }

    /** When it next goes off after [now], or null for a one-off already past. */
    fun next(r: Reminder, now: LocalDateTime): LocalDateTime? {
        val time = LocalTime.of(r.minute / 60 % 24, r.minute % 60)
        if (r.daily) {
            val today = now.toLocalDate().atTime(time)
            return if (today.isAfter(now)) today else today.plusDays(1)
        }
        val at = LocalDate.ofEpochDay(r.day).atTime(time)
        return if (at.isAfter(now)) at else null
    }

    /** "Today 19:00", "Tomorrow 08:00", "Every day 08:00", "Thu 19:00". */
    fun label(r: Reminder, today: Long, h24: Boolean = true): String {
        val time = clock(r.minute, h24)
        if (r.daily) return "Every day $time"
        return "${dayName(r.day, today)} $time"
    }

    /** As short as it can be and still be unambiguous, for a chip on a row. */
    fun short(r: Reminder, today: Long, h24: Boolean = true): String {
        val time = clock(r.minute, h24)
        return when {
            r.daily -> time
            r.day == today -> time
            else -> "${dayName(r.day, today)} $time"
        }
    }

    private fun dayName(day: Long, today: Long): String = when (day - today) {
        0L -> "Today"
        1L -> "Tomorrow"
        else -> LocalDate.ofEpochDay(day).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
    }

    /**
     * The quick times to offer at [now]: an hour from now, the next two named moments still
     * ahead today, and tomorrow morning. Four, because four can be read at a glance.
     */
    fun choices(now: LocalDateTime, h24: Boolean = true): List<ReminderChoice> {
        val today = now.toLocalDate().toEpochDay()
        val minute = now.hour * 60 + now.minute
        val out = mutableListOf<ReminderChoice>()

        // Rounded up to the next five minutes, so it reads like a time someone would pick.
        val inAnHour = ((minute + 60 + 4) / 5) * 5
        val (hourDay, hourMinute) = if (inAnHour >= DAY) today + 1 to inAnHour - DAY else today to inAnHour
        out += ReminderChoice("In an hour · ${clock(hourMinute, h24)}", hourDay, hourMinute)

        SLOTS.filter { (_, at) -> at >= minute + LEAD && (hourDay != today || at > hourMinute + LEAD) }
            .take(2)
            .forEach { (name, at) -> out += ReminderChoice("$name · ${clock(at, h24)}", today, at) }

        out += ReminderChoice("Tomorrow morning · ${clock(MORNING, h24)}", today + 1, MORNING)
        return out
    }

    /** Set, replacing any reminder already set for the same thing. */
    fun upsert(list: List<Reminder>, r: Reminder): List<Reminder> =
        list.filterNot { it.link == r.link } + r

    fun remove(list: List<Reminder>, link: String): List<Reminder> = list.filterNot { it.link == link }

    /** One-offs that have already gone off are dropped; daily ones stay until removed. */
    fun live(list: List<Reminder>, now: LocalDateTime): List<Reminder> =
        list.filter { it.daily || next(it, now) != null }

    /**
     * The thing was done. A one-off for it has done its job; a daily one is for tomorrow too.
     * A mission's link carries its level, which has just changed, so one-offs for that whole
     * ladder go.
     */
    fun afterDone(list: List<Reminder>, link: String): List<Reminder> {
        val ladder = link.takeIf { it.startsWith(Today.TIER) }?.substringBeforeLast(':')
        return list.filterNot { r ->
            val of = base(r.link)
            !r.daily && (of == link || (ladder != null && (of.startsWith("$ladder:") || of == OUTSIDE)))
        }
    }

    /** The reminder on one of today's four: its own, or the daily one for today's mission. */
    fun forQuest(list: List<Reminder>, q: Quest): Reminder? =
        list.firstOrNull { it.link == q.link && q.link.isNotEmpty() }
            ?: if (q.kind == QuestKind.OUTSIDE) list.firstOrNull { it.link == OUTSIDE } else null

    /** The reminder set for exactly [link]. */
    fun forLink(list: List<Reminder>, link: String): Reminder? = list.firstOrNull { it.link == link }

    /**
     * The thing a link is for. A snoozed daily reminder lives under "care:BODY#later", so that
     * tomorrow's still goes off at the usual time; it opens, and is checked against, the same
     * thing as the daily one.
     */
    fun base(link: String): String = link.substringBefore('#')
}
