package com.debubble.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.data.Repository
import com.debubble.app.data.Note
import com.debubble.app.data.Notes
import com.debubble.app.data.dirt
import com.debubble.app.data.outsidePillar
import com.debubble.app.data.quests
import com.debubble.app.data.serve
import com.debubble.app.engine.AuditCatalogue
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.BudgetTier
import com.debubble.app.engine.CampaignPool
import com.debubble.app.engine.Care
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Family
import com.debubble.app.engine.Feel
import com.debubble.app.engine.Home
import com.debubble.app.engine.ItemCatalogue
import com.debubble.app.engine.Size
import com.debubble.app.engine.Spot
import com.debubble.app.engine.Cues
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Forecasts
import com.debubble.app.engine.Intention
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
import com.debubble.app.engine.Protocol
import com.debubble.app.engine.Quest
import com.debubble.app.engine.RoomTap
import com.debubble.app.engine.Today
import com.debubble.app.engine.Look
import com.debubble.app.engine.Served
import com.debubble.app.widget.DayWidget
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
    /** [reward] names the WORLD item a mission just earned, if one arrived. */
    data class Completion(val pillar: Pillar, val tierCleared: Int, val reward: String? = null) : Route
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
    /** Writing the if-then plan and the prediction, before doing the thing. */
    data class Plan(
        val challengeId: String,
        val title: String,
        /** The challenge's own anchor, opener and prediction, carried through to pre-fill. */
        val cue: String = "",
        val response: String = "",
        val prediction: String = ""
    ) : Route
    /** Checking the prediction against what happened, after doing it. */
    data class Review(val forecastId: Long) : Route

    /* ---- the room as a mirror ---- */

    /** The honest check-in. First run is "moving in"; after that it is reachable any time. */
    data class CheckIn(val firstRun: Boolean) : Route
    /** One care routine: pick the size, read why, do it, say so. */
    data class Care(val domain: Domain) : Route
    /** The moment after: the room cleans, anything new arrives, and how did it feel? */
    data class CareDone(
        val domain: Domain,
        val before: Int,
        val after: Int,
        val granted: List<String>,
        val celebrate: String
    ) : Route
    /** The room, full screen, for moving things around. */
    data object Room : Route
}

/** The widget's "check your prediction" link. Matched, not parsed. */
const val WIDGET_REVIEW = "review"

/** A widget link that opens the room to arrange it. */
const val LINK_ROOM = "room"

// `app` is a property, not just a constructor argument: the widget helpers below need a
// context after construction, and a stored application reference is the one kind that is
// safe to hold in a view model.
class DeBubbleViewModel(private val app: Application) : AndroidViewModel(app) {

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

