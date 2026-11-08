package com.debubble.app.engine

/**
 * The room the app is drawn inside, and how it opens up.
 *
 * The original plan was a separate 8-bit room on its own screen, decorated with things the
 * user had unlocked, and this comment used to argue against it: a room on its own screen is
 * a screen people stop visiting, and then it is decorating nothing. The objection was about
 * the *screen*, not the decorating, and it does not apply here — the room is the dashboard,
 * the first thing you see, so furnishing it is furnishing the main surface of the app.
 *
 * So progress does both now. It opens the walls, and it hands you things to put in the room.
 * The second half matters more than it looks: before it, clearing a rung incremented a
 * counter and nothing else, which is a product that asks people to do frightening things and
 * gives back a number.
 *
 * Seven stages, and they are deliberately not a reward schedule. Each one is reached by
 * clearing rungs, announced once, and then simply true from then on; nothing here can be
 * lost by missing a day and no stage can be taken back. The furniture follows the same rule —
 * an item you have earned stays earned. That rules out the one shape this app must never
 * take, a thing you keep opening to protect a number, while still giving the surface a
 * visible memory of how far someone has come.
 *
 * Everything here is pure. A stage is drawn by the room itself — its walls, its window, its
 * door — and not by tinting the app around it, so the app stays one colour on every stage
 * and nothing written on it can lose its contrast as the room opens.
 */
enum class Stage(
    /** Cleared rungs needed to reach this stage. */
    val threshold: Int,
    val label: String,
    /** One line, shown when it is reached and in the profile. Describes the room, not a score. */
    val note: String,
    /** How far the walls have gone, 0f..1f. The backdrop reads this, not the ordinal. */
    val openness: Float
) {
    SEALED(
        threshold = 0,
        label = "Sealed room",
        note = "Four walls and a ceiling. Everyone starts here, and it is not a verdict.",
        openness = 0f
    ),
    WINDOW(
        threshold = 6,
        label = "One window",
        note = "Something to look out of. You have been outside enough times to have a view.",
        openness = 0.16f
    ),
    DOOR(
        threshold = 20,
        label = "Door open",
        note = "The door stands open now. It has opened enough times to stop being a question.",
        openness = 0.33f
    ),
    STREET(
        threshold = 45,
        label = "The street",
        note = "No walls on one side. What used to be the edge of your world is the middle of it.",
        openness = 0.50f
    ),
    OPEN(
        threshold = 90,
        label = "Open ground",
        note = "Ground in every direction. Fifty rungs ago this view did not exist for you.",
        openness = 0.67f
    ),
    HORIZON(
        threshold = 150,
        label = "Horizon",
        note = "You can see the far edge from here. It is further out than the whole room was.",
        openness = 0.84f
    ),
    UNWALLED(
        threshold = 240,
        label = "No walls",
        note = "There is no room any more. There is just where you are and where you go next.",
        openness = 1f
    );

    companion object {
        val all: List<Stage> = entries.toList()
    }
}

object Habitat {

    /**
     * How far the room has opened, from cleared rungs alone.
     *
     * Cleared rungs and nothing else: not XP, which rep logging inflates, and not streaks,
     * which would make the surface someone's living room darken because they had a bad week.
     * [PillarState.cleared] never decreases even when a tier steps back, so the room cannot
     * close again — and that is the point. A stage that could be lost is a stage people
     * protect, and protecting a number is the behaviour this app exists to interrupt.
     */
    fun cleared(pillars: Map<Pillar, PillarState>): Int =
        Pillar.order.sumOf { pillars[it]?.cleared ?: 0 }

    fun stageFor(cleared: Int): Stage =
        Stage.all.last { cleared >= it.threshold }

    fun next(stage: Stage): Stage? = Stage.all.getOrNull(stage.ordinal + 1)

    /** Rungs still to clear before the room opens again, or null at the last stage. */
    fun toNext(cleared: Int): Int? =
        next(stageFor(cleared))?.let { (it.threshold - cleared).coerceAtLeast(0) }

    /** Progress through the current stage, 0f..1f, for the bar under the label. */
    fun progress(cleared: Int): Float {
        val stage = stageFor(cleared)
        val after = next(stage) ?: return 1f
        val span = (after.threshold - stage.threshold).toFloat()
        if (span <= 0f) return 1f
        return ((cleared - stage.threshold) / span).coerceIn(0f, 1f)
    }

    /**
     * One line for the dashboard, under the room's name.
     *
     * Says the number of rungs rather than a percentage, because "fourteen more" is something
     * a person can picture doing and "62%" is something they can only watch.
     */
    fun caption(cleared: Int): String {
        val remaining = toNext(cleared) ?: return "The room is fully open. Nothing left to unseal."
        val stage = next(stageFor(cleared)) ?: return ""
        return when (remaining) {
            0 -> "${stage.label} is next."
            1 -> "One more mission outside and the room opens again."
            else -> "$remaining more missions outside to ${stage.label.lowercase()}."
        }
    }
}
