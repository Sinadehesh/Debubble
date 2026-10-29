package com.debubble.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Forecasts
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Protocol
import com.debubble.app.engine.Stage
import com.debubble.app.engine.World
import com.debubble.app.ui.components.FieldBox
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.NerveSlider
import com.debubble.app.ui.components.PaperLabel
import com.debubble.app.ui.components.PaperText
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.paper
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.centered
import kotlinx.coroutines.delay

private const val POSTCARD_MAX = 90

/**
 * Back from a mission.
 *
 * The same moment as finishing a care task, so it looks the same: your room, celebrating,
 * and anything the outside sent back for it. Then the three things worth doing while it is
 * fresh — say the celebration out loud (Fogg: the feeling within seconds is what makes it
 * stick), check the guess you wrote before going against what happened (the comparison is
 * where the learning is), and, if you like, one line on a postcard for your journal.
 */
@Composable
fun CompletionScreen(
    art: RoomArt,
    stage: Stage,
    home: HomeState,
    dirt: Map<Domain, Int>,
    pillar: Pillar,
    tierCleared: Int,
    /** An item the outside sent back for the room, if one arrived. */
    reward: String?,
    /** The guess written before going, if there was one. */
    forecast: Forecast?,
    onFinish: (note: String, actual: String, distress: Int?) -> Unit
) {
    val reduced = rememberReducedMotion()
    var bursting by remember { mutableStateOf(!reduced) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(2400)
            bursting = false
        }
    }
    var note by remember { mutableStateOf("") }
    var actual by remember { mutableStateOf("") }
    var distress by remember { mutableIntStateOf(forecast?.predictedDistress ?: 4) }
    var checked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 18.dp, bottom = Space.gutter),
        verticalArrangement = Arrangement.spacedBy(Space.gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HouseScene(art = art, stage = stage, home = home, dirt = dirt, burst = bursting, description = "Your room")

        VSpace(4)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SpriteIcon(art, World.road(pillar), pixelScale = pixelScale(2f))
            Label("${World.roadName(pillar)} · level $tierCleared", color = pillar.accent, strong = true)
        }
        Text(
            text = when (pillar) {
                Pillar.ACCESS -> "You went somewhere new"
                Pillar.ACTIVITY -> "You did something new"
                Pillar.SOCIAL -> "You spoke to someone"
            },
            color = Ink.Primary,
            style = MaterialTheme.typography.displayMedium.centered
        )
        Text(
            text = "Your bubble is bigger than it was this morning.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyLarge.centered
        )

        // Said, not shown: the celebration is an instruction, so it is the loudest thing here.
        Column(
            modifier = Modifier.fillMaxWidth().paper().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PaperLabel("Say this out loud")
            Text(
                text = Protocol.celebration(pillar, tierCleared),
                color = Ink.PaperText,
                style = MaterialTheme.typography.titleLarge.centered
            )
            PaperLabel("Three seconds. It's what makes the next one easier.")
        }

        reward?.let { id -> art.catalogue.item(id) }?.let { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(fill = Ink.SurfaceHigh, border = Ink.Gold)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    SpriteIcon(art, item.id, pixelScale = if (item.w > 24) 4 else 6)
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Label("Sent back from out there", color = Ink.Gold, strong = true)
                    Text(item.label, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                    Label("It's already in your room. Move it wherever you like.")
                }
            }
        }

        if (forecast != null && !forecast.isResolved) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(fill = Ink.Surface, border = Ink.Ember.copy(alpha = 0.7f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Label("Your guess, before", color = Ink.Ember, strong = true)
                Text("\"${forecast.predicted}\"", color = Ink.Primary, style = MaterialTheme.typography.bodyLarge)
                Label("${forecast.predictedDistress}/10 · ${Forecasts.label(forecast.predictedDistress)}")
                NerveSlider(
                    label = "How hard was it really?",
                    value = distress,
                    onChange = {
                        distress = it
                        checked = true
                    },
                    accent = Ink.Access
                )
                FieldBox(
                    value = actual,
                    onChange = {
                        actual = it
                        checked = true
                    },
                    placeholder = "What actually happened? (optional)",
                    lines = 2
                )
                if (checked) Gap(forecast.predictedDistress, distress)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth().paper(fill = Ink.PaperShade).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SpriteIcon(art, World.POSTCARD, pixelScale = pixelScale(2f))
                PaperLabel("A postcard for your journal (optional)", color = Ink.PaperText)
            }
            FieldBox(
                value = note,
                onChange = { note = it },
                placeholder = "One line about how it went",
                onPaper = true,
                max = POSTCARD_MAX
            )
        }

        VSpace(4)
        PrimaryButton(text = "Back home", accent = pillar.accent) {
            onFinish(note, actual, if (forecast != null && checked) distress else null)
        }
    }
}

