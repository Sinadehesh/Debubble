package com.debubble.app.ui.screens

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
            .background(roomGround())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SpriteIcon(art, World.TAB_OUT, pixelScale = pixelScale(2.6f))
            Column {
                Text(
                    text = "Outside",
                    modifier = Modifier.semantics { heading() },
                    color = Ink.Primary,
                    style = MaterialTheme.typography.headlineMedium
                )
                Label("Everything past your walls")
            }
        }

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
                Shell(
                    st,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .alpha(if (reached) 1f else 0.35f)
                        .drawWithContent {
                            drawContent()
                            if (here) {
                                drawRoundRect(
                                    color = Ink.Gold,
                                    cornerRadius = CornerRadius(8.dp.toPx()),
                                    style = Stroke(width = 6.dp.toPx())
                                )
                            }
                        }
                )
                Text(
                    text = st.label,
                    color = if (here) Ink.Gold else if (reached) Ink.Primary else Ink.Muted,
                    style = MaterialTheme.typography.labelLarge
                )
                Label(
                    when {
                        here -> "You are here"
                        reached -> "Been here"
                        else -> "At ${st.threshold} missions"
                    }
                )
            }
        }
    }
}

/** A room shell, drawn whole and crisp at the largest whole scale that fits 132dp. */
@Composable
private fun Shell(stage: Stage, modifier: Modifier = Modifier) {
    val image = ImageBitmap.imageResource(RoomPainter.shellFor(stage))
    val density = LocalDensity.current
    val scale = with(density) { (132.dp.toPx() / image.width).toInt().coerceAtLeast(1) }
    val w = with(density) { (image.width * scale).toDp() }
    val h = with(density) { (image.height * scale).toDp() }
    Canvas(modifier = modifier.size(w, h)) {
        drawImage(
            image = image,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(image.width, image.height),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(image.width * scale, image.height * scale),
            filterQuality = FilterQuality.None
        )
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
            .panel(fill = Ink.Surface, border = if (todays && !done) accent else Ink.Border, borderWidth = if (todays && !done) 2.dp else 1.dp)
            .clickable(role = Role.Button, enabled = !done, onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(Space.radius))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                SpriteIcon(art, World.road(pillar), pixelScale = pixelScale(2f))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(World.roadName(pillar), color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                Label("${pillar.display} · level $tier of ${Engine.MAX_TIER}", color = accent)
            }
            when {
                done -> SpriteIcon(art, "icon_done", pixelScale = pixelScale(2f))
                todays -> Label("Today's", color = accent, strong = true)
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
                Glyph(Glyphs.ArrowRight, colour = Ink.Muted, size = 16)
            }
        }
    }
}

/** The road itself: a hundred levels, a post every ten, and you on it. */
@Composable
private fun Road(tier: Int, accent: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp)
            .drawBehind {
                val y = size.height / 2f
                val track = 6.dp.toPx()
                drawRoundRect(
                    color = Ink.Well,
                    topLeft = Offset(0f, y - track / 2f),
                    size = Size(size.width, track),
                    cornerRadius = CornerRadius(track / 2f)
                )
                val at = size.width * (tier.coerceIn(1, Engine.MAX_TIER) / Engine.MAX_TIER.toFloat())
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(0f, y - track / 2f),
                    size = Size(at, track),
                    cornerRadius = CornerRadius(track / 2f)
                )
                for (post in 1..9) {
                    val x = size.width * post / 10f
                    drawRect(
                        color = if (x <= at) accent else Ink.Faint,
                        topLeft = Offset(x - 1.dp.toPx(), y - size.height / 2f),
                        size = Size(2.dp.toPx(), size.height)
                    )
                }
                drawCircle(color = Ink.Paper, radius = size.height / 2.4f, center = Offset(at.coerceAtLeast(size.height / 2.4f), y))
                drawCircle(color = accent, radius = size.height / 3.6f, center = Offset(at.coerceAtLeast(size.height / 2.4f), y))
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
            .panel(fill = Ink.Surface, border = if (done.isEmpty()) Ink.Border else Ink.Access.copy(alpha = 0.6f))
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
                            else -> "${World.roadName(e.pillarEnum)} · level ${e.tier}"
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
