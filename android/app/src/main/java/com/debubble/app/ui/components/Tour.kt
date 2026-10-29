package com.debubble.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.debubble.app.engine.Area
import com.debubble.app.engine.Look
import com.debubble.app.engine.RoomTap
import com.debubble.app.engine.Tour
import com.debubble.app.engine.TourAction
import com.debubble.app.engine.TourSpot
import com.debubble.app.engine.TourStep
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import kotlinx.coroutines.flow.first
import kotlin.math.roundToInt

/**
 * Where the parts of the home screen are, for the tour to point at.
 *
 * Only provided while the tour is showing, so nothing outside it pays for the bookkeeping.
 * Bounds are in root coordinates and unclipped: a row below the fold still has one, which is
 * how the tour knows how far to scroll to it.
 */
@Stable
class TourTargets {
    val rects = mutableStateMapOf<TourSpot, Rect>()

    /** The scrolling column most of the spots live in, and where it sits on screen. */
    var scroll: ScrollState? by mutableStateOf(null)
    var viewport: Rect? by mutableStateOf(null)

    /** The room picture at [origin], drawn at [scale], and the tasks drawn in it. */
    fun room(origin: Offset, scale: Int, w: Int, h: Int, spots: Map<RoomTap, Area>) {
        rects[TourSpot.ROOM] = Rect(origin, Size((w * scale).toFloat(), (h * scale).toFloat()))
        IN_ROOM.forEach { (tap, spot) ->
            val a = spots[tap]
            if (a == null) {
                rects.remove(spot)
            } else {
                rects[spot] = Rect(
                    left = origin.x + a.x * scale,
                    top = origin.y + a.y * scale,
                    right = origin.x + (a.x + a.w) * scale,
                    bottom = origin.y + (a.y + a.h) * scale
                )
            }
        }
    }

    private companion object {
        val IN_ROOM = mapOf(
            RoomTap.BODY to TourSpot.ME,
            RoomTap.ROOM to TourSpot.MESS,
            RoomTap.HEART to TourSpot.BUDDY,
            RoomTap.OUTSIDE to TourSpot.LETTER
        )
    }
}

val LocalTourTargets = compositionLocalOf<TourTargets?> { null }

/** Report where this is, for the tour. Does nothing when no tour is showing. */
fun Modifier.tourSpot(spot: TourSpot): Modifier = composed {
    val targets = LocalTourTargets.current
    if (targets == null) Modifier
    else Modifier.onGloballyPositioned { c -> targets.rects[spot] = Rect(c.positionInRoot(), c.size.toSize()) }
}

/** Mark the column the spots scroll in. Put it before `verticalScroll`, so it measures the window. */
fun Modifier.tourViewport(scroll: ScrollState): Modifier = composed {
    val targets = LocalTourTargets.current
    if (targets == null) Modifier
    else {
        SideEffect { targets.scroll = scroll }
        Modifier.onGloballyPositioned { c -> targets.viewport = Rect(c.positionInRoot(), c.size.toSize()) }
    }
}

/**
 * The tour: the screen dimmed, one part of it lit, and a card beside it saying what it is
 * and what to do with it. See [Tour] for why it is built this way.
 *
 * Touches on the screen underneath are held while it shows, so a tap meant for Next cannot
 * land on a task by accident; the card's own buttons, and Back, are the only way through.
 */
