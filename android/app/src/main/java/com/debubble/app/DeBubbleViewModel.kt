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
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.BubbleState
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

    /** Choosing or changing the campaign. */
    data class GoalPicker(val firstRun: Boolean) : Route
    /** The day's mission — same Action Screen, different source. */
    data object Mission : Route
    /** The reading list for the active campaign, and a single principle. */
    data object Principles : Route
    /** The campaign rendered as terrain. */
    data object Campaign : Route
    data class ReadPrinciple(val index: Int) : Route
}

class DeBubbleViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    /** Loaded once. If the assets are broken we want to know immediately, not silently. */
    val curriculum: Curriculum = repo.loadCurriculum()

    /** The five goal campaigns, likewise. */
    val goals: Map<Goal, GoalTrack> = repo.loadGoals()

    fun track(goal: Goal): GoalTrack = goals.getValue(goal)

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
            _route.value = when {
                !loaded.onboarded -> Route.Calibration
                loaded.goalEnum == null -> Route.GoalPicker(firstRun = true)
                else -> Route.Dashboard
            }
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

    /**
     * The day's mission, or null when no campaign is active or it is already finished.
     *
     * Returned as a [Served] so the Action Screen, the completion beat and the friction exit
     * are shared with pillar challenges rather than reimplemented.
     */
    fun serveMission(s: AppState): Served? {
        val goal = s.goalEnum ?: return null
        val gs = s.goalState(goal)
        if (Goals.isComplete(gs)) return null
        val t = track(goal)
        val m = t.mission(gs.step)
        return Served(
            pillar = goal.homePillar,
            tier = m.step,
            directive = m.directive,
            coach = m.coach,
            minutes = m.minutes,
            cost = 0,
            exposure = m.exposure,
            substituted = false,
            kind = "MISSION",
            phase = t.phaseOf(m.step),
            repTarget = m.repTarget
        )
    }

    /**
     * Everything the living bubble reads. Energy spikes on a fresh completion and otherwise
     * reflects how much of today has already been taken.
     */
    fun bubble(s: AppState, pulsing: Boolean): BubbleState =
        BubbleState.from(
            tiers = Pillar.order.associateWith { s.state(it).tier },
            daysSinceActive = s.daysSinceActive(today()),
            energy = if (pulsing) 1f else (s.doneToday.size / 3f) * 0.35f
        )

    /** Rep types available today, empty when no campaign is active. */
    fun repTypes(s: AppState) = s.goalEnum?.let { track(it).reps } ?: emptyList()

    /** How many reps today's mission asks for. Zero means no target, reps still welcome. */
    fun repTargetToday(s: AppState): Int = serveMission(s)?.repTarget ?: 0

    // ------------------------------------------------------------------ navigation

    fun goDashboard() { _route.value = Route.Dashboard }
    fun goProfile() { _route.value = Route.Profile }
    fun goRecalibrate() { _route.value = Route.Calibration }
    fun goGoalPicker() { _route.value = Route.GoalPicker(firstRun = false) }
    fun goPrinciples() { _route.value = Route.Principles }
    fun goCampaign() { _route.value = Route.Campaign }
    fun openPrinciple(index: Int) { _route.value = Route.ReadPrinciple(index) }
    fun openMission() { _route.value = Route.Mission }

    fun openChallenge(pillar: Pillar) { _route.value = Route.Challenge(pillar) }

    /** Back out of a challenge without penalty. Abandoning is not friction — only saying so is. */
    fun abort() { _route.value = Route.Dashboard }

    // ------------------------------------------------------------------ actions

    fun finishCalibration(baseline: Baseline) {
        viewModelScope.launch {
            // Decided inside the update, because state.value has not caught up yet out here.
            var needsGoal = false
            repo.update { s ->
                needsGoal = s.goalEnum == null
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
            _route.value =
                if (needsGoal) Route.GoalPicker(firstRun = true) else Route.Dashboard
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

    // ------------------------------------------------------------------ the goal layer

    /**
     * Choose or change the campaign. Progress on every goal is kept, so someone can run the
     * friends campaign for a month, switch to craft, and come back to step 14 exactly.
     */
    fun chooseGoal(goal: Goal) {
        viewModelScope.launch {
            repo.update { s ->
                s.copy(
                    goal = goal.name,
                    goalStates = if (s.goalStates.containsKey(goal.name)) s.goalStates
                    else s.goalStates + (goal.name to GoalState()),
                    missionDoneToday = false
                ).rolledTo(today())
            }
            _route.value = Route.Dashboard
        }
    }

    fun completeMission(minutes: Int, step: Int, title: String) {
        val goal = state.value.goalEnum ?: return
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val advanced = Goals.onMissionCompleted(rolled.goalState(goal))
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled.withGoal(goal, advanced).copy(
                    missionDoneToday = true,
                    streak = streak,
                    bestStreak = maxOf(rolled.bestStreak, streak),
                    lastActiveDay = today(),
                    log = listOf(
                        LogEntry(
                            id = System.currentTimeMillis(),
                            pillar = goal.homePillar.name,
                            tier = step,
                            title = title,
                            friction = false,
                            epochDay = today(),
                            minutes = minutes,
                            kind = "MISSION"
                        )
                    ) + rolled.log
                )
            }
            _pulse.value = goal.homePillar
            _route.value = Route.Completion(goal.homePillar, step)
        }
    }

    fun missionFriction(step: Int, title: String) {
        val goal = state.value.goalEnum ?: return
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled.copy(
                    friction = rolled.friction + 1,
                    streak = streak,
                    bestStreak = maxOf(rolled.bestStreak, streak),
                    lastActiveDay = today(),
                    log = listOf(
                        LogEntry(
                            id = System.currentTimeMillis(),
                            pillar = goal.homePillar.name,
                            tier = step,
                            title = title,
                            friction = true,
                            epochDay = today(),
                            kind = "MISSION"
                        )
                    ) + rolled.log
                )
            }
            goDashboard()
        }
    }

    /**
     * Log one rep. Unlimited per day, and the reason the app has something to do after the
     * daily cards are gone. A rep flagged as friction feeds the anti-score, because being
     * turned down is the evidence that the attempt was honest.
     */
    fun logRep(key: String, isFriction: Boolean, label: String) {
        val goal = state.value.goalEnum ?: return
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val totalKey = "${goal.name}:$key"
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled
                    .withGoal(goal, Goals.onRep(rolled.goalState(goal)))
                    .copy(
                        repsToday = rolled.repsToday + (key to (rolled.repsToday[key] ?: 0) + 1),
                        repTotals = rolled.repTotals + (totalKey to (rolled.repTotals[totalKey] ?: 0) + 1),
                        friction = rolled.friction + if (isFriction) 1 else 0,
                        streak = streak,
                        bestStreak = maxOf(rolled.bestStreak, streak),
                        lastActiveDay = today(),
                        log = listOf(
                            LogEntry(
                                id = System.currentTimeMillis(),
                                pillar = goal.homePillar.name,
                                tier = rolled.goalState(goal).step,
                                title = label,
                                friction = isFriction,
                                epochDay = today(),
                                kind = "REP"
                            )
                        ) + rolled.log
                    )
            }
        }
    }

    fun markRead(index: Int) {
        val goal = state.value.goalEnum ?: return
        viewModelScope.launch {
            repo.update { s ->
                s.withGoal(goal, s.goalState(goal).let { it.copy(read = it.read + index) })
            }
        }
    }

    fun clearPulse() { _pulse.value = null }

    fun dayIndex(s: AppState): Long = s.dayIndex(today())
}
