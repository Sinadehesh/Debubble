package com.debubble.app.engine

import kotlinx.serialization.Serializable
import kotlin.math.exp
import kotlin.math.ln

/**
 * Looking after yourself, in three places: your body, your room, and your heart.
 *
 * The room on the dashboard is a mirror of these. When the real ones are neglected the
 * pixel one gets messy — a sock on the floor, a scruffy bubble, a cloud overhead — and
 * doing the real task is the only thing that cleans it. That makes the room a
 * **self-monitoring** device, which is one of the behaviour-change techniques with the best
 * evidence behind it: across 138 studies, monitoring progress toward a goal reliably
 * promoted reaching it, and more so when the record was physical and visible (Harkin et
 * al., 2016, Psychological Bulletin). A room you see every time you open the app is about as
 * physical and visible as an app can make a record.
 *
 * Four design rules follow from the research, and each is load-bearing:
 *
 *  1. **Same act, same cue, every day.** Automaticity comes from context-dependent
 *     repetition (Lally et al., 2010; Wood & Rünger, 2016), so each domain has one chosen
 *     [Routine] at one cue, and what varies is its [Size] — never the act itself.
 *
 *  2. **Tiny counts in full.** A two-minute version earns the day's rep exactly as the deep
 *     one does, because the rep is what is being built. Size changes how clean the room
 *     gets, not whether the habit grew. Repetition beats intensity for habits, and an app
 *     that paid more for big sessions would teach people to wait until they had energy for
 *     one, which is the failure mode of every New Year's resolution.
 *
 *  3. **Mess is never punishment.** It is capped at [MAX_DIRT], it never takes away anything
 *     earned (items and room stages are permanent), and any one task — even a tiny one —
 *     visibly improves it. Shame drives avoidance (Tangney & Dearing, 2002), and procrastination
 *     is largely short-term mood repair (Sirois & Pychyl, 2013): a mirror that made people
 *     feel bad would be closed, and a closed mirror monitors nothing.
 *
 *  4. **Missing a day is not a reset.** Lally found a single missed repetition did not
 *     materially affect habit formation, so nothing here is a streak that breaks. The room
 *     just gets a bit messier, which is what rooms do.
 *
 * Everything in this file is pure: same inputs, same outputs, no clock, no Android.
 */
enum class Domain(
    /** Days without care before this domain slips one level. Mirrored in routines.json. */
    val daysPerLevel: Int,
    /** The family of things it earns for the room. */
    val family: Family
) {
    /** A body needs looking after daily: skip one and it shows. */
    BODY(daysPerLevel = 1, family = Family.BODY),
    /** Rooms and hearts accumulate more slowly. */
    ROOM(daysPerLevel = 2, family = Family.ROOM),
    HEART(daysPerLevel = 2, family = Family.HEART);

    companion object {
        /** Fixed order everywhere, so the three cards are always in the same place. */
        val order = listOf(BODY, ROOM, HEART)

        fun from(name: String?): Domain? = entries.firstOrNull { it.name == name }
    }
}

/**
 * How much of the routine, today. Chosen by the user, before starting.
 *
 * Asking "how much have you got?" is the graded task assignment from behavioural activation
 * turned into a single tap: on a bad day the step gets smaller, it does not get skipped.
 */
enum class Size(val label: String) {
    TINY("Tiny"),
    STANDARD("Standard"),
    DEEP("Deep")
}

/** How the user felt afterwards, compared with before. Optional, one tap. */
enum class Feel(val label: String) {
    WORSE("Worse"),
    SAME("About the same"),
    BETTER("Better")
}

/** Display text for one domain. Authored in routines.json. */
@Serializable
data class DomainInfo(
    val id: String,
    val name: String,
    val blurb: String,
    /** One short name per level, 0 = clean. */
    val levels: List<String>,
    /** The honest check-in answer for each level, in the user's voice. */
    val checkin: List<String>
)

/**
 * One care routine: the same act at three sizes, with the reason it works.
 *
 * Authored in android/tools/gen_care.py, which refuses to write a routine whose tiny size
 * is over two minutes or whose suggested cues fail [Cues.isSpecific].
 */
@Serializable
data class Routine(
    val id: String,
    val domain: String,
    val title: String,
    val tiny: String,
    val standard: String,
    val deep: String,
    val tinyMin: Int,
    val standardMin: Int,
    val deepMin: Int,
    /** The mechanism, in plain words. */
    val why: String,
    /** Where that comes from. */
    val source: String,
    /** One change to the surroundings that makes the act easier. */
    val ease: String,
    /** Suggested cues for the if-then plan, phrased to follow "When ". */
    val anchors: List<String>,
    /** Said or done the instant it is finished. */
    val celebrate: String,
    /** The routine a domain starts with, before the user picks their own. */
    val starter: Boolean = false
) {
    fun text(size: Size): String = when (size) {
        Size.TINY -> tiny
        Size.STANDARD -> standard
        Size.DEEP -> deep
    }

    fun minutes(size: Size): Int = when (size) {
        Size.TINY -> tinyMin
        Size.STANDARD -> standardMin
        Size.DEEP -> deepMin
    }
}

