package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.audio.rememberSound
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.Divider
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.GlitchBurst
import com.debubble.app.ui.components.HoldToCommit
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.Shockwave
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.rememberHaptics
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * The action screen: one thing to do, and two honest ways out of it.
 *
 * Committing is a three-second hold. Saying it was too much is a full amber card of its own —
 * it used to be a line of small grey text at the bottom of a scroll, which is exactly the
 * wrong place for the option this app most wants people to feel able to take.
 */
@Composable
fun ChallengeScreen(
    served: Served,
    soundOn: Boolean,
    ambientOn: Boolean,
    onCommit: (Int) -> Unit,
    onFriction: () -> Unit,
    onSwap: (() -> Unit)?,
    onBack: () -> Unit
) {
    val pillar = served.pillar
    val haptics = rememberHaptics()
    val sound = rememberSound()

    var committed by remember { mutableStateOf(false) }
    var glitching by remember { mutableStateOf(false) }

    // The bed is opt-in and tied to this screen only; it never survives leaving it.
    LaunchedEffect(ambientOn, served.exposure) {
        if (ambientOn) sound.startBed(served.exposure / 10f) else sound.stopBed()
    }
    DisposableEffect(Unit) { onDispose { sound.stopBed() } }

    Box(modifier = Modifier.fillMaxSize().background(Ink.Void)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                title = if (served.kind == "MISSION") "Campaign step" else pillar.display,
                subtitle = if (served.kind == "MISSION") "Step ${served.tier} of 30"
                else "${pillar.dimension} · Level ${served.tier}",
                accent = pillar.accent,
                onBack = onBack,
                action = {
                    if (onSwap != null) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = Space.tap)
                                .panel(border = Ink.Border)
                                .clickable(role = Role.Button) { onSwap() }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Label("Swap", color = Ink.Primary, strong = true)
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter)
            ) {
                VSpace(8)

                if (served.phase != null) {
                    Pill(served.phase, pillar.accent)
                    VSpace(14)
                }

                Text(
                    text = served.directive,
                    color = Ink.Primary,
                    style = MaterialTheme.typography.displayMedium
                )

                VSpace(20)

                // The three facts about the task, each in its own boxed cell.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Space.gap)
                ) {
                    Fact("Time", Engine.formatMinutes(served.minutes), Modifier.weight(1f))
                    Fact(
                        "Cost",
                        if (served.cost == 0) "Free" else "${served.cost}",
                        Modifier.weight(1f)
                    )
                    Fact(
                        "Nerve",
                        "${served.exposure}/10",
                        Modifier.weight(1f),
                        accent = Ink.Ember
                    )
                }

                VSpace(18)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .panel(border = pillar.accent.copy(alpha = 0.5f))
                        .padding(15.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Label(
                        if (served.kind == "MISSION") "Why this step" else "Why this one",
                        color = pillar.accent,
                        strong = true
                    )
                    Text(
                        text = served.coach,
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }

                if (served.substituted && served.substitutionReason != null) {
                    VSpace(12)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .panel(fill = Ink.SurfaceHigh)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(Glyphs.Check, colour = pillar.accent, size = 18)
                        Text(
                            text = served.substitutionReason,
                            color = Ink.Secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                if (served.repTarget > 0) {
                    VSpace(12)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .panel(fill = Ink.SurfaceHigh)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Glyph(Glyphs.Spark, colour = Ink.Gold, size = 18)
                        Text(
                            text = "Aim for ${served.repTarget} attempts today. " +
                                "Log them on the home screen.",
                            color = Ink.Secondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                VSpace(30)

                HoldToCommit(
                    accent = pillar.accent,
                    label = "Hold to say you did it",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!committed) {
                        committed = true
                        if (soundOn) sound.chime()
                        onCommit(served.minutes)
                    }
                }

                VSpace(30)
                Divider()
                VSpace(18)

                FrictionCard {
                    glitching = true
                    haptics.friction()
                    if (soundOn) sound.stab()
                    onFriction()
                }

                VSpace(12)
                SecondaryButton("Not today", onClick = onBack)
                VSpace(24)
            }
        }

        if (committed) {
            Shockwave(accent = pillar.accent, modifier = Modifier.fillMaxSize())
        }
        if (glitching) {
            GlitchBurst(modifier = Modifier.fillMaxSize())
        }
    }
}

/**
 * The friction exit, as a real card.
 *
 * The whole design rests on people being willing to press this, so it gets amber, an icon, a
 * headline, an explanation of what happens, and a button — not a grey word under a fold.
 */
@Composable
private fun FrictionCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh, border = Ink.Ember, borderWidth = 2.dp)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Ember, size = 22)
            Text(
                text = "This one was too much",
                color = Ink.Ember,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = "Tried it and bailed? Asked and got a no? Could not make yourself start? " +
                "Say so. You earn a Friction point, and the next one gets easier, not harder.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Space.tap)
                .panel(fill = Ink.Ember, border = Ink.Ember)
                .clickable(role = Role.Button, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Glyph(Glyphs.Spark, colour = Ink.OnAccent, size = 18)
                Text(
                    text = "Log friction",
                    color = Ink.OnAccent,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun Fact(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: androidx.compose.ui.graphics.Color = Ink.Primary
) {
    Column(
        modifier = modifier
            .panel(fill = Ink.SurfaceHigh)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Label(label)
        Text(
            text = value,
            color = accent,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
