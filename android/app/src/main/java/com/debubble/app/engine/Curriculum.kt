package com.debubble.app.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One rung of one ladder. Authored in the assets/curriculum JSON, never generated at runtime. */
@Serializable
data class Challenge(
    @SerialName("t") val tier: Int,
    @SerialName("do") val directive: String,
    @SerialName("why") val coach: String,
    @SerialName("min") val minutes: Int,
    @SerialName("cost") val cost: Int,
    @SerialName("exp") val exposure: Int,
    @SerialName("needs") val needs: List<String> = emptyList(),
    @SerialName("alt") val alternate: String,
    /* ---- the protocol. See [Protocol] for why each of these exists. ---- */
    /** A precise moment, first person: "I put my shoes on tomorrow morning". */
    @SerialName("when") val anchor: String = "",
    /** The under-two-minute entry move. Not the task — the thing that makes refusing it absurd. */
    @SerialName("open") val opener: String = "",
    /** The success criterion. Without one, a challenge cannot be finished, only abandoned. */
    @SerialName("done") val done: String = "",
    /** The safety behaviour to remove, so surviving it does not credit the safety behaviour. */
    @SerialName("stop") val drop: String = "",
    /** The prediction to be disconfirmed, in the user's likely words. */
    @SerialName("test") val test: String = "",
    /** The actual sentence, where the barrier is a missing sentence rather than missing nerve. */
    @SerialName("say") val script: String = ""
)

@Serializable
data class Ladder(
    @SerialName("pillar") val pillar: String,
    @SerialName("arc") val arc: String,
    @SerialName("tiers") val tiers: List<Challenge>
) {
    fun at(tier: Int): Challenge = tiers[tier.coerceIn(1, tiers.size) - 1]

    /**
     * The rung to offer when this one does not fit: two below, or the authored alternate at
     * the bottom of the ladder where there is nothing below to point at.
     *
     * Derived rather than authored on purpose. Behavioural activation's graded task assignment
     * only works if the smaller step is genuinely smaller, and the ladder already encodes that
     * ordering — so computing it cannot drift out of calibration the way 300 hand-written
     * fallbacks would.
     */
    fun easierThan(tier: Int): String {
        val below = tier - 2
        return if (below >= 1) at(below).directive else at(tier).alternate
    }
}

/** The three ladders, loaded once from assets. */
class Curriculum(private val ladders: Map<Pillar, Ladder>) {

    fun ladder(pillar: Pillar): Ladder = ladders.getValue(pillar)

    fun challenge(pillar: Pillar, tier: Int): Challenge = ladder(pillar).at(tier)

    fun arc(pillar: Pillar): String = ladder(pillar).arc
}

/**
 * A challenge as actually served to this user today — after gating against their baseline
 * and their stated capacity. [substituted] means the user is seeing the accessible variant
 * because the canonical directive needed something they told us they do not have.
 */
data class Served(
    val pillar: Pillar,
    val tier: Int,
    val directive: String,
    val coach: String,
    val minutes: Int,
    val cost: Int,
    val exposure: Int,
    val substituted: Boolean,
    val substitutionReason: String? = null,
    /**
     * "TIER" for a pillar challenge, "MISSION" for a goal campaign step. Missions reuse the
     * whole Action Screen rather than duplicating it — the commit gesture, the coach block
     * and the friction exit are identical acts, only the source of the directive differs.
     */
    val kind: String = "TIER",
    /** Phase label, mission only: "Go first", "Express it", and so on. */
    val phase: String? = null,
    /** Suggested reps for the day, mission only. */
    val repTarget: Int = 0,
    /* ---- the protocol, resolved for this user ---- */
    val anchor: String = "",
    val opener: String = "",
    val done: String = "",
    val drop: String = "",
    val test: String = "",
    val script: String = "",
    /** The smaller rung, offered in the same breath rather than behind a menu. */
    val ease: String = "",
    /** Shown at the instant of completion, never saved, never scored. */
    val celebration: String = ""
) {
    /** Whether there is enough authored protocol to be worth drawing the block at all. */
    val hasProtocol: Boolean
        get() = opener.isNotBlank() || done.isNotBlank() || drop.isNotBlank()
}
