package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.audio.rememberSound
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Forecasts
import com.debubble.app.engine.Served
import com.debubble.app.engine.Tour
import com.debubble.app.engine.World
import com.debubble.app.ui.components.FieldBox
import com.debubble.app.ui.components.FirstTimeSteps
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.LetterCard
import com.debubble.app.ui.components.NerveSlider
import com.debubble.app.ui.components.OptionRow
import com.debubble.app.ui.components.PaperText
import com.debubble.app.ui.components.PaperTitle
import com.debubble.app.ui.components.RemindHost
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.Shockwave
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TaskFooter
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.components.rememberHaptics
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * A mission, as the letter you were holding in the room.
 *
 * Top to bottom it is the order the decision happens in: what it is and why (the letter),
 * how to do it (the steps), what you expect (the guess), and then the two honest ends — I
 * did it, or it did not go to plan — with "remind me" beside "I did it" for the far more
 * common third answer, "not now".
 *
 * The old version of this screen was a dark dashboard of boxes in a different visual
 * language from the room. The content is the same; it now looks like it came from there.
 */
@Composable
fun ChallengeScreen(
    art: RoomArt,
    served: Served,
    /** Where it sits: "Someone new · level 4", "Your journey · step 4 of 30". */
    kicker: String,
    soundOn: Boolean,
    ambientOn: Boolean,
    /** A guess already written for this one, if there is one. */
    forecast: Forecast?,
    remind: RemindHost,
    onPredict: (predicted: String, distress: Int) -> Unit,
    onCommit: (Int) -> Unit,
    onFriction: (reason: String) -> Unit,
    onSwap: (() -> Unit)?,
    onBack: () -> Unit,
    /** No mission tried yet: say how this screen works, in order. */
    firstTime: Boolean = false
) {
    val pillar = served.pillar
    val accent = pillar.accent
    val haptics = rememberHaptics()
    val sound = rememberSound()

    var committed by remember { mutableStateOf(false) }
    var frictionOpen by remember { mutableStateOf(false) }

    // The bed is opt-in and tied to this screen only; it never survives leaving it.
    LaunchedEffect(ambientOn, served.exposure) {
        if (ambientOn) sound.startBed(served.exposure / 10f) else sound.stopBed()
    }
    DisposableEffect(Unit) { onDispose { sound.stopBed() } }

    Box(modifier = Modifier.fillMaxSize().background(Ink.Void)) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            TopBar(
                title = if (served.kind == "MISSION") "Your journey" else "Today's letter",
                subtitle = kicker,
                accent = accent,
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
                            Label("Another one", color = Ink.Primary, strong = true)
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter)
                    .padding(top = 6.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (firstTime) FirstTimeSteps(Tour.OUTSIDE_HOW, accent)

                LetterCard(kicker = served.phase ?: "For today", accent = accent) {
                    PaperTitle(served.directive)
                    Facts(served)
                    PaperText(served.coach, muted = true)
                }

                if (served.substituted && served.substitutionReason != null) {
                    Note(Glyphs.Check, accent, served.substitutionReason)
                }
                if (served.repTarget > 0) {
                    Note(
                        Glyphs.Spark,
                        Ink.Gold,
                        "Aim for ${served.repTarget} tries today. Count them on your journey."
                    )
                }

                if (served.hasProtocol) Steps(served, accent)

                Guess(served = served, forecast = forecast, onPredict = onPredict)

                if (served.ease.isNotBlank()) Ease(served.ease)

                // The second honest end. Not a grey link under a fold: the app most wants
                // people to feel able to take this one.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Space.tap)
                        .panel(fill = Ink.SurfaceHigh, border = Ink.Ember)
                        .clickable(role = Role.Button) { frictionOpen = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpriteIcon(art, World.SPARK, pixelScale = pixelScale(2.2f))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("It didn't go to plan", color = Ink.Ember, style = MaterialTheme.typography.titleMedium)
                        Label("A no, a silence, turning back. It still counts.")
                    }
                    Glyph(Glyphs.ArrowRight, colour = Ink.Ember, size = 16)
                }

                TextAction("Not today", onClick = onBack)
            }

            TaskFooter(
                art = art,
                accent = accent,
                host = remind,
                holdLabel = "I did it",
                onDone = {
                    if (!committed) {
                        committed = true
                        if (soundOn) sound.chime()
                        onCommit(served.minutes)
                    }
                },
                modifier = Modifier
                    .padding(horizontal = Space.gutter)
                    .padding(top = 8.dp, bottom = 12.dp)
                    .navigationBarsPadding()
            )
        }

        if (committed) Shockwave(accent = accent, modifier = Modifier.fillMaxSize())
    }

    if (frictionOpen) {
        FrictionSheet(
            art = art,
            onPick = { reason ->
                frictionOpen = false
                haptics.friction()
                if (soundOn) sound.stab()
                onFriction(reason)
            },
            onDismiss = { frictionOpen = false }
        )
    }
}

