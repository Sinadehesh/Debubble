package com.debubble.app.data

import com.debubble.app.engine.AvatarState
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.BudgetTier
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.Progress
import kotlinx.serialization.Serializable

/** One line in the history. Completions and friction live in the same stream, deliberately. */
@Serializable
data class LogEntry(
    val id: Long,
    val pillar: String,
    val tier: Int,
    val title: String,
    val note: String = "",
    val friction: Boolean = false,
    val epochDay: Long,
    val minutes: Int = 0,
    /** "PILLAR" | "MISSION" | "REP" — the history is one stream carrying all three. */
    val kind: String = "PILLAR"
) {
    val pillarEnum: Pillar get() = runCatching { Pillar.valueOf(pillar) }.getOrDefault(Pillar.ACCESS)
}

/**
 * Everything the app knows, in one serializable snapshot.
 *
 * Persistence is a single JSON blob in DataStore rather than a relational store: this is
 * one user's local data, a few hundred rows at most, with no queries beyond "the log, newest
 * first". Room would add a codegen toolchain for no benefit at this size.
 */
@Serializable
data class AppState(
    /** The three-card intro has been seen. Separate from [onboarded] so someone who
     *  recalibrates later is not shown the explainer again. */
    val introSeen: Boolean = false,
    val onboarded: Boolean = false,
    val baseline: Baseline = Baseline(),
    val pillars: Map<String, PillarState> = Pillar.order.associate { it.name to PillarState() },

    /**
     * The anti-score. Nothing in the app ever spends or penalises it; the one thing that
     * lowers it is the user undoing a rep they logged by mistake, which is a correction
     * rather than a cost.
     */
    val friction: Int = 0,

    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: Long = 0L,
    val startedDay: Long = 0L,

    /** Which pillars have been completed today, and which the user swapped. */
    val servedDay: Long = 0L,
    val doneToday: Set<String> = emptySet(),
    val swappedToday: Set<String> = emptySet(),

    /* ---- the goal layer: what the user actually wants, and the volume they put in ---- */

    /**
     * The single active campaign, kept only so state written before parallel campaigns
     * existed still knows what it was running. Read through [activeGoals].
     */
    val goal: String? = null,
    /**
     * Every campaign running right now. More than one is supported and expected — the
     * router serves them in rotation and bridges them where a challenge satisfies both.
     */
    val activeGoals: Set<String> = emptySet(),
    /** Per-goal progress, kept for every goal ever started so switching costs nothing. */
    val goalStates: Map<String, GoalState> = emptyMap(),
    val missionDoneToday: Boolean = false,
    /** repKey -> count logged today. Reset by the day roll. */
    val repsToday: Map<String, Int> = emptyMap(),
    /** "GOAL:repKey" -> lifetime count. Never reset — this is the evidence. */
    val repTotals: Map<String, Int> = emptyMap(),

    /** Pillars whose level-100 ending has been shown. It only ever plays once. */
    val transcended: Set<String> = emptySet(),

    /* ---- the avatar: how the user is shown back to themselves ---- */

    /** Earned by doing things. Unlocks casual wear. */
    val xp: Int = 0,
    val avatar: AvatarState = AvatarState(),

    /* ---- the Systems Audit, the budget, and what the router has already served ---- */

    /** Behaviours the user flagged. Drives remediation challenges and Learn articles. */
    val debuffs: Set<String> = emptySet(),
    /** True once the audit has been seen, so it is never forced a second time. */
    val auditDone: Boolean = false,
    /** Strict spending ceiling. Nothing above it is ever shown. */
    val budgetLevel: Int = -1,

    /** Ids of remediation and campaign challenges already cleared, so they are not reserved. */
    val clearedRemediation: Set<String> = emptySet(),
    val clearedCampaign: Set<String> = emptySet(),
    /** Which of the two routed slots have been taken today. */
    val routedDoneToday: Set<String> = emptySet(),

    /* ---- Learn and Notes ---- */

    val lessonsRead: Set<String> = emptySet(),
    val notes: List<Note> = emptyList(),

    /** One-shot feedback on deliberate actions. Expected, so it is on. */
    val soundOn: Boolean = true,
    /** The continuous bed. An intrusion if it starts by itself, so it is opt-in. */
    val ambientOn: Boolean = false,

    val log: List<LogEntry> = emptyList()
) {
    fun state(p: Pillar): PillarState = pillars[p.name] ?: PillarState()

    fun withPillar(p: Pillar, s: PillarState): AppState =
        copy(pillars = pillars + (p.name to s))

    val tiersCleared: Int get() = Pillar.order.sumOf { state(it).cleared }

    val minutesInvested: Int get() = Pillar.order.sumOf { state(it).minutesInvested }

    /** Day index the user is on, 1-based. Not the same as tier — tiers are earned. */
    fun dayIndex(today: Long): Long =
        if (startedDay == 0L) 1L else (today - startedDay + 1).coerceAtLeast(1L)

    fun isDoneToday(p: Pillar): Boolean = p.name in doneToday

    /**
     * Days since the user last did anything. Drives how heavy the bubble looks — a neglected
     * membrane thickens rather than shrinking, because nothing earned is ever taken back.
     */
    fun daysSinceActive(today: Long): Long =
        if (lastActiveDay == 0L) 0L else (today - lastActiveDay).coerceAtLeast(0L)

    fun evidence(p: Pillar): Int = log.count { !it.friction && it.pillar == p.name }

    /* ---- levels and armour ---- */

    val level: Int get() = Progress.level(xp)
    val levelProgress: Float get() = Progress.levelProgress(xp)
    val plates: Int get() = Progress.plates(friction)

    /* ---- goal helpers ---- */

    val goalEnum: Goal? get() = Goal.from(goal)

    /**
     * Campaigns actually running.
     *
     * Falls back to the old single [goal] for state written before parallel campaigns, so an
     * existing user opens the new build already running what they were running.
     */
    val running: Set<String>
        get() = if (activeGoals.isNotEmpty()) activeGoals else setOfNotNull(goal)

    val runningGoals: List<Goal> get() = running.mapNotNull { Goal.from(it) }.sortedBy { it.name }

    /** The campaign whose 30-step ladder drives the mission card. First alphabetically. */
    val primaryGoal: Goal? get() = runningGoals.firstOrNull()

    fun isRunning(g: Goal): Boolean = g.name in running

    /**
     * The spending ceiling. Older state has no [budgetLevel] and only the free-text cash
     * figure from setup, which maps cleanly onto a tier.
     */
    val budget: BudgetTier
        get() = if (budgetLevel >= 0) BudgetTier.fromLevel(budgetLevel)
        else BudgetTier.fromLegacyCash(baseline.budgetPerChallenge)

    fun goalState(g: Goal): GoalState = goalStates[g.name] ?: GoalState()

    fun withGoal(g: Goal, s: GoalState): AppState =
        copy(goalStates = goalStates + (g.name to s))

    /** Reps logged today across every type. The number the dashboard counts against target. */
    val repsToTodayTotal: Int get() = repsToday.values.sum()

    fun repTotal(g: Goal, key: String): Int = repTotals["${g.name}:$key"] ?: 0

    /** Lifetime reps on the active campaign — the headline volume number. */
    fun repsOnGoal(g: Goal): Int =
        repTotals.entries.filter { it.key.startsWith("${g.name}:") }.sumOf { it.value }

    fun hasStarted(g: Goal): Boolean = goalStates.containsKey(g.name)

    fun goalProgress(g: Goal): Float = Goals.progress(goalState(g))

    /**
     * A new day resets what was served without touching any progress. Called on load and
     * on completion so the app never shows yesterday's finished cards as still done.
     */
    fun rolledTo(today: Long): AppState =
        if (servedDay == today) this
        else copy(
            servedDay = today,
            doneToday = emptySet(),
            swappedToday = emptySet(),
            missionDoneToday = false,
            repsToday = emptyMap(),
            routedDoneToday = emptySet()
        )
}
