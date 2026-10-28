package com.debubble.app.ui.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.debubble.app.R
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Draw
import com.debubble.app.engine.Home
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.ItemCatalogue
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Spot
import com.debubble.app.engine.Stage
import com.debubble.app.ui.theme.Ink
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

/**
 * Your room, drawn in pixel art: the shell for your stage, everything you have earned, you,
 * and the state of your body, room and heart.
 *
 * It is a mirror. When the real ones are neglected this one gets messy, and doing the real
 * task is what cleans it. See engine/Care.kt for why that is a self-monitoring device and not
 * a guilt trip, and engine/Home.kt for [Scene], which decides what is drawn and in what order.
 *
 * Two rendering rules, both of which silently ruin pixel art:
 *
 *  - **Integer scale.** The room is 160x100 and is drawn at the largest whole multiple that
 *    fits. At 6.75x some pixels would come out six screen pixels wide and some seven, which
 *    shimmers the moment anything moves. The leftover width is margin.
 *  - **`FilterQuality.None`** on every draw. Any filtering turns a one-pixel outline into a
 *    three-pixel smear.
 */
class RoomArt(val atlas: ImageBitmap, val catalogue: ItemCatalogue) {

    private val rects: Map<String, IntArray> = buildMap {
        catalogue.items.forEach { put(it.id, intArrayOf(it.x, it.y, it.w, it.h)) }
        catalogue.fx.forEach { put(it.id, intArrayOf(it.x, it.y, it.w, it.h)) }
    }

    /** x, y, w, h of a sprite in the atlas, item or effect alike. */
    fun rect(id: String): IntArray? = rects[id]
}

/** Decode the atlas once. Not density-scaled: these are pixels, not artwork. */
@Composable
fun rememberRoomArt(catalogue: ItemCatalogue): RoomArt {
    val context = LocalContext.current
    return remember(catalogue) {
        val options = BitmapFactory.Options().apply { inScaled = false }
        val bitmap = context.assets.open(catalogue.atlas).use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: error("could not decode ${catalogue.atlas}")
        RoomArt(bitmap.asImageBitmap(), catalogue)
    }
}

/**
 * A slow tick for the room's idle life: you bob, the weather drifts, sparkles twinkle.
 * Frozen at zero when the system asks for reduced motion.
 */
@Composable
fun rememberTicker(running: Boolean, periodMillis: Long = 560L): Int {
    val tick by produceState(0, running) {
        value = 0
        if (!running) return@produceState
        while (true) {
            delay(periodMillis)
            value += 1
        }
    }
    return tick
}