/** The numbers the engine runs on, as written into the asset for the web preview. */
@Serializable
data class CareRules(
    val daysPerLevel: Map<String, Int>,
    val maxDirt: Int,
    val rewardAt: List<Int>,
    val automaticityDays: Int
)

/** Everything in assets/care/routines.json. */
@Serializable
data class CareLibrary(
    val rules: CareRules,
    val domains: List<DomainInfo>,
    val routines: List<Routine>
) {
    fun info(d: Domain): DomainInfo = domains.first { it.id == d.name }

    fun routines(d: Domain): List<Routine> = routines.filter { it.domain == d.name }

    fun starter(d: Domain): Routine = routines(d).first { it.starter }

    fun byId(id: String): Routine? = routines.firstOrNull { it.id == id }

    /**
     * The routine this person keeps in this domain. Falls back to the starter for a blank
     * choice, and for an id a later build removed — a stale id must not leave a domain with
     * no task at all.
     */
    fun chosen(d: Domain, s: CareState): Routine =
        byId(s.routine)?.takeIf { it.domain == d.name } ?: starter(d)

    fun levelName(d: Domain, level: Int): String =
        info(d).levels[level.coerceIn(0, Care.MAX_DIRT)]
}

/** Where one domain stands. Stored per domain in AppState. */
@Serializable
data class CareState(
    /** The level as last set, 0..[Care.MAX_DIRT]. Read through [Care.dirt], never directly. */
    val dirt: Int = Care.START_DIRT,
    /** The day [dirt] was set, by a check-in or a completion. Slipping counts from here. */
    val setDay: Long = 0L,
    /**
     * Days this routine was done, ever. At most one per day, so it measures repetition —
     * the thing habits are made of — and cannot be farmed by tapping. Never decreases.
     */
    val reps: Int = 0,
    val lastDoneDay: Long = 0L,
    /** The chosen routine's id. Blank means the domain's starter. */
    val routine: String = "",
    /**
     * [reps] at the moment the current routine was chosen. Rewards follow the domain, so a
     * switch keeps everything earned; automaticity follows the act, so it starts again.
     */
    val routineFrom: Int = 0,
    /** The user's cue, completing "When ___". Blank means the routine's first suggestion. */
    val anchor: String = "",
    /* How it felt afterwards. The user's own evidence that doing it helps. */
    val better: Int = 0,
    val same: Int = 0,
    val worse: Int = 0
)

object Care {

    const val MAX_DIRT = 3

    /** Where a domain sits before the user has said anything: lived-in, not a judgement. */
    const val START_DIRT = 1

    /**
     * Care-days at which something new arrives for the room. Fast at first — the first one
     * is on day one, because an early win is what gets anyone to a second day — then
     * spreading out. The last is 66, the median days to automaticity in Lally et al.
     *
     * The timing is fixed and shown on screen. Which item arrives is the surprise. A fixed
     * schedule with a variable reward keeps the novelty that makes a reward land
     * (Bunzeck & Düzel, 2006) without the variable-ratio timing that slot machines use.
     */
    val REWARD_AT = listOf(1, 3, 6, 10, 15, 21, 30, 45, 66)

    /** Lally et al. (2010): the median participant reached 95% of their plateau by day 66. */
    const val AUTOMATICITY_DAYS = 66

    /** Repetitions before the suggested size stops being tiny. One week of the smallest act. */
    const val TINY_FIRST_REPS = 7

    /** A gap this long and the suggestion drops back to tiny, so returning is easy. */
    const val COMING_BACK_DAYS = 3L

    /** Feelings recorded before the app says anything about them. Two is an anecdote. */
    const val EVIDENCE_AFTER = 3

    /* ---------------------------------------------------------------- the mirror */

    /**
     * How clean this domain is today, 0 (clean) to [MAX_DIRT].
     *
     * It slips one level every [Domain.daysPerLevel] days after it was last set. Before the
     * first check-in (no [CareState.setDay]) it holds still, so an install that has not
     * moved in yet does not open on a room that has gone to seed on its own.
     */
    fun dirt(d: Domain, s: CareState, today: Long): Int {
        val base = s.dirt.coerceIn(0, MAX_DIRT)
        if (s.setDay <= 0L) return base
        val days = (today - s.setDay).coerceAtLeast(0L)
        val slipped = minOf(days / d.daysPerLevel, MAX_DIRT.toLong()).toInt()
        return (base + slipped).coerceAtMost(MAX_DIRT)
    }

    fun doneToday(s: CareState, today: Long): Boolean = s.lastDoneDay == today

