package com.debubble.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.data.Repository
import com.debubble.app.data.Note
import com.debubble.app.data.Notes
import com.debubble.app.engine.AuditCatalogue
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.BudgetTier
import com.debubble.app.engine.CampaignPool
import com.debubble.app.engine.LearnCurriculum
import com.debubble.app.engine.Remediation
import com.debubble.app.engine.RemediationPool
import com.debubble.app.engine.Lesson
import com.debubble.app.engine.Routed
import com.debubble.app.engine.Router
import com.debubble.app.engine.Calibration
import com.debubble.app.engine.Copy
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Engine
import com.debubble.app.engine.AvatarState
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Progress
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.RingState
import com.debubble.app.ui.screens.Tab
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
    /** The three-card explainer, first run only. */
    data object Intro : Route
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
    /** The end of one ladder. Shown once, then never again. */
    data class Transcendence(val pillar: Pillar) : Route
    /** Dressing the avatar. */
    data object AvatarStudio : Route
    /** The Systems Audit — flagging behaviours to work on. */
    data class Audit(val firstRun: Boolean) : Route
    /** The knowledge base. */
    data object Learn : Route
    data class ReadLesson(val id: String) : Route
    /** The journal. */
    data object Notes : Route
    data class EditNote(val id: Long?, val linkLogId: Long?, val linkLessonId: String?) : Route
    /** A routed extra — a campaign challenge or a remediation. */
    data class Routed(val remediation: Boolean) : Route
}

class DeBubbleViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app)

    /** Loaded once. If the assets are broken we want to know immediately, not silently. */
    val curriculum: Curriculum = repo.loadCurriculum()

    /** The five goal campaigns, likewise. */
    val goals: Map<Goal, GoalTrack> = repo.loadGoals()

    /** The Systems Audit catalogue, its remediation pools, the campaign pool and Learn. */
    val audit: AuditCatalogue = repo.loadAudit()
    val remediation: RemediationPool = repo.loadRemediation(audit)
    val campaignPool: CampaignPool = repo.loadCampaignPool()
    val learn: LearnCurriculum = repo.loadLearn()

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
                !loaded.introSeen -> Route.Intro
                !loaded.onboarded -> Route.Calibration
                !loaded.auditDone -> Route.Audit(firstRun = true)
                loaded.running.isEmpty() -> Route.GoalPicker(firstRun = true)
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
        val goal = s.primaryGoal ?: return null
        val gs = s.goalState(goal)
        if (Goals.isComplete(gs)) return null
        val t = track(goal)
        val m = t.mission(gs.step)
        return Served(
            pillar = goal.homePillar,
            tier = m.step,
            directive = Copy.adapt(m.directive, s.baseline.modes),
            coach = Copy.adapt(m.coach, s.baseline.modes),
            minutes = m.minutes,
            cost = 0,
            exposure = m.exposure,
            substituted = false,
            kind = "MISSION",
            phase = t.phaseOf(m.step),
            repTarget = m.repTarget
        )
    }

    /** Everything the bubble ring reads. */
    fun ring(s: AppState): RingState =
        RingState.from(
            tiers = Pillar.order.associateWith { s.state(it).tier },
            daysSinceActive = s.daysSinceActive(today())
        )

    /**
     * Rep types available today.
     *
     * Drawn from every running campaign, de-duplicated by key. Someone running two campaigns
     * that both count conversations should see one counter, not two that mean the same thing.
     */
    fun repTypes(s: AppState) = s.runningGoals
        .flatMap { track(it).reps }
        .distinctBy { it.key }

    /** How many reps today's mission asks for. Zero means no target, reps still welcome. */
    fun repTargetToday(s: AppState): Int = serveMission(s)?.repTarget ?: 0

    // ------------------------------------------------------- routing: the extra two

    /**
     * Today's routed pair: one campaign challenge and one remediation.
     *
     * Capped at two by [Router]. With five campaigns and forty-eight audit items available,
     * an uncapped router would hand someone fourteen things on a Tuesday and lose them.
     */
    fun routed(s: AppState): Routed = Router.route(
        day = dayIndex(s),
        activeGoals = s.running,
        debuffs = audit.known(s.debuffs),
        budget = s.budget,
        pool = campaignPool,
        remediation = remediation,
        clearedRemediation = s.clearedRemediation,
        clearedCampaign = s.clearedCampaign
    )

    // ------------------------------------------------------------------ navigation

    fun goDashboard() { _route.value = Route.Dashboard }
    fun goAvatar() { _route.value = Route.AvatarStudio }
    fun goAudit() { _route.value = Route.Audit(firstRun = false) }

    /** The four-tab bar. One entry point rather than four near-identical lambdas. */
    fun selectTab(tab: Tab) {
        _route.value = when (tab) {
            Tab.TODAY -> Route.Dashboard
            Tab.LEARN -> Route.Learn
            Tab.NOTES -> Route.Notes
            Tab.PROFILE -> Route.Profile
        }
    }

    /** Which day of the user's run a given epoch day was. Used by the Notes list. */
    fun dayIndexOfDay(s: AppState, epochDay: Long): Long =
        if (s.startedDay == 0L) 1L else (epochDay - s.startedDay + 1).coerceAtLeast(1L)
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

    /** The explainer is seen once, and never blocks the app again. */
    fun finishIntro() {
        viewModelScope.launch {
            repo.update { it.copy(introSeen = true) }
            _route.value = Route.Calibration
        }
    }

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
                        xp = rolled.xp + Progress.XP_CHALLENGE,
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
            // Reaching the summit outranks the ordinary completion beat.
            val reached = repo.state.first()
            _route.value =
                if (reached.state(pillar).tier >= Engine.MAX_TIER &&
                    pillar.name !in reached.transcended
                ) {
                    Route.Transcendence(pillar)
                } else {
                    Route.Completion(pillar, tier)
                }
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
                        xp = rolled.xp + Progress.XP_FRICTION,
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
                    xp = rolled.xp + Progress.XP_MISSION,
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
                    xp = rolled.xp + Progress.XP_FRICTION,
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
                        xp = rolled.xp + Progress.XP_REP +
                            if (isFriction) Progress.XP_FRICTION else 0,
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

    /**
     * Take one rep back.
     *
     * Reps are logged with a single tap on a phone that lives in a pocket, so mis-taps are
     * inevitable and a counter with no way back is a counter people stop trusting. This
     * reverses everything the tap did — the daily count, the lifetime total, the campaign's
     * rep tally, the XP, and the friction point if it was a friction rep — and drops the log
     * line it wrote. It never goes below zero, and it will not touch a rep from a previous
     * day: yesterday's record is closed.
     */
    fun unlogRep(key: String, wasFriction: Boolean) {
        val goal = state.value.goalEnum ?: return
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val todayCount = rolled.repsToday[key] ?: 0
                if (todayCount <= 0) return@update rolled

                val totalKey = "${goal.name}:$key"
                val newToday =
                    if (todayCount == 1) rolled.repsToday - key
                    else rolled.repsToday + (key to todayCount - 1)
                val newTotal = ((rolled.repTotals[totalKey] ?: 0) - 1).coerceAtLeast(0)

                // Drop only the most recent matching entry, and only if it is from today.
                val index = rolled.log.indexOfFirst {
                    it.kind == "REP" && it.epochDay == today() && it.friction == wasFriction
                }
                val trimmedLog =
                    if (index >= 0) rolled.log.filterIndexed { i, _ -> i != index } else rolled.log

                rolled
                    .withGoal(goal, Goals.onRepUndone(rolled.goalState(goal)))
                    .copy(
                        repsToday = newToday,
                        repTotals = rolled.repTotals + (totalKey to newTotal),
                        friction = (rolled.friction - if (wasFriction) 1 else 0).coerceAtLeast(0),
                        xp = (rolled.xp - Progress.XP_REP -
                            if (wasFriction) Progress.XP_FRICTION else 0).coerceAtLeast(0),
                        log = trimmedLog
                    )
            }
        }
    }

    /** Dress the avatar. Nothing here is validated against level — the studio only offers
     *  what is already unlocked, and a saved choice is never taken away. */
    fun setAvatar(avatar: AvatarState) {
        viewModelScope.launch { repo.update { it.copy(avatar = avatar) } }
    }

    // ------------------------------------------------------------ the Systems Audit

    /**
     * Save the audit.
     *
     * Selections are filtered against the catalogue on the way in, so a saved id from an
     * older build that no longer exists cannot silently starve the remediation rotation.
     */
    fun saveAudit(selected: Set<String>, firstRun: Boolean) {
        viewModelScope.launch {
            repo.update { it.copy(debuffs = audit.known(selected), auditDone = true) }
            _route.value =
                if (firstRun) Route.GoalPicker(firstRun = true) else Route.Dashboard
        }
    }

    fun setBudget(tier: BudgetTier) {
        viewModelScope.launch { repo.update { it.copy(budgetLevel = tier.level) } }
    }

    // ------------------------------------------------------------ campaigns, in parallel

    /**
     * Turn a campaign on or off.
     *
     * Switching one off keeps every step, rep and reading it accumulated, so it can be
     * resumed months later exactly where it stopped. Nothing is ever discarded.
     */
    fun toggleGoal(goal: Goal) {
        viewModelScope.launch {
            repo.update { s ->
                val now = s.running
                val next = if (goal.name in now) now - goal.name else now + goal.name
                s.copy(
                    activeGoals = next,
                    goal = next.minOrNull(),
                    goalStates = if (s.goalStates.containsKey(goal.name)) s.goalStates
                    else s.goalStates + (goal.name to GoalState())
                ).rolledTo(today())
            }
        }
    }

    /** Leave the picker. Only reachable once at least one campaign is running. */
    fun confirmGoals() {
        viewModelScope.launch {
            if (repo.state.first().running.isEmpty()) return@launch
            _route.value = Route.Dashboard
        }
    }

    // ------------------------------------------------------------------ routed extras

    fun openRoutedCampaign() { _route.value = Route.Routed(remediation = false) }
    fun openRoutedRemediation() { _route.value = Route.Routed(remediation = true) }

    /**
     * Clear a routed extra.
     *
     * Both kinds pay the same XP as a pillar challenge and write the same kind of log line,
     * because from the user's side they were the same act. The only difference is that the
     * id is recorded so the router does not serve it again.
     */
    fun completeRouted(
        id: String,
        pillar: Pillar,
        minutes: Int,
        title: String,
        remediation: Boolean
    ) {
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled.copy(
                    clearedRemediation =
                    if (remediation) rolled.clearedRemediation + id else rolled.clearedRemediation,
                    clearedCampaign =
                    if (remediation) rolled.clearedCampaign else rolled.clearedCampaign + id,
                    routedDoneToday = rolled.routedDoneToday + id,
                    xp = rolled.xp + Progress.XP_CHALLENGE,
                    streak = streak,
                    bestStreak = maxOf(rolled.bestStreak, streak),
                    lastActiveDay = today(),
                    log = listOf(
                        LogEntry(
                            id = System.currentTimeMillis(),
                            pillar = pillar.name,
                            tier = 0,
                            title = title,
                            friction = false,
                            epochDay = today(),
                            minutes = minutes,
                            kind = if (remediation) "AUDIT" else "CAMPAIGN"
                        )
                    ) + rolled.log
                )
            }
            _pulse.value = pillar
            goDashboard()
        }
    }

    /** Friction on a routed extra. Does not mark it cleared — it comes back around. */
    fun frictionRouted(id: String, pillar: Pillar, title: String) {
        viewModelScope.launch {
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled.copy(
                    routedDoneToday = rolled.routedDoneToday + id,
                    friction = rolled.friction + 1,
                    xp = rolled.xp + Progress.XP_FRICTION,
                    streak = streak,
                    bestStreak = maxOf(rolled.bestStreak, streak),
                    lastActiveDay = today(),
                    log = listOf(
                        LogEntry(
                            id = System.currentTimeMillis(),
                            pillar = pillar.name,
                            tier = 0,
                            title = title,
                            friction = true,
                            epochDay = today(),
                            kind = "AUDIT"
                        )
                    ) + rolled.log
                )
            }
            goDashboard()
        }
    }

    // ------------------------------------------------------------------------- Learn

    fun goLearn() { _route.value = Route.Learn }

    fun openLesson(id: String) { _route.value = Route.ReadLesson(id) }

    fun markLessonRead(id: String) {
        viewModelScope.launch { repo.update { it.copy(lessonsRead = it.lessonsRead + id) } }
    }

    /** Everything readable right now, course and audit-triggered together. */
    fun lessons(s: AppState): List<Lesson> =
        learn.unlocked(dayIndex(s), audit.known(s.debuffs))

    // ------------------------------------------------------------------------- Notes

    fun goNotes() { _route.value = Route.Notes }

    fun newNote(linkLogId: Long? = null, linkLessonId: String? = null) {
        _route.value = Route.EditNote(id = null, linkLogId = linkLogId, linkLessonId = linkLessonId)
    }

    fun openNote(id: Long) {
        _route.value = Route.EditNote(id = id, linkLogId = null, linkLessonId = null)
    }

    /**
     * Write or rewrite a note.
     *
     * An empty body deletes rather than storing a blank, because a list full of empty entries
     * is how a journal stops being opened.
     */
    fun saveNote(
        id: Long?,
        body: String,
        tagsRaw: String,
        linkLogId: Long?,
        linkLessonId: String?
    ) {
        viewModelScope.launch {
            val text = body.trim()
            repo.update { s ->
                if (text.isEmpty()) {
                    return@update if (id == null) s
                    else s.copy(notes = s.notes.filterNot { it.id == id })
                }
                val tags = Notes.cleanTags(tagsRaw)
                val linked = linkLogId?.let { lid -> s.log.firstOrNull { it.id == lid } }
                val existing = id?.let { nid -> s.notes.firstOrNull { it.id == nid } }
                val note = existing?.copy(body = text, tags = tags) ?: Note(
                    id = System.currentTimeMillis(),
                    body = text,
                    epochDay = today(),
                    tags = tags,
                    linkedLogId = linkLogId,
                    linkedTitle = linked?.title ?: (linkLessonId?.let { learn.byId(it)?.title } ?: ""),
                    linkedKind = when {
                        linked?.friction == true -> "FRICTION"
                        linked != null -> linked.kind
                        linkLessonId != null -> "LESSON"
                        else -> ""
                    },
                    linkedLessonId = linkLessonId
                )
                s.copy(notes = listOf(note) + s.notes.filterNot { it.id == note.id })
            }
            goNotes()
        }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            repo.update { s -> s.copy(notes = s.notes.filterNot { it.id == id }) }
            goNotes()
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

    /** Mark an ending as seen and return to the app it has just finished arguing against. */
    fun closeTranscendence(pillar: Pillar) {
        viewModelScope.launch {
            repo.update { it.copy(transcended = it.transcended + pillar.name) }
            goDashboard()
        }
    }

    fun toggleSound() {
        viewModelScope.launch { repo.update { it.copy(soundOn = !it.soundOn) } }
    }

    fun toggleAmbient() {
        viewModelScope.launch { repo.update { it.copy(ambientOn = !it.ambientOn) } }
    }

    fun clearPulse() { _pulse.value = null }

    fun dayIndex(s: AppState): Long = s.dayIndex(today())
}
