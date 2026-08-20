package com.debubble.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.RepType
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.NumberFamily
import com.debubble.app.ui.theme.Space

/**
 * Reps: the unlimited half of the app.
 *
 * The daily challenges are capped at one each by design, which leaves someone who has done
 * them with nothing to do. Reps are the volume work — log them all day, no ceiling.
 *
 * Every counter has both a plus and a minus. That is not a nicety: these get tapped one-handed
 * on a phone in a pocket, mis-taps are certain, and a number that can only go up is a number
 * people quietly stop trusting. Friction reps in particular *must* be correctable, because an
 * accidental one inflates the score the whole app is built around.
 */
@Composable
fun RepTracker(
    reps: List<RepType>,
    today: Map<String, Int>,
    target: Int,
    lifetime: Int,
    accent: Color,
    onLog: (RepType) -> Unit,
    onUndo: (RepType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reps.isEmpty()) return
    val done = today.values.sum()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(shape = RoundedCornerShape(Space.radiusLarge))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Label("Today's attempts", strong = true, color = Ink.Primary)
                Text(
                    text = if (target > 0) "$done of $target" else "$done logged",
                    color = if (target > 0 && done >= target) accent else Ink.Primary,
                    style = MaterialTheme.typography.headlineLarge.copy(fontFamily = NumberFamily),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            Label("$lifetime all time")
        }

        if (target > 0) {
            ProgressTrack(
                fraction = done.toFloat() / target,
                accent = accent,
                height = 8
            )
        }

        Text(
            text = when {
                target > 0 && done >= target ->
                    "Target hit. Anything past this is extra, and extra is what adds up."
                done == 0 && target > 0 ->
                    "Challenges are once a day. These are not — log as many as you get."
                else -> "Log every attempt, including the ones that went badly."
            },
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )

        reps.forEach { rep ->
            RepRow(
                rep = rep,
                count = today[rep.key] ?: 0,
                accent = if (rep.friction) Ink.Ember else accent,
                onLog = { onLog(rep) },
                onUndo = { onUndo(rep) }
            )
        }
    }
}

@Composable
private fun RepRow(
    rep: RepType,
    count: Int,
    accent: Color,
    onLog: () -> Unit,
    onUndo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(
                fill = Ink.SurfaceHigh,
                border = if (count > 0) accent.copy(alpha = 0.7f) else Ink.Border
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = rep.label,
                    color = Ink.Primary,
                    style = MaterialTheme.typography.titleMedium
                )
                if (rep.friction) Pill("Friction", Ink.Ember)
            }
            Text(
                text = rep.hint,
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            // The reason this counter exists, in one line, next to the button rather than
            // buried in a reading screen nobody opens.
            Text(
                text = rep.why,
                color = Ink.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RoundButton(
                glyph = Glyphs.Minus,
                description = "Remove one ${rep.label}",
                accent = accent,
                enabled = count > 0,
                onClick = onUndo
            )
            Box(
                modifier = Modifier
                    .widthIn(min = 54.dp)
                    .semantics {
                        contentDescription = "$count logged today"
                        liveRegion = LiveRegionMode.Polite
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = count.toString(),
                    color = if (count > 0) accent else Ink.Muted,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = NumberFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            RoundButton(
                glyph = Glyphs.Plus,
                description = "Log one ${rep.label}",
                accent = accent,
                filled = true,
                onClick = onLog
            )
            Box(modifier = Modifier.weight(1f))
            if (count > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Glyph(Glyphs.Undo, colour = Ink.Muted, size = 15)
                    Label("Minus undoes a mis-tap")
                }
            }
        }
    }
}
