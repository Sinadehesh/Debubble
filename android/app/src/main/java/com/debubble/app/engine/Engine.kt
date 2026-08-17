package com.debubble.app.engine

import kotlinx.serialization.Serializable

/** Where the user stands on one ladder. */
@Serializable
data class PillarState(
    val tier: Int = 1,
    /** Consecutive clean completions at this pillar. Three in a row earns a double step. */
    val cleanRun: Int = 0,
    /** Friction logged at the *current* tier. Two means the tier is too big; step back. */
    val frictionAtTier: Int = 0,
    /** Total tiers cleared on this pillar. Never decreases, even when the tier steps back. */
    val cleared: Int = 0,
    /** Minutes invested in cleared challenges. Evidence, not score. */
    val minutesInvested: Int = 0
)

/**
 * The 100-day escalation logic.
 *
 * Every function here is pure: same inputs, same outputs, no Android, no clock, no IO.
 * That is what makes the ladder testable, and the tests are what stop a bad edit from
 * quietly serving someone tier 40 on their second day.
 */
object Engine {

    const val MAX_TIER = 100

    /** Clean completions needed before the ladder allows a two-tier step. */
    const val DOUBLE_STEP_AFTER = 3

    /** Friction events at one tier before the ladder steps back down. */
    const val STEP_BACK_AFTER = 2

    // ---------------------------------------------------------------- serving

    /**
     * Pick what this user sees for this pillar today.
     *
     * Gating order matters: a challenge the user physically cannot do is worse than a
     * challenge that is slightly too easy, so an unmet [Challenge.needs] or a cost over
     * budget falls back to the authored [Challenge.alternate] rather than skipping a tier.
     */
    fun serve(
        pillar: Pillar,
        state: PillarState,
        curriculum: Curriculum,
        baseline: Baseline,
        forceAlternate: Boolean = false
    ): Served {
        val c = curriculum.challenge(pillar, state.tier)

        val missing = c.needs.firstOrNull { !baseline.has(it) }
        val overBudget = c.cost > baseline.budgetPerChallenge
        val useAlternate = forceAlternate || missing != null || overBudget

        val reason = when {
            forceAlternate -> "You swapped this one."
            missing != null -> "Adjusted — you told us ${missingLabel(missing)}."
            overBudget -> "Adjusted to stay inside your budget."
            else -> null
        }

        return Served(
            pillar = pillar,
            tier = state.tier,
            directive = if (useAlternate) c.alternate else c.directive,
            coach = c.coach,
            // The alternate is designed to sit at the same rung, but it is by construction
            // the cheaper, closer, shorter route to the same lesson.
            minutes = if (useAlternate) maxOf(1, (c.minutes * 2) / 3) else c.minutes,
            cost = if (useAlternate) minOf(c.cost, baseline.budgetPerChallenge) else c.cost,
            exposure = c.exposure,
            substituted = useAlternate,
            substitutionReason = reason
        )
    }

    private fun missingLabel(need: String): String = when (need) {
        Needs.TRANSIT -> "you have no transit access"
        Needs.BIKE -> "you have no bike"
        Needs.CAR -> "you have no car"
        Needs.OVERNIGHT, Needs.MULTIDAY -> "you cannot be away overnight"
        Needs.PASSPORT -> "you have no passport"
        else -> "this one does not fit"
    }

    // ---------------------------------------------------------------- progression

    /** A challenge was completed. The only thing that advances a tier. */
    fun onCompleted(state: PillarState, minutes: Int): PillarState {
        val run = state.cleanRun + 1
        val earnedDoubleStep = run >= DOUBLE_STEP_AFTER
        val step = if (earnedDoubleStep) 2 else 1
        return state.copy(
            tier = (state.tier + step).coerceAtMost(MAX_TIER),
            cleanRun = if (earnedDoubleStep) 0 else run,
            frictionAtTier = 0,
            cleared = state.cleared + 1,
            minutesInvested = state.minutesInvested + minutes
        )
    }

    /**
     * The user hit friction — it got awkward, they were refused, or it was too hard.
     *
     * This is never a penalty. The tier holds, and a second friction at the same tier
     * steps *back* so the ladder meets them where they are instead of stalling them
     * against a rung that does not fit yet.
     */
    fun onFriction(state: PillarState): PillarState {
        val count = state.frictionAtTier + 1
        return if (count >= STEP_BACK_AFTER) {
            state.copy(
                tier = (state.tier - 1).coerceAtLeast(1),
                cleanRun = 0,
                frictionAtTier = 0
            )
        } else {
            state.copy(cleanRun = 0, frictionAtTier = count)
        }
    }

    // ---------------------------------------------------------------- readouts

    /**
     * Bubble radius for the map, 0f..1f. Uses a root curve so the first tiers produce
     * visible growth — early progress has to be felt or nobody reaches tier 10.
     */
    fun radius(tier: Int): Float {
        val t = (tier.coerceIn(1, MAX_TIER)).toFloat() / MAX_TIER
        return 0.20f + Math.pow(t.toDouble(), 0.62).toFloat() * 0.80f
    }

    /**
     * Streak counts consecutive days the user *engaged* — a completion or a logged
     * friction both count. Missing a day pauses the streak; it never erases Friction
     * or cleared tiers, and nothing in the app punishes the gap.
     */
    fun updateStreak(currentStreak: Int, lastActiveDay: Long, today: Long): Int = when {
        lastActiveDay == today -> currentStreak.coerceAtLeast(1)
        lastActiveDay == today - 1 -> currentStreak + 1
        else -> 1
    }

    /** Courage badge tier. Monotonic by construction — Friction can only ever go up. */
    fun courageBadge(friction: Int): String = when {
        friction >= 50 -> "Weathered"
        friction >= 25 -> "Iron"
        friction >= 10 -> "Tempered"
        friction >= 3 -> "Contact"
        else -> "Untested"
    }

    /** Human-readable duration. The ladder spans two minutes to thirty days. */
    fun formatMinutes(min: Int): String = when {
        min < 60 -> "$min min"
        min < 60 * 24 -> {
            val h = min / 60
            val m = min % 60
            if (m == 0) "$h hr" else "$h hr $m"
        }
        min < 60 * 24 * 14 -> {
            val d = min / (60 * 24)
            if (d == 1) "1 day" else "$d days"
        }
        else -> "${min / (60 * 24 * 7)} wk"
    }
}
