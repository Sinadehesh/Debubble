package com.debubble.app.engine

/**
 * The first-day tour of the home screen: what each part is, and what to do with it.
 *
 * Shown once, the first time someone lands on Today after moving in, and again on request
 * from the You tab. It is a tour of the real screen rather than a deck of slides before it,
 * because of three findings from multimedia learning (Mayer, 2009):
 *
 *  - **Segmenting.** People learn more from short learner-paced parts than from one long
 *    explanation, so every step is one idea, a sentence or three, and moves only when they
 *    tap Next.
 *  - **Signalling.** Highlighting the essential thing and dimming the rest cuts the effort
 *    spent on working out where to look. Each step lights up one part of the actual screen.
 *  - **Spatial contiguity.** Words placed next to what they describe beat the same words on
 *    another page. The explanation sits beside the thing it is about.
 *
 * And it ends on doing, not on "got it": the last step opens the first of today's four.
 * Things done are remembered better than things read about (the enactment effect; Cohen,
 * 1981), and a first small success on day one is what behavioural activation is built on.
 *
 * Pure. The screen decides which spots are actually there — the mess is not drawn in a clean
 * room, the letter is not held once the mission is done — and [visible] adapts the steps.
 */
enum class TourSpot {
    /** The whole room picture. Always there, so it is also the fallback. */
    ROOM,
    /** You, in the room. */
    ME,
    /** The mess on the floor. */
    MESS,
    /** Your heart buddy. */
    BUDDY,
    /** The letter in your hand. */
    LETTER,
    /** "Today", the count and the four bars. */
    TODAY,
    ROW_BODY,
    ROW_ROOM,
    ROW_HEART,
    ROW_OUTSIDE,
    /** How far the room has opened, and how many things are in it. */
    WORLD,
    /** The tab bar. */
    TABS,
    /** No spotlight: the card sits in the middle of a dimmed screen. */
    NONE;

    companion object {
        fun row(d: Domain): TourSpot = when (d) {
            Domain.BODY -> ROW_BODY
            Domain.ROOM -> ROW_ROOM
            Domain.HEART -> ROW_HEART
        }
    }
}

/** What a step's main button does, besides moving on. */
enum class TourAction {
    /** Close the tour and open the next of today's four. */
    START_NEXT,
    /** Ask the launcher to pin the widget. */
    ADD_WIDGET
}

data class TourStep(
    val spot: TourSpot,
    /** A sprite from the atlas drawn beside the title, so the card shows what it means. */
    val icon: String?,
    val title: String,
    val body: String,
    /**
     * What to say instead when [spot] is not on screen, with the spotlight on the room.
     * Null means the step is dropped instead.
     */
    val absent: String? = null,
    val action: TourAction? = null,
    /** The label on the main button. */
    val button: String = "Next"
)

object Tour {

    /** The first time on a care screen: what to do there, in the words of its buttons. */
    val CARE_HOW = listOf(
        "Pick when. The same moment every day is what makes it stick.",
        "Pick how much you have got. Tiny counts in full.",
        "Put the phone down and do it for real.",
        "Come back, then press and hold the button at the bottom until it fills."
    )

    /** The first time on a mission: likewise. */
    val OUTSIDE_HOW = listOf(
        "Read what it is, and why it is this one.",
        "Tap Plan it and pick the moment you will go.",
        "Go and do it.",
        "Back here, press and hold the button until it fills. If it went wrong, tap Log " +
            "friction instead: that counts too."
    )

