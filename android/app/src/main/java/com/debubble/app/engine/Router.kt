package com.debubble.app.engine

/**
 * What the router decided to serve today, beyond the three pillar challenges.
 *
 * At most two extra items. That ceiling is the most important number in this file: the app
 * now has five campaigns, forty-eight debuffs and a bridge system, and without a hard cap a
 * user running three campaigns with a dozen debuffs ticked would open it to fourteen things
 * to do and close it again permanently.
 */
data class Routed(
    val campaign: CampaignChallenge? = null,
    val remediation: Remediation? = null,
    /** One plain sentence on why this pairing, shown under the section heading. */
    val rationale: String = ""
) {
    val isEmpty: Boolean get() = campaign == null && remediation == null
}

/**
 * Dynamic routing.
 *
 * Three inputs decide what someone gets: which campaigns are running, which behaviours they
 * flagged in the Systems Audit, and what they can afford. Every one of them is a filter or a
 * weight over the authored pools — nothing is generated at runtime, so every string a user
 * ever reads was written and reviewed.
 *
 * Selection is **deterministic on the day index**. The same day always produces the same
 * pairing, which means the app can be closed and reopened without the day's work changing
 * underneath the user, and it means the whole thing is testable without mocking a clock.
 */
object Router {

    /** How often a bridge is preferred over a single-campaign challenge, when one exists. */
    const val BRIDGE_EVERY = 3

    fun route(
        day: Long,
        activeGoals: Set<String>,
        debuffs: Set<String>,
        budget: BudgetTier,
        pool: CampaignPool,
        remediation: RemediationPool,
        clearedRemediation: Set<String>,
        clearedCampaign: Set<String>
    ): Routed {
        val campaign = pickCampaign(day, activeGoals, budget, pool, clearedCampaign)
        val fix = pickRemediation(day, debuffs, budget, remediation, clearedRemediation)
        return Routed(
            campaign = campaign,
            remediation = fix,
            rationale = rationale(campaign, fix, activeGoals.size, debuffs.size)
        )
    }

    /**
     * One campaign challenge.
     *
     * With two or more campaigns running, every third day prefers a bridge — often enough
     * that someone notices the two goals are related, rarely enough that the specific
     * campaigns still feel like separate things with their own material.
     */
    fun pickCampaign(
        day: Long,
        activeGoals: Set<String>,
        budget: BudgetTier,
        pool: CampaignPool,
        cleared: Set<String>
    ): CampaignChallenge? {
        if (activeGoals.isEmpty()) return null

        val wantsBridge = activeGoals.size > 1 && day % BRIDGE_EVERY == 0L
        if (wantsBridge) {
            pool.bridges(activeGoals, budget).pickFresh(day, cleared)?.let { return it }
        }

        // Rotate which campaign gets the slot, so a second campaign is never starved by
        // whichever one happens to sort first.
        val order = activeGoals.sorted()
        val turn = order[((day % order.size).toInt())]
        val mine = pool.singles(activeGoals, budget).filter { it.goals.first() == turn }
        mine.pickFresh(day, cleared)?.let { return it }

        // That campaign is exhausted at this budget; fall back to anything servable.
        return pool.forActive(activeGoals, budget).pickFresh(day, cleared)
    }

    /**
     * One remediation.
     *
     * Rotates across the flagged debuffs rather than draining one at a time. Someone who
     * ticked eight things should see all eight move, not finish posture in a fortnight while
     * everything else waits.
     */
    fun pickRemediation(
        day: Long,
        debuffs: Set<String>,
        budget: BudgetTier,
        pool: RemediationPool,
        cleared: Set<String>
    ): Remediation? {
        if (debuffs.isEmpty()) return null
        val order = debuffs.sorted()

        // Walk the rotation from today's position so an exhausted debuff yields to the next
        // one instead of producing an empty slot.
        for (offset in order.indices) {
            val index = (((day + offset) % order.size).toInt())
            pool.next(order[index], cleared, budget)?.let { return it }
        }
        return null
    }

    /**
     * Which debuffs the user flagged that a given campaign's themes speak to.
     *
     * Used to explain a pairing rather than to select it — the weighting already happened.
     * Being told *why* today looks like this is most of what makes the routing feel designed
     * rather than random.
     */
    fun overlap(challenge: CampaignChallenge, debuffs: Set<String>, catalogue: AuditCatalogue): List<Debuff> =
        debuffs.mapNotNull { catalogue.debuff(it) }
            .filter { d -> d.tags.any { it in challenge.themes } }

    private fun rationale(
        campaign: CampaignChallenge?,
        fix: Remediation?,
        goals: Int,
        debuffs: Int
    ): String = when {
        campaign?.isBridge == true ->
            "This one counts for both of your goals at once."

        campaign != null && fix != null ->
            "One for your goal, one for something you flagged in your audit."

        campaign != null && goals > 1 ->
            "Your goals take turns. This one is up today."

        campaign != null -> "From your goal."

        fix != null && debuffs > 1 ->
            "Your audit items take turns. This one is up today."

        fix != null -> "From your audit."

        else -> ""
    }

    /**
     * Deterministic pick with a stable offset, skipping anything already cleared.
     *
     * Not random: the same day must produce the same challenge, or reopening the app
     * reshuffles work someone had already decided to do.
     */
    private fun List<CampaignChallenge>.pickFresh(day: Long, cleared: Set<String>): CampaignChallenge? {
        val fresh = filter { it.id !in cleared }
        // Everything done: cycle the full list again rather than going blank. A repeat is a
        // far better failure than an empty screen, and these are all repeatable acts.
        val from = fresh.ifEmpty { this }
        if (from.isEmpty()) return null
        return from[((day % from.size).toInt())]
    }
}
