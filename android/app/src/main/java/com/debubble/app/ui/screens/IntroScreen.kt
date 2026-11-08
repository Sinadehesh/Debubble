package com.debubble.app.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Domain
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Stage
import com.debubble.app.engine.Today
import com.debubble.app.engine.World
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.Ticks
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.wash
import kotlinx.coroutines.delay

/**
 * The three-card explainer, shown once before anything else.
 *
 * Three cards is the smallest thing that answers "what is this", "what will I do every day"
 * and "why does it reward the thing I am afraid of" — and the third one matters most, because
 * a spark for a no is the mechanic nobody expects.
 *
 * Every card is the room: the first shows its walls opening, the second the real room and
 * the real icons, the third the jar on your shelf filling up. So the first thing anyone sees
 * is the world they are about to live in, and the home screen is already familiar when they
 * get there. The last card says what setup asks and how long it takes: people stay with a
 * sequence they can see the end of.
 */
@Composable
fun IntroScreen(art: RoomArt, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val steps = 3

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Space.gutter)
            .padding(top = 20.dp, bottom = Space.gutter)
    ) {
        Ticks(total = steps, done = step + 1, accent = Ink.Heart, height = 10)

        // Centred when it fits, scrollable when it does not: the second card is the longest
        // and must not be cut off on a small phone.
        BoxWithConstraints(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    // A fresh scroll per card, so the next one does not open halfway down.
                    .verticalScroll(remember(step) { ScrollState(0) })
                    .heightIn(min = maxHeight)
                    .padding(vertical = 18.dp),
                verticalArrangement = Arrangement.Center
            ) {
                when (step) {
                    0 -> IntroCard(
                        kicker = "1 of 3",
                        title = "Your room is your bubble",
                        body = "Your bubble is everywhere you already go and everything you " +
                            "already do. It is comfortable, and it gets smaller every year " +
                            "without you noticing.\n\n" +
                            "Here it is a room. Every small step outside opens it a little: " +
                            "a window, then a door, then the street.",
                        picture = { OpeningRoom(art) }
                    )

                    1 -> IntroCard(
                        kicker = "2 of 3",
                        title = "Four small things a day",
                        body = "Look after your body, your room and your heart, and take one " +
                            "small step outside. That is the whole day.\n\n" +
                            "You start in an empty room. Everything in it, you earn by doing " +
                            "those things for real.",
                        picture = { RoomArtPreview(art) },
                        extra = { FourList(art) }
                    )

                    else -> IntroCard(
                        kicker = "3 of 3",
                        title = "Getting turned down counts",
                        body = "Most apps only count wins. This one counts the awkward stuff " +
                            "too.\n\n" +
                            "Ask and get a no. Start a conversation that dies. Turn back " +
                            "halfway. Each of those is a spark of courage, and it goes in " +
                            "your jar. Nobody who stayed inside their bubble ever fills one.",
                        picture = { FillingJar(art) },
                        extra = { SetupAhead() }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            PrimaryButton(text = if (step == steps - 1) "Set me up" else "Next") {
                if (step == steps - 1) onDone() else step++
            }
            if (step > 0) {
                TextAction("Back") { step-- }
            } else {
                TextAction("Skip the intro") { onDone() }
            }
        }
    }
}

@Composable
private fun IntroCard(
    kicker: String,
    title: String,
    body: String,
    picture: @Composable () -> Unit,
    extra: (@Composable () -> Unit)? = null
) {
    Column {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { picture() }
        VSpace(22)
        Label(kicker, color = Ink.Heart, strong = true)
        VSpace(6)
        Text(
            text = title,
            modifier = Modifier.semantics { heading() },
            color = Ink.Primary,
            style = MaterialTheme.typography.displayMedium
        )
        VSpace(12)
        Text(
            text = body,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyLarge
        )
        if (extra != null) {
            VSpace(18)
            extra()
        }
    }
}

/**
 * Your room, opening: the same you and the same heart, the walls going stage by stage, the
 * way they will for real. Holds still on the open door under reduced motion.
 */
@Composable
private fun OpeningRoom(art: RoomArt) {
    val stages = listOf(Stage.SEALED, Stage.WINDOW, Stage.DOOR, Stage.STREET)
    val reduced = rememberReducedMotion()
    var at by remember { mutableIntStateOf(if (reduced) 2 else 0) }
    LaunchedEffect(reduced) {
        if (!reduced) while (true) {
            delay(1600)
            at = (at + 1) % stages.size
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HouseScene(
            art = art,
            stage = stages[at],
            home = HomeState(),
            dirt = Domain.order.associateWith { 0 },
            modifier = Modifier.width(300.dp),
            description = "An empty room with you in it, opening up: a window, then a door, then the street."
        )
        Label("${at + 1} of ${stages.size} · ${stages[at].label}", color = Ink.Primary, strong = true)
    }
}

/** The room you will move into, as it will look: you, your heart, today's letter. */
@Composable
private fun RoomArtPreview(art: RoomArt) {
    HouseScene(
        art = art,
        stage = Stage.SEALED,
        home = HomeState(),
        dirt = Domain.order.associateWith { 1 },
        letter = true,
        modifier = Modifier.width(300.dp),
        description = "An empty room, with you in it, your heart beside you, and a letter in your hand."
    )
}

/** The jar on your shelf, a spark at a time. Full under reduced motion. */
@Composable
private fun FillingJar(art: RoomArt) {
    val reduced = rememberReducedMotion()
    var level by remember { mutableIntStateOf(if (reduced) 4 else 0) }
    LaunchedEffect(reduced) {
        if (!reduced) while (true) {
            delay(900)
            level = (level + 1) % 5
        }
    }
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, World.SPARK, pixelScale = pixelScale(4f))
        SpriteIcon(art, "jar$level", pixelScale = pixelScale(9f))
        SpriteIcon(art, World.SPARK, pixelScale = pixelScale(3f))
    }
}

/** The four, each with the icon it has on the home screen and where it lives in the room. */
@Composable
private fun FourList(art: RoomArt) {
    val rows = listOf(
        Triple(Today.icon(Domain.BODY), Domain.BODY.accent, "Body" to "Wash, water, move, eat, sleep. You are your body."),
        Triple(Today.icon(Domain.ROOM), Domain.ROOM.accent, "Room" to "A few minutes of tidying your real room. The mess is your room."),
        Triple(Today.icon(Domain.HEART), Domain.HEART.accent, "Heart" to "Something kind for your mind. The little heart is yours."),
        Triple(Today.ICON_OUT, Ink.Gold, "Outside" to "One small step out of your bubble. The letter in your hand.")
    )
    val scale = pixelScale(2.2f)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { (icon, colour, text) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(fill = colour.wash)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(modifier = Modifier.width(36.dp), contentAlignment = Alignment.Center) {
                    SpriteIcon(art, icon, pixelScale = scale)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = text.first, color = colour, style = MaterialTheme.typography.labelLarge)
                    Text(text = text.second, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

/** What setup asks, and how long it takes, so the questions ahead have an end in sight. */
@Composable
private fun SetupAhead() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Label("Next, about three minutes", color = Ink.Primary, strong = true)
        Text(
            text = "A few questions so the missions fit you, a journey to aim for, then you " +
                "move in and pick how you look. After that, a short tour shows you around.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
