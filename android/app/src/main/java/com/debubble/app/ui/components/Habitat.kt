package com.debubble.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.debubble.app.R
import com.debubble.app.engine.Habitat
import com.debubble.app.engine.Stage
import com.debubble.app.ui.theme.Ink

/**
 * The room, in pixel art.
 *
 * Two previous versions of this were wrong in opposite directions. The first was a
 * procedural Canvas drawing whose palette was solved against the WCAG floors, which made its
 * ground move about 2% across seven stages — defensible and invisible. The second was seven
 * rendered images: visible, and navy, cinematic and gloomy, which is not what a room you are
 * meant to want to come back to should feel like.
 *
 * This one is a sunny flat drawn at 160x100 pixels, and the things in it are yours. Progress
 * opens the walls, and it also hands you things to put in the room — which is the part the
 * app had been missing entirely, because clearing a rung used to increment a counter and
 * nothing else.
 *
 * Two rules the renderer must keep, both of which silently destroy pixel art:
 *
 *  - `FilterQuality.None`. Any filtering turns a one-pixel outline into a three-pixel smear,
 *    and the whole thing stops reading as pixel art and starts reading as a blurry JPEG.
 *  - Nothing is drawn over it, and there is no scrim. The old hero faded its bottom edge into
 *    the page because text sat underneath; here the room has a hard edge and a floor, and
 *    fading the floor out would cut the legs off whatever is standing on it.
 */
@Composable
fun HabitatHero(stage: Stage, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(artFor(stage)),
        contentDescription = "${stage.label}. ${stage.note}",
        contentScale = ContentScale.FillWidth,
        // The one line that decides whether this is pixel art or a smear.
        filterQuality = FilterQuality.None,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(ROOM_W.toFloat() / ROOM_H)
    )
}

/** The room's pixel dimensions. Must match `room` in assets/pixel/items.json. */
const val ROOM_W = 160
const val ROOM_H = 100

/**
 * The room shell for a stage. Every stage has one — they are generated together by
 * android/tools/pixel/room.py, so there is no missing case to fall back from.
 */
@DrawableRes
private fun artFor(stage: Stage): Int = when (stage) {
    Stage.SEALED -> R.drawable.room_sealed
    Stage.WINDOW -> R.drawable.room_window
    Stage.DOOR -> R.drawable.room_door
    Stage.STREET -> R.drawable.room_street
    Stage.OPEN -> R.drawable.room_open
    Stage.HORIZON -> R.drawable.room_horizon
    Stage.UNWALLED -> R.drawable.room_unwalled
}

/**
 * The room's name, and how far off the next one is.
 *
 * Says the number of rungs rather than a percentage, because "fourteen more" is a thing a
 * person can picture doing and "62%" is a thing they can only watch. There is no way to lose
 * a stage, which is why this can be shown without it becoming something to protect.
 */
@Composable
fun HabitatRow(stage: Stage, cleared: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Color(stage.ground), border = Color(stage.structure))
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stage.label,
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            Label("$cleared cleared")
        }
        Text(
            text = stage.note,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        ProgressTrack(Habitat.progress(cleared), Ink.Secondary, height = 6)
        Label(Habitat.caption(cleared))
    }
}

/**
 * Shown once, on the day the room opens.
 *
 * Deliberately quiet — no confetti, no full-screen takeover. The room is already different
 * behind it; the banner only names what changed, so the user does not have to wonder whether
 * they imagined it.
 */
@Composable
fun HabitatAnnouncement(stage: Stage, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Color(stage.horizon), border = Ink.Gold)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Gold, size = 18)
            Label("The room changed", color = Ink.Gold, strong = true)
        }
        Text(
            text = stage.label,
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stage.note,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        SecondaryButton("Got it", onClick = onDismiss)
    }
}
