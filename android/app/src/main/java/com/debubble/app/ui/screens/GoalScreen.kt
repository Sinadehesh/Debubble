package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Principle
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.litSurface
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.tierCode
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * What do you actually want?
 *
 * The three pillars measure the shape of a bubble; this is where the user says which
 * direction they want it to grow. Every goal shows its real promise and its actual length,
 * because a campaign you can see the end of is one you might finish.
 */
@Composable
fun GoalPickerScreen(
    current: Goal?,
    firstRun: Boolean,
    progressOf: (Goal) -> GoalState,
    onChoose: (Goal) -> Unit,
    onCancel: (() -> Unit)?
) {
    var chosen by remember { mutableStateOf(current) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .padding(horizontal = Space.gutter)
            .padding(top = 14.dp, bottom = Space.gutter)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Instrument(if (firstRun) "One more question" else "Change focus")
            VSpace(12)
            Text(
                text = if (firstRun) "What do you\nactually want?" else "Pick a\ndifferent one.",
                color = Ink.Primary,
                style = MaterialTheme.typography.displayMedium
            )
            VSpace(12)
            Text(
                text = "The three daily challenges expand your bubble in general. A campaign " +
                    "points that expansion at something specific — thirty steps, with reps and " +
                    "reading of its own. You can switch whenever you like and nothing is lost.",
                color = Ink.Dim,
                style = MaterialTheme.typography.bodyMedium
            )
            VSpace(24)

            Goal.all.forEach { goal ->
                val gs = progressOf(goal)
                val started = gs.completed > 0
                val on = goal == chosen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Space.gap)
                        // Selected surfaces catch more light. The semantics announce the
                        // selection too, so nothing depends on noticing the brightness.
                        .litSurface(
                            tint = goal.homePillar.accent,
                            emphasis = if (on) 2.4f else 0.6f,
                            shape = RoundedCornerShape(Space.radiusLarge)
                        )
                        // Selection is signalled by a tint and a border, so it is also spoken.
                        .semantics { selected = on }
                        .clickable(role = Role.RadioButton) { chosen = goal }
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Instrument(
                            goal.homePillar.display,
                            color = goal.homePillar.accent,
                            small = true
                        )
                        if (started) {
                            Instrument(
                                if (Goals.isComplete(gs)) "Complete"
                                else "Step ${tierCode(gs.step)} / 030",
                                color = Ink.Ash,
                                small = true
                            )
                        }
                    }
                    Text(
                        text = goal.display,
                        color = Ink.Primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = goal.promise,
                        color = Ink.Ash,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (started) {
                        VSpace(2)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Ink.EdgeSoft)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(Goals.progress(gs).coerceIn(0.02f, 1f))
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(goal.homePillar.accent)
                            )
                        }
                    }
                }
            }
            VSpace(8)
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            chosen?.let { pick ->
                PrimaryButton(
                    when {
                        pick == current -> "Keep this campaign"
                        progressOf(pick).completed > 0 -> "Resume this campaign"
                        else -> "Start this campaign"
                    }
                ) { onChoose(pick) }
            }
            if (onCancel != null) GhostButton("Cancel") { onCancel() }
        }
    }
}

/** The reading list. Principles unlock as the campaign advances, so reading tracks doing. */
@Composable
fun PrinciplesScreen(
    goal: Goal,
    track: GoalTrack,
    state: GoalState,
    onOpen: (Int) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 10.dp, bottom = 24.dp)
    ) {
        Instrument(
            "← Back",
            modifier = Modifier
                .sizeIn(minHeight = 48.dp)
                .clickable(role = Role.Button, onClick = onBack)
                .padding(vertical = 14.dp)
        )
        VSpace(18)
        Text(
            text = "The ideas\nunderneath.",
            color = Ink.Primary,
            style = MaterialTheme.typography.displayMedium
        )
        VSpace(12)
        Text(
            text = track.premise,
            color = Ink.Ash,
            style = MaterialTheme.typography.bodyMedium
        )
        VSpace(24)

        track.principles.forEachIndexed { i, p ->
            val unlocked = p.unlocksAt <= state.step
            val read = i in state.read
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Space.gap)
                    .litSurface(
                        tint = if (unlocked) goal.homePillar.accent else Ink.Primary,
                        emphasis = if (unlocked) 0.9f else 0.4f
                    )
                    .sizeIn(minHeight = 48.dp)
                    // Locked rows look dimmed; that has to be spoken too.
                    .semantics {
                        stateDescription = when {
                            !unlocked -> "Locked until step ${p.unlocksAt}"
                            read -> "Read"
                            else -> "Unread"
                        }
                    }
                    .then(
                        if (unlocked) Modifier.clickable(role = Role.Button) { onOpen(i) }
                        else Modifier
                    )
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Instrument(
                        if (unlocked) "Idea ${i + 1}" else "Unlocks at step ${tierCode(p.unlocksAt)}",
                        color = if (unlocked) goal.homePillar.accent else Ink.Faint,
                        small = true
                    )
                    if (read) Instrument("Read", color = Ink.Faint, small = true)
                }
                Text(
                    text = if (unlocked) p.title else "Locked",
                    color = if (unlocked) Ink.Primary else Ink.Faint,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
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
            .padding(horizontal = Space.gutter)
            .padding(top = 10.dp, bottom = Space.gutter)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Instrument(
                "← Back",
                modifier = Modifier
                    .sizeIn(minHeight = 48.dp)
                    .clickable(role = Role.Button, onClick = onDone)
                    .padding(vertical = 14.dp)
            )
            VSpace(26)
            Instrument(goal.display, color = goal.homePillar.accent)
            VSpace(14)
            Text(
                text = principle.title,
                color = Ink.Primary,
                style = MaterialTheme.typography.displayMedium
            )
            VSpace(18)
            Text(
                text = principle.body,
                color = Ink.Ash,
                style = MaterialTheme.typography.bodyLarge
            )
            if (principle.source.isNotBlank()) {
                VSpace(20)
                Instrument(principle.source, color = Ink.Faint, small = true)
            }
            VSpace(20)
        }
        PrimaryButton("Got it") { onDone() }
    }
}