@Composable
fun TourOverlay(
    steps: List<TourStep>,
    targets: TourTargets,
    art: RoomArt,
    /** The colour of today's mission outside, for the steps about it. */
    outside: Color,
    /** How you look, so the "this is you" step shows you and not the default. */
    look: Look,
    onAction: (TourAction) -> Unit,
    onDone: () -> Unit
) {
    val density = LocalDensity.current
    val reduced = rememberReducedMotion()

    // Wait for the screen to be measured, then settle which steps can be shown: the mess is
    // only there if the room is messy, the letter only while the mission is open.
    var shown by remember { mutableStateOf<List<TourStep>?>(null) }
    LaunchedEffect(steps) {
        snapshotFlow { targets.rects.keys.toSet() }.first { TourSpot.ROOM in it && TourSpot.TODAY in it }
        withFrameNanos { }
        shown = Tour.visible(steps, targets.rects.keys.toSet())
    }

    var index by rememberSaveable { mutableIntStateOf(0) }
    val list = shown
    val step = list?.getOrNull(index.coerceIn(0, (list.size - 1).coerceAtLeast(0)))

    fun back() {
        index = (index - 1).coerceAtLeast(0)
    }
    fun forward() {
        if (index < (shown?.lastIndex ?: 0)) index += 1 else onDone()
    }

    BackHandler { if (index > 0) back() else onDone() }

    // Scroll the spot into view, leaving room for the card on whichever side it goes.
    val cardRoom = with(density) { 300.dp.toPx() }
    val margin = with(density) { 20.dp.toPx() }
    LaunchedEffect(step) {
        val spot = step?.spot ?: return@LaunchedEffect
        val scroll = targets.scroll ?: return@LaunchedEffect
        val view = targets.viewport ?: return@LaunchedEffect
        val r = targets.rects[spot] ?: return@LaunchedEffect
        if (spot == TourSpot.TABS || spot == TourSpot.NONE) return@LaunchedEffect
        val top = r.top - view.top + scroll.value
        val want = if (top + r.height + cardRoom <= view.height) 0
        else (top - margin).roundToInt().coerceIn(0, scroll.maxValue)
        if (reduced) scroll.scrollTo(want) else scroll.animateScrollTo(want, tween(420))
    }

    var origin by remember { mutableStateOf(Offset.Zero) }
    var height by remember { mutableIntStateOf(0) }
    var cardHeight by remember { mutableIntStateOf(0) }

    val pad = with(density) { 6.dp.toPx() }
    val hole = step?.takeIf { it.spot != TourSpot.NONE }
        ?.let { targets.rects[it.spot] }
        ?.let { r -> Rect(r.topLeft - origin, r.size).inflate(pad) }
    val lit by animateValueAsState(
        targetValue = hole ?: Rect.Zero,
        typeConverter = Rect.VectorConverter,
        animationSpec = if (reduced) snap() else spring(stiffness = 500f),
        label = "spotlight"
    )
    val glow by rememberInfiniteTransition(label = "tour").animateFloat(
        initialValue = 0f,
        targetValue = if (reduced) 0f else 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Restart),
        label = "glow"
    )
    val accent = step?.let { accentOf(it.spot, outside) } ?: Ink.Gold

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { c ->
                origin = c.positionInRoot()
                height = c.size.height
            }
            // Hold every touch that is not on the card.
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Ink.Void.copy(alpha = 0.84f))
            if (hole != null) {
                val corner = CornerRadius(12.dp.toPx())
                drawRoundRect(
                    color = Color.Black,
                    topLeft = lit.topLeft,
                    size = lit.size,
                    cornerRadius = corner,
                    blendMode = BlendMode.Clear
                )
                drawRoundRect(
                    color = accent,
                    topLeft = lit.topLeft,
                    size = lit.size,
                    cornerRadius = corner,
                    style = Stroke(width = 2.5.dp.toPx())
                )
                // A ring that breathes outwards, so the eye finds the spot before the words.
                if (!reduced) {
                    val grow = glow * 10.dp.toPx()
                    drawRoundRect(
                        color = accent.copy(alpha = (1f - glow) * 0.6f),
                        topLeft = lit.topLeft - Offset(grow, grow),
                        size = Size(lit.width + grow * 2, lit.height + grow * 2),
                        cornerRadius = CornerRadius(12.dp.toPx() + grow),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
        }

        if (list != null && step != null) {
            val gap = with(density) { 14.dp.toPx() }
            val edge = with(density) { 12.dp.toPx() }
            val below = hole == null || height - hole.bottom >= hole.top
            val y = when {
                hole == null -> (height - cardHeight) / 2f
                below -> hole.bottom + gap
                else -> hole.top - gap - cardHeight
            }.coerceIn(edge, (height - cardHeight - edge).coerceAtLeast(edge))

            TourCard(
                step = step,
                number = index + 1,
                of = list.size,
                art = art,
                look = look,
                accent = accent,
                onBack = if (index > 0) ::back else null,
                onNext = {
                    when (step.action) {
                        TourAction.START_NEXT -> onAction(TourAction.START_NEXT)
                        TourAction.ADD_WIDGET -> {
                            onAction(TourAction.ADD_WIDGET)
                            forward()
                        }
                        null -> forward()
                    }
                },
                onLater = when (step.action) {
                    TourAction.START_NEXT -> onDone
                    TourAction.ADD_WIDGET -> ::forward
                    null -> null
                },
                onSkip = onDone,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset { IntOffset(0, y.roundToInt()) }
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .onSizeChanged { cardHeight = it.height }
                    // Hidden for the one frame before it knows its height, so it never
                    // flashes over the thing it is pointing at.
                    .alpha(if (cardHeight == 0) 0f else 1f)
            )
        }
    }
}

