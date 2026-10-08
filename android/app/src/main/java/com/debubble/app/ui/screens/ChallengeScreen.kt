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
import com.debubble.app.engine.Intention
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
    plan: Intention?,
    onPlan: () -> Unit,
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

                if (served.hasProtocol) {
                    VSpace(14)
                    ProtocolBlock(served = served, accent = pillar.accent)
                }

                VSpace(22)
                PlanBlock(plan = plan, accent = pillar.accent, onPlan = onPlan)

                VSpace(26)

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

                if (served.ease.isNotBlank()) {
                    VSpace(14)
                    EaseBlock(served.ease)
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
 * The protocol: when, the first move, the words, the finish line, and what comes off.
 *
 * This block is the whole of this phase of the work. The challenge text above it was always
 * fine as prose and useless as an instruction, because it answered only "what" — and the four
 * literatures the curriculum now draws on all say the same thing about why that fails. A cue
 * with no moment in it competes with the rest of the day and loses. An entry step that is not
 * trivially small gets deferred. A task with no success criterion cannot be completed, only
 * abandoned. And a frightening thing survived while holding a safety behaviour teaches that
 * the safety behaviour was what saved you. [Protocol] carries the sources.
 *
 * One panel rather than five cards, deliberately. These are five parts of one instruction, and
 * five separate boxes would read as five more things to do.
 */
@Composable
private fun ProtocolBlock(served: Served, accent: androidx.compose.ui.graphics.Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(border = Ink.Border)
    ) {
        if (served.anchor.isNotBlank()) {
            Step(
                label = "When",
                text = "When ${served.anchor}.",
                accent = accent,
                emphasis = true
            )
        }
        if (served.opener.isNotBlank()) {
            Divider()
            Step(
                label = "Start with this — under two minutes",
                text = served.opener,
                accent = accent,
                emphasis = true
            )
        }
        if (served.script.isNotBlank()) {
            Divider()
            Step(label = "Words, if you want them", text = served.script, accent = accent)
        }
        if (served.done.isNotBlank()) {
            Divider()
            Step(label = "Done when", text = served.done.replaceFirstChar { it.uppercase() }, accent = accent)
        }
        if (served.drop.isNotBlank()) {
            Divider()
            // Amber, like Friction, because it belongs to the same honest-discomfort family
            // and never to the error family. Nothing in this app is red.
            Step(label = "Leave this behind", text = served.drop, accent = Ink.Ember)
        }
    }
}

@Composable
private fun Step(
    label: String,
    text: String,
    accent: androidx.compose.ui.graphics.Color,
    emphasis: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Label(label, color = accent, strong = true)
        Text(
            text = text,
            color = if (emphasis) Ink.Primary else Ink.Secondary,
            style = if (emphasis) {
                MaterialTheme.typography.titleMedium
            } else {
                MaterialTheme.typography.bodyLarge
            }
        )
    }
}

/**
 * The smaller rung, offered without being asked for.
 *
 * Behavioural activation calls this graded task assignment, and the detail that matters is the
 * timing: the smaller step has to be visible *before* someone decides they cannot do the
 * bigger one, because by the time they have decided, they have closed the app. It sits under
 * the Friction card rather than above it, so the order on the page reads "it was too much" and
 * then "here is the smaller one" — which is the order the conversation actually happens in.
 *
 * It is not a button. Tapping it would mean logging a completion against a rung the user is
 * not standing on, and the ladder would start lying about where they are.
 */
@Composable
private fun EaseBlock(ease: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Well, border = Ink.Faint)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Label("If this one is too big today", color = Ink.Muted, strong = true)
        Text(
            text = ease,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Do that instead. It counts as the day, and the level stays where it is.",
            color = Ink.Muted,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * The if-then plan, or the offer to make one.
 *
 * Sits directly above the commit button because that is the decision point. Writing the plan
 * is optional, and saying so matters — made compulsory it becomes a toll booth in front of
 * the thing someone was already reluctant to do, and they will stop opening the challenge at
 * all. Offered, it roughly doubles the chance the challenge actually happens.
 */
@Composable
private fun PlanBlock(plan: Intention?, accent: androidx.compose.ui.graphics.Color, onPlan: () -> Unit) {
    if (plan != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .panel(border = Ink.Activity.copy(alpha = 0.65f))
                .clickable(role = Role.Button, onClick = onPlan)
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Glyph(Glyphs.Check, colour = Ink.Activity, size = 18)
                Label("Your plan", color = Ink.Activity, strong = true)
            }
            Text(
                text = plan.sentence,
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            Label("Tap to change it")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh, border = accent.copy(alpha = 0.55f))
            .clickable(role = Role.Button, onClick = onPlan)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Glyph(Glyphs.Spark, colour = accent, size = 18)
            Text(
                text = "Decide when, before you close this",
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
        Text(
            text = "Naming the exact moment roughly doubles the odds this actually happens. " +
                "Takes about a minute, and you write down what you think will go wrong.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Label("Plan it", color = accent, strong = true)
            Glyph(Glyphs.ArrowRight, colour = accent, size = 15)
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
