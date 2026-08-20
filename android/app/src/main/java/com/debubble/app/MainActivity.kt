package com.debubble.app

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.screens.AuditScreen
import com.debubble.app.ui.screens.AvatarScreen
import com.debubble.app.ui.screens.CalibrationScreen
import com.debubble.app.ui.screens.CampaignScreen
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
import com.debubble.app.ui.screens.PrinciplesScreen
import com.debubble.app.ui.screens.ProfileScreen
import com.debubble.app.ui.screens.Tab
import com.debubble.app.ui.screens.TabBar
import com.debubble.app.ui.screens.TranscendenceScreen
import com.debubble.app.ui.theme.DeBubbleTheme
import com.debubble.app.ui.theme.Ink
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DeBubbleTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = Ink.Void) {
                    DeBubbleApp()
                }
            }
        }
    }
}

@Composable
private fun DeBubbleApp(vm: DeBubbleViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val route by vm.route.collectAsStateWithLifecycle()
    val pulse by vm.pulse.collectAsStateWithLifecycle()

    // The pulse is a one-shot: clear it so re-entering the dashboard does not replay it.
    LaunchedEffect(pulse) {
        if (pulse != null) {
            delay(1400)
            vm.clearPulse()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .statusBarsPadding()
    ) {
        when (val r = route) {
            is Route.Loading -> Box(modifier = Modifier.fillMaxSize())

            is Route.Intro -> IntroScreen(onDone = vm::finishIntro)

            is Route.Calibration -> {
                // Reachable again from the profile, so it has to be cancellable.
                CalibrationScreen(
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
                    catalogue = vm.audit,
                    initial = state.debuffs,
                    firstRun = r.firstRun,
                    onSave = { vm.saveAudit(it, r.firstRun) },
                    onBack = if (r.firstRun) null else vm::goDashboard
                )
            }

            is Route.Dashboard -> {
                val goal = state.primaryGoal
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardScreen(
                            state = state,
                            dayIndex = vm.dayIndex(state),
                            served = Pillar.order.associateWith { vm.serve(it, state) },
                            ring = vm.ring(state),
                            mission = vm.serveMission(state),
                            reps = vm.repTypes(state),
                            principles = goal?.let { vm.track(it).principles } ?: emptyList(),
                            routed = vm.routed(state),
                            routedDone = state.routedDoneToday,
                            debuffLabel = { id -> vm.audit.debuff(id)?.label ?: "From your audit" },
                            pulse = pulse,
                            onOpen = vm::openChallenge,
                            onOpenMission = vm::openMission,
                            onLogRep = { rep -> vm.logRep(rep.key, rep.friction, rep.label) },
                            onUndoRep = { rep -> vm.unlogRep(rep.key, rep.friction) },
                            onOpenRoutedCampaign = vm::openRoutedCampaign,
                            onOpenRoutedRemediation = vm::openRoutedRemediation,
                            onOpenPrinciple = vm::openPrinciple,
                            onAllPrinciples = vm::goPrinciples,
                            onPickGoal = vm::goGoalPicker,
                            onOpenCampaign = vm::goCampaign,
                            onOpenAvatar = vm::goAvatar
                        )
                    }
                    TabBar(
                        current = Tab.TODAY,
                        onSelect = vm::selectTab,
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }

            is Route.Profile -> {
                BackHandler { vm.goDashboard() }
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        ProfileScreen(
                            state = state,
                            dayIndex = vm.dayIndex(state),
                            track = state.goalEnum?.let { vm.track(it) },
                            onRecalibrate = vm::goRecalibrate,
                            onChangeGoal = vm::goGoalPicker,
                            onOpenAvatar = vm::goAvatar,
                            onOpenAudit = vm::goAudit,
                            onSetBudget = vm::setBudget,
                            onToggleSound = vm::toggleSound,
                            onToggleAmbient = vm::toggleAmbient
                        )
                    }
                    TabBar(
                        current = Tab.PROFILE,
                        onSelect = vm::selectTab,
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }

            is Route.AvatarStudio -> {
                BackHandler { vm.goDashboard() }
                Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    AvatarScreen(
                        avatar = state.avatar,
                        xp = state.xp,
                        friction = state.friction,
                        onChange = vm::setAvatar,
                        onBack = vm::goDashboard
                    )
                }
            }

            is Route.Challenge -> {
                BackHandler { vm.abort() }
                // No tab bar here on purpose: the action screen is a locked focus.
                val served = vm.serve(r.pillar, state)
                Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    ChallengeScreen(
                        served = served,
                        soundOn = state.soundOn,
                        ambientOn = state.ambientOn,
                        onCommit = { minutes ->
                            vm.complete(
                                pillar = r.pillar,
                                minutes = minutes,
                                tier = served.tier,
                                title = served.directive
                            )
                        },
                        onFriction = {
                            vm.logFriction(
                                pillar = r.pillar,
                                tier = served.tier,
                                title = served.directive
                            )
                        },
                        onSwap = if (r.pillar.name !in state.swappedToday) {
                            { vm.swap(r.pillar) }
                        } else null,
                        onBack = vm::abort
                    )
                }
            }

            is Route.GoalPicker -> {
                if (!r.firstRun) BackHandler { vm.goDashboard() }
                GoalPickerScreen(
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
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        ChallengeScreen(
                            served = mission,
                            soundOn = state.soundOn,
                            ambientOn = state.ambientOn,
                            onCommit = { minutes ->
                                vm.completeMission(
                                    minutes = minutes,
                                    step = mission.tier,
                                    title = mission.directive
                                )
                            },
                            onFriction = {
                                vm.missionFriction(
                                    step = mission.tier,
                                    title = mission.directive
                                )
                            },
                            onSwap = null,
                            onBack = vm::abort
                        )
                    }
                }
            }

            is Route.Campaign -> {
                BackHandler { vm.goDashboard() }
                val goal = state.goalEnum
                if (goal == null) {
                    LaunchedEffect(Unit) { vm.goDashboard() }
                } else {
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        CampaignScreen(
                            goal = goal,
                            track = vm.track(goal),
                            state = state.goalState(goal),
                            onOpenStep = vm::openMission,
                            onBack = vm::goDashboard
                        )
                    }
                }
            }

            is Route.Principles -> {
                BackHandler { vm.goDashboard() }
                val goal = state.goalEnum
                if (goal == null) {
                    LaunchedEffect(Unit) { vm.goDashboard() }
                } else {
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        PrinciplesScreen(
                            goal = goal,
                            track = vm.track(goal),
                            state = state.goalState(goal),
                            onOpen = vm::openPrinciple,
                            onBack = vm::goDashboard
                        )
                    }
                }
            }

            is Route.ReadPrinciple -> {
                BackHandler { vm.goDashboard() }
                val goal = state.goalEnum
                val principle = goal?.let { vm.track(it).principles.getOrNull(r.index) }
                if (goal == null || principle == null) {
                    LaunchedEffect(Unit) { vm.goDashboard() }
                } else {
                    LaunchedEffect(r.index) { vm.markRead(r.index) }
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        PrincipleScreen(
                            goal = goal,
                            principle = principle,
                            onDone = vm::goDashboard
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        LearnScreen(
                            curriculum = vm.learn,
                            catalogue = vm.audit,
                            dayIndex = vm.dayIndex(state),
                            debuffs = state.debuffs,
                            read = state.lessonsRead,
                            onOpen = vm::openLesson
                        )
                    }
                    TabBar(
                        current = Tab.LEARN,
                        onSelect = vm::selectTab,
                        modifier = Modifier.navigationBarsPadding()
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
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        NotesScreen(
                            notes = state.notes,
                            dayIndexOf = { note -> vm.dayIndexOfDay(state, note.epochDay) },
                            onOpen = vm::openNote,
                            onNew = { vm.newNote() }
                        )
                    }
                    TabBar(
                        current = Tab.NOTES,
                        onSelect = vm::selectTab,
                        modifier = Modifier.navigationBarsPadding()
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
                    Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                        ChallengeScreen(
                            served = served,
                            soundOn = state.soundOn,
                            ambientOn = state.ambientOn,
                            onCommit = { minutes ->
                                vm.completeRouted(
                                    id = id,
                                    pillar = served.pillar,
                                    minutes = minutes,
                                    title = served.directive,
                                    remediation = r.remediation
                                )
                            },
                            onFriction = {
                                vm.frictionRouted(id, served.pillar, served.directive)
                            },
                            onSwap = null,
                            onBack = vm::goDashboard
                        )
                    }
                }
            }

            is Route.Completion -> {
                // Backing out of the note is the same as skipping it.
                BackHandler { vm.goDashboard() }
                Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    CompletionScreen(
                        pillar = r.pillar,
                        tierCleared = r.tierCleared,
                        onSave = vm::journal,
                        onSkip = vm::goDashboard
                    )
                }
            }
        }
    }
}