@Composable
private fun TourCard(
    step: TourStep,
    number: Int,
    of: Int,
    art: RoomArt,
    look: Look,
    accent: Color,
    onBack: (() -> Unit)?,
    onNext: () -> Unit,
    onLater: (() -> Unit)?,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .panel(fill = Ink.SurfaceHigh, border = accent, borderWidth = 2.dp)
            .semantics {
                paneTitle = "Tour"
                liveRegion = LiveRegionMode.Polite
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Label("$number of $of", color = accent, strong = true)
            if (number < of) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(Space.radius))
                        .clickable(role = Role.Button, onClick = onSkip)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Label("Skip the tour")
                }
            }
        }
        // Where you are in it, as segments: the end in sight is what keeps people going.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(of) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (i < number) accent else Ink.Faint)
                )
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (step.icon != null && art.rect(step.icon) != null) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(Space.radius))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    val tall = (art.rect(step.icon)?.get(3) ?: 0) > 14
                    SpriteIcon(art, step.icon, pixelScale = pixelScale(if (tall) 1.6f else 2.6f), look = look)
                }
            }
            Text(
                text = step.title,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() },
                color = Ink.Primary,
                style = MaterialTheme.typography.titleLarge
            )
        }
        Text(text = step.body, color = Ink.Secondary, style = MaterialTheme.typography.bodyLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            if (onBack != null) {
                Box(modifier = Modifier.weight(1f)) { SecondaryButton("Back", onClick = onBack) }
            }
            Box(modifier = Modifier.weight(2f)) { PrimaryButton(step.button, accent = accent, onClick = onNext) }
        }
        if (onLater != null) {
            TextAction(
                text = if (step.action == TourAction.START_NEXT) "I'll do it later" else "Not now",
                onClick = onLater
            )
        }
    }
}

private fun accentOf(spot: TourSpot, outside: Color): Color = when (spot) {
    TourSpot.ME, TourSpot.ROW_BODY -> Ink.Body
    TourSpot.MESS, TourSpot.ROW_ROOM -> Ink.Room
    TourSpot.BUDDY, TourSpot.ROW_HEART -> Ink.Heart
    TourSpot.LETTER, TourSpot.ROW_OUTSIDE -> outside
    else -> Ink.Gold
}

/**
 * The first time on a task screen: what to do here, in order, in the words of the buttons on
 * it. Gone after the first one is done, when it would only be in the way.
 */
@Composable
fun FirstTimeSteps(steps: List<String>, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = accent.copy(alpha = 0.10f), border = accent.copy(alpha = 0.6f))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Label("First time? Here's how", color = accent, strong = true)
        steps.forEachIndexed { i, s ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${i + 1}", color = Ink.OnAccent, style = MaterialTheme.typography.labelMedium)
                }
                Text(
                    text = s,
                    modifier = Modifier.weight(1f),
                    color = Ink.Primary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
