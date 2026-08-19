package com.debubble.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.RepType
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.InstrumentFamily
import com.debubble.app.ui.theme.Space

/**
 * Reps: the unlimited half of the app.
 *
 * The three daily challenges and the mission are capped at one each per day by design — a
 * tier is supposed to be earned once. That leaves someone who has done all four with nothing
 * to do, which is exactly the failure mode this fixes. Reps are the volume work: unlimited,
 * loggable all day, and the thing that actually moves someone.
 *
 * A rep marked [RepType.friction] adds to the anti-score when tapped, so being turned down
 * is recorded as output rather than as failure.
 */
@Composable
fun RepTracker(
    reps: List<RepType>,
    today: Map<String, Int>,
    target: Int,
    lifetime: Int,
    accent: Color,
    onLog: (RepType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (reps.isEmpty()) return
    val done = today.values.sum()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .litSurface(emphasis = 0.7f, shape = RoundedCornerShape(Space.radiusLarge))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Instrument("Reps today")
                Text(
                    text = if (target > 0) "$done / $target" else "$done",
                    color = if (target > 0 && done >= target) accent else Ink.Primary,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = InstrumentFamily
                    )
                )
            }
            Instrument("$lifetime all time", color = Ink.Faint, small = true)
        }

        if (target > 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Ink.EdgeSoft)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((done.toFloat() / target).coerceIn(0f, 1f))
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent)
                )
            }
        }

        Text(
            text = when {
                target > 0 && done >= target ->
                    "Target met. Every rep past this one is surplus, and surplus is where it compounds."
                done == 0 && target > 0 ->
                    "Tiers are earned once a day. Reps are not — this is the part with no ceiling."
                else -> "Log every attempt, including the ones that went badly. Especially those."
            },
            color = Ink.Dim,
            style = MaterialTheme.typography.bodyMedium
        )

        reps.forEach { rep ->
            val count = today[rep.key] ?: 0
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .litSurface(tint = if (rep.friction) Ink.Ember else accent, emphasis = 0.75f)
                    // The count and the "+" are visual; the whole row is one button, so it
                    // announces the label, the running total and what tapping will do.
                    .semantics {
                        role = Role.Button
                        contentDescription =
                            "${rep.label}. Logged $count today. Tap to log another."
                    }
                    .clickable { onLog(rep) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = rep.label,
                        color = if (rep.friction) Ink.Ember else Ink.Primary,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Medium
                        )
                    )
                    Text(
                        text = rep.hint,
                        color = Ink.Dim,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (count > 0) {
                    Text(
                        modifier = Modifier.semantics { contentDescription = "" },
                        text = count.toString(),
                        color = if (rep.friction) Ink.Ember else accent,
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = InstrumentFamily
                        )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .semantics { contentDescription = "" }
                        .clip(RoundedCornerShape(50))
                        .background((if (rep.friction) Ink.Ember else accent).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "+",
                        color = if (rep.friction) Ink.Ember else accent,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            }
        }
    }
}