    /**
     * Every step, in order, for today's [quests].
     *
     * @param widget whether to include the home-screen widget step: not when one is already
     *   placed. [canPin] decides whether it offers a button or says where to find it.
     */
    fun steps(quests: List<Quest>, widget: Boolean, canPin: Boolean): List<TourStep> {
        val care = quests.filter { it.kind == QuestKind.CARE }
        // The care row the "how to" step points at: the first one still to do, so the
        // explanation is about something they could do next.
        val howTo = care.firstOrNull { !it.done } ?: care.firstOrNull()
        val next = Today.next(quests)

        return buildList {
            add(
                TourStep(
                    spot = TourSpot.ROOM,
                    icon = null,
                    title = "This is your room",
                    body = "It is a mirror of you. Look after yourself and it looks better; " +
                        "let things slide and it shows. Nothing you earn here is ever taken " +
                        "away, and a messy day is just a messy day."
                )
            )
            add(
                TourStep(
                    spot = TourSpot.ME,
                    icon = Scene.meFrame(0),
                    title = "This is you: your Body",
                    body = "How you look follows how your body is being looked after — " +
                        "washing, water, moving, food, sleep. Tap yourself to do today's " +
                        "body task."
                )
            )
            add(
                TourStep(
                    spot = TourSpot.MESS,
                    icon = Today.icon(Domain.ROOM),
                    title = "The mess is your Room",
                    body = "Dust and clutter here follow your real room. Tidy something real " +
                        "— five things put away is enough — and it clears here too. Tap the " +
                        "mess to start.",
                    absent = "When your real room needs a tidy, dust and clutter turn up on " +
                        "this floor. Tidy for real and it clears here too. Your Room task is " +
                        "in the list below."
                )
            )
            add(
                TourStep(
                    spot = TourSpot.BUDDY,
                    icon = Scene.buddy(1),
                    title = "This little heart is your Heart",
                    body = "Its face follows how you are doing inside. Tap it for something " +
                        "kind for your mind: three slow breaths, naming what you feel, or a " +
                        "message to someone."
                )
            )
            add(
                TourStep(
                    spot = TourSpot.LETTER,
                    icon = Today.ICON_OUT,
                    title = "The letter is today's mission",
                    body = "One small step outside your bubble, new every day: somewhere new " +
                        "(Access), something new (Activity) or someone new (Social). Tap the " +
                        "letter to read it.",
                    absent = "On days you have not been out yet, you hold a letter: that " +
                        "day's mission outside your bubble. Today's is already done."
                )
            )
            add(
                TourStep(
                    spot = TourSpot.TODAY,
                    icon = Today.ICON_TODO,
                    title = "Today: four things",
                    body = "The same four, as a list. Do them in any order. Each one says " +
                        "when to do it and its smallest version — and the smallest version " +
                        "counts in full."
                )
            )
            if (howTo != null) {
                add(
                    TourStep(
                        spot = TourSpot.row(howTo.domain ?: Domain.BODY),
                        icon = howTo.icon,
                        title = "How to do one",
                        body = "Tap it. Pick how much you have got — Tiny is fine. Do it for " +
                            "real, then press and hold the button until it fills. Your room " +
                            "changes right then."
                    )
                )
            }
            add(
                TourStep(
                    spot = TourSpot.ROW_OUTSIDE,
                    icon = Today.ICON_OUT,
                    title = "Going out works a bit differently",
                    body = "Read it, plan the moment you will do it, then go. Back home, hold " +
                        "the button to say you did it. If it went wrong — a no, an awkward " +
                        "silence, you turned back — tap Log friction. That still counts: it " +
                        "means you reached the edge."
                )
            )
            add(
                TourStep(
                    spot = TourSpot.WORLD,
                    icon = Today.ICON_DONE,
                    title = "Your room fills up",
                    body = "You start with nothing. Looking after yourself brings things for " +
                        "inside. Missions outside open the walls: first a window, then a " +
                        "door, then the street. Tap the room any time to arrange it."
                )
            )
            if (widget) {
                add(
                    TourStep(
                        spot = TourSpot.NONE,
                        icon = Scene.meFrame(0),
                        title = "Keep it on your home screen",
                        body = if (canPin) {
                            "The widget shows your room and today's four, so they are in " +
                                "front of you when the moment comes, not only when you " +
                                "remember to open the app."
                        } else {
                            "The widget shows your room and today's four. Long-press an " +
                                "empty spot on your home screen, choose Widgets, then " +
                                "DeBubble."
                        },
                        action = if (canPin) TourAction.ADD_WIDGET else null,
                        button = if (canPin) "Add the widget" else "Next"
                    )
                )
            }
            add(
                TourStep(
                    spot = TourSpot.TABS,
                    icon = null,
                    title = "Everything else",
                    body = "Learn explains why each part works. Notes is your journal. You " +
                        "has your goal, your look and your settings — and this tour, if you " +
                        "want it again."
                )
            )
            add(
                if (next != null) {
                    TourStep(
                        spot = spotOf(next),
                        icon = next.icon,
                        title = "Start with one now",
                        body = "${next.name}: ${next.first.trimEnd('.')}. " +
                            (if (next.minutes > 0) "About ${next.minutes} min. " else "") +
                            "Doing one now, before you close the app, is what makes " +
                            "tomorrow easier.",
                        action = TourAction.START_NEXT,
                        button = "Do it now"
                    )
                } else {
                    TourStep(
                        spot = TourSpot.TODAY,
                        icon = Today.ICON_DONE,
                        title = "That is today done",
                        body = "All four, ticked. Tomorrow's will be waiting here, and on " +
                            "your home screen.",
                        button = "Finish"
                    )
                }
            )
        }
    }

    /** The row a quest is drawn in. */
    fun spotOf(q: Quest): TourSpot =
        q.domain?.let { TourSpot.row(it) } ?: TourSpot.ROW_OUTSIDE

    /**
     * The steps as they can be shown, given which spots are on screen right now: a missing
     * spot falls back to the room with its [TourStep.absent] text, or the step is dropped.
     */
    fun visible(steps: List<TourStep>, present: Set<TourSpot>): List<TourStep> =
        steps.mapNotNull { s ->
            when {
                s.spot == TourSpot.NONE || s.spot in present -> s
                s.absent != null -> s.copy(spot = TourSpot.ROOM, body = s.absent)
                else -> null
            }
        }
}
