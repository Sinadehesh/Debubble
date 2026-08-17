package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.HoldToCommit
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.rememberHaptics
import com.debubble.app.ui.components.tierCode
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.line
import com.debubble.app.ui.theme.tint

/**
 * The Action Screen.
 *
 * One challenge, full bleed, nothing else reachable — the tab bar is gone. Two exits:
 * complete it, or log the friction. Both are honourable; only pretending is not.
 */
@Composable
fun ChallengeScreen(
    served: Served,
    canSwap: Boolean,
    onAbort: () -> Unit,
    onSwap: () -> Unit,
    onComplete: () -> Unit,
    onFriction: () -> Unit
) {
    val pillar = served.pillar
    val haptics = rememberHaptics()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 10.dp, bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Instrument(
                "← Abort",
                modifier = Modifier
                    .clickable(onClick = onAbort)
                    .padding(vertical = 6.dp, horizontal = 2.dp)
            )
            Instrument("Tier ${tierCode(served.tier)} · ${pillar.code}", color = pillar.accent)
        }

        VSpace(22)
        Instrument("${pillar.display} · ${pillar.dimension}")
        VSpace(14)

        Text(
            text = served.directive,
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineLarge
        )

        VSpace(24)

        // TIME · COST · EXPOSURE. Exposure is the vulnerability rating and the number that
        // climbs hardest across a hundred tiers.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Space.radius))
                .background(Ink.EdgeSoft),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Param("Time", Engine.formatMinutes(served.minutes), Modifier.weight(1f))
            Param("Cost", if (served.cost == 0) "Free" else "${served.cost}", Modifier.weight(1f))
            Param("Exposure", "${served.exposure}/10", Modifier.weight(1f), pillar.accent)
        }

        VSpace(20)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(Space.radius))
                .background(pillar.tint)
                .border(1.dp, pillar.line, RoundedCornerShape(Space.radius))
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Instrument("Why this tier", color = pillar.accent, small = true)
            Text(
                text = served.coach,
                color = Ink.Primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        if (served.substituted && served.substitutionReason != null) {
            VSpace(12)
            Instrument(served.substitutionReason, color = pillar.accent.copy(alpha = 0.8f), small = true)
        }

        VSpace(30)

        HoldToCommit(
            accent = pillar.accent,
            label = "Hold to complete",
            modifier = Modifier.fillMaxWidth(),
            onCommit = onComplete
        )

        VSpace(10)

        // The second exit, in ember. Not hidden, not shamed — it is the courage counter's
        // entire supply line.
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Instrument(
                "Too much today — log friction",
                color = Ink.Ember,
                modifier = Modifier
                    .clickable {
                        haptics.friction()
                        onFriction()
                    }
                    .padding(10.dp)
            )
        }

        if (canSwap) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Instrument(
                    "Not this one — swap it",
                    color = Ink.Dim,
                    modifier = Modifier
                        .clickable(onClick = onSwap)
                        .padding(8.dp),
                    small = true
                )
            }
        }
    }
}

@Composable
private fun Param(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColour: androidx.compose.ui.graphics.Color = Ink.Primary
) {
    Column(
        modifier = modifier
            .background(Ink.Strata)
            .padding(horizontal = 11.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Instrument(label, small = true)
        Text(
            text = value,
            color = valueColour,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
internal fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Ink.EdgeSoft)
    )
}