/** The gap between the guess and the real thing, said plainly as it is being chosen. */
@Composable
private fun Gap(predicted: Int, actual: Int) {
    val violation = predicted - actual
    val (colour, line) = when {
        violation >= Forecasts.MEANINGFUL_VIOLATION ->
            Ink.Activity to "$violation easier than you thought. That gap is the whole point: the fear " +
                "was wrong in the way fear usually is, and now it's written down."
        violation <= -Forecasts.MEANINGFUL_VIOLATION ->
            Ink.Ember to "${-violation} harder than you thought. That usually means the step was too big, " +
                "not that you are."
        actual >= Forecasts.REALLY_FEARED ->
            Ink.Secondary to "Hard, and you called it. Worth doing this one again before moving on."
        else -> Ink.Secondary to "About what you expected. On to a bigger one."
    }
    Text(text = line, color = colour, style = MaterialTheme.typography.bodyMedium)
}

/**
 * Checking a guess later: "you have mail". The guess comes first, in your own words, and only
 * then the question — remembering the forecast before reporting the outcome is what makes the
 * difference between them land.
 */
@Composable
fun ReviewScreen(
    forecast: Forecast,
    onResolve: (actual: String, distress: Int) -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var distress by remember { mutableIntStateOf(forecast.predictedDistress) }

    Column(modifier = Modifier.fillMaxSize().imePadding()) {
        TopBar(title = "You have mail", subtitle = forecast.challengeTitle.take(48), onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().paper().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PaperLabel("Before you went, you wrote")
                PaperText("\"${forecast.predicted}\"")
                PaperLabel("${forecast.predictedDistress}/10 · ${Forecasts.label(forecast.predictedDistress)}")
            }
            NerveSlider(label = "How hard was it really?", value = distress, onChange = { distress = it }, accent = Ink.Access)
            FieldBox(
                value = actual,
                onChange = { actual = it },
                placeholder = "What actually happened? Plainly, including if it went badly.",
                lines = 3
            )
            Column(modifier = Modifier.fillMaxWidth().panel().padding(14.dp)) {
                Gap(forecast.predictedDistress, distress)
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = Space.gutter, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryButton(text = "Save it", accent = Ink.Access) { onResolve(actual, distress) }
            // Never nag twice. Someone who does not want to answer gets to not answer.
            TextAction("I'd rather not log this one") { onDismiss() }
        }
    }
}

/**
 * Right after "it didn't go to plan": the jar, a spark going in, and what it means. The
 * reward lands on the attempt, because the attempt is what is being learned.
 */
@Composable
fun CourageScreen(art: RoomArt, friction: Int, reason: String, onDone: () -> Unit) {
    val reduced = rememberReducedMotion()
    val pop = remember { Animatable(if (reduced) 1f else 0.6f) }
    LaunchedEffect(Unit) {
        if (!reduced) pop.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 40.dp, bottom = Space.gutter),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SpriteIcon(art, World.jar(friction), pixelScale = pixelScale(9f), modifier = Modifier.scale(pop.value))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            SpriteIcon(art, World.SPARK, pixelScale = pixelScale(2.4f))
            Text("+1 spark of courage", color = Ink.Ember, style = MaterialTheme.typography.headlineMedium)
        }
        if (reason.isNotBlank()) {
            Text(
                text = "\"$reason.\" That took nerve.",
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium.centered
            )
        }
        Text(
            text = "You went to the edge of your bubble, and that's the part that changes it — " +
                "whatever came back. The next one will be the same size or smaller, never bigger.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyLarge.centered
        )
        Label(if (friction == 1) "Your first spark. Nobody who stays inside earns one." else "$friction sparks in your jar now.")
        VSpace(10)
        PrimaryButton(text = "Back home", accent = Ink.Ember, onClick = onDone)
    }
}
