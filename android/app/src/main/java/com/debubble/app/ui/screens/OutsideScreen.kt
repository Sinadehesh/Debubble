package com.debubble.app.ui.screens

import com.debubble.app.ui.theme.fill
import kotlin.math.floor
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import com.debubble.app.ui.components.castShadow
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PageHeader
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Forecasts
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Served
import com.debubble.app.engine.Stage
import com.debubble.app.engine.World
import com.debubble.app.ui.components.CourageJar
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.PageTip
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.RoomPainter
import com.debubble.app.ui.components.RoomShell
import com.debubble.app.ui.components.SignHeader
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.StatTile
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.roomGround
import com.debubble.app.ui.theme.wash
import com.debubble.app.ui.theme.Cut

/**
 * Outside: everything past your walls.
 *
 * The older half of the app — the three ladders, friction, predictions, the history — used
 * to live in a profile screen of bars and tiles in a different style from the room. It is
 * the same information here, drawn as the world the room opens onto: three roads out of your
 * bubble, a jar of courage, what your guesses turned out to be, and where you have been.
 * Seeing a long-term goal as a place you are travelling towards, with the distance covered
 * visible, is what keeps effort up between the days that feel like progress.
 */
@Composable
fun OutsideScreen(
    art: RoomArt,
    state: AppState,
    served: Map<Pillar, Served>,
    /** The road today's letter comes from. */
    todays: Pillar,
    showTip: Boolean,
    onTipSeen: () -> Unit,
    onOpenRoad: (Pillar) -> Unit,
    onOpenJourney: () -> Unit,
    onPickGoal: () -> Unit,
    mission: Served?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        PageHeader(art = art, icon = World.TAB_OUT, title = "Outside", subtitle = "Everything past your walls")

        if (showTip) {
            PageTip(
                art = art,
                title = "This is your bubble, from outside",
                body = "Every mission walks you a little further down one of three roads. The " +
                    "further you go, the more your room opens: a window, a door, then the street.",
                onDismiss = onTipSeen
            )
        }

        VSpace(6)
        SignHeader(art, World.TAB_HOME, "Your bubble", trailing = "${state.clearedRungs} missions")
        Stages(state.stage, state.clearedRungs)

        VSpace(10)
        SignHeader(art, World.TAB_OUT, "Three roads out")
        Pillar.order.forEach { p ->
            RoadRow(
                art = art,
                pillar = p,
                tier = state.state(p).tier,
                next = served[p],
                todays = p == todays,
                done = state.isDoneToday(p),
                onOpen = { onOpenRoad(p) }
            )
        }

        VSpace(10)
        SignHeader(art, World.SPARK, "Courage")
        CourageJar(art, state.friction)

        VSpace(10)
        SignHeader(art, World.POSTCARD, "What your guesses showed")
        Guesses(state)

        VSpace(10)
        SignHeader(art, state.primaryGoal?.let { World.landmark(it) } ?: World.BOOK, "Your journey")
        JourneyCard(art = art, state = state, mission = mission, onOpen = onOpenJourney, onPick = onPickGoal)

        VSpace(10)
        SignHeader(art, World.TAB_JOURNAL, "Recent trips", trailing = "${state.minutesInvested} min out there")
        Trips(art, state)
    }
}

/**
 * Every stage the room goes through, as little pictures: the ones reached in full, the one
 * you are in outlined, the ones ahead dimmed with the number of missions they open at. A
 * locked picture someone can see is a reason to go out again, and none of these can be lost.
 */
@Composable
private fun Stages(current: Stage, cleared: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Stage.all.forEach { st ->
            val reached = cleared >= st.threshold
            val here = st == current
            Column(
                modifier = Modifier.width(132.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .then(if (here) Modifier.castShadow(Cut.card, 3.dp, Ink.Border) else Modifier)
                        .clip(Cut.card)
                        .border(BorderStroke(if (here) 3.dp else 2.dp, if (reached) Ink.Border else Ink.Faint), Cut.card)
                ) {
                    RoomShell(st, modifier = Modifier.alpha(if (reached) 1f else 0.4f))
                }
                Text(
                    text = st.label,
                    color = if (reached) Ink.Primary else Ink.Muted,
                    style = MaterialTheme.typography.labelLarge
                )
                if (here) {
                    Pill("You are here", Ink.Gold, filled = true)
                } else {
                    Label(if (reached) "Been here" else "At ${st.threshold} missions")
                }
            }
        }
    }
}

/**
 * One road: its sign, how far along it you are, with a post every ten levels, and the next
 * letter waiting on it. The road today's letter came from is marked.
 */
