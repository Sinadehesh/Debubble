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
import com.debubble.app.engine.World
import com.debubble.app.ui.components.CheckBox
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SetupSteps
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
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
    art: RoomArt,
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
            title = if (firstRun) "Pick a journey" else "Change your journey",
            onBack = onCancel
        )
        if (firstRun) SetupSteps(art, 3)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Text(
                text = if (firstRun) "Where do you want to get to?" else "Where are you headed?",
                color = Ink.Primary,
                style = MaterialTheme.typography.displayMedium
            )
            VSpace(12)
            Text(
                text = "Each is a journey of thirty small stops, one a day when you want it. " +
                    "Pick as many as you like: they take turns, and where two overlap you get " +
                    "one step that counts for both.",
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
                        SpriteIcon(art, World.landmark(goal), pixelScale = pixelScale(2f))
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
                                if (Goals.isComplete(gs)) "Arrived" else "Stop ${gs.step}",
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
