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
    @SerialName("alt") val alternate: String
)

@Serializable
data class Ladder(
    @SerialName("pillar") val pillar: String,
    @SerialName("arc") val arc: String,
    @SerialName("tiers") val tiers: List<Challenge>
) {
    fun at(tier: Int): Challenge = tiers[tier.coerceIn(1, tiers.size) - 1]
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
    val repTarget: Int = 0
)
