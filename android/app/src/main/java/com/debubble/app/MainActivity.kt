package com.debubble.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.debubble.app.data.AppState
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.QuestKind
import com.debubble.app.engine.Reminders
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Today
import com.debubble.app.engine.Tour
import com.debubble.app.engine.TourAction
import com.debubble.app.engine.TourSpot
import com.debubble.app.engine.World
import com.debubble.app.ui.components.LocalTourTargets
import com.debubble.app.ui.components.RemindHost
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.TourOverlay
import com.debubble.app.ui.components.TourTargets
import com.debubble.app.ui.screens.AuditScreen
import com.debubble.app.ui.screens.CareDoneScreen
import com.debubble.app.ui.screens.CareScreen
import com.debubble.app.ui.screens.CheckInScreen
import com.debubble.app.ui.screens.CalibrationScreen
import com.debubble.app.ui.screens.ChallengeScreen
import com.debubble.app.ui.screens.CompletionScreen
import com.debubble.app.ui.screens.DashboardScreen
import com.debubble.app.ui.screens.GoalPickerScreen
import com.debubble.app.ui.screens.IntroScreen
import com.debubble.app.ui.screens.LearnScreen
import com.debubble.app.ui.screens.LessonScreen
import com.debubble.app.ui.screens.NoteEditorScreen
import com.debubble.app.ui.screens.NotesScreen
import com.debubble.app.ui.screens.PrincipleScreen
import com.debubble.app.ui.screens.ProfileScreen
import com.debubble.app.ui.screens.ReviewScreen
import com.debubble.app.ui.screens.RoomScreen
import com.debubble.app.ui.screens.Tab
import com.debubble.app.ui.screens.TabBar
import com.debubble.app.ui.screens.TranscendenceScreen
import com.debubble.app.ui.theme.DeBubbleTheme
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.LocalStage
import com.debubble.app.ui.components.rememberRoomArt
import com.debubble.app.ui.components.tourSpot
import com.debubble.app.ui.screens.CourageScreen
import com.debubble.app.ui.screens.JourneyScreen
import com.debubble.app.ui.screens.OutsideScreen
import com.debubble.app.ui.screens.WardrobeScreen
import com.debubble.app.ui.theme.accent
import com.debubble.app.widget.DayWidget
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    /**
     * What the home-screen widget asked for, if anything.
     *
     * Compose state rather than a read of `intent`, because the widget's PendingIntent carries
     * CLEAR_TOP: a second tap reuses this activity, and a composable that read the intent once
     * at startup would never see the new one. The listener is androidx's rather than an
     * `onNewIntent` override, whose parameter nullability has moved between SDK versions.
     */
    private var widgetLink by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        widgetLink = linkOf(intent)
        addOnNewIntentListener { next -> widgetLink = linkOf(next) }
        setContent {
            DeBubbleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink.Void) {
                    DeBubbleApp(
                        widgetLink = widgetLink,
                        onWidgetLinkHandled = { widgetLink = null }
                    )
                }
            }
        }
    }

    private fun linkOf(intent: Intent?): String? = when {
        intent == null -> null
        intent.hasExtra(DayWidget.EXTRA_OPEN_REVIEW) -> WIDGET_REVIEW
        else -> intent.getStringExtra(DayWidget.EXTRA_OPEN_CHALLENGE)?.takeIf { it.isNotBlank() }
    }
}

