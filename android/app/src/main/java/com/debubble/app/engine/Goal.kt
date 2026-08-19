package com.debubble.app.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What the user actually wants out of their life — chosen at calibration, changeable any time.
 *
 * The three pillars measure the *shape* of someone's bubble. A goal gives that expansion a
 * direction: the same courage that gets you onto an unfamiliar bus is the courage that gets
 * you into a conversation you want, but nobody experiences those as the same project.
 *
 * Each goal rides on a home pillar so the palette stays at three accents plus ember.
 */
enum class Goal(
    val display: String,
    val promise: String,
    val homePillar: Pillar,
    val asset: String
) {
    INTERESTING(
        display = "A life worth describing",
        promise = "Stop having the same week on repeat. Build a life with stories in it.",
        homePillar = Pillar.ACCESS,
        asset = "goals/interesting.json"
    ),
    FRIENDS(
        display = "A real circle",
        promise = "Turn acquaintances into friends, and friends into people who show up.",
        homePillar = Pillar.SOCIAL,
        asset = "goals/friends.json"
    ),
    PARTNER(
        display = "Find someone",
        promise = "Meet people you actually want, honestly, without becoming someone else.",
        homePillar = Pillar.SOCIAL,
        asset = "goals/partner.json"
    ),
    INTIMACY(
        display = "Closer, and honest about it",
        promise = "Say what you want out loud. Build the confidence and the communication for real intimacy.",
        homePillar = Pillar.SOCIAL,
        asset = "goals/intimacy.json"
    ),
    CRAFT(
        display = "Get good at something",
        promise = "Take a hobby from curiosity to competence, and become interesting in the process.",
        homePillar = Pillar.ACTIVITY,
        asset = "goals/craft.json"
    );

    companion object {
        val all = listOf(INTERESTING, FRIENDS, PARTNER, INTIMACY, CRAFT)
        fun from(name: String?): Goal? = all.firstOrNull { it.name == name }
    }
}

/** One step of a goal campaign. Thirty of these per goal, in three phases of ten. */
@Serializable
data class Mission(
    @SerialName("t") val step: Int,
    @SerialName("do") val directive: String,
    @SerialName("why") val coach: String,
    @SerialName("min") val minutes: Int,
    @SerialName("exp") val exposure: Int,
    /** How many reps to aim for on the day this mission is served. This is the daily volume. */
    @SerialName("reps") val repTarget: Int = 0,
    /** Index into [GoalTrack.principles] — the idea this mission is an application of. */
    @SerialName("read") val principle: Int = -1
)

/**
 * A repeatable courage action. Unlimited per day.
 *
 * This is the answer to "there is nothing to do after the daily cards": tiers are earned once
 * a day by design, but reps are volume, and volume is what actually moves someone. A rep
 * marked [friction] adds to the anti-score when logged, because being turned down is the
 * evidence that you were honest rather than careful.
 */
@Serializable
data class RepType(
    @SerialName("key") val key: String,
    @SerialName("label") val label: String,
    @SerialName("hint") val hint: String,
    @SerialName("friction") val friction: Boolean = false
)

/** A short readable idea. Unlocked by progress so the reading tracks the doing. */
@Serializable
data class Principle(
    @SerialName("t") val unlocksAt: Int,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String,
    @SerialName("source") val source: String = ""
)

@Serializable
data class GoalTrack(
    @SerialName("goal") val goal: String,
    @SerialName("premise") val premise: String,
    @SerialName("phases") val phases: List<String> = emptyList(),
    @SerialName("reps") val reps: List<RepType> = emptyList(),
    @SerialName("principles") val principles: List<Principle> = emptyList(),
    @SerialName("missions") val missions: List<Mission> = emptyList()
) {
    fun mission(step: Int): Mission = missions[step.coerceIn(1, missions.size) - 1]

    /** Phase label for a step: three phases of ten across a thirty-step campaign. */
    fun phaseOf(step: Int): String {
        if (phases.isEmpty()) return ""
        val size = (missions.size.coerceAtLeast(1) + phases.size - 1) / phases.size
        return phases[((step - 1) / size).coerceIn(0, phases.lastIndex)]
    }

    fun unlockedPrinciples(step: Int): List<Principle> = principles.filter { it.unlocksAt <= step }
}

/** Where the user stands on one goal campaign. Kept per goal, so switching never loses ground. */
@Serializable
data class GoalState(
    val step: Int = 1,
    val completed: Int = 0,
    val repsLogged: Int = 0,
    val read: Set<Int> = emptySet()
)

object Goals {

    const val CAMPAIGN_LENGTH = 30

    /** Advance one step. Missions are a campaign, not a ladder — no double steps, no stepping back. */
    fun onMissionCompleted(s: GoalState): GoalState = s.copy(
        step = (s.step + 1).coerceAtMost(CAMPAIGN_LENGTH),
        completed = s.completed + 1
    )

    fun onRep(s: GoalState): GoalState = s.copy(repsLogged = s.repsLogged + 1)

    /**
     * Derived from completions, not from [GoalState.step]. The step caps at 30, so a
     * step-based bar would stall at 29/30 on a campaign the user had actually finished.
     */
    fun progress(s: GoalState): Float =
        (s.completed.toFloat() / CAMPAIGN_LENGTH).coerceIn(0f, 1f)

    fun isComplete(s: GoalState): Boolean = s.completed >= CAMPAIGN_LENGTH
}
