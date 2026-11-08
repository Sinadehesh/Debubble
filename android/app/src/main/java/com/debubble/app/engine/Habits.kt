package com.debubble.app.engine

/**
 * Your habits, in detail: what you did, when, and what it seems to do for you.
 *
 * Self-monitoring is one of the best-supported ways to change behaviour, and it works best
 * when the record is detailed and visible (Harkin et al., 2016, Psychological Bulletin, 138
 * studies). So every habit gets the same honest page: how many days, this week, the last
 * twelve weeks as squares, the time of day it usually happens, and, where it was asked, how
 * you felt afterwards.
 *
 * Two things are deliberately missing. There is no streak, because a single missed day did
 * not set habits back in Lally et al. (2010) and a broken streak teaches people to stop. And
 * there is no score: a day is done or it is not, and every done day stays done for good.
 *
 * Pure: same inputs, same outputs, no clock and no Android.
 */
enum class HabitKind(val title: String, val icon: String, val blurb: String) {
    BODY("Body", "icon_body", "Your body routine"),
    ROOM("Room", "icon_room", "Your room routine"),
    HEART("Heart", "icon_heart", "Your heart routine"),
    OUTSIDE("Outside", "icon_out", "Steps outside your bubble, done or tried"),
    TRAINING("Training", "dumbbells", "Any set of anything"),
    READING("Reading", "books", "Any page of any book");

    val domain: Domain?
        get() = when (this) {
            BODY -> Domain.BODY
            ROOM -> Domain.ROOM
            HEART -> Domain.HEART
            else -> null
        }

    companion object {
        fun of(d: Domain): HabitKind = when (d) {
            Domain.BODY -> BODY
            Domain.ROOM -> ROOM
            Domain.HEART -> HEART
        }

        fun from(name: String?): HabitKind? = entries.firstOrNull { it.name == name }
    }
}

/** One time a habit was done: the day, and the minute of that day (-1 when unknown). */
data class Mark(val day: Long, val minute: Int = -1)

data class HabitStats(
    val kind: HabitKind,
    /** Different days it was done, ever. Only ever goes up. */
    val total: Int,
    /** This week, Monday to Sunday. */
    val week: List<Boolean>,
    val last30: Int,
    /** The most days in any one Monday-to-Sunday week. */
    val bestWeek: Int,
    /** [Habits.GRID_WEEKS] weeks of days, oldest first, each week Monday first. */
    val grid: List<Boolean>,
    /** The Monday the grid starts on. */
    val gridStart: Long,
    /** The typical minute of the day it happens, when there are enough timed marks. */
    val usualMinute: Int?,
    val first: Long?,
    val last: Long?,
    /** Days done on each weekday, Monday first. */
    val byWeekday: List<Int>
) {
    val weekCount: Int get() = week.count { it }
}

/** Something your habits show together. */
data class Insight(val title: String, val body: String, val icon: String)

object Habits {

    const val GRID_WEEKS = 12

    /** Insights look at the last eight weeks: recent enough to be you now. */
    const val INSIGHT_DAYS = 56

    /** Below this many marks, a time of day is a coincidence rather than a habit. */
    const val TIMED_MARKS = 3

    private val WEEKDAYS = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

    fun weekday(day: Long): Int = Math.floorMod(day + 3, 7L).toInt()

    fun weekdayName(index: Int): String = WEEKDAYS[index.coerceIn(0, 6)]

    fun stats(kind: HabitKind, marks: List<Mark>, today: Long): HabitStats {
        val days = marks.map { it.day }.filter { it <= today }.toSet()
        val monday = Training.weekStart(today)
        val week = (0..6).map { (monday + it) in days }
        val gridStart = monday - (GRID_WEEKS - 1) * 7L
        val grid = (0 until GRID_WEEKS * 7).map { (gridStart + it) in days }
        val bestWeek = days.groupBy { Training.weekStart(it) }.values.maxOfOrNull { it.size } ?: 0
        val timed = marks.filter { it.minute >= 0 }.map { it.minute }.sorted()
        val usual = if (timed.size >= TIMED_MARKS) timed[timed.size / 2] else null
        val byWeekday = (0..6).map { w -> days.count { weekday(it) == w } }
        return HabitStats(
            kind = kind,
            total = days.size,
            week = week,
            last30 = days.count { it in (today - 29)..today },
            bestWeek = bestWeek,
            grid = grid,
            gridStart = gridStart,
            usualMinute = usual,
            first = days.minOrNull(),
            last = days.maxOrNull(),
            byWeekday = byWeekday
        )
    }

    /** "morning", "evening": the part of the day a minute falls in. */
    fun partOfDay(minute: Int): String = when (minute / 60) {
        in 5..7 -> "early morning"
        in 8..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..20 -> "evening"
        else -> "night"
    }

    /** "8:15 am" or "20:15". */
    fun clock(minute: Int, h24: Boolean): String {
        val h = (minute / 60) % 24
        val m = minute % 60
        return if (h24) "%02d:%02d".format(h, m)
        else "${if (h % 12 == 0) 12 else h % 12}:%02d ${if (h < 12) "am" else "pm"}".format(m)
    }

