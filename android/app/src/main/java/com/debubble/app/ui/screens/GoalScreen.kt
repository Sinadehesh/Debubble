package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Principle
import com.debubble.app.ui.components.CheckBox
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.selectablePanel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * Picking what you are actually aiming at.
 *
 * Selection here is loud on purpose — this is the choice that shapes the next month of the
 * app, and the old version signalled it with a slightly brighter panel.
 */
@Composable
fun GoalPickerScreen(
    running: Set<String>,
    firstRun: Boolean,
    progressOf: (Goal) -> GoalState,
    onToggle: (Goal) -> Unit,
    onConfirm: () -> Unit,
    onCancel: (() -> Unit)?
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = if (firstRun) "One more thing" else "Change your goal",
            onBack = onCancel
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Text(
                text = if (firstRun) "What do you actually want?" else "What are you working on?",
                color = Ink.Primary,
                style = MaterialTheme.typography.displayMedium
            )
            VSpace(12)
            Text(
                text = "Pick as many as you want. They run at the same time and take turns, " +
                    "and where two of them overlap you get a single challenge that counts " +
                    "for both.",
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyLarge
            )
            VSpace(8)
            Text(
                text = "Switching one off keeps everything it earned. Nothing is ever lost.",
                color = Ink.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
            VSpace(22)

            Goal.all.forEach { goal ->
                val gs = progressOf(goal)
                val started = gs.completed > 0
                val on = goal.name in running
                val accent = goal.homePillar.accent

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Space.gap)
                        .selectablePanel(on, accent, RoundedCornerShape(Space.radiusLarge))
                        .clickable(role = Role.Checkbox) { onToggle(goal) }
                        .semantics {
                            stateDescription = if (on) "Selected" else "Not selected"
                        }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        CheckBox(selected = on, accent = accent)
                        Text(
                            text = goal.display,
                            modifier = Modifier.weight(1f),
                            color = Ink.Primary,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold
                            )
                        )
                        if (started) {
                            Pill(
                                if (Goals.isComplete(gs)) "Done" else "Step ${gs.step}",
                                accent
                            )
                        }
                    }
                    Text(
                        text = goal.promise,
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (started) {
                        ProgressTrack(Goals.progress(gs), accent, height = 6)
                    }
                }
            }
            VSpace(10)
        }

        Column(
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryButton(
                text = when (running.size) {
                    0 -> "Pick at least one to continue"
                    1 -> "Start"
                    else -> "Start all ${running.size}"
                },
                enabled = running.isNotEmpty()
            ) { onConfirm() }
            if (onCancel != null) TextAction("Done") { onCancel() }
        }
    }
}

/** The reading list. Ideas unlock as the campaign advances, so reading tracks doing. */
@Composable
fun PrinciplesScreen(
    goal: Goal,
    track: GoalTrack,
    state: GoalState,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit
) {
    val unlocked = track.principles.count { it.unlocksAt <= state.step }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = "The ideas underneath",
            subtitle = "$unlocked of ${track.principles.size} unlocked",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Text(
                text = track.premise,
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyLarge
            )
            VSpace(22)

            track.principles.forEachIndexed { i, p ->
                val open = p.unlocksAt <= state.step
                val read = i in state.read
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Space.gap)
                        .panel(
                            fill = if (open) Ink.Surface else Ink.Void,
                            border = if (open) goal.homePillar.accent.copy(alpha = 0.55f)
                            else Ink.Faint
                        )
                        .semantics {
                            stateDescription = when {
                                !open -> "Locked until step ${p.unlocksAt}"
                                read -> "Read"
                                else -> "Unread"
                            }
                        }
                        .then(
                            if (open) Modifier.clickable(role = Role.Button) { onOpen(i) }
                            else Modifier
                        )
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Glyph(
                            if (open) Glyphs.Spark else Glyphs.Lock,
                            colour = if (open) goal.homePillar.accent else Ink.Muted,
                            size = 17
                        )
                        Text(
                            text = if (open) p.title else "Idea ${i + 1}",
                            modifier = Modifier.weight(1f),
                            color = if (open) Ink.Primary else Ink.Muted,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (open && read) Pill("Read", Ink.Activity)
                    }
                    if (!open) {
                        Label("Unlocks at step ${p.unlocksAt}. You are on step ${state.step}.")
                        ProgressTrack(
                            fraction = state.step.toFloat() / p.unlocksAt,
                            accent = Ink.Faint,
                            height = 6
                        )
                    }
                }
            }
            VSpace(20)
        }
    }
}

/** One idea, full screen. Short enough to finish standing up. */
@Composable
fun PrincipleScreen(
    goal: Goal,
    principle: Principle,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = goal.display,
            accent = goal.homePillar.accent,
            onBack = onDone
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            VSpace(8)
            Text(
                text = principle.title,
                color = Ink.Primary,
                style = MaterialTheme.typography.displayMedium
            )
            VSpace(18)
            Text(
                text = principle.body,
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyLarge
            )
            if (principle.source.isNotBlank()) {
                VSpace(22)
                Label(principle.source)
            }
            VSpace(24)
        }
        Column(modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter)) {
            PrimaryButton("Got it", accent = goal.homePillar.accent) { onDone() }
        }
    }
}
