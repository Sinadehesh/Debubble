package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.Topography
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.topographyHeight
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * The campaign, as ground you have crossed.
 *
 * A vertical list of thirty rows with checkboxes would be read as admin and abandoned. This is
 * the same thirty steps rendered as terrain: what is behind you stays lit permanently, what is
 * ahead is fog, and switching campaigns leaves each landscape exactly as you left it.
 */
@Composable
fun CampaignScreen(
    goal: Goal,
    track: GoalTrack,
    state: GoalState,
    onOpenStep: () -> Unit,
    onBack: () -> Unit
) {
    val accent = goal.homePillar.accent
    val complete = Goals.isComplete(state)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = goal.display,
            subtitle = if (complete) "All three phases done" else track.phaseOf(state.step),
            accent = accent,
            onBack = onBack,
            action = {
                Pill(
                    if (complete) "Done" else "${state.step} / 30",
                    accent,
                    filled = complete
                )
            }
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
                .padding(top = 4.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            ProgressTrack(Goals.progress(state), accent, height = 8)
            Label(
                if (complete) "Every step behind you"
                else "${state.completed} done, ${30 - state.completed} to go"
            )
        }

        // The terrain scrolls: the whole campaign is taller than any phone.
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Topography(
                step = state.step,
                completed = state.completed,
                accent = accent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(topographyHeight)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (complete) {
                Text(
                    text = "All 30 done. This ground stays yours whether or not you come back.",
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = track.mission(state.step).directive,
                    color = Ink.Primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                PrimaryButton("Open step ${state.step}", accent = accent) { onOpenStep() }
            }
        }
    }
}
