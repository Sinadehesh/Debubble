package com.debubble.app.engine

/**
 * The room the app is drawn inside, and how it opens up.
 *
 * The original plan was a separate 8-bit room on its own screen, decorated with things the
 * user had unlocked. That has a problem the literature on the endowment effect does not fix:
 * a room on its own screen is a screen people stop visiting, and then it is decorating
 * nothing. The version that earns its keep is the app itself being the room — so progress
 * does not buy furniture, it opens the walls.
 *
 * Seven stages, and they are deliberately not a reward schedule. Each one is reached by
 * clearing rungs, announced once, and then simply true from then on; there is nothing to
 * collect, nothing to lose by missing a day, and no stage that can be taken back. That rules
 * out the one shape this app must never take — a thing you keep opening to protect a number —
 * while still giving the surface a visible memory of how far someone has come.
 *
 * Everything here is pure. The colours are verified against the WCAG floors by
 * android/tools/check_habitat.py, because a theme that drifts is a theme that eventually
 * makes a sentence unreadable on someone's phone and nobody finds out.
 */
enum class Stage(
    /** Cleared rungs needed to reach this stage. */
    val threshold: Int,
    val label: String,
    /** One line, shown when it is reached and in the profile. Describes the room, not a score. */
    val note: String,
    /*
     * The ground, as ARGB. These are not free choices.
     *
     * In a dark theme with these inks the usable brightness range is narrow: Muted text needs
     * 4.5:1 and a panel's border needs 3.0:1 against whatever is behind it, and both fall as
     * the ground lifts. The solver in android/tools/check_habitat.py found the ceiling, and
     * it is reached at the last stage with about 0.2 of margin left. So the ground barely
     * changes across seven stages, and that is correct — the room opening is carried by the
     * geometry the backdrop draws, not by brightness, because geometry is not rationed.
     */
    val ground: Long,
    val horizon: Long,
    /**
     * Walls, a frame, a skyline. Brighter than the ground, because it is only ever drawn in
     * the top band where the ring sits and no text goes — which is the whole reason it is
     * allowed to be bright enough to see.
     */
    val structure: Long,
    /** How far the walls have gone, 0f..1f. The backdrop reads this, not the ordinal. */
    val openness: Float
) {
    SEALED(
        threshold = 0,
        label = "Sealed room",
        note = "Four walls and a ceiling. Everyone starts here, and it is not a verdict.",
        ground = 0xFF0E1116,
        horizon = 0xFF0E1116,
        structure = 0xFF1B2130,
        openness = 0f
    ),
    WINDOW(
        threshold = 6,
        label = "One window",
        note = "Something to look out of. You have been outside enough times to have a view.",
        ground = 0xFF0F1218,
        horizon = 0xFF10151C,
        structure = 0xFF222C3F,
        openness = 0.16f
    ),
    DOOR(
        threshold = 20,
        label = "Door open",
        note = "The door stands open now. It has opened enough times to stop being a question.",
        ground = 0xFF10131A,
        horizon = 0xFF131922,
        structure = 0xFF29364F,
        openness = 0.33f
    ),
    STREET(
        threshold = 45,
        label = "The street",
        note = "No walls on one side. What used to be the edge of your world is the middle of it.",
        ground = 0xFF10141C,
        horizon = 0xFF161C28,
        structure = 0xFF30405E,
        openness = 0.50f
    ),
    OPEN(
        threshold = 90,
        label = "Open ground",
        note = "Ground in every direction. Fifty rungs ago this view did not exist for you.",
        ground = 0xFF11161D,
        horizon = 0xFF18202F,
        structure = 0xFF364B6D,
        openness = 0.67f
    ),
    HORIZON(
        threshold = 150,
        label = "Horizon",
        note = "You can see the far edge from here. It is further out than the whole room was.",
        ground = 0xFF12171F,
        horizon = 0xFF1A2435,
        structure = 0xFF3D567D,
        openness = 0.84f
    ),
    UNWALLED(
        threshold = 240,
        label = "No walls",
        note = "There is no room any more. There is just where you are and where you go next.",
        ground = 0xFF131821,
        horizon = 0xFF1D283B,
        structure = 0xFF44608C,
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
            1 -> "One more rung and the room opens again."
            else -> "$remaining more rungs to ${stage.label.lowercase()}."
        }
    }
}
