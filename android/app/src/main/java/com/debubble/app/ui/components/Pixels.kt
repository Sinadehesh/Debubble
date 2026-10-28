package com.debubble.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import androidx.annotation.DrawableRes
import com.debubble.app.R
import com.debubble.app.engine.Care
import com.debubble.app.engine.Domain
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.ItemCatalogue
import com.debubble.app.engine.Look
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Stage

/**
 * The sprite atlas as a plain Android bitmap, and the one place that paints the room without
 * Compose. The app draws through [RoomArt] on top of this; the home-screen widget, which
 * runs in a broadcast receiver and can only hand the launcher a bitmap, paints with
 * [RoomPainter]. Both read the same atlas and the same [Scene.draws], so the room on the
 * home screen is the room in the app.
 */
class PixelAtlas(val base: Bitmap, val catalogue: ItemCatalogue) {

    private val rects: Map<String, Rect> = buildMap {
        catalogue.items.forEach { put(it.id, Rect(it.x, it.y, it.x + it.w, it.y + it.h)) }
        catalogue.fx.forEach { put(it.id, Rect(it.x, it.y, it.x + it.w, it.y + it.h)) }
    }

    /** Where a sprite is in the atlas, item or effect alike. */
    fun rect(id: String): Rect? = rects[id]

    private val meRects: List<Rect> = (0..Care.MAX_DIRT).mapNotNull { rect(Scene.meFrame(it)) }

    private var cachedLook: Look? = null
    private var cached: Bitmap? = null

    /**
     * The atlas with you drawn in [look]. Swapped by exact colour inside your own frames
     * only — the look ramps are used nowhere else in the room (fx.py checks this), so a
     * skin tone can never leak into a cushion. One look is cached; it rarely changes.
     */
    @Synchronized
    fun forLook(look: Look): Bitmap {
        if (look == cachedLook) cached?.let { return it }
        val swaps = catalogue.looks.swaps(look)
        val out = if (swaps.isEmpty()) base else base.copy(Bitmap.Config.ARGB_8888, true).also { b ->
            meRects.forEach { r ->
                val px = IntArray(r.width() * r.height())
                b.getPixels(px, 0, r.width(), r.left, r.top, r.width(), r.height())
                for (i in px.indices) swaps[px[i]]?.let { px[i] = it }
                b.setPixels(px, 0, r.width(), r.left, r.top, r.width(), r.height())
            }
        }
        cachedLook = look
        cached = out
        return out
    }

    companion object {
        /** Decode once. Not density-scaled: these are pixels, not artwork. */
        fun load(context: Context, catalogue: ItemCatalogue): PixelAtlas {
            val options = BitmapFactory.Options().apply {
                inScaled = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val bitmap = context.assets.open(catalogue.atlas).use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: error("could not decode ${catalogue.atlas}")
            return PixelAtlas(bitmap, catalogue)
        }
    }
}

object RoomPainter {

    /** Nearest-neighbour, always: any filtering turns a one-pixel outline into a smear. */
    private val crisp = Paint().apply {
        isFilterBitmap = false
        isAntiAlias = false
        isDither = false
    }

    /**
     * The room shell for a stage. Every stage has one — they are generated together by
     * android/tools/pixel/room.py, so there is no missing case to fall back from.
     */
    @DrawableRes
    fun shellFor(stage: Stage): Int = when (stage) {
        Stage.SEALED -> R.drawable.room_sealed
        Stage.WINDOW -> R.drawable.room_window
        Stage.DOOR -> R.drawable.room_door
        Stage.STREET -> R.drawable.room_street
        Stage.OPEN -> R.drawable.room_open
        Stage.HORIZON -> R.drawable.room_horizon
        Stage.UNWALLED -> R.drawable.room_unwalled
    }

    /** The whole room, still, at a whole-number [scale]. For the widget. */
    fun paint(
        context: Context,
        atlas: PixelAtlas,
        stage: Stage,
        home: HomeState,
        dirt: Map<Domain, Int>,
        letter: Boolean,
        scale: Int
    ): Bitmap {
        val room = atlas.catalogue.room
        val options = BitmapFactory.Options().apply { inScaled = false }
        val shell = BitmapFactory.decodeResource(context.resources, shellFor(stage), options)
        val out = Bitmap.createBitmap(room.w * scale, room.h * scale, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (shell != null) {
            canvas.drawBitmap(shell, Rect(0, 0, room.w, room.h), Rect(0, 0, room.w * scale, room.h * scale), crisp)
        }
        val sheet = atlas.forLook(home.look)
        Scene.draws(atlas.catalogue, stage, home, dirt, letter = letter).forEach { d ->
            val r = atlas.rect(d.id) ?: return@forEach
            val top = d.b - r.height()
            canvas.drawBitmap(
                sheet, r,
                Rect(d.x * scale, top * scale, (d.x + r.width()) * scale, d.b * scale),
                crisp
            )
        }
        return out
    }

    /** One sprite on its own, at a whole-number [scale]. For the widget's task icons. */
    fun icon(atlas: PixelAtlas, id: String, scale: Int): Bitmap? {
        val r = atlas.rect(id) ?: return null
        val out = Bitmap.createBitmap(r.width() * scale, r.height() * scale, Bitmap.Config.ARGB_8888)
        Canvas(out).drawBitmap(atlas.base, r, Rect(0, 0, out.width, out.height), crisp)
        return out
    }
}