    /** The care routines and the room's item catalogue. */
    val care: CareLibrary = repo.loadCare()
    val items: ItemCatalogue = repo.loadItems()

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
            repo.update { rolled ->
                val s = rolled.rolledTo(today())
                // Seed the habitat on first sight rather than leaving it empty, so an
                // install that upgrades into this feature already several stages in does
                // not open with a banner announcing a room it has always had.
                val seen = if (s.habitatSeen.isEmpty()) s.copy(habitatSeen = s.stage.name) else s
                // Missions already cleared have earned their WORLD items. Settling is derived
                // from the count, so this grants an upgrader what they are owed and is a
                // no-op every launch after.
                settleWorld(seen).first
            }
            val loaded = repo.state.first()
            _route.value = when {
                !loaded.introSeen -> Route.Intro
                !loaded.onboarded -> Route.Calibration
                !loaded.auditDone -> Route.Audit(firstRun = true)
                loaded.running.isEmpty() -> Route.GoalPicker(firstRun = true)
                !loaded.movedIn -> Route.CheckIn(firstRun = true)
                else -> Route.Dashboard
            }
        }
    }

    // ------------------------------------------------------------------ serving

    fun serve(pillar: Pillar, s: AppState): Served = s.serve(pillar, curriculum)

    /** Today's four: body, room, heart, and one mission outside. See engine/Today.kt. */
    fun quests(s: AppState): List<Quest> = s.quests(today(), care, curriculum)

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
            repTarget = m.repTarget,
            anchor = Protocol.genericAnchor(goal.homePillar),
            celebration = Protocol.celebration(goal.homePillar, m.step)
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

    /** If-then plans whose moment has not yet passed. */
    fun livePlans(s: AppState): List<Intention> = s.liveIntentions(today())

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

    /**
     * Arrived from the home-screen widget.
     *
     * Only ever called once the app has settled on the dashboard, so it cannot jump over
     * onboarding or the calibration. Unrecognised links do nothing rather than guess — a
     * widget placed before an update and tapped after one must not drop someone into a
     * screen that no longer means what it did.
     */
    fun openFromWidget(link: String) = openLink(link)

    /**
     * Follow a link from a quest, the room or the widget: "care:BODY", "tier:SOCIAL:4",
     * "mission:…", "room", or the review marker. Anything it does not recognise opens
     * nothing, which on the dashboard is the right thing to do.
     */
    private fun openLink(link: String) {
        val s = state.value
        when {
            link.startsWith(Today.CARE) -> Domain.from(link.removePrefix(Today.CARE))?.let { openCare(it) }
            link == LINK_ROOM -> openRoom()
            link == WIDGET_REVIEW -> {
                // The oldest unchecked prediction, because that is the one most at risk of
                // being forgotten entirely.
                s.openForecasts.minByOrNull { it.createdDay }?.let { openReview(it.id) }
            }
            link.startsWith("tier:") -> {
                val pillar = link.split(':').getOrNull(1)
                    ?.let { name -> Pillar.order.firstOrNull { it.name == name } }
                if (pillar != null && !s.isDoneToday(pillar)) openChallenge(pillar)
            }
            link.startsWith("mission:") -> if (!s.missionDoneToday) _route.value = Route.Mission
        }
    }

    // ------------------------------------------------------------------ the tour

    /**
     * Whether the tour is showing, as asked for in this session: true when replayed from
     * You, false once closed. Null follows the saved flag, so it shows by itself only until
     * it has been finished or skipped once.
     */
    private val _tour = MutableStateFlow<Boolean?>(null)
    val tour: StateFlow<Boolean?> = _tour

    fun touring(s: AppState, asked: Boolean?): Boolean = s.movedIn && (asked ?: !s.tourSeen)

    fun replayTour() {
        _tour.value = true
        goDashboard()
    }

    /** Finished or skipped. Hidden at once, rather than when the write lands. */
    fun endTour() {
        _tour.value = false
        viewModelScope.launch { repo.update { it.copy(tourSeen = true) } }
    }

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
            // Missions earn WORLD things for the room, settled in the same write as the rung.
            var arrived: String? = null
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val advanced = Engine.onCompleted(rolled.state(pillar), minutes)
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                val updated = rolled
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
                val (settled, granted) = settleWorld(updated)
                arrived = granted.lastOrNull()?.label
                settled
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
                    Route.Completion(pillar, tier, arrived)
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
            val s = repo.state.first()
            if (s.running.isEmpty()) return@launch
            _route.value = if (s.movedIn) Route.Dashboard else Route.CheckIn(firstRun = true)
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

    // --------------------------------------------------------- plans and predictions

    fun goPlan(challengeId: String, served: Served) {
        _route.value = Route.Plan(
            challengeId = challengeId,
            title = served.directive,
            cue = served.anchor,
            response = served.opener,
            prediction = served.test
        )
    }

    /**
     * Save the plan and the prediction together.
     *
     * One screen, because they belong to the same moment and splitting them would mean two
     * interruptions before a challenge someone is already reluctant to start. The plan is
     * what gets it done; the prediction is what makes the doing teach something.
     */
    fun savePlan(
        challengeId: String,
        title: String,
        cue: String,
        response: String,
        predicted: String,
        predictedDistress: Int
    ) {
        viewModelScope.launch {
            repo.update { s ->
                val now = System.currentTimeMillis()
                val plan = Intention(
                    id = now,
                    challengeId = challengeId,
                    challengeTitle = title,
                    cue = cue.trim(),
                    response = response.trim(),
                    createdDay = today(),
                    dueDay = today() + Cues.WINDOW_DAYS
                )
                val forecast = Forecast(
                    id = now + 1,
                    challengeId = challengeId,
                    challengeTitle = title,
                    predicted = predicted.trim(),
                    predictedDistress = predictedDistress.coerceIn(0, Forecasts.SCALE),
                    createdDay = today()
                )
                s.copy(
                    // Replacing rather than appending: re-planning the same challenge is an
                    // edit, not a second commitment to the same act.
                    intentions = s.intentions.filterNot {
                        it.challengeId == challengeId && !it.resolved
                    } + plan,
                    forecasts = s.forecasts.filterNot {
                        it.challengeId == challengeId && !it.isResolved
                    } + forecast
                )
            }
            goDashboard()
        }
    }

    fun openReview(forecastId: Long) { _route.value = Route.Review(forecastId) }

    /**
     * Close the loop: what actually happened, and how bad it actually was.
     *
     * This writes the only number in the app that measures the product's actual claim — the
     * gap between what someone was sure would happen and what did.
     */
    fun resolveForecast(forecastId: Long, actual: String, actualDistress: Int) {
        viewModelScope.launch {
            repo.update { s ->
                val f = s.forecasts.firstOrNull { it.id == forecastId } ?: return@update s
                val resolved = f.copy(
                    actual = actual.trim(),
                    actualDistress = actualDistress.coerceIn(0, Forecasts.SCALE),
                    resolvedDay = today()
                )
                s.copy(
                    forecasts = s.forecasts.map { if (it.id == forecastId) resolved else it },
                    intentions = s.intentions.map {
                        if (it.challengeId == f.challengeId && !it.resolved) it.copy(resolved = true)
                        else it
                    }
                )
            }
            goDashboard()
        }
    }

    /** Drop a prediction without answering it. Never nag twice about the same thing. */
    fun dismissForecast(forecastId: Long) {
        viewModelScope.launch {
            repo.update { s -> s.copy(forecasts = s.forecasts.filterNot { it.id == forecastId }) }
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

    /* ---------------------------------------------------------------- the widget */

    /**
     * Whether to offer the home-screen widget on the dashboard.
     *
     * Four conditions, and all four matter. Not already placed, because offering someone
     * something they have is noise. The launcher must support pinning, because asking and
     * then failing is worse than never asking. Something must have been completed, because
     * asking for home-screen space on day one is asking before the app has earned any. And
     * it is asked once — the launcher never reports back whether the user accepted, so a
     * card that waited to detect a widget would wait forever for everyone who declined.
     */
    fun shouldOfferWidget(s: AppState): Boolean =
        !s.widgetOffered &&
            s.log.any { !it.friction } &&
            DayWidget.canRequestPin(app) &&
            !DayWidget.isInstalled(app)

    fun canPinWidget(): Boolean = DayWidget.canRequestPin(app)

    /** Hand it to the launcher, which shows its own dialog. We are told nothing either way. */
    fun requestWidget() {
        DayWidget.requestPin(app)
        viewModelScope.launch { repo.update { it.copy(widgetOffered = true) } }
    }

    fun declineWidget() {
        viewModelScope.launch { repo.update { it.copy(widgetOffered = true) } }
    }

    /** True once one is actually on a home screen, for the profile's wording. */
    fun widgetPlaced(): Boolean = DayWidget.isInstalled(app)

    /** The user has seen the room open. Shown once; there is no way to lose a stage. */
    fun acknowledgeHabitat() {
        viewModelScope.launch { repo.update { it.copy(habitatSeen = it.stage.name) } }
    }


    // ------------------------------------------------------------------ the room

    /** Grant whatever WORLD items cleared rungs have earned. Pure; returns what arrived. */
    private fun settleWorld(s: AppState) =
        Home.settle(s.home, items, Family.WORLD, s.clearedRungs, s.rewardSeed).let { (home, granted) ->
            s.copy(home = home) to granted
        }

    fun openCheckIn() { _route.value = Route.CheckIn(firstRun = false) }
    fun openCare(domain: Domain) { _route.value = Route.Care(domain) }
    fun openRoom() { _route.value = Route.Room }

    /** Today's level for each domain. What the room draws and what the cards say. */
    fun dirt(s: AppState): Map<Domain, Int> = s.dirt(today())

    fun careDoneToday(s: AppState, d: Domain): Boolean = Care.doneToday(s.care(d), today())

    fun suggestedSize(s: AppState, d: Domain): Size = Care.suggestedSize(s.care(d), today())

    fun sizeNote(s: AppState, d: Domain): String? = Care.sizeNote(s.care(d), today())

    fun welcomeBack(s: AppState): String? = Care.welcomeBack(s.daysSinceCare(today()))

    /**
     * Save the honest check-in.
     *
     * Moving in for the first time also fixes the surprise seed, so the order things arrive
     * in is this person's own rather than everyone's.
     */
    fun saveCheckIn(levels: Map<Domain, Int>, firstRun: Boolean) {
        viewModelScope.launch {
            repo.update { s ->
                var next = s
                levels.forEach { (d, level) -> next = next.withCare(d, Care.checkIn(next.care(d), level, today())) }
                next.copy(
                    movedIn = true,
                    homeSeed = if (s.homeSeed != 0L) s.homeSeed else System.currentTimeMillis()
                )
            }
            goDashboard()
        }
    }

    fun chooseRoutine(domain: Domain, id: String) {
        val r = care.byId(id)?.takeIf { it.domain == domain.name } ?: return
        viewModelScope.launch {
            repo.update { s ->
                val cs = s.care(domain)
                if (care.chosen(domain, cs).id == r.id) return@update s
                // A new routine means a new cue, because the old one belonged to a different
                // act, and a new automaticity count, because so did the habit.
                s.withCare(domain, cs.copy(routine = r.id, anchor = "", routineFrom = cs.reps))
            }
        }
    }

    fun setAnchor(domain: Domain, anchor: String) {
        viewModelScope.launch {
            repo.update { s -> s.withCare(domain, s.care(domain).copy(anchor = anchor.trim())) }
        }
    }

    /**
     * The routine was done.
     *
     * Cleans the domain (one level for tiny, fully otherwise), counts the day's rep once,
     * settles whatever the family is now owed, and pays XP and the streak like any other
     * real act. Then the done screen, where the room visibly changes.
     */
    fun completeCare(domain: Domain, size: Size) {
        viewModelScope.launch {
            var before = 0
            var after = 0
            var granted: List<String> = emptyList()
            var celebrate = ""
            repo.update { s ->
                val rolled = s.rolledTo(today())
                val cs = rolled.care(domain)
                val routine = care.chosen(domain, cs)
                before = Care.dirt(domain, cs, today())
                val done = Care.afterCare(domain, cs, size, today())
                after = Care.dirt(domain, done, today())
                celebrate = routine.celebrate
                val (home, arrived) = Home.settle(
                    rolled.home, items, domain.family, done.reps, rolled.rewardSeed
                )
                granted = arrived.map { it.id }
                val streak = Engine.updateStreak(rolled.streak, rolled.lastActiveDay, today())
                rolled.withCare(domain, done).copy(
                    home = home,
                    // Only the first of the day pays, matching the rep. A second session is
                    // welcome and cleans the room, but it is not a way to farm levels.
                    xp = rolled.xp + if (cs.lastDoneDay != today()) Progress.XP_CARE else 0,
                    streak = streak,
                    bestStreak = maxOf(rolled.bestStreak, streak),
                    lastActiveDay = today(),
                    log = listOf(
                        LogEntry(
                            id = System.currentTimeMillis(),
                            pillar = domain.name,
                            tier = done.reps,
                            title = routine.title,
                            note = size.label,
                            epochDay = today(),
                            minutes = routine.minutes(size),
                            kind = "CARE"
                        )
                    ) + rolled.log
                )
            }
            _route.value = Route.CareDone(domain, before, after, granted, celebrate)
        }
    }

    /** The one optional question afterwards. Skipping it records nothing. */
    fun recordFeeling(domain: Domain, feel: Feel?) {
        viewModelScope.launch {
            if (feel != null) {
                repo.update { s -> s.withCare(domain, Care.afterFeeling(s.care(domain), feel)) }
            }
            goDashboard()
        }
    }

    /** Move an item. The spot is clamped to the floor or the wall, whichever it belongs to. */
    fun moveItem(id: String, x: Int, b: Int) {
        val def = items.item(id) ?: return
        val spot = Home.clamp(items.room, def.w, def.h, def.onWall, x, b)
        viewModelScope.launch {
            repo.update { s ->
                if (id !in s.home.owned) s
                else s.copy(home = s.home.copy(placed = s.home.placed + (id to spot)))
            }
        }
    }

    fun moveMe(x: Int, b: Int) {
        val spot = Home.clamp(items.room, items.me.w, items.me.h, false, x, b)
        viewModelScope.launch { repo.update { s -> s.copy(home = s.home.copy(me = spot)) } }
    }

    /** Take an item out of the room. Still owned; it waits in the tray. */
    fun putAway(id: String) {
        viewModelScope.launch {
            repo.update { s -> s.copy(home = s.home.copy(placed = s.home.placed - id)) }
        }
    }

    /** Put an item back, at the spot it first arrived in. */
    fun putBack(id: String) {
        val def = items.item(id) ?: return
        viewModelScope.launch {
            repo.update { s ->
                if (id !in s.home.owned) s
                else s.copy(home = s.home.copy(placed = s.home.placed + (id to def.spot)))
            }
        }
    }

    /** How you look. Any time, from moving in or from the room. */
    fun setLook(look: Look) {
        viewModelScope.launch { repo.update { s -> s.copy(home = s.home.copy(look = look)) } }
    }

    /**
     * A tap on something in the room that is a task: you, the mess, your heart, the letter.
     * A mission already done today has no letter to tap, so OUTSIDE only ever opens one
     * that is still open.
     */
    fun openFromRoom(tap: RoomTap) {
        val s = state.value
        when (tap) {
            RoomTap.BODY -> openCare(Domain.BODY)
            RoomTap.ROOM -> openCare(Domain.ROOM)
            RoomTap.HEART -> openCare(Domain.HEART)
            RoomTap.OUTSIDE -> s.outsidePillar(today()).let { if (!s.isDoneToday(it)) openChallenge(it) }
        }
    }

    /** Open whatever a quest row points at. */
    fun openQuest(q: Quest) = openLink(q.link)

    /** New arrivals have been seen in the room. */
    fun markRoomSeen() {
        viewModelScope.launch {
            repo.update { s ->
                if (s.home.unseen.isEmpty()) s else s.copy(home = s.home.copy(unseen = emptySet()))
            }
        }
    }

    fun clearPulse() { _pulse.value = null }

    fun dayIndex(s: AppState): Long = s.dayIndex(today())
}
