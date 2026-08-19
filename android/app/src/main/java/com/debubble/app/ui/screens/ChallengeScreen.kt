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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.debubble.app.audio.rememberSound
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.FractureOverlay
import com.debubble.app.ui.components.HoldToCommit
import com.debubble.app.ui.components.Shockwave
import com.debubble.app.ui.components.fractureEffect
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.rememberHaptics
import com.debubble.app.ui.components.tierCode
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.InstrumentFamily
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
    frictionAfter: Int,
    soundOn: Boolean,
    ambientOn: Boolean,
    onAbort: () -> Unit,
    onSwap: () -> Unit,
    onComplete: () -> Unit,
    onFriction: () -> Unit
) {
    val pillar = served.pillar
    val haptics = rememberHaptics()
    val sound = rememberSound()

    // The bed only comes up for challenges that genuinely frighten people, and only if the
    // user asked for it. Exposure 7 is roughly where the ladder stops being a nudge.
    DisposableEffect(ambientOn, served.exposure, served.tier) {
        if (ambientOn && served.exposure >= 7) {
            sound.startBed(((served.exposure - 6) / 4f).coerceIn(0f, 1f))
        }
        onDispose { sound.stopBed() }
    }

    // The snap needs somewhere to land. Navigating on the same frame as the commit throws
    // away the moment the three-second hold just bought.
    var committing by remember { mutableStateOf(false) }
    LaunchedEffect(committing) {
        if (committing) {
            kotlinx.coroutines.delay(380)
            onComplete()
        }
    }

    // The fracture. Snaps open fast and settles slowly, because a discharge has a sharp
    // leading edge and a long tail — and because the number underneath needs time to be read.
    val fracture = remember { Animatable(0f) }
    var fracturing by remember { mutableStateOf(false) }
    LaunchedEffect(fracturing) {
        if (!fracturing) return@LaunchedEffect
        fracture.animateTo(1f, tween(90))
        fracture.animateTo(0f, tween(620))
        onFriction()
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.Void)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .fractureEffect(fracture.value)
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
                    // The arrow is decoration; the spoken label has to carry the destination.
                    "← Abort",
                    modifier = Modifier
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Leave this challenge without logging anything",
                            onClick = onAbort
                        )
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .wrapContentSize(Alignment.CenterStart)
                )
                Instrument(
                    if (served.kind == "MISSION") "Step ${tierCode(served.tier)} / 030"
                    else "Tier ${tierCode(served.tier)} · ${pillar.code}",
                    color = pillar.accent
                )
            }

            VSpace(22)
            Instrument(served.phase ?: "${pillar.display} · ${pillar.dimension}")
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
                Instrument(if (served.kind == "MISSION") "Why this step" else "Why this tier", color = pillar.accent, small = true)
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

            if (served.repTarget > 0) {
                VSpace(14)
                Instrument(
                    "Then log ${served.repTarget} rep${if (served.repTarget == 1) "" else "s"} today",
                    color = Ink.Ash,
                    small = true
                )
            }

            VSpace(30)

            HoldToCommit(
                accent = pillar.accent,
                label = "Hold to commit",
                modifier = Modifier.fillMaxWidth(),
                onCommit = {
                sound.stopBed()
                if (soundOn) sound.chime()
                committing = true
            }
            )

            VSpace(10)

            // The second exit, in ember. Not hidden, not shamed — it is the courage counter's
            // entire supply line.
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Instrument(
                    "Too much today — log friction",
                    color = Ink.Ember,
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            haptics.friction()
                            onFriction()
                        }
                        .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                        .wrapContentSize(Alignment.Center)
                )
            }

            if (canSwap) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Instrument(
                        "Not this one — swap it",
                        color = Ink.Dim,
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onSwap)
                            .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                            .wrapContentSize(Alignment.Center),
                        small = true
                    )
                }
            }
        }

        // Fired the instant the hold closes, and the reason navigation waits 380ms:
        // the snap needs somewhere to land.
        if (committing) {
            Shockwave(accent = pillar.accent, modifier = Modifier.fillMaxSize())
        }

        // Friction: the surface tears, and what is standing when it settles is a bigger
        // number than was there before. Damage would read as an error; this reads as output.
        if (fracture.value > 0.001f) {
            FractureOverlay(amount = fracture.value, modifier = Modifier.fillMaxSize())
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = frictionAfter.toString(),
                    color = Ink.Ember,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontFamily = InstrumentFamily
                    )
                )
                VSpace(8)
                Instrument("Friction", color = Ink.Ember)
                VSpace(10)
                Instrument("Contact with the edge", small = true)
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
