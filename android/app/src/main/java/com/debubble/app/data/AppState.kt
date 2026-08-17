package com.debubble.app.data

import com.debubble.app.engine.Baseline
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
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
    val minutes: Int = 0
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
    val onboarded: Boolean = false,
    val baseline: Baseline = Baseline(),
    val pillars: Map<String, PillarState> = Pillar.order.associate { it.name to PillarState() },

    /** The anti-score. Monotonic: nothing in the app decreases it. */
    val friction: Int = 0,

    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: Long = 0L,
    val startedDay: Long = 0L,

    /** Which pillars have been completed today, and which the user swapped. */
    val servedDay: Long = 0L,
    val doneToday: Set<String> = emptySet(),
    val swappedToday: Set<String> = emptySet(),

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

    fun evidence(p: Pillar): Int = log.count { !it.friction && it.pillar == p.name }

    /**
     * A new day resets what was served without touching any progress. Called on load and
     * on completion so the app never shows yesterday's finished cards as still done.
     */
    fun rolledTo(today: Long): AppState =
        if (servedDay == today) this
        else copy(servedDay = today, doneToday = emptySet(), swappedToday = emptySet())
}