@Composable
private fun DeBubbleApp(
    widgetLink: String? = null,
    onWidgetLinkHandled: () -> Unit = {},
    vm: DeBubbleViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val route by vm.route.collectAsStateWithLifecycle()
    val pulse by vm.pulse.collectAsStateWithLifecycle()
    val tourAsked by vm.tour.collectAsStateWithLifecycle()
    // The room's sprites, decoded once for the whole app.
    val art = rememberRoomArt(vm.items)

    // Deliberately gated on the dashboard. The widget must not be able to skip the intro or
    // the calibration, and it must not fire while the state is still loading, which is when
    // "done today" and "no live plan" both read as false.
    LaunchedEffect(widgetLink, route) {
        if (widgetLink != null && route is Route.Dashboard) {
            onWidgetLinkHandled()
            vm.openFromWidget(widgetLink)
        }
    }

    // The pulse is a one-shot: clear it so re-entering the dashboard does not replay it.
    LaunchedEffect(pulse) {
        if (pulse != null) {
            delay(1400)
            vm.clearPulse()
        }
    }

    // The ground under every screen follows the habitat, so the room is the app rather than
    // a screen inside it. Screens that paint their own background — the action screen is a
    // locked focus surface — keep Ink.Void and are unaffected. The stage palette is solved
    // against the WCAG floors, so no stage can make a sentence unreadable.
    CompositionLocalProvider(LocalStage provides state.stage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(state.stage.ground))
                .statusBarsPadding()
        ) {
            when (val r = route) {
                is Route.Loading -> Box(modifier = Modifier.fillMaxSize())

                is Route.Intro -> IntroScreen(art = art, onDone = vm::finishIntro)

                is Route.Calibration -> {
                    // Reachable again from the profile, so it has to be cancellable.
                    CalibrationScreen(
                        art = art,
                        initial = state.baseline,
                        initialTier = state.budget,
                        isRecalibration = state.onboarded,
                        onDone = { baseline, tier ->
                            vm.setBudget(tier)
                            vm.finishCalibration(baseline)
                        },
                        onCancel = if (state.onboarded) vm::goDashboard else null
                    )
                }

                is Route.Audit -> {
                    if (!r.firstRun) BackHandler { vm.goDashboard() }
                    AuditScreen(
                        art = art,
                        catalogue = vm.audit,
                        initial = state.debuffs,
                        firstRun = r.firstRun,
                        onSave = { vm.saveAudit(it, r.firstRun) },
                        onBack = if (r.firstRun) null else vm::goDashboard
                    )
                }

                is Route.Dashboard -> {
                    val quests = vm.quests(state)
                    val touring = vm.touring(state, tourAsked)
                    val targets = remember { TourTargets() }
                    Box(modifier = Modifier.fillMaxSize()) {
                        CompositionLocalProvider(LocalTourTargets provides if (touring) targets else null) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    // Under the tour the screen is a picture: TalkBack reads the card,
                                    // not the rows it is pointing at.
                                    .then(if (touring) Modifier.clearAndSetSemantics { } else Modifier)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    DashboardScreen(
                                        state = state,
                                        dayIndex = vm.dayIndex(state),
                                        served = Pillar.order.associateWith { vm.serve(it, state) },
                                        quests = quests,
                                        reminderChip = { q -> vm.reminderChip(state, q) },
                                        mission = vm.serveMission(state),
                                        routed = vm.routed(state),
                                        routedDone = state.routedDoneToday,
                                        openForecasts = state.openForecasts,
                                        debuffLabel = { id -> vm.audit.debuff(id)?.label ?: "From your audit" },
                                        onOpenQuest = vm::openQuest,
                                        onOpen = vm::openChallenge,
                                        onReview = vm::openReview,
                                        onOpenRoutedCampaign = vm::openRoutedCampaign,
                                        onOpenRoutedRemediation = vm::openRoutedRemediation,
                                        onPickGoal = vm::goGoalPicker,
                                        onOpenJourney = vm::goJourney,
                                        onOpenWardrobe = { vm.goWardrobe(fromYou = false) },
                                        offerWidget = vm.shouldOfferWidget(state),
                                        onAddWidget = vm::requestWidget,
                                        onDismissWidget = vm::declineWidget,
                                        onAcknowledgeHabitat = vm::acknowledgeHabitat,
                                        art = art,
                                        library = vm.care,
                                        dirt = vm.dirt(state),
                                        welcomeBack = vm.welcomeBack(state),
                                        onRoomTap = vm::openFromRoom,
                                        onOpenRoom = vm::openRoom,
                                        onCheckIn = vm::openCheckIn
                                    )
                                }
                                TabBar(
                                    art = art,
                                    look = state.home.look,
                                    current = Tab.HOME,
                                    onSelect = vm::selectTab,
                                    modifier = Modifier.navigationBarsPadding().tourSpot(TourSpot.TABS)
                                )
                            }
                        }
                        if (touring) {
                            // Fixed for the length of the tour: nothing underneath can change it.
                            val steps = remember {
                                Tour.steps(quests, widget = !vm.widgetPlaced(), canPin = vm.canPinWidget())
                            }
                            TourOverlay(
                                steps = steps,
                                targets = targets,
                                art = art,
                                look = state.home.look,
                                outside = quests.firstOrNull { it.kind == QuestKind.OUTSIDE }?.accent ?: Ink.Gold,
                                onAction = { action ->
                                    when (action) {
                                        TourAction.START_NEXT -> {
                                            val next = Today.next(quests)
                                            vm.endTour()
                                            if (next != null) vm.openQuest(next)
                                        }
                                        TourAction.ADD_WIDGET -> vm.requestWidget()
                                    }
                                },
                                onDone = vm::endTour
                            )
                        }
                    }
                }

                is Route.Profile -> {
                    BackHandler { vm.goDashboard() }
                    Tabbed(art, state, Tab.YOU, vm::selectTab) {
                        ProfileScreen(
                            art = art,
                            state = state,
                            dayIndex = vm.dayIndex(state),
                            showTip = vm.showTip(state, TIP_YOU),
                            onTipSeen = { vm.tipSeen(TIP_YOU) },
                            onOpenWardrobe = { vm.goWardrobe(fromYou = true) },
                            onRecalibrate = vm::goRecalibrate,
                            onChangeGoal = vm::goGoalPicker,
                            onOpenAudit = vm::goAudit,
                            onSetBudget = vm::setBudget,
                            onToggleSound = vm::toggleSound,
                            onToggleAmbient = vm::toggleAmbient,
                            widgetPlaced = vm.widgetPlaced(),
                            onAddWidget = vm::requestWidget,
                            onReplayTour = vm::replayTour
                        )
                    }
                }

                is Route.Outside -> {
                    BackHandler { vm.goDashboard() }
                    Tabbed(art, state, Tab.OUTSIDE, vm::selectTab) {
                        OutsideScreen(
                            art = art,
                            state = state,
                            served = Pillar.order.associateWith { vm.serve(it, state) },
                            todays = vm.todaysRoad(state),
                            showTip = vm.showTip(state, TIP_OUTSIDE),
                            onTipSeen = { vm.tipSeen(TIP_OUTSIDE) },
                            onOpenRoad = vm::openChallenge,
                            onOpenJourney = vm::goJourney,
                            onPickGoal = vm::goGoalPicker,
                            mission = vm.serveMission(state)
                        )
                    }
                }

                is Route.Wardrobe -> {
                    val back = { if (r.fromYou) vm.goProfile() else vm.goDashboard() }
                    BackHandler { back() }
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        WardrobeScreen(
                            art = art,
                            look = state.home.look,
                            level = state.level,
                            onLook = vm::setLook,
                            onBack = back
                        )
                    }
                }

                is Route.Challenge -> {
                    BackHandler { vm.abort() }
                    // No tab bar here on purpose: a letter is one thing at a time.
                    val served = vm.serve(r.pillar, state)
                    // Stable across the day so a guess written this morning is still attached
                    // to the same challenge this evening.
                    val challengeKey = "${Today.TIER}${r.pillar.name}:${served.tier}"
                    val todays = vm.todaysRoad(state) == r.pillar
                    Box(modifier = Modifier.fillMaxSize()) {
                        ChallengeScreen(
                            art = art,
                            served = served,
                            kicker = "${World.roadName(r.pillar)} · level ${served.tier}",
                            soundOn = state.soundOn,
                            ambientOn = state.ambientOn,
                            forecast = state.forecastFor(challengeKey),
                            remind = vm.remindHost(
                                s = state,
                                link = challengeKey,
                                what = if (todays) "Today's letter · ${World.roadName(r.pillar)}" else World.roadName(r.pillar),
                                text = served.directive,
                                daily = if (todays) "Every day, for that day's letter" else null,
                                dailyLink = if (todays) Reminders.OUTSIDE else challengeKey
                            ),
                            onPredict = { text, distress -> vm.savePrediction(challengeKey, served.directive, text, distress) },
                            onCommit = { minutes ->
                                vm.complete(
                                    pillar = r.pillar,
                                    minutes = minutes,
                                    tier = served.tier,
                                    title = served.directive
                                )
                            },
                            onFriction = { reason ->
                                vm.logFriction(
                                    pillar = r.pillar,
                                    tier = served.tier,
                                    title = served.directive,
                                    reason = reason
                                )
                            },
                            onSwap = if (r.pillar.name !in state.swappedToday) {
                                { vm.swap(r.pillar) }
                            } else null,
                            onBack = vm::abort,
                            firstTime = state.neverOut
                        )
                    }
                }

                is Route.GoalPicker -> {
                    if (!r.firstRun) BackHandler { vm.goDashboard() }
                    GoalPickerScreen(
                        art = art,
                        running = state.running,
                        firstRun = r.firstRun,
                        progressOf = { state.goalState(it) },
                        onToggle = vm::toggleGoal,
                        onConfirm = vm::confirmGoals,
                        onCancel = if (r.firstRun) null else vm::goDashboard
                    )
                }

                is Route.Mission -> {
                    BackHandler { vm.abort() }
                    val mission = vm.serveMission(state)
                    if (mission == null) {
                        LaunchedEffect(Unit) { vm.goDashboard() }
                    } else {
                        val missionKey = "mission:${mission.tier}"
                        Box(modifier = Modifier.fillMaxSize()) {
                            ChallengeScreen(
                                art = art,
                                served = mission,
                                kicker = "Stop ${mission.tier} of 30" + (mission.phase?.let { " · $it" } ?: ""),
                                soundOn = state.soundOn,
                                ambientOn = state.ambientOn,
                                forecast = state.forecastFor(missionKey),
                                remind = vm.remindHost(
                                    s = state,
                                    link = missionKey,
                                    what = "Your journey · stop ${mission.tier}",
                                    text = mission.directive
                                ),
                                onPredict = { text, distress -> vm.savePrediction(missionKey, mission.directive, text, distress) },
                                onCommit = { minutes ->
                                    vm.completeMission(
                                        minutes = minutes,
                                        step = mission.tier,
                                        title = mission.directive
                                    )
                                },
                                onFriction = { reason ->
                                    vm.missionFriction(step = mission.tier, title = mission.directive, reason = reason)
                                },
                                onSwap = null,
                                onBack = vm::abort,
                                firstTime = state.neverOut
                            )
                        }
                    }
                }

                is Route.Journey -> {
                    BackHandler { vm.goDashboard() }
                    val goal = state.primaryGoal
                    if (goal == null) {
                        LaunchedEffect(Unit) { vm.goGoalPicker() }
                    } else {
                        val stop = vm.serveMission(state)
                        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                            JourneyScreen(
                                art = art,
                                look = state.home.look,
                                goal = goal,
                                track = vm.track(goal),
                                state = state.goalState(goal),
                                stop = stop,
                                stopDoneToday = state.missionDoneToday,
                                reps = vm.repTypes(state),
                                repsToday = state.repsToday,
                                repTarget = stop?.repTarget ?: 0,
                                repsLifetime = state.repsOnGoal(goal),
                                showTip = vm.showTip(state, TIP_JOURNEY),
                                onTipSeen = { vm.tipSeen(TIP_JOURNEY) },
                                onOpenStop = vm::openMission,
                                onLogRep = { rep -> vm.logRep(rep.key, rep.friction, rep.label) },
                                onUndoRep = { rep -> vm.unlogRep(rep.key, rep.friction) },
                                onOpenPrinciple = vm::openPrinciple,
                                onChangeGoal = vm::goGoalPicker,
                                onBack = vm::goDashboard
                            )
                        }
                    }
                }

                is Route.ReadPrinciple -> {
                    BackHandler { vm.goJourney() }
                    val goal = state.primaryGoal
                    val principle = goal?.let { vm.track(it).principles.getOrNull(r.index) }
                    if (goal == null || principle == null) {
                        LaunchedEffect(Unit) { vm.goJourney() }
                    } else {
                        LaunchedEffect(r.index) { vm.markRead(r.index) }
                        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                            PrincipleScreen(
                                goal = goal,
                                principle = principle,
                                onDone = vm::goJourney
                            )
                        }
                    }
                }

                is Route.Transcendence -> {
                    // No BackHandler: this one is closed deliberately or not at all.
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        TranscendenceScreen(
                            pillar = r.pillar,
                            onClose = { vm.closeTranscendence(r.pillar) }
                        )
                    }
                }

                is Route.Learn -> {
                    BackHandler { vm.goDashboard() }
                    Tabbed(art, state, Tab.LIBRARY, vm::selectTab) {
                        LearnScreen(
                            art = art,
                            showTip = vm.showTip(state, TIP_LIBRARY),
                            onTipSeen = { vm.tipSeen(TIP_LIBRARY) },
                            curriculum = vm.learn,
                            catalogue = vm.audit,
                            dayIndex = vm.dayIndex(state),
                            debuffs = state.debuffs,
                            read = state.lessonsRead,
                            onOpen = vm::openLesson
                        )
                    }
                }

                is Route.ReadLesson -> {
                    BackHandler { vm.goLearn() }
                    val lesson = vm.learn.byId(r.id)
                    if (lesson == null) {
                        LaunchedEffect(Unit) { vm.goLearn() }
                    } else {
                        // Opening it is reading it. A separate "mark as read" that people forget
                        // to press just makes the counter lie.
                        LaunchedEffect(r.id) { vm.markLessonRead(r.id) }
                        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                            LessonScreen(
                                lesson = lesson,
                                isRead = r.id in state.lessonsRead,
                                onAddNote = { vm.newNote(linkLessonId = r.id) },
                                onBack = vm::goLearn
                            )
                        }
                    }
                }

                is Route.Notes -> {
                    BackHandler { vm.goDashboard() }
                    Tabbed(art, state, Tab.JOURNAL, vm::selectTab) {
                        NotesScreen(
                            art = art,
                            notes = state.notes,
                            dayIndexOf = { note -> vm.dayIndexOfDay(state, note.epochDay) },
                            showTip = vm.showTip(state, TIP_JOURNAL),
                            onTipSeen = { vm.tipSeen(TIP_JOURNAL) },
                            onOpen = vm::openNote,
                            onNew = { vm.newNote() }
                        )
                    }
                }

                is Route.EditNote -> {
                    BackHandler { vm.goNotes() }
                    val existing = r.id?.let { id -> state.notes.firstOrNull { it.id == id } }
                    val linkedEntry = r.linkLogId?.let { id -> state.log.firstOrNull { it.id == id } }
                    val linkedKind = when {
                        existing != null -> existing.linkedKind
                        linkedEntry?.friction == true -> "FRICTION"
                        linkedEntry != null -> linkedEntry.kind
                        r.linkLessonId != null -> "LESSON"
                        else -> ""
                    }
                    val linkedTitle = existing?.linkedTitle
                        ?: linkedEntry?.title
                        ?: r.linkLessonId?.let { vm.learn.byId(it)?.title }
                        ?: ""
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        NoteEditorScreen(
                            existing = existing,
                            linkedTitle = linkedTitle,
                            linkedKind = linkedKind,
                            onSave = { body, tags ->
                                vm.saveNote(r.id, body, tags, r.linkLogId, r.linkLessonId)
                            },
                            onDelete = r.id?.let { id -> { vm.deleteNote(id) } },
                            onBack = vm::goNotes
                        )
                    }
                }

                is Route.Routed -> {
                    BackHandler { vm.goDashboard() }
                    val today = vm.routed(state)
                    val campaign = today.campaign
                    val fix = today.remediation
                    val served = when {
                        r.remediation && fix != null ->
                            fix.toServed(vm.audit.debuff(fix.debuff)?.label ?: "From your audit")
                        !r.remediation && campaign != null -> campaign.toServed()
                        else -> null
                    }
                    val id = if (r.remediation) fix?.id else campaign?.id
                    if (served == null || id == null) {
                        LaunchedEffect(Unit) { vm.goDashboard() }
                    } else {
                        val link = if (r.remediation) FOCUS_FIX else FOCUS_CAMPAIGN
                        Box(modifier = Modifier.fillMaxSize()) {
                            ChallengeScreen(
                                art = art,
                                served = served,
                                kicker = served.phase ?: "An extra, picked for you",
                                soundOn = state.soundOn,
                                ambientOn = state.ambientOn,
                                forecast = state.forecastFor(id),
                                remind = vm.remindHost(
                                    s = state,
                                    link = link,
                                    what = "An extra for today",
                                    text = served.directive
                                ),
                                onPredict = { text, distress -> vm.savePrediction(id, served.directive, text, distress) },
                                onCommit = { minutes ->
                                    vm.completeRouted(
                                        id = id,
                                        pillar = served.pillar,
                                        minutes = minutes,
                                        title = served.directive,
                                        remediation = r.remediation
                                    )
                                },
                                onFriction = { reason ->
                                    vm.frictionRouted(id, served.pillar, served.directive, reason)
                                },
                                onSwap = null,
                                onBack = vm::goDashboard,
                                firstTime = state.neverOut
                            )
                        }
                    }
                }

                is Route.Review -> {
                    BackHandler { vm.goDashboard() }
                    val forecast = state.forecasts.firstOrNull { it.id == r.forecastId }
                    if (forecast == null) {
                        LaunchedEffect(Unit) { vm.goDashboard() }
                    } else {
                        Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                            ReviewScreen(
                                forecast = forecast,
                                onResolve = { actual, distress ->
                                    vm.resolveForecast(r.forecastId, actual, distress)
                                },
                                onDismiss = { vm.dismissForecast(r.forecastId) },
                                onBack = vm::goDashboard
                            )
                        }
                    }
                }

                is Route.CheckIn -> {
                    if (!r.firstRun) BackHandler { vm.goDashboard() }
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        CheckInScreen(
                            firstRun = r.firstRun,
                            art = art,
                            library = vm.care,
                            stage = state.stage,
                            home = state.home,
                            // A first move-in starts from the middle rather than from a guess.
                            initial = if (r.firstRun) Domain.order.associateWith { 1 } else vm.dirt(state),
                            onLook = vm::setLook,
                            onSave = { levels -> vm.saveCheckIn(levels, r.firstRun) },
                            onBack = if (r.firstRun) null else vm::goDashboard
                        )
                    }
                }

                is Route.Care -> {
                    BackHandler { vm.goDashboard() }
                    val d = r.domain
                    val quest = vm.quests(state).first { it.domain == d }
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        CareScreen(
                            domain = d,
                            library = vm.care,
                            care = state.care(d),
                            level = vm.dirt(state).getValue(d),
                            doneToday = vm.careDoneToday(state, d),
                            suggested = vm.suggestedSize(state, d),
                            sizeNote = vm.sizeNote(state, d),
                            onChooseRoutine = { id -> vm.chooseRoutine(d, id) },
                            onSetAnchor = { a -> vm.setAnchor(d, a) },
                            onDone = { size -> vm.completeCare(d, size) },
                            onBack = vm::goDashboard,
                            art = art,
                            remind = vm.remindHost(
                                s = state,
                                link = quest.link,
                                what = quest.title,
                                text = quest.line,
                                daily = "Every day",
                                dailyFirst = true
                            ),
                            firstTime = state.neverCared
                        )
                    }
                }

                is Route.CareDone -> {
                    BackHandler { vm.recordFeeling(r.domain, null) }
                    val after = vm.dirt(state)
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        CareDoneScreen(
                            art = art,
                            library = vm.care,
                            stage = state.stage,
                            home = state.home,
                            domain = r.domain,
                            care = state.care(r.domain),
                            dirtBefore = after + (r.domain to r.before),
                            dirtAfter = after,
                            granted = r.granted,
                            celebrate = r.celebrate,
                            onFeel = { feel -> vm.recordFeeling(r.domain, feel) }
                        )
                    }
                }

                is Route.Room -> {
                    BackHandler { vm.goDashboard() }
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        RoomScreen(
                            art = art,
                            stage = state.stage,
                            home = state.home,
                            dirt = vm.dirt(state),
                            rungs = state.clearedRungs,
                            careReps = Domain.order.associateWith { state.care(it).reps },
                            onMove = { id, x, b ->
                                if (id == Scene.ME) vm.moveMe(x, b) else vm.moveItem(id, x, b)
                            },
                            onPutAway = vm::putAway,
                            onPutBack = vm::putBack,
                            onLook = vm::setLook,
                            letter = Today.letter(vm.quests(state)),
                            widgetPlaced = vm.widgetPlaced(),
                            canPinWidget = vm.canPinWidget(),
                            onAddWidget = vm::requestWidget,
                            onSeen = vm::markRoomSeen,
                            onBack = vm::goDashboard
                        )
                    }
                }

                is Route.Completion -> {
                    // Backing out of the postcard is the same as leaving it blank.
                    BackHandler { vm.goDashboard() }
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        CompletionScreen(
                            art = art,
                            stage = state.stage,
                            home = state.home,
                            dirt = vm.dirt(state),
                            pillar = r.pillar,
                            tierCleared = r.tierCleared,
                            reward = r.reward,
                            forecast = state.forecastFor(r.key),
                            onFinish = { note, actual, distress -> vm.finishMission(r.key, note, actual, distress) }
                        )
                    }
                }

                is Route.Courage -> {
                    BackHandler { vm.goDashboard() }
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        CourageScreen(art = art, friction = r.friction, reason = r.reason, onDone = vm::goDashboard)
                    }
                }
            }
        }
    }
}