    /**
     * The routine was done. Tiny lifts the domain one level; standard and deep clean it.
     *
     * The rep counts once per day however many times it is done, so a second session
     * still cleans the room but does not inflate the habit count or the rewards.
     */
    fun afterCare(d: Domain, s: CareState, size: Size, today: Long): CareState {
        val now = dirt(d, s, today)
        val cleaned = if (size == Size.TINY) (now - 1).coerceAtLeast(0) else 0
        val counts = s.lastDoneDay != today
        return s.copy(
            dirt = cleaned,
            setDay = today,
            reps = s.reps + if (counts) 1 else 0,
            lastDoneDay = today
        )
    }

    /** An honest answer to "how is it, really?" Sets the level directly, no penalty. */
    fun checkIn(s: CareState, level: Int, today: Long): CareState =
        s.copy(dirt = level.coerceIn(0, MAX_DIRT), setDay = today)

    fun afterFeeling(s: CareState, feel: Feel): CareState = when (feel) {
        Feel.BETTER -> s.copy(better = s.better + 1)
        Feel.SAME -> s.copy(same = s.same + 1)
        Feel.WORSE -> s.copy(worse = s.worse + 1)
    }

    /* ---------------------------------------------------------------- sizing */

    /**
     * Which size to pre-select. The user can always change it; this only decides what a
     * tired thumb lands on.
     *
     * Tiny for the first week, because the first job is wiring the cue to the act and
     * Fogg's point is that the act has to be small enough to do on the worst day. Tiny
     * again after a gap, because coming back is the moment most likely to fail.
     */
    fun suggestedSize(s: CareState, today: Long): Size = when {
        s.reps < TINY_FIRST_REPS -> Size.TINY
        s.lastDoneDay > 0L && today - s.lastDoneDay >= COMING_BACK_DAYS -> Size.TINY
        else -> Size.STANDARD
    }

    /** One line under the size picker explaining the suggestion, or null when it needs none. */
    fun sizeNote(s: CareState, today: Long): String? = when {
        s.reps < TINY_FIRST_REPS ->
            "Tiny is the right size this week. It counts in full: the habit is built by " +
                "doing it at the same moment each day, not by how much you do."
        s.lastDoneDay > 0L && today - s.lastDoneDay >= COMING_BACK_DAYS ->
            "Welcome back. Start tiny. A gap does not undo a habit, and the smallest " +
                "version is the one that gets you going again."
        else -> null
    }

    /* ---------------------------------------------------------------- rewards */

    /** How many items a count of care-days (or cleared rungs) has earned in total. */
    fun owed(count: Int): Int = REWARD_AT.count { it <= count }

    /** How many more until the next one, or null when every reward is earned. */
    fun nextRewardIn(count: Int): Int? = REWARD_AT.firstOrNull { it > count }?.let { it - count }

    /* ---------------------------------------------------------------- habit strength */

    /** Days done with the current routine, which is what its automaticity is built from. */
    fun daysWithRoutine(s: CareState): Int = (s.reps - s.routineFrom).coerceAtLeast(0)

    /**
     * An estimate of how automatic the routine is, 0f..1f, from repetitions alone.
     *
     * Lally's participants followed an asymptotic curve, with the median reaching 95% of its
     * plateau at day 66. This is that curve for the median person: A(n) = 1 − e^(−kn), with
     * k chosen so A(66) = 0.95. The range in the study ran from 18 to 254 days, so the UI
     * labels this an estimate and says why.
     */
    fun automaticity(reps: Int): Float {
        if (reps <= 0) return 0f
        val k = ln(20.0) / AUTOMATICITY_DAYS
        return (1.0 - exp(-k * reps)).toFloat().coerceIn(0f, 1f)
    }

    /* ---------------------------------------------------------------- evidence */

    /**
     * The user's own evidence, once there is enough of it.
     *
     * Behavioural activation's central lesson is that action comes before mood, and the
     * most persuasive version of that is your own record rather than a study. Silent until
     * [EVIDENCE_AFTER] answers, because two is an anecdote.
     */
    fun evidence(d: DomainInfo, s: CareState): String? {
        val total = s.better + s.same + s.worse
        if (total < EVIDENCE_AFTER) return null
        return when {
            s.better * 2 >= total ->
                "You felt better after ${s.better} of $total ${d.name.lowercase()} tasks."
            s.worse > s.better ->
                "These have not been lifting you much. Try a different routine for a week " +
                    "— the one that works is the one to keep."
            else ->
                "Mostly neutral so far, ${s.better} better out of $total. Neutral still " +
                    "cleans the room."
        }
    }

    /**
     * What to say when someone opens the app after a gap, or null when there was none.
     *
     * Self-compassion after a lapse increases the motivation to try again rather than
     * reducing it (Breines & Chen, 2012), so this never mentions what was missed.
     */
    fun welcomeBack(daysAway: Long): String? = when {
        daysAway < COMING_BACK_DAYS -> null
        daysAway < 14 ->
            "Welcome back. Things got a bit messy, which is what they do. Pick the smallest " +
                "thing in one corner and start there."
        else ->
            "Welcome back. It has been a while, and that is fine. Nothing you earned has " +
                "gone anywhere. One tiny thing today is plenty."
    }
}
