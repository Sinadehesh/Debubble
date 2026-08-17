package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.PillarBar
import com.debubble.app.ui.components.StatTile
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.tierCode
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.InstrumentFamily
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * Profile and the Anti-Score.
 *
 * The screen that decides whether the product works. Streaks reward compliance; Friction
 * rewards contact with the edge — so Friction sits above the streak, in the largest numeral
 * on the surface. The hierarchy states the thesis without a word of copy.
 */
@Composable
fun ProfileScreen(
    state: AppState,
    dayIndex: Long,
    onRecalibrate: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 10.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(Space.block)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = "Your friction",
                color = Ink.Primary,
                style = MaterialTheme.typography.headlineMedium
            )
            Instrument("Day ${tierCode(dayIndex.toInt())}")
        }

        FrictionCard(friction = state.friction)

        Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            StatTile(
                label = "Day streak",
                value = state.streak.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                label = "Tiers cleared",
                value = state.tiersCleared.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = "A missed day pauses the streak. It never clears your friction, your tiers, " +
                "or your evidence — nothing you have already done can be taken back.",
            color = Ink.Dim,
            style = MaterialTheme.typography.bodyMedium
        )

        Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
            Pillar.order.forEach { p ->
                PillarBar(pillar = p, tier = state.state(p).tier)
            }
        }

        EvidenceBlock(state)

        // History: completions and friction in one stream, never a separate failures list.
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Instrument("History")
                Instrument("${state.log.size} entries")
            }
            if (state.log.isEmpty()) {
                Divider()
                VSpace(12)
                Text(
                    text = "Nothing logged yet. Complete a challenge and it lands here.",
                    color = Ink.Dim,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                state.log.take(40).forEach { entry ->
                    Divider()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 11.dp),
                        horizontalArrangement = Arrangement.spacedBy(11.dp)
                    ) {
                        Dot(if (entry.friction) Ink.Ember else entry.pillarEnum.accent, size = 6)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = entry.title,
                                color = Ink.Primary,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Text(
                                text = when {
                                    entry.note.isNotBlank() -> "“${entry.note}”"
                                    entry.friction -> "Logged as friction. Same tier tomorrow."
                                    else -> "Completed."
                                },
                                color = Ink.Ash,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Instrument(
                            text = (if (entry.friction) "FR" else "T") + tierCode(entry.tier),
                            color = if (entry.friction) Ink.Ember else Ink.Faint,
                            small = true
                        )
                    }
                }
            }
        }

        VSpace(4)
        GhostButton("Recalibrate baseline") { onRecalibrate() }
        Text(
            text = "Life changes. Recalibrating re-pitches the ladder to where you are now " +
                "and keeps every tier you have already cleared.",
            color = Ink.Faint,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun FrictionCard(friction: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Space.radiusLarge))
            .background(Ink.Ridge)
            .border(1.dp, Ink.Ember.copy(alpha = 0.3f), RoundedCornerShape(Space.radiusLarge))
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = friction.toString(),
                    color = Ink.Ember,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = InstrumentFamily,
                        fontWeight = FontWeight.Bold
                    )
                )
                Instrument("Friction score", color = Ink.Ember)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Ink.Ember.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .padding(horizontal = 11.dp, vertical = 6.dp)
            ) {
                Instrument(Engine.courageBadge(friction), color = Ink.Ember, small = true)
            }
        }
        Text(
            text = if (friction == 0) {
                "No contact with the edge yet. A zero here means the tiers are still too easy."
            } else {
                "$friction point${if (friction == 1) "" else "s"} of contact with the edge — " +
                    "awkward, refused, or abandoned as too hard. This number only goes up."
            },
            color = Ink.Ash,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Evidence, not score.
 *
 * Streaks and tiers measure compliance with the app. This measures what actually changed
 * about the person — stated in things done, not points earned. It is the answer to "am I
 * actually different?", which is the only question that matters at day 60.
 */
@Composable
private fun EvidenceBlock(state: AppState) {
    val hours = state.minutesInvested / 60
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Space.radiusLarge))
            .background(Ink.Strata)
            .border(1.dp, Ink.EdgeSoft, RoundedCornerShape(Space.radiusLarge))
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp)
    ) {
        Instrument("Evidence")
        Pillar.order.forEach { p ->
            val n = state.evidence(p)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = n.toString(),
                    color = p.accent,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = InstrumentFamily
                    )
                )
                Text(
                    text = p.evidenceNoun,
                    color = Ink.Ash,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Divider()
        Text(
            text = if (hours < 1) {
                "Under an hour invested so far. It compounds."
            } else {
                "$hours hour${if (hours == 1) "" else "s"} invested in becoming someone with " +
                    "a bigger radius. None of it was spent on this screen."
            },
            color = Ink.Dim,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