/** First-visit tips, by place. */
private const val TIP_OUTSIDE = "outside"
private const val TIP_JOURNAL = "journal"
private const val TIP_LIBRARY = "library"
private const val TIP_YOU = "you"
private const val TIP_JOURNEY = "journey"

/** A screen with the tab bar under it. */
@Composable
private fun Tabbed(
    art: RoomArt,
    state: AppState,
    tab: Tab,
    onSelect: (Tab) -> Unit,
    content: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) { content() }
        TabBar(
            art = art,
            look = state.home.look,
            current = tab,
            onSelect = onSelect,
            modifier = Modifier.navigationBarsPadding()
        )
    }
}

/**
 * What a task's bell needs: the reminder already set, the quick times, and what to do when
 * one is picked. [dailyLink] is where an every-day reminder lives when it is not the same as
 * the one-off's — today's mission changes every day, so "every day" is for whichever it is.
 */
private fun DeBubbleViewModel.remindHost(
    s: AppState,
    link: String,
    what: String,
    text: String,
    daily: String? = null,
    dailyLink: String = link,
    dailyFirst: Boolean = false
): RemindHost {
    val links = listOf(link, dailyLink).distinct()
    return RemindHost(
        what = what,
        current = reminderFor(s, link) ?: reminderFor(s, dailyLink),
        choices = reminderChoices(),
        today = todayDay(),
        h24 = h24(),
        dailyOption = daily,
        dailyFirst = dailyFirst,
        onSet = { day, minute, every ->
            val target = if (every) dailyLink else link
            setReminder(target, what, text, day, minute, every, replaces = links.filter { it != target })
        },
        onRemove = { removeReminder(*links.toTypedArray()) }
    )
}
