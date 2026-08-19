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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.Topography
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.tierCode
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
        Column(modifier = Modifier.padding(horizontal = Space.gutter).padding(top = 10.dp)) {
            Instrument(
                "← Back",
                modifier = Modifier
                    .clickable(role = Role.Button, onClick = onBack)
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .wrapContentSize(Alignment.CenterStart)
            )
            VSpace(10)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = goal.display,
                    color = Ink.Primary,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                Instrument(
                    if (complete) "Cleared" else "${tierCode(state.step)} / 030",
                    color = accent
                )
            }
            VSpace(8)
            Instrument(if (complete) "All three phases" else track.phaseOf(state.step))
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
                    text = "Thirty monuments, all of them lit. This ground stays yours whether " +
                        "or not you come back to it.",
                    color = Ink.Ash,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Text(
                    text = track.mission(state.step).directive,
                    color = Ink.Primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                PrimaryButton("Open step ${tierCode(state.step)}") { onOpenStep() }
            }
        }
    }
}
