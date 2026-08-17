package com.debubble.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.data.Repository
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.Calibration
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Served
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Which surface is on screen. Five screens, so an explicit route beats a nav graph. */
sealed interface Route {
    data object Loading : Route
    data object Calibration : Route
    data object Dashboard : Route
    data class Challenge(val pillar: Pillar) : Route
    data class Completion(val pillar: Pillar, val tierCleared: Int) : Route
    data object Profile : Route
}

class DeBubbleViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    /** Loaded once. If the assets are broken we want to know immediately, not silently. */
    val curriculum: Curriculum = repo.loadCurriculum()

    val state: StateFlow<AppState> = repo.state.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AppState()
    )

    private val _route = MutableStateFlow<Route>(Route.Loading)
    val route: StateFlow<Route> = _route

    /** Set briefly after a completion so the Bubble Map fires its shockwave once. */
    private val _pulse = MutableStateFlow<Pillar?>(null)
    val pulse: StateFlow<Pillar?> = _pulse

    private fun today(): Long = LocalDate.now().toEpochDay()

    init {
        viewModelScope.launch {
            // Roll the day over on launch, then decide where the user lands.
            repo.update { it.rolledTo(today()) }
            val loaded = repo.state.first()
            _route.value = if (loaded.onboarded) Route.Dashboard else Route.Calibration
        }
    }

    // ------------------------------------------------------------------ serving

    fun serve(pillar: Pillar, s: AppState): Served = Engine.serve(
        pillar = pillar,
        state = s.state(pillar),
        curriculum = curriculum,
        baseline = s.baseline,
        forceAlternate = pillar.name in s.swappedToday
    )

    // ------------------------------------------------------------------ navigation

    fun goDashboard() { _route.value = Route.Dashboard }
    fun goProfile() { _route.value = Route.Profile }
    fun goRecalibrate() { _route.value = Route.Calibration }

    fun openChallenge(pillar: Pillar) { _route.value = Route.Challenge(pillar) }

    /** Back out of a challenge without penalty. Abandoning is not friction — only saying so is. */
    fun abort() { _route.value = Route.Dashboard }

    // ------------------------------------------------------------------ actions

    fun finishCalibration(baseline: Baseline) {
        viewModelScope.launch {
            repo.update { s ->
                val entry = Calibration.entryTiers(baseline)
                s.copy(
                    onboarded = true,
                    baseline = baseline,
                    startedDay = if (s.startedDay == 0L) today() else s.startedDay,
                    // Recalibrating re-pitches the ladder but never discards earned history:
                    // cleared counts and minutes invested carry over.
                    pillars = Pillar.order.associate { p ->
                        val prev = s.state(p)
                        p.name to prev.copy(
                            tier = entry.getValue(p).coerceAtLeast(if (s.onboarded) prev.tier else 1),
                            cleanRun = 0,
                            frictionAtTier = 0
                        )
                    }
                ).rolledTo(today())
            }
            _route.value = Route.Dashboard
        }
    }

    /** Swap today's card for the authored alternate at the same tier. */
    fun swap(pillar: Pillar) {
        viewModelScope.launch {
            repo.update { it.copy(swappedToday = it.swappedToday + pillar.name) }
        }
    }

    fun complete(pillar: Pillar, minutes: Int, tier: Int, title: String) {
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val advanced = Engine.onCompleted(rolled.state(pillar), minutes)
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled
                    .withPillar(pillar, advanced)
                    .copy(
                        doneToday = rolled.doneToday + pillar.name,
                        streak = streak,
                        bestStreak = maxOf(rolled.bestStreak, streak),
                        lastActiveDay = today(),
                        log = listOf(
                            LogEntry(
                                id = System.currentTimeMillis(),
                                pillar = pillar.name,
                                tier = tier,
                                title = title,
                                friction = false,
                                epochDay = today(),
                                minutes = minutes
                            )
                        ) + rolled.log
                    )
            }
            _pulse.value = pillar
            _route.value = Route.Completion(pillar, tier)
        }
    }

    /** Attach the one-sentence journal line to the entry just written. */
    fun journal(note: String) {
        val trimmed = note.trim().take(90)
        if (trimmed.isEmpty()) { goDashboard(); return }
        viewModelScope.launch {
            repo.update { s ->
                val head = s.log.firstOrNull() ?: return@update s
                s.copy(log = listOf(head.copy(note = trimmed)) + s.log.drop(1))
            }
            goDashboard()
        }
    }

    /**
     * The second exit. Logs a point of contact with the edge and holds — or steps back — the
     * tier. Nothing here is scored as a failure.
     */
    fun logFriction(pillar: Pillar, tier: Int, title: String) {
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val adjusted = Engine.onFriction(rolled.state(pillar))
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled
                    .withPillar(pillar, adjusted)
                    .copy(
                        friction = rolled.friction + 1,
                        streak = streak,
                        bestStreak = maxOf(rolled.bestStreak, streak),
                        lastActiveDay = today(),
                        log = listOf(
                            LogEntry(
                                id = System.currentTimeMillis(),
                                pillar = pillar.name,
                                tier = tier,
                                title = title,
                                friction = true,
                                epochDay = today()
                            )
                        ) + rolled.log
                    )
            }
            goDashboard()
        }
    }

    fun clearPulse() { _pulse.value = null }

    fun dayIndex(s: AppState): Long = s.dayIndex(today())
}