@Composable
fun HouseScene(
    art: RoomArt,
    stage: Stage,
    home: HomeState,
    dirt: Map<Domain, Int>,
    modifier: Modifier = Modifier,
    description: String = "",
    /** Sparkles everywhere, for the moment right after something was cleaned. */
    burst: Boolean = false,
    /** Drag things around. Off on the dashboard, where a scroll must stay a scroll. */
    editable: Boolean = false,
    selected: String? = null,
    onSelect: (String?) -> Unit = {},
    onMove: (id: String, x: Int, b: Int) -> Unit = { _, _, _ -> }
) {
    val cat = art.catalogue
    val room = cat.room
    val shell = ImageBitmap.imageResource(shellFor(stage))
    val frame = rememberTicker(running = !rememberReducedMotion())

    // The item under the finger and where it is right now. Kept until the saved state comes
    // back, so a dropped item does not flick back to its old spot for a frame.
    var moving by remember { mutableStateOf<Pair<String, Spot>?>(null) }
    LaunchedEffect(home) { moving = null }

    // Gesture handlers are created once per key; these keep them reading the latest values.
    val currentHome by rememberUpdatedState(home)
    val currentDirt by rememberUpdatedState(dirt)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnSelect by rememberUpdatedState(onSelect)

    BoxWithConstraints(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        val available = if (constraints.hasBoundedWidth) constraints.maxWidth else room.w * 3
        val scale = maxOf(1, available / room.w)
        val density = LocalDensity.current
        val width = with(density) { (room.w * scale).toDp() }
        val height = with(density) { (room.h * scale).toDp() }
        val shape = RoundedCornerShape(10.dp)

        val gestures = if (!editable) Modifier else Modifier
            .pointerInput(stage, scale) {
                detectTapGestures { o ->
                    currentOnSelect(
                        Scene.hit(cat, stage, currentHome, currentDirt, (o.x / scale).toInt(), (o.y / scale).toInt())
                    )
                }
            }
            .pointerInput(stage, scale) {
                var dragId: String? = null
                var origin = Spot(0, 0)
                var travel = Offset.Zero
                detectDragGestures(
                    onDragStart = { o ->
                        val id = Scene.hit(
                            cat, stage, currentHome, currentDirt,
                            (o.x / scale).toInt(), (o.y / scale).toInt()
                        )
                        dragId = id
                        travel = Offset.Zero
                        if (id != null) {
                            origin = if (id == Scene.ME) Home.meSpot(currentHome, cat)
                            else currentHome.placed[id] ?: Spot(0, 0)
                            currentOnSelect(id)
                            moving = id to origin
                        }
                    },
                    onDrag = { change, amount ->
                        val id = dragId
                        if (id != null) {
                            change.consume()
                            travel += amount
                            moving = id to spotFor(
                                cat, id,
                                origin.x + (travel.x / scale).roundToInt(),
                                origin.b + (travel.y / scale).roundToInt()
                            )
                        }
                    },
                    onDragEnd = {
                        val m = moving
                        if (dragId != null && m != null) currentOnMove(m.first, m.second.x, m.second.b)
                        dragId = null
                    },
                    onDragCancel = {
                        dragId = null
                        moving = null
                    }
                )
            }

        Canvas(
            modifier = Modifier
                .size(width, height)
                .clip(shape)
                .border(2.dp, Ink.RoomFrame, shape)
                .then(gestures)
                .semantics { contentDescription = description }
        ) {
            drawImage(
                image = shell,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(room.w, room.h),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(room.w * scale, room.h * scale),
                filterQuality = FilterQuality.None
            )
            val draws = Scene.draws(cat, stage, home, dirt, frame, moving, burst)
            draws.forEach { sprite(art, it, scale) }

            // The selected thing gets a frame, so it is obvious what "put away" will take.
            val pick = selected ?: moving?.first
            if (editable && pick != null) {
                val spot = moving?.takeIf { it.first == pick }?.second
                    ?: if (pick == Scene.ME) Home.meSpot(home, cat) else home.placed[pick]
                val (w, h) = if (pick == Scene.ME) Home.ME_W to Home.ME_H
                else cat.item(pick)?.let { it.w to it.h } ?: (0 to 0)
                if (spot != null && w > 0) {
                    drawRect(
                        color = Ink.Gold,
                        topLeft = Offset(((spot.x - 1) * scale).toFloat(), ((spot.b - h - 1) * scale).toFloat()),
                        size = Size(((w + 2) * scale).toFloat(), ((h + 2) * scale).toFloat()),
                        style = Stroke(width = (scale.coerceAtMost(4)).toFloat())
                    )
                }
            }
        }
    }
}

/** One sprite at its spot, scaled by a whole number, never filtered. */
private fun DrawScope.sprite(art: RoomArt, d: Draw, scale: Int) {
    val r = art.rect(d.id) ?: return
    drawImage(
        image = art.atlas,
        srcOffset = IntOffset(r[0], r[1]),
        srcSize = IntSize(r[2], r[3]),
        dstOffset = IntOffset(d.x * scale, (d.b - r[3]) * scale),
        dstSize = IntSize(r[2] * scale, r[3] * scale),
        filterQuality = FilterQuality.None
    )
}

/** Where a dragged thing may go: floor things on the floor, wall things on the wall. */
private fun spotFor(cat: ItemCatalogue, id: String, x: Int, b: Int): Spot =
    if (id == Scene.ME) Home.clamp(cat.room, Home.ME_W, Home.ME_H, false, x, b)
    else cat.item(id)?.let { Home.clamp(cat.room, it.w, it.h, it.onWall, x, b) } ?: Spot(x, b)

/**
 * The room shell for a stage. Every stage has one — they are generated together by
 * android/tools/pixel/room.py, so there is no missing case to fall back from.
 */
@DrawableRes
private fun shellFor(stage: Stage): Int = when (stage) {
    Stage.SEALED -> R.drawable.room_sealed
    Stage.WINDOW -> R.drawable.room_window
    Stage.DOOR -> R.drawable.room_door
    Stage.STREET -> R.drawable.room_street
    Stage.OPEN -> R.drawable.room_open
    Stage.HORIZON -> R.drawable.room_horizon
    Stage.UNWALLED -> R.drawable.room_unwalled
}

/** One sprite on its own, at a whole-number scale: an item in the tray or a new arrival. */
@Composable
fun SpriteIcon(art: RoomArt, id: String, pixelScale: Int, modifier: Modifier = Modifier) {
    val r = art.rect(id) ?: return
    val density = LocalDensity.current
    val width = with(density) { (r[2] * pixelScale).toDp() }
    val height = with(density) { (r[3] * pixelScale).toDp() }
    Canvas(modifier = modifier.size(width, height)) {
        drawImage(
            image = art.atlas,
            srcOffset = IntOffset(r[0], r[1]),
            srcSize = IntSize(r[2], r[3]),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(r[2] * pixelScale, r[3] * pixelScale),
            filterQuality = FilterQuality.None
        )
    }
}
