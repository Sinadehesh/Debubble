package com.debubble.app.widget

import com.debubble.app.data.AppState
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar

/**
 * What the home-screen widget should say, decided without touching Android.
 *
 * Deliberately separated from [DayWidget] so it can be tested. The widget is the one surface
 * nobody will ever see a stack trace from: it draws on someone else's launcher, in a process
 * that is not ours, and a wrong decision here shows up as stale or blank text rather than a
 * crash anybody reports. So the decision is pure and the rendering is dumb.
 */
data class WidgetCue(
    /** Small accented line at the top. */
    val kicker: String,
    val tone: Tone,
    /** The largest text. Always a moment, never a score. */
    val cue: String,
    val body: String,
    /** Top right. Empty rather than "0 days". */
    val badge: String,
    /** Whether a prediction is waiting to be checked from a previous day. */
    val review: Boolean,
    /** What tapping it opens. Empty means just open the app. */
    val link: String
) {
    enum class Tone { ACCESS, ACTIVITY, SOCIAL, PLAN, DONE, NEUTRAL }
}

object WidgetCues {

    /** Shown when state cannot be read at all. Never a spinner, never blank. */
    val unavailable = WidgetCue(
        kicker = "DeBubble",
        tone = WidgetCue.Tone.NEUTRAL,
        cue = "Open the app to pick up today.",
        body = "",
        badge = "",
        review = false,
        link = ""
    )

    /**
     * The order of these branches is the whole design.
     *
     * A live if-then plan comes first, because that is the thing the widget exists for: the
     * plan works by being retrieved when the cue arrives, and a plan filed inside an app is
     * retrieved by people who already remembered. Everything else is a fallback.
     */
    fun of(state: AppState, today: Long, curriculum: Curriculum): WidgetCue {
        val rolled = state.rolledTo(today)

        // An unchecked prediction from a previous day. Surfaced as a second action rather than
        // taking over the widget, because it is a loose end and not today's instruction.
        val stale = rolled.openForecasts.any { it.createdDay < today }

        val plan = rolled.liveIntentions(today).firstOrNull()
        if (plan != null) {
            return WidgetCue(
                kicker = "Your plan",
                tone = WidgetCue.Tone.PLAN,
                cue = plan.sentence,
                body = plan.challengeTitle,
                badge = badge(rolled),
                review = stale,
                link = plan.challengeId
            )
        }

        val next = Pillar.order.firstOrNull { !rolled.isDoneToday(it) }
            ?: return WidgetCue(
                kicker = "Done for today",
                tone = WidgetCue.Tone.DONE,
                cue = "All three, today.",
                body = "Nothing else is asked of you. The next one is tomorrow.",
                badge = badge(rolled),
                review = stale,
                link = ""
            )

        val served = Engine.serve(
            pillar = next,
            state = rolled.state(next),
            curriculum = curriculum,
            baseline = rolled.baseline
        )
        return WidgetCue(
            kicker = "${next.display} · level ${served.tier}",
            tone = when (next) {
                Pillar.ACCESS -> WidgetCue.Tone.ACCESS
                Pillar.ACTIVITY -> WidgetCue.Tone.ACTIVITY
                Pillar.SOCIAL -> WidgetCue.Tone.SOCIAL
            },
            // The anchor, as a sentence. This is the part that decides whether it happens, so
            // it gets the large type and the directive goes underneath.
            cue = "When ${served.anchor}.",
            body = served.opener.ifBlank { served.directive },
            badge = badge(rolled),
            review = stale,
            link = "tier:${next.name}:${served.tier}"
        )
    }

    /** A one-day streak is not a streak, so it says nothing rather than "1 day". */
    private fun badge(state: AppState): String =
        if (state.streak >= 2) "${state.streak} days" else ""
}
