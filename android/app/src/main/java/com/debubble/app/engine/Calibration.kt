package com.debubble.app.engine

import kotlinx.serialization.Serializable

/**
 * What the user told us during Baseline Calibration. Every field maps to exactly one
 * onboarding card, and every field is used — nothing is collected for decoration.
 *
 * This is the whole reason the ladder is personal: a cyclist who ranged 40km last month
 * must never be served "walk to the end of your street".
 */
@Serializable
data class Baseline(
    /** Which of [Needs.transportModes] the user actually has access to. */
    val transport: Set<String> = setOf(Needs.TRANSIT),
    /** Furthest distance from home in the last 30 days, km. */
    val radiusKm: Int = 4,
    /** Percentage of this week that repeated last week. */
    val routinePct: Int = 80,
    /** How recently they last tried something genuinely new: 0 = can't remember … 4 = this week. */
    val noveltyRecency: Int = 1,
    /** Self-reported difficulty of talking to a stranger, 0 = effortless … 10 = physically hard. */
    val socialResistance: Int = 7,
    /** Conversations longer than five minutes in the last week. */
    val longConversations: Int = 2,
    /** What they can spend on a single challenge without it being a problem. 0 = nothing. */
    val budgetPerChallenge: Int = 10,
    /** Minutes they can realistically give on an ordinary day. */
    val capacityMinutes: Int = 45,
    /** Days per week they intend to show up. Used for streak grace, never for guilt. */
    val daysPerWeek: Int = 5,
    /** Can they be away overnight / for several days at all? */
    val canStayOut: Boolean = true,
    val hasPassport: Boolean = false
) {
    fun has(need: String): Boolean = when (need) {
        Needs.TRANSIT, Needs.BIKE, Needs.CAR -> need in transport
        Needs.OVERNIGHT, Needs.MULTIDAY -> canStayOut
        Needs.PASSPORT -> hasPassport
        // Nothing else is a hard gate: a kitchen and pocket money are assumed present
        // unless the budget says otherwise, which is handled separately.
        else -> true
    }
}

/**
 * Rule 4 — Ambiguity & Calibration. Maps baseline answers onto a starting tier per pillar
 * so the engine serves mathematically appropriate challenges from day one.
 *
 * Deliberately capped at [MAX_ENTRY]: however capable someone is, they still walk the
 * ladder rather than being dropped near the top, because the ladder is a habit-builder
 * before it is a difficulty curve.
 */
object Calibration {

    const val MAX_ENTRY = 14

    fun entryTier(pillar: Pillar, b: Baseline): Int = when (pillar) {
        Pillar.ACCESS -> 1 + (b.radiusKm / 6) + b.transport.size
        Pillar.ACTIVITY -> 1 + ((100 - b.routinePct) / 11) + b.noveltyRecency
        Pillar.SOCIAL -> 1 + (((10 - b.socialResistance) * 8) / 10) + (b.longConversations / 4)
    }.coerceIn(1, MAX_ENTRY)

    fun entryTiers(b: Baseline): Map<Pillar, Int> =
        Pillar.order.associateWith { entryTier(it, b) }
}
