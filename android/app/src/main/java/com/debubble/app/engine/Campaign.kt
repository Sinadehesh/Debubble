package com.debubble.app.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A challenge that belongs to one campaign, or bridges two.
 *
 * Bridges are the reason campaigns can run in parallel without doubling the daily workload.
 * Someone chasing a partner and a social circle at the same time is doing one thing, not two,
 * and serving them two separate errands on the same evening is how an app gets deleted.
 * "Host something and invite two people you do not know well" satisfies both and costs one
 * evening.
 */
@Serializable
data class CampaignChallenge(
    @SerialName("id") val id: String,
    @SerialName("goals") val goals: List<String> = emptyList(),
    @SerialName("do") val directive: String,
    @SerialName("why") val coach: String,
    @SerialName("min") val minutes: Int,
    @SerialName("cost") val cost: Int,
    @SerialName("tier") val tier: Int,
    @SerialName("exp") val exposure: Int,
    @SerialName("pillar") val pillar: String,
    @SerialName("themes") val themes: List<String> = emptyList()
) {
    val isBridge: Boolean get() = goals.size > 1

    val pillarEnum: Pillar
        get() = runCatching { Pillar.valueOf(pillar) }.getOrDefault(Pillar.SOCIAL)

    val goalEnums: List<Goal> get() = goals.mapNotNull { Goal.from(it) }

    /** Servable only when every campaign it names is actually running. */
    fun servableFor(active: Set<String>): Boolean =
        goals.isNotEmpty() && active.containsAll(goals)

    /**
     * As a [Served], so the action screen, the commit gesture and the friction exit are the
     * ones already built rather than a second copy of all three.
     */
    fun toServed(): Served = Served(
        pillar = pillarEnum,
        tier = 0,
        directive = directive,
        coach = coach,
        minutes = minutes,
        cost = cost,
        exposure = exposure,
        substituted = false,
        kind = "CAMPAIGN",
        phase = if (isBridge) {
            "Counts for " + goalEnums.joinToString(" and ") { it.display }
        } else {
            goalEnums.firstOrNull()?.display
        }
    )
}

@Serializable
data class CampaignPool(
    @SerialName("challenges") val challenges: List<CampaignChallenge> = emptyList()
) {
    fun forActive(active: Set<String>, budget: BudgetTier): List<CampaignChallenge> =
        challenges.filter { it.servableFor(active) && budget.allows(it.tier, it.cost) }

    fun bridges(active: Set<String>, budget: BudgetTier): List<CampaignChallenge> =
        forActive(active, budget).filter { it.isBridge }

    fun singles(active: Set<String>, budget: BudgetTier): List<CampaignChallenge> =
        forActive(active, budget).filter { !it.isBridge }

    fun byId(id: String): CampaignChallenge? = challenges.firstOrNull { it.id == id }
}
