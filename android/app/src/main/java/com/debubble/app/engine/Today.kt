package com.debubble.app.engine

/**
 * Today, as one list: your body, your room, your heart, and one thing outside.
 *
 * It used to be two lists in two visual languages — three care cards, then a separate
 * "Step outside" section with its own ring, its own levels and its own colours — which made
 * the missions read as a second app bolted under the first. They are one day now, and the
 * room shows all four: you (body), the floor (room), the heart at your shoulder (heart), and
 * the letter in your hand (outside). The list and the room are two views of the same thing.
 *
 * Four, and the same four every day, on purpose:
 *
 *  - **A short fixed list lowers the cost of starting.** Behavioural activation schedules a
 *    small number of specific activities in advance, because "what should I do?" is itself
 *    the barrier on a low day. Nothing here asks the user to choose what today is for.
 *  - **Visible progress through a set pulls people to finish it.** Effort rises as the end
 *    of a goal gets nearer (the goal-gradient effect; Kivetz, Urminsky & Zheng, 2006), so
 *    the list counts "2 of 4" and the widget shows four boxes, not a score.
 *  - **One mission, not three.** Exposure works by staying with one feared situation long
 *    enough to learn something new about it, not by collecting several a day. The others
 *    stay available as extras for anyone who wants more, and the pillar rotates daily so no
 *    ladder is always the one that gets skipped.
 *
 * Every quest leads with its cue and its smallest version — the tiny act for care, the
 * two-minute opener for a mission — because the cue is what makes it happen without a fresh
 * decision (Gollwitzer & Sheeran, 2006) and the smallest version is what makes refusing it
 * absurd. Pure: no clock, no Android, no AppState.
 */
enum class QuestKind { CARE, OUTSIDE }

data class Quest(
    val kind: QuestKind,
    /** Set for CARE. */
    val domain: Domain? = null,
    /** Set for OUTSIDE. */
    val pillar: Pillar? = null,
    /** The atlas sprite beside it, in the app and on the widget. */
    val icon: String,
    /** "Body", "Room", "Heart", "Outside". */
    val name: String,
    /** The routine, or the mission. */
    val title: String,
    /** Completes "When ___": the moment it happens. */
    val cue: String,
    /** The smallest version: the tiny care act, or the mission's first two minutes. */
    val first: String,
    val minutes: Int,
    /** "Lived-in" for care; "Social · level 4" for a mission. */
    val status: String,
    val done: Boolean,
    /** What tapping it opens: "care:BODY", "tier:SOCIAL:4", or empty for nothing. */
    val link: String
) {
    /** The cue and the first move as one sentence, the way a plan is said. */
    val line: String
        get() = "When ${cue.trimEnd('.')}: ${first.replaceFirstChar { it.lowercase() }}"
}

object Today {

    const val ICON_OUT = "icon_out"
    const val ICON_GOAL = "icon_goal"
    const val ICON_DONE = "icon_done"
    const val ICON_TODO = "icon_todo"

    fun icon(d: Domain): String = "icon_${d.name.lowercase()}"

    /** Every icon the list and the widget draw. Checked against the atlas on load. */
    val ICONS: List<String> =
        Domain.order.map { icon(it) } + listOf(ICON_OUT, ICON_GOAL, ICON_DONE, ICON_TODO)

    /** Link prefixes, shared with the widget's deep links. */
    const val CARE = "care:"
    const val TIER = "tier:"

    /**
     * The ladder today's mission comes from.
     *
     * One that is already done today wins, so a finished mission stays on the list as
     * finished instead of being replaced by another one. Otherwise the pillars take turns
     * by day, so all three get the same share of days.
     */
    fun outsidePillar(dayIndex: Long, doneToday: Set<Pillar>): Pillar =
        Pillar.order.firstOrNull { it in doneToday }
            ?: Pillar.order[Math.floorMod(dayIndex - 1, Pillar.order.size.toLong()).toInt()]

    fun care(
        d: Domain,
        library: CareLibrary,
        s: CareState,
        level: Int,
        today: Long
    ): Quest {
        val r = library.chosen(d, s)
        return Quest(
            kind = QuestKind.CARE,
            domain = d,
            icon = icon(d),
            name = library.info(d).name,
            title = r.title,
            cue = s.anchor.ifBlank { r.anchors.first() },
            first = r.tiny,
            minutes = r.tinyMin,
            status = library.levelName(d, level),
            done = Care.doneToday(s, today),
            link = CARE + d.name
        )
    }

    /** Today's mission, still to do. A plan the user made for it supplies the cue. */
    fun outside(served: Served, plan: Intention?): Quest = Quest(
        kind = QuestKind.OUTSIDE,
        pillar = served.pillar,
        icon = ICON_OUT,
        name = "Outside",
        title = served.directive,
        cue = plan?.cue?.takeIf { it.isNotBlank() } ?: served.anchor,
        first = served.opener.ifBlank { served.directive },
        minutes = served.minutes,
        status = "${served.pillar.display} · level ${served.tier}",
        done = false,
        link = "$TIER${served.pillar.name}:${served.tier}"
    )

    /** Today's mission, done. Shows what was done, not the next rung up. */
    fun outsideDone(pillar: Pillar, title: String, tier: Int): Quest = Quest(
        kind = QuestKind.OUTSIDE,
        pillar = pillar,
        icon = ICON_OUT,
        name = "Outside",
        title = title,
        cue = "",
        first = "",
        minutes = 0,
        status = "${pillar.display} · level $tier",
        done = true,
        link = ""
    )

    fun doneCount(quests: List<Quest>): Int = quests.count { it.done }

    /** The one to do next: the first not done, in list order. */
    fun next(quests: List<Quest>): Quest? = quests.firstOrNull { !it.done }

    /** Whether you are holding a letter: a mission outside is still open today. */
    fun letter(quests: List<Quest>): Boolean =
        quests.any { it.kind == QuestKind.OUTSIDE && !it.done }

    /** One line over the list. Counts what is done, never what is missing. */
    fun headline(quests: List<Quest>): String {
        val done = doneCount(quests)
        return when {
            quests.isEmpty() -> ""
            done == quests.size -> "All ${quests.size} done. That is a whole day of looking after yourself."
            done == 0 -> "Start with whichever is nearest. Tiny counts."
            else -> "$done of ${quests.size} done."
        }
    }
}
