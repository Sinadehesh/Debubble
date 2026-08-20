package com.debubble.app.engine

/**
 * What the user can spend on a single challenge.
 *
 * A strict filter, not a preference. Someone on [FREE] never sees a challenge that costs
 * money — not greyed out, not "upgrade to unlock", not present. An app aimed at people who
 * are isolated cannot make the good version of itself contingent on having money, and
 * showing someone a locked challenge they cannot afford is a small cruelty with no upside.
 *
 * Every campaign and every debuff has enough free material to run indefinitely on [FREE].
 * That is asserted by the generators, not hoped for.
 */
enum class BudgetTier(
    val level: Int,
    val display: String,
    val detail: String,
    /** Hard cash ceiling per challenge, in whatever the user's currency is. */
    val cap: Int
) {
    FREE(
        level = 0,
        display = "Free",
        detail = "Nothing that costs money. Ever.",
        cap = 0
    ),
    CHEAP(
        level = 1,
        display = "Cheap",
        detail = "Up to the price of a coffee or a bus fare.",
        cap = 20
    ),
    PREMIUM(
        level = 2,
        display = "Open",
        detail = "Classes, kit and travel are on the table.",
        cap = 120
    );

    /** True when a challenge at [tier] costing [cost] is inside this budget. */
    fun allows(tier: Int, cost: Int): Boolean = tier <= level && cost <= cap

    companion object {
        val all = listOf(FREE, CHEAP, PREMIUM)

        fun fromLevel(level: Int): BudgetTier = all.firstOrNull { it.level == level } ?: FREE

        /**
         * Recovers a tier from the old free-text budget field, for state written before the
         * slider existed. Zero meant "nothing", which is exactly [FREE].
         */
        fun fromLegacyCash(perChallenge: Int): BudgetTier = when {
            perChallenge <= 0 -> FREE
            perChallenge <= CHEAP.cap -> CHEAP
            else -> PREMIUM
        }
    }
}