    /** How the days before it felt afterwards, for a care habit: better, same, worse. */
    data class Feelings(val better: Int, val same: Int, val worse: Int) {
        val total: Int get() = better + same + worse
    }

    /**
     * What the habits show together, newest evidence only. Each insight appears only once
     * there is enough to say it honestly, and none of them is a judgement.
     */
    fun insights(
        marks: Map<HabitKind, List<Mark>>,
        feelings: Map<HabitKind, Feelings>,
        sets: List<WorkoutSet>,
        readingPace: Float,
        today: Long,
        h24: Boolean
    ): List<Insight> {
        val from = today - INSIGHT_DAYS + 1
        val recent = marks.mapValues { (_, list) -> list.map { it.day }.filter { it in from..today }.toSet() }
        val out = mutableListOf<Insight>()

        // Your best day of the week, across everything.
        val byWeekday = IntArray(7)
        recent.values.forEach { days -> days.forEach { byWeekday[weekday(it)]++ } }
        val total = byWeekday.sum()
        if (total >= 10) {
            val best = byWeekday.indices.maxByOrNull { byWeekday[it] } ?: 0
            out += Insight(
                "${weekdayName(best)}s are your best day",
                "${byWeekday[best]} habits done on ${weekdayName(best)}s in the last eight weeks. " +
                    "If a day is going to be hard, it helps to know which ones usually are not.",
                "icon_spark"
            )
        }

        // Two habits that go together: doing one is a good cue for the other.
        val kinds = recent.filterValues { it.size >= 5 }.keys.toList()
        var pair: Triple<HabitKind, HabitKind, Float>? = null
        for (i in kinds.indices) for (j in i + 1 until kinds.size) {
            val a = recent.getValue(kinds[i])
            val b = recent.getValue(kinds[j])
            val both = a.intersect(b).size
            val either = a.union(b).size
            val score = if (either == 0) 0f else both / either.toFloat()
            if (score >= 0.4f && (pair == null || score > pair.third)) pair = Triple(kinds[i], kinds[j], score)
        }
        val together = pair
        if (together != null) {
            val (a, b, _) = together
            val da = recent.getValue(a)
            val db = recent.getValue(b)
            val both = da.intersect(db).size
            val either = da.union(db).size
            out += Insight(
                "${a.title} and ${b.title} go together",
                "On $both of the $either days you did either, you did both. Doing one straight after " +
                    "the other turns the first into the cue for the second.",
                b.icon
            )
        }

        // What seems to help your mood, in your own answers.
        val helps = feelings.entries.filter { it.value.total >= 3 }
            .maxByOrNull { it.value.better / it.value.total.toFloat() }
        if (helps != null && helps.value.better * 2 >= helps.value.total) {
            val kind = helps.key
            val f = helps.value
            out += Insight(
                "${kind.title} helps",
                "After your ${kind.title.lowercase()} routine you felt better ${f.better} " +
                    "${if (f.better == 1) "time" else "times"} out of ${f.total}. That is your own evidence, not a study's.",
                kind.icon
            )
        }

        // When a habit happens: the most-timed one.
        val timed = marks.map { (k, list) -> stats(k, list, today) }
            .filter { it.usualMinute != null && it.total >= 5 }
            .maxByOrNull { it.total }
        if (timed != null) {
            val minute = timed.usualMinute ?: 0
            out += Insight(
                "${timed.kind.title} is a ${partOfDay(minute)} habit",
                "Usually around ${clock(minute, h24)}. Doing it at the same moment each day is " +
                    "what makes it automatic.",
                timed.kind.icon
            )
        }

        // Getting stronger: the biggest gain at any exercise from its first session.
        val gains = sets.groupBy { it.exercise }.mapNotNull { (id, list) ->
            val ex = Moves.byId(id) ?: return@mapNotNull null
            val firstDay = list.minOf { it.epochDay }
            if (list.none { it.epochDay > firstDay }) return@mapNotNull null
            val start = list.filter { it.epochDay == firstDay }.maxOf { if (ex.weighted) it.kg.toInt() else it.amount }
            val best = list.maxOf { if (ex.weighted) it.kg.toInt() else it.amount }
            if (best > start) Triple(ex, start, best) else null
        }
        val gain = gains.maxByOrNull { (_, start, best) -> (best - start) / start.coerceAtLeast(1).toFloat() }
        if (gain != null) {
            val (ex, start, best) = gain
            val unit = if (ex.weighted) " kg" else when (ex.measure) {
                Measure.REPS -> ""
                Measure.SECONDS -> " s"
                Measure.MINUTES -> " min"
            }
            out += Insight(
                "${ex.name}: $start$unit to $best$unit",
                "From your first session to your best. Strength comes back fast at the start, " +
                    "so this is the best time to keep going.",
                ex.icon
            )
        }

        if (readingPace >= 1f) {
            val pages = readingPace.toInt()
            out += Insight(
                "About $pages ${if (pages == 1) "page" else "pages"} a day",
                "Your reading pace over the last two weeks. At that pace, a 300-page book takes " +
                    "${(300 / readingPace).toInt()} days.",
                "books"
            )
        }
        return out
    }
}
