package com.debubble.app.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The Systems Audit: a catalogue of specific, changeable behaviours the user can flag about
 * themselves, and the graded remediation for each.
 *
 * Everything in the catalogue is a **behaviour**, never a verdict. That is a deliberate
 * constraint rather than squeamishness: "I look away when someone looks at me" has a first
 * rep and a finish line, and "I am ugly" has neither. A checkbox that stores someone's worst
 * belief about themselves and then has the app agree with it every morning is a mechanic that
 * makes isolated people worse, and it cannot be remediated because there is nothing to do.
 *
 * Where a fixed trait was wanted, the entry here is the behaviour that trait produces —
 * avoiding photographs, dressing to disappear, shrinking in a room — which routes to exactly
 * the same style, grooming and humour work and can actually be completed.
 */
@Serializable
data class DebuffCategory(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("blurb") val blurb: String,
    @SerialName("note") val note: String
)

@Serializable
data class Debuff(
    @SerialName("id") val id: String,
    @SerialName("cat") val category: String,
    @SerialName("label") val label: String,
    @SerialName("detail") val detail: String,
    @SerialName("tags") val tags: List<String> = emptyList()
)

@Serializable
data class AuditCatalogue(
    @SerialName("categories") val categories: List<DebuffCategory> = emptyList(),
    @SerialName("debuffs") val debuffs: List<Debuff> = emptyList()
) {
    private val byId: Map<String, Debuff> by lazy { debuffs.associateBy { it.id } }

    fun debuff(id: String): Debuff? = byId[id]

    fun inCategory(category: String): List<Debuff> = debuffs.filter { it.category == category }

    fun categoryOf(id: String): String? = byId[id]?.category

    /** Only ids the catalogue actually knows. Guards against stale saved state. */
    fun known(ids: Set<String>): Set<String> = ids.filter { it in byId }.toSet()

    fun countIn(category: String, selected: Set<String>): Int =
        selected.count { byId[it]?.category == category }
}

/**
 * One rung of remediation for one debuff.
 *
 * Three rungs each, and rung 1 is always free and low-exposure by construction — if the
 * gentlest response to a debuff is still frightening, the debuff was written wrong.
 */
@Serializable
data class Remediation(
    @SerialName("id") val id: String,
    @SerialName("debuff") val debuff: String,
    @SerialName("rung") val rung: Int,
    @SerialName("do") val directive: String,
    @SerialName("why") val coach: String,
    @SerialName("min") val minutes: Int,
    @SerialName("cost") val cost: Int,
    @SerialName("tier") val tier: Int,
    @SerialName("exp") val exposure: Int,
    @SerialName("pillar") val pillar: String
) {
    val pillarEnum: Pillar
        get() = runCatching { Pillar.valueOf(pillar) }.getOrDefault(Pillar.SOCIAL)

    /** As a [Served], so it reuses the action screen rather than duplicating it. */
    fun toServed(label: String): Served = Served(
        pillar = pillarEnum,
        tier = rung,
        directive = directive,
        coach = coach,
        minutes = minutes,
        cost = cost,
        exposure = exposure,
        substituted = false,
        kind = "AUDIT",
        phase = label
    )
}

@Serializable
data class RemediationPool(
    @SerialName("challenges") val challenges: List<Remediation> = emptyList()
) {
    private val byDebuff: Map<String, List<Remediation>> by lazy {
        challenges.groupBy { it.debuff }.mapValues { (_, v) -> v.sortedBy { it.rung } }
    }

    fun forDebuff(id: String): List<Remediation> = byDebuff[id] ?: emptyList()

    /**
     * The next rung for this debuff that the user has not cleared and can afford.
     *
     * Falls through rather than stopping: someone on the free tier whose next rung costs
     * money gets the rung after it rather than nothing at all, because a debuff that goes
     * silent because of a budget setting looks like a bug.
     */
    fun next(id: String, cleared: Set<String>, budget: BudgetTier): Remediation? =
        forDebuff(id).firstOrNull { it.id !in cleared && budget.allows(it.tier, it.cost) }
            ?: forDebuff(id).lastOrNull { budget.allows(it.tier, it.cost) }

    fun byId(id: String): Remediation? = challenges.firstOrNull { it.id == id }
}