/** Time, cost and nerve, as tags on the letter. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Facts(served: Served) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Tag(Engine.formatMinutes(served.minutes))
        Tag(if (served.cost == 0) "Free" else "Costs ${served.cost}")
        Tag("Nerve ${served.exposure}/10")
    }
}

@Composable
private fun Tag(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Ink.PaperShade)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text, color = Ink.PaperText, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Note(glyph: Glyphs, colour: Color, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Glyph(glyph, colour = colour, size = 18)
        Text(text = text, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * How to do it: the moment, the first two minutes, the words, the finish line, and the thing
 * to leave behind — numbered, because they happen in that order.
 *
 * A cue with no moment in it competes with the rest of the day and loses; a first step that
 * is not trivially small gets put off; a task with no finish line can only be abandoned; and
 * a frightening thing survived while holding a safety behaviour teaches that the safety
 * behaviour was what saved you. [com.debubble.app.engine.Protocol] carries the sources.
 */
@Composable
private fun Steps(served: Served, accent: Color) {
    val steps = buildList {
        if (served.anchor.isNotBlank()) add("When" to "When ${served.anchor}.")
        if (served.opener.isNotBlank()) add("First two minutes" to served.opener)
        if (served.script.isNotBlank()) add("Words, if you want them" to served.script)
        if (served.done.isNotBlank()) add("Done when" to served.done.replaceFirstChar { it.uppercase() })
        if (served.drop.isNotBlank()) add("Leave this behind" to served.drop)
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "How to do it",
            modifier = Modifier.semantics { heading() },
            color = Ink.Primary,
            style = MaterialTheme.typography.titleMedium
        )
        steps.forEachIndexed { i, (label, text) ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(if (label == "Leave this behind") Ink.Ember else accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", color = Ink.OnAccent, style = MaterialTheme.typography.labelLarge)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Label(label, color = if (label == "Leave this behind") Ink.Ember else accent, strong = true)
                    Text(text = text, color = Ink.Primary, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/**
 * The guess, before. Fear makes a specific, confident prediction and then quietly forgets it
 * when things go fine, so nothing is learned; writing it down first, and checking it after,
 * is what turns a mission that went fine into evidence (Craske et al., 2014, on expectancy
 * violation). Optional, and short.
 */
@Composable
private fun Guess(served: Served, forecast: Forecast?, onPredict: (String, Int) -> Unit) {
    var text by remember(served.directive) { mutableStateOf("") }
    var distress by remember(served.directive) { mutableIntStateOf(6) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Surface, border = Ink.Ember.copy(alpha = 0.7f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Label("Before you go", color = Ink.Ember, strong = true)
        if (forecast != null) {
            Text("Your guess", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Text("\"${forecast.predicted}\"", color = Ink.Primary, style = MaterialTheme.typography.bodyLarge)
            Label("${forecast.predictedDistress}/10 · ${Forecasts.label(forecast.predictedDistress)}")
            Label("When you're back, you'll see how close it was.")
        } else {
            Text(
                text = "What do you think will happen?",
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            Label("Write the bad version. You'll check it against what really happens, which is how a fear gets smaller.")
            FieldBox(
                value = text,
                onChange = { text = it },
                placeholder = served.test.ifBlank { "They'll think I'm strange and I'll have to leave." },
                lines = 2,
                accent = Ink.Ember
            )
            NerveSlider(label = "How hard will it be?", value = distress, onChange = { distress = it })
            SecondaryButton(
                text = "Save my guess",
                colour = if (text.isBlank()) Ink.Muted else Ink.Ember
            ) { if (text.isNotBlank()) onPredict(text.trim(), distress) }
        }
    }
}

/**
 * The smaller step, offered before it is needed: by the time someone has decided they cannot
 * do the bigger one, they have closed the app. Not a button — logging it against a rung they
 * are not on would make the ladder lie about where they are.
 */
@Composable
private fun Ease(ease: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Well, border = Ink.Faint)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Label("Too big today?", color = Ink.Primary, strong = true)
        Text(text = ease, color = Ink.Secondary, style = MaterialTheme.typography.bodyLarge)
        Label("Do that instead. It counts as the day, and your level stays where it is.")
    }
}

/**
 * "It didn't go to plan." One tap on what happened, and it is logged as courage: approaching
 * the thing is the point, whatever came back (that is what exposure rewards), and naming it
 * kindly rather than as failure is what keeps people coming back after a bad one.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrictionSheet(art: RoomArt, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Ink.Surface,
        contentColor = Ink.Primary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteIcon(art, World.jar(1), pixelScale = pixelScale(3.4f))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "That still counts",
                        modifier = Modifier.semantics { heading() },
                        color = Ink.Ember,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = "You went to the edge. It goes in your courage jar, and the next one " +
                            "gets easier, not harder.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Label("What happened?", color = Ink.Primary, strong = true)
            REASONS.forEach { reason -> OptionRow(label = reason, onClick = { onPick(reason) }) }
        }
    }
}

private val REASONS = listOf(
    "I asked and got a no",
    "It got awkward",
    "I turned back",
    "I couldn't make myself start",
    "It was too big today"
)
