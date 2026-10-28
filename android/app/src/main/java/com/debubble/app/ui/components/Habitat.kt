package com.debubble.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Habitat
import com.debubble.app.engine.Stage
import com.debubble.app.ui.theme.Ink

/**
 * The room's name, and how far off the next one is.
 *
 * Says the number of rungs rather than a percentage, because "fourteen more" is a thing a
 * person can picture doing and "62%" is a thing they can only watch. There is no way to lose
 * a stage, which is why this can be shown without it becoming something to protect.
 */
@Composable
fun HabitatRow(stage: Stage, cleared: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Color(stage.ground), border = Color(stage.structure))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stage.label,
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            Label("$cleared cleared")
        }
        Text(
            text = stage.note,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        ProgressTrack(Habitat.progress(cleared), Ink.Secondary, height = 6)
        Label(Habitat.caption(cleared))
    }
}

/**
 * Shown once, on the day the room opens.
 *
 * Deliberately quiet — no confetti, no full-screen takeover. The room is already different
 * behind it; the banner only names what changed, so the user does not have to wonder whether
 * they imagined it.
 */
@Composable
fun HabitatAnnouncement(stage: Stage, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Color(stage.horizon), border = Ink.Gold)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Gold, size = 18)
            Label("The room changed", color = Ink.Gold, strong = true)
        }
        Text(
            text = stage.label,
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stage.note,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        SecondaryButton("Got it", onClick = onDismiss)
    }
}
