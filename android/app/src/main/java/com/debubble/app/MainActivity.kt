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
import kotlinx.coroutines.delay
import com.debubble.app.ui.screens.CalibrationScreen
import com.debubble.app.ui.screens.ChallengeScreen
import com.debubble.app.ui.screens.CompletionScreen
import com.debubble.app.ui.screens.DashboardScreen
import com.debubble.app.ui.screens.ProfileScreen
import com.debubble.app.ui.screens.Tab
import com.debubble.app.ui.screens.TabBar
import com.debubble.app.ui.theme.DeBubbleTheme
import com.debubble.app.ui.theme.Ink

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

    // The shockwave is a one-shot: clear it so re-entering the dashboard does not replay it.
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

            is Route.Calibration -> {
                // Recalibration is reachable from the profile, so it must be cancellable.
                CalibrationScreen(
                    initial = state.baseline,
                    isRecalibration = state.onboarded,
                    onDone = vm::finishCalibration,
                    onCancel = if (state.onboarded) vm::goDashboard else null
                )
            }

            is Route.Dashboard -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardScreen(
                            state = state,
                            dayIndex = vm.dayIndex(state),
                            served = Pillar.order.associateWith { vm.serve(it, state) },
                            pulse = pulse,
                            onOpen = vm::openChallenge
                        )
                    }
                    TabBar(
                        current = Tab.TODAY,
                        onSelect = { if (it == Tab.PROFILE) vm.goProfile() },
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
                            onRecalibrate = vm::goRecalibrate
                        )
                    }
                    TabBar(
                        current = Tab.PROFILE,
                        onSelect = { if (it == Tab.TODAY) vm.goDashboard() },
                        modifier = Modifier.navigationBarsPadding()
                    )
                }
            }

            is Route.Challenge -> {
                BackHandler { vm.abort() }
                // No tab bar here on purpose: the Action Screen is a locked focus.
                val served = vm.serve(r.pillar, state)
                Box(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
                    ChallengeScreen(
                        served = served,
                        canSwap = r.pillar.name !in state.swappedToday,
                        onAbort = vm::abort,
                        onSwap = { vm.swap(r.pillar) },
                        onComplete = {
                            vm.complete(
                                pillar = r.pillar,
                                minutes = served.minutes,
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
                        }
                    )
                }
            }

            is Route.Completion -> {
                // Back out of the journal is the same as skipping it.
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