@Composable
private fun RoadRow(
    art: RoomArt,
    pillar: Pillar,
    tier: Int,
    next: Served?,
    todays: Boolean,
    done: Boolean,
    onOpen: () -> Unit
) {
    val accent = pillar.accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(
                fill = if (todays && !done) accent.wash else Ink.Surface,
                borderWidth = if (todays && !done) 3.dp else 2.dp,
                shadow = if (todays && !done) Ink.Border else Ink.Shadow
            )
            .clickable(role = Role.Button, enabled = !done, onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(Cut.small)
                    .background(if (todays && !done) Ink.Surface else accent.wash)
                    .border(BorderStroke(2.dp, Ink.Border), Cut.small),
                contentAlignment = Alignment.Center
            ) {
                SpriteIcon(art, World.road(pillar), pixelScale = pixelScale(2f))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(World.roadName(pillar), color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                Label("Step $tier of ${Engine.MAX_TIER}", color = accent, strong = true)
            }
            when {
                done -> SpriteIcon(art, "icon_done", pixelScale = pixelScale(2f))
                todays -> Pill("Today's", accent, filled = true)
            }
        }
        Road(tier = tier, accent = accent)
        if (next != null && !done) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = next.directive,
                    modifier = Modifier.weight(1f),
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2
                )
                Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
            }
        }
    }
}

/**
 * The road itself, the way the room would draw it: a sunken track with a plum edge, filled
 * in the road's colour as far as you have walked, a post every ten steps, and you on it as a
 * block. Every edge lands on a whole pixel.
 */
@Composable
private fun Road(tier: Int, accent: Color) {
    val fillColour = accent.fill
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp)
            .drawBehind {
                fun px(v: Float) = floor(v)
                val line = px(2.dp.toPx()).coerceAtLeast(1f)
                val track = px(12.dp.toPx())
                val top = px((size.height - track) / 2f)
                val marker = px(size.height)
                val reach = size.width - marker
                val at = px(marker / 2f + reach * (tier.coerceIn(1, Engine.MAX_TIER) / Engine.MAX_TIER.toFloat()))
                // The track: plum edge, sunken middle, walked part in the road's colour.
                drawRect(Ink.Border, topLeft = Offset(0f, top), size = Size(size.width, track))
                drawRect(Ink.Well, topLeft = Offset(line, top + line), size = Size(size.width - line * 2, track - line * 2))
                drawRect(fillColour, topLeft = Offset(line, top + line), size = Size(at - line, track - line * 2))
                // A post every ten steps.
                for (post in 1..9) {
                    val x = px(marker / 2f + reach * post / 10f)
                    drawRect(
                        color = if (x <= at) Ink.Border else Ink.Faint,
                        topLeft = Offset(x - line / 2f, top + line),
                        size = Size(line, track - line * 2)
                    )
                }
                // You, as a block on the road.
                val left = px(at - marker / 2f)
                drawRect(Ink.Border, topLeft = Offset(left, 0f), size = Size(marker, marker))
                drawRect(Ink.Surface, topLeft = Offset(left + line, line), size = Size(marker - line * 2, marker - line * 2))
                drawRect(fillColour, topLeft = Offset(left + line * 2, line * 2), size = Size(marker - line * 4, marker - line * 4))
            }
    )
}

/**
 * What the guesses showed. The one number in the app that measures its actual claim: not
 * that going out stopped being frightening, but that the fear was wrong about it, in the
 * person's own words, checked by them.
 */
@Composable
private fun Guesses(state: AppState) {
    val done = Forecasts.resolved(state.forecasts)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (done.isNotEmpty()) {
            val mean = Forecasts.meanViolation(state.forecasts)
            Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
                StatTile("Checked", "${done.size}", Modifier.weight(1f))
                StatTile(
                    "Easier than feared",
                    "${(Forecasts.overestimateRate(state.forecasts) * 100).toInt()}%",
                    Modifier.weight(1f),
                    accent = Ink.Activity
                )
                StatTile(
                    "Average gap",
                    if (mean >= 0) "−%.1f".format(mean) else "+%.1f".format(-mean),
                    Modifier.weight(1f),
                    accent = if (mean >= 0) Ink.Activity else Ink.Ember
                )
            }
        }
        Text(text = Forecasts.summary(state.forecasts), color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
        if (done.isEmpty()) {
            Label("Before a mission, write what you think will happen. When you're back, check it.")
        }
    }
}

/** Where you have been: missions done and tried, with the postcard or the reason if there was one. */
@Composable
private fun Trips(art: RoomArt, state: AppState) {
    val trips = state.log.filter { it.isOutside }.take(12)
    if (trips.isEmpty()) {
        Text(
            text = "Nothing yet. Today's letter is the first trip.",
            color = Ink.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        trips.forEach { e ->
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                SpriteIcon(
                    art,
                    if (e.friction) World.SPARK else World.road(e.pillarEnum),
                    pixelScale = pixelScale(1.6f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(e.title, color = Ink.Primary, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                    if (e.note.isNotBlank()) {
                        Text("\"${e.note}\"", color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
                    }
                    Label(
                        (if (e.friction) "A spark · " else "") + when (e.kind) {
                            "MISSION" -> "Journey stop ${e.tier}"
                            "AUDIT", "CAMPAIGN" -> "Extra"
                            else -> "${World.roadName(e.pillarEnum)} · step ${e.tier}"
                        } + " · day ${state.dayIndexOf(e.epochDay)}",
                        color = if (e.friction) Ink.Ember else Ink.Muted
                    )
                }
            }
        }
    }
}

private fun AppState.dayIndexOf(epochDay: Long): Long =
    if (startedDay == 0L) 1L else (epochDay - startedDay + 1).coerceAtLeast(1L)
