package com.debubble.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Care
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.CareState
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Feel
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Stage
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.centered
import kotlinx.coroutines.delay

/**
 * The moment after.
 *
 * Fogg's rule is that the reward has to land in seconds, so this screen exists to make the
 * act and the payoff one event: the room is shown as it was, then cleans itself in front of
 * you with a burst of sparkle, and anything newly earned appears. The celebration line comes
 * with it — a thing to say or do, because a feeling is the reward and a badge is only a
 * record of one.
 *
 * Then one optional question: better, same, or worse than before? That is behavioural
 * activation's activity-and-mood record reduced to a tap. Over a few weeks it becomes the
 * user's own evidence that doing the thing changes how they feel, which persuades better
 * than any study quoted at them.
 */
@Composable
fun CareDoneScreen(
    art: RoomArt,
    library: CareLibrary,
    stage: Stage,
    home: HomeState,
    domain: Domain,
    care: CareState,
    dirtBefore: Map<Domain, Int>,
    dirtAfter: Map<Domain, Int>,
    granted: List<String>,
    celebrate: String,
    onFeel: (Feel?) -> Unit
) {
    val info = library.info(domain)
    val accent = domain.accent
    val reduced = rememberReducedMotion()
    // Before, then after. The change is the reward, so it has to be seen happening.
    var cleaned by remember { mutableStateOf(reduced) }
    var bursting by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!reduced) {
            delay(750)
            cleaned = true
            bursting = true
            delay(2200)
            bursting = false
        }
    }
    val before = dirtBefore[domain] ?: 0
    val after = dirtAfter[domain] ?: 0
    val next = Care.nextRewardIn(care.reps)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 18.dp, bottom = Space.gutter),
        verticalArrangement = Arrangement.spacedBy(Space.gap),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HouseScene(
            art = art,
            stage = stage,
            home = home,
            dirt = if (cleaned) dirtAfter else dirtBefore,
            burst = bursting,
            description = "${info.name}: ${info.levels[after]}"
        )

        VSpace(6)
        Label(info.name, color = accent, strong = true)
        Text(
            text = if (before == after) info.levels[after]
            else "${info.levels[before]} to ${info.levels[after]}",
            color = Ink.Primary,
            style = MaterialTheme.typography.displayMedium.centered
        )
        Text(
            text = celebrate,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyLarge.centered
        )

        granted.mapNotNull { id -> art.catalogue.item(id) }.forEach { item ->
            VSpace(4)
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Glyph(Glyphs.Spark, colour = Ink.Gold, size = 15)
                        Label("New for your room", color = Ink.Gold, strong = true)
                    }
                    Text(
                        text = item.label,
                        color = Ink.Primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Label("It is already in there. Move it wherever you like.")
                }
            }
        }

        Label(
            when (next) {
                null -> "Everything for your ${info.name.lowercase()} is earned. The habit is yours now."
                1 -> "One more day of this and something new arrives."
                else -> "$next more days of this and something new arrives."
            }
        )

        VSpace(10)
        Text(
            text = "How do you feel now, compared with before?",
            color = Ink.Primary,
            style = MaterialTheme.typography.titleMedium.centered
        )
        Label("Your answers become your own evidence. Skip it any time.")
        Feel.entries.reversed().forEach { f ->
            SecondaryButton(f.label, colour = if (f == Feel.BETTER) accent else Ink.Primary) {
                onFeel(f)
            }
        }
        TextAction("Skip", onClick = { onFeel(null) })
    }
}
