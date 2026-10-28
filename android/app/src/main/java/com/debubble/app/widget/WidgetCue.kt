package com.debubble.app.widget

import com.debubble.app.data.AppState
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Quest
import com.debubble.app.engine.QuestKind
import com.debubble.app.engine.Today

/**
 * What the home-screen widget should say, decided without touching Android.
 *
 * Deliberately separated from [DayWidget] so it can be tested. The widget is the one surface
 * nobody will ever see a stack trace from: it draws on someone else's launcher, in a process
 * that is not ours, and a wrong decision here shows up as stale or blank text rather than a
 * crash anybody reports. So the decision is pure and the rendering is dumb.
 *
 * The widget is the room and today's four, the same as the top of the app. Each row leads
 * with its cue — "When I wake up: splash water on my face" — because an if-then plan works
 * by being retrieved at the moment it names (Gollwitzer & Sheeran, 2006), and a plan filed
 * inside an app is only retrieved by people who already remembered. On the home screen it is
 * in front of them when the moment arrives.
 *
 * There is still no "mark it done" button. Finishing runs through the before-and-after and,
 * for a mission, a prediction to check — the parts that do the work. A one-tap tick on the
 * home screen would let someone skip both and collect the tick instead.
 */
data class WidgetCue(
    /** "Today", or a whole-day message when all four are done. */
    val title: String,
    /** "2 of 4". Empty when there is nothing to count. */
    val count: String,
    val rows: List<Row>,
    /** A live plan for something that is not one of the four, said as the plan. */
    val plan: String,
    /** Whether a prediction is waiting to be checked from a previous day. */
    val review: Boolean,
    /** Whether the room should be drawn at all, or only the message. */
    val ready: Boolean
) {
    data class Row(
        val icon: String,
        /** "Body · Lived-in", "Outside · Social, level 4". */
        val name: String,
        /** The cue and the smallest version, or "Done today". */
        val text: String,
        val done: Boolean,
        val tone: Tone,
        /** What tapping it opens. Empty means just open the app. */
        val link: String
    )

    enum class Tone { BODY, ROOM, HEART, ACCESS, ACTIVITY, SOCIAL }
}

object WidgetCues {

    /** Shown before moving in, or when state cannot be read. Never a spinner, never blank. */
    val unavailable = WidgetCue(
        title = "Open DeBubble to move in.",
        count = "",
        rows = emptyList(),
        plan = "",
        review = false,
        ready = false
    )

    fun of(state: AppState, today: Long, quests: List<Quest>): WidgetCue {
        if (!state.onboarded || !state.movedIn || quests.isEmpty()) return unavailable

        val done = Today.doneCount(quests)
        val outsideLink = quests.firstOrNull { it.kind == QuestKind.OUTSIDE }?.link.orEmpty()
        // A plan for one of the four is already that row's cue. Any other live plan — a
        // goal step, a focus challenge — gets its own line, because it is still a moment
        // the user chose and the widget is where it gets remembered.
        val other = state.liveIntentions(today).firstOrNull { it.challengeId != outsideLink }

        return WidgetCue(
            title = if (done == quests.size) "All done today" else "Today",
            count = "$done of ${quests.size}",
            rows = quests.map { q ->
                WidgetCue.Row(
                    icon = q.icon,
                    name = "${q.name} · ${q.status}",
                    text = if (q.done) "Done today" else q.line,
                    done = q.done,
                    tone = toneOf(q),
                    link = if (q.done && q.kind == QuestKind.OUTSIDE) "" else q.link
                )
            },
            plan = other?.sentence.orEmpty(),
            review = state.openForecasts.any { it.createdDay < today },
            ready = true
        )
    }

    private fun toneOf(q: Quest): WidgetCue.Tone = when {
        q.domain == Domain.BODY -> WidgetCue.Tone.BODY
        q.domain == Domain.ROOM -> WidgetCue.Tone.ROOM
        q.domain == Domain.HEART -> WidgetCue.Tone.HEART
        q.pillar == Pillar.ACTIVITY -> WidgetCue.Tone.ACTIVITY
        q.pillar == Pillar.SOCIAL -> WidgetCue.Tone.SOCIAL
        else -> WidgetCue.Tone.ACCESS
    }
}
