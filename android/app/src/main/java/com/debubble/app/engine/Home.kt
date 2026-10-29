package com.debubble.app.engine

import kotlinx.serialization.Serializable

/**
 * The things in your room, and how they arrive.
 *
 * You start with nothing. Every item is earned by doing something real, and what arrives is
 * related to what you did: looking after your body brings things for the body (a bed, a jug
 * of water, a mirror), looking after your room brings things for the room, and so on. Missions
 * outside the bubble bring things from the world — a map, a backpack, a bike.
 *
 * Built rather than given, because people value what they made themselves well above the
 * same thing handed to them (Norton, Mochon & Ariely, 2012, the "IKEA effect"). A room that
 * came furnished would be the app's room. This one is yours, and it is a record of a couple
 * of months of looking after yourself that you can see at a glance.
 *
 * Nothing here can be lost. An item stays owned through any amount of mess or absence; the
 * mess is a separate layer drawn on top (see [Care]) and cleaning it is all it takes.
 */
enum class Family {
    BODY, ROOM, HEART,
    /** Earned by missions outside the bubble, on cleared rungs. */
    WORLD
}

/** A position in the room, in room pixels: left edge and baseline (bottom edge). */
@Serializable
data class Spot(val x: Int, val b: Int)

/** Where one item lives in the atlas, which family earns it, and where it lands. */
@Serializable
data class ItemDef(
    val id: String,
    val label: String,
    val family: String,
    /** "floor" or "wall". */
    val anchor: String,
    /** Lies on the floor under everything that stands: a rug, a mat. */
    val flat: Boolean = false,
    /** The family's first item, chosen to change the empty room the most. */
    val first: Boolean = false,
    /** Where it goes when it arrives. */
    val spot: Spot,
    val x: Int,
    val y: Int,
    val w: Int,
    val h: Int
) {
    val onWall: Boolean get() = anchor == "wall"
}

/** A sprite that is not an item: you, your heart, the mess, a letter, an icon, a sparkle. */
@Serializable
data class FxDef(val id: String, val x: Int, val y: Int, val w: Int, val h: Int)

/** A piece of Room mess, shown at [level] and above. */
@Serializable
data class MessDef(val fx: String, val x: Int, val b: Int, val level: Int)

@Serializable
data class SparkleDef(val x: Int, val y: Int)

/**
 * Where you stand by default, how big you are, and where the two things that go with you sit:
 * your heart buddy at one shoulder and, on a day with a mission still open, a letter in the
 * other hand. Offsets are from your left edge and up from your feet. See fx.py.
 */
@Serializable
data class MeDef(
    val x: Int,
    val b: Int,
    val w: Int,
    val h: Int,
    val buddyDx: Int,
    val buddyDy: Int,
    val letterDx: Int,
    val letterDy: Int,
    val bangGap: Int
)

/**
 * The ramps a character can be drawn in, as "#RRGGBB", light to dark. Entry 0 of each list is
 * what the atlas holds. Written by android/tools/pixel/export.py from palette.LOOKS.
 */
@Serializable
data class Looks(
    /** Light, mid, shadow, blush. Skin carries its own blush, because one pink does not suit all. */
    val skin: List<List<String>>,
    val hair: List<List<String>>,
    val top: List<List<String>>
) {
    /**
     * Colour swaps that turn the atlas's look into [look], as ARGB ints, base to chosen.
     * Applied by exact colour inside the "me" frames only; empty for the atlas's own look.
     */
    fun swaps(look: Look): Map<Int, Int> {
        val out = mutableMapOf<Int, Int>()
        fun add(ramps: List<List<String>>, pick: Int) {
            val from = ramps.first()
            val to = ramps[pick.coerceIn(ramps.indices)]
            from.indices.forEach { i ->
                val a = argb(from[i])
                val b = argb(to[i])
                if (a != b) out[a] = b
            }
        }
        add(skin, look.skin)
        add(hair, look.hair)
        add(top, look.top)
        return out
    }

    companion object {
        /** "#RRGGBB" as an opaque ARGB int. */
        fun argb(hex: String): Int = (0xFF000000L or hex.removePrefix("#").toLong(16)).toInt()
    }
}

/** How you look: an index into each of [Looks]' ramps. Chosen on moving in. */
@Serializable
data class Look(
    val skin: Int = 0,
    val hair: Int = 0,
    val top: Int = 0,
    /** 0 for none, else 1 + an index into [ItemCatalogue.hats]. */
    val hat: Int = 0
)

/** A hat you can wear, and the level it arrives at. Drawn in your frame from its top row. */
@Serializable
data class HatDef(val id: String, val level: Int, val label: String)

@Serializable
data class RoomGeometry(val w: Int, val h: Int, val floorY: Int, val skirt: Int)

/** Everything in assets/pixel/items.json. Written by android/tools/pixel/export.py. */
@Serializable
data class ItemCatalogue(
    val atlas: String,
    val atlasWidth: Int,
    val atlasHeight: Int,
    val room: RoomGeometry,
    val stages: List<String>,
    val families: List<String>,
    val items: List<ItemDef>,
    val fx: List<FxDef>,
    val mess: List<MessDef>,
    val sparkles: List<SparkleDef>,
    val me: MeDef,
    val looks: Looks,
    val maxDirt: Int,
    val hats: List<HatDef> = emptyList()
) {
    /** The hat [look] is wearing, if it is one that exists. */
    fun hat(look: Look): HatDef? = hats.getOrNull(look.hat - 1)

    fun item(id: String): ItemDef? = items.firstOrNull { it.id == id }

    fun fx(id: String): FxDef? = fx.firstOrNull { it.id == id }

    fun family(f: Family): List<ItemDef> = items.filter { it.family == f.name }
}

/** What the user has, and where they put it. */
@Serializable
data class HomeState(
    /** Item ids, in the order they arrived. */
    val owned: List<String> = emptyList(),
    /** Where each owned item stands. An owned item with no entry is put away, not lost. */
    val placed: Map<String, Spot> = emptyMap(),
    /** Where you stand, if you have been moved. Null means the default spot. */
    val me: Spot? = null,
    /** Arrived since the room was last opened, so they can be pointed out once. */
    val unseen: Set<String> = emptySet(),
    /** How you look. */
    val look: Look = Look()
)

object Home {

    fun ownedIn(home: HomeState, cat: ItemCatalogue, f: Family): Int =
        home.owned.count { id -> cat.item(id)?.family == f.name }

    /**
     * The next item a family gives, or null when it has given everything.
     *
     * The family's `first` item always comes first. After that the choice is a surprise —
     * deterministic in [seed] so it is testable and stable across a reinstall of the same
     * state, but not predictable from the screen.
     */
    fun pick(cat: ItemCatalogue, f: Family, owned: Collection<String>, seed: Long): ItemDef? {
        val left = cat.family(f).filter { it.id !in owned }
        if (left.isEmpty()) return null
        left.firstOrNull { it.first }?.let { return it }
        return left[Math.floorMod(mix(seed, owned.size.toLong()), left.size.toLong()).toInt()]
    }

    /**
     * Grant everything a family is owed for [count] (care-days, or cleared rungs for WORLD),
     * and return what arrived.
     *
     * Derived from the count rather than incremented, so it heals itself: an install that
     * upgrades into this feature with forty rungs already cleared receives what forty rungs
     * have earned, and running it twice grants nothing twice.
     */
    fun settle(
        home: HomeState,
        cat: ItemCatalogue,
        f: Family,
        count: Int,
        seed: Long
    ): Pair<HomeState, List<ItemDef>> {
        val owe = Care.owed(count) - ownedIn(home, cat, f)
        var h = home
        val granted = mutableListOf<ItemDef>()
        for (i in 0 until owe.coerceAtLeast(0)) {
            val next = pick(cat, f, h.owned, seed) ?: break
            h = h.copy(
                owned = h.owned + next.id,
                placed = h.placed + (next.id to next.spot),
                unseen = h.unseen + next.id
            )
            granted += next
        }
        return h to granted
    }

    /**
     * Keep a dragged item inside the part of the room it belongs to.
     *
     * Floor things stand on the floor and wall things hang on the wall — the clamp is what
     * stops a rug ending up on the ceiling. [x] and [b] are the requested left edge and
     * baseline in room pixels.
     */
    fun clamp(room: RoomGeometry, w: Int, h: Int, onWall: Boolean, x: Int, b: Int): Spot {
        val minB = if (onWall) h + 2 else room.floorY + 2
        val maxB = if (onWall) room.floorY - room.skirt else room.h
        return Spot(
            x = x.coerceIn(0, (room.w - w).coerceAtLeast(0)),
            b = b.coerceIn(minB, maxB.coerceAtLeast(minB))
        )
    }

    fun meSpot(home: HomeState, cat: ItemCatalogue): Spot =
        home.me ?: Spot(cat.me.x, cat.me.b)

    /** Wall items come down when there is no wall left to hang them on. */
    fun wallsStand(stage: Stage): Boolean = stage != Stage.UNWALLED

    /** A cheap, well-mixed 64-bit hash (SplitMix64's finaliser). */
    private fun mix(seed: Long, n: Long): Long {
        var z = seed + n * -0x61c8864680b583ebL
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }
}

/** One sprite to draw: which atlas entry, and where its bottom-left corner sits. */
data class Draw(val id: String, val x: Int, val b: Int)

/** A rectangle in room pixels: left, top, width, height. */
data class Area(val x: Int, val y: Int, val w: Int, val h: Int) {
    val cx: Int get() = x + w / 2
    val cy: Int get() = y + h / 2
}

/** The things in the room that are a task, and what tapping each one opens. */
enum class RoomTap {
    /** You: your Body task. */
    BODY,
    /** Any of the mess: your Room task. */
    ROOM,
    /** Your heart buddy: your Heart task. */
    HEART,
    /** The letter in your hand: today's mission outside. */
    OUTSIDE
}

/**
 * What the room looks like, as an ordered list of sprites. Pure, so the order is tested
 * rather than eyeballed — draw order is the part that goes subtly wrong.
 *
 *  1. wall items, then the cobweb in the corner over them
 *  2. flat things on the floor (a rug, a mat), under everything that stands
 *  3. everything standing — furniture, mess, you and your hat — sorted by baseline, so whatever is
 *     nearer the viewer is drawn over whatever is further back
 *  4. today's letter in your hand, with its "!", while a mission outside is still open
 *  5. your heart buddy at your shoulder, a beat out of step with you
 *  6. sparkles, last, because they are light
 *
 * The room shell itself is drawn first by the caller; it is a separate bitmap per stage.
 * Mirrors android/tools/pixel/scene.py, which renders the reference images.
 */
object Scene {

    const val ME = "me"
    const val LETTER = "letter"
    const val BANG = "bang"
    private const val SPARKLE = "sparkle"

    /** Points the celebration burst twinkles across, as (x, baseline) in room pixels. */
    private val BURST = listOf(
        14 to 18, 40 to 30, 66 to 12, 92 to 26, 118 to 16, 146 to 30,
        24 to 50, 58 to 46, 104 to 52, 136 to 48, 30 to 76, 80 to 70, 128 to 74, 150 to 88
    )

    fun meFrame(body: Int): String = "me${body.coerceIn(0, Care.MAX_DIRT)}"

    fun buddy(heart: Int): String = "buddy${heart.coerceIn(0, Care.MAX_DIRT)}"

    /**
     * Left edges of your heart buddy and your letter, for you standing at [me]. The buddy
     * takes your right unless that would leave the room; the letter takes the other hand.
     */
    fun sides(cat: ItemCatalogue, me: Spot): Pair<Int, Int> {
        val m = cat.me
        val bw = cat.fx(buddy(0))?.w ?: 0
        val lw = cat.fx(LETTER)?.w ?: 0
        val right = me.x + m.buddyDx + bw <= cat.room.w
        val bx = if (right) me.x + m.buddyDx else me.x + m.w - m.buddyDx - bw
        val lx = if (right) me.x + m.letterDx else me.x + m.w - m.letterDx - lw
        return bx to lx.coerceIn(0, (cat.room.w - lw).coerceAtLeast(0))
    }

    /**
     * @param frame an animation tick. Odd frames bob you by a pixel, your buddy floats the
     *   other way, and the sparkles swap. Pass 0 forever for reduced motion.
     * @param moving an item being dragged and where it currently is, drawn there instead of
     *   at its saved spot.
     * @param burst sparkles everywhere — the moment after something was cleaned.
     * @param letter a mission outside is still open today, so you are holding its letter.
     */
    fun draws(
        cat: ItemCatalogue,
        stage: Stage,
        home: HomeState,
        dirt: Map<Domain, Int>,
        frame: Int = 0,
        moving: Pair<String, Spot>? = null,
        burst: Boolean = false,
        letter: Boolean = false
    ): List<Draw> {
        val body = dirt[Domain.BODY] ?: Care.START_DIRT
        val room = dirt[Domain.ROOM] ?: Care.START_DIRT
        val heart = dirt[Domain.HEART] ?: Care.START_DIRT
        val walls = Home.wallsStand(stage)

        fun spotOf(id: String, def: Spot): Spot =
            if (moving != null && moving.first == id) moving.second else def

        val placed = home.owned.mapNotNull { id ->
            val def = cat.item(id) ?: return@mapNotNull null
            val at = home.placed[id] ?: return@mapNotNull null
            def to spotOf(id, at)
        }
        val out = mutableListOf<Draw>()

        if (walls) {
            placed.filter { it.first.onWall }.forEach { (d, s) -> out += Draw(d.id, s.x, s.b) }
            cat.mess.filter { it.fx == "cobweb" && room >= it.level }
                .forEach { out += Draw(it.fx, it.x, it.b) }
        }
        placed.filter { !it.first.onWall && it.first.flat }
            .forEach { (d, s) -> out += Draw(d.id, s.x, s.b) }

        // (baseline, tiebreak, draw): at equal depth furniture, then mess, then you.
        val standing = mutableListOf<Triple<Int, Int, Draw>>()
        placed.filter { !it.first.onWall && !it.first.flat }
            .forEach { (d, s) -> standing += Triple(s.b, 0, Draw(d.id, s.x, s.b)) }
        cat.mess.filter { it.fx != "cobweb" && room >= it.level }
            .forEach { standing += Triple(it.b, 1, Draw(it.fx, it.x, it.b)) }
        val me = spotOf(ME, Home.meSpot(home, cat))
        val bob = if (frame % 2 == 1) 1 else 0
        standing += Triple(me.b, 2, Draw(meFrame(body), me.x, me.b - bob))
        // Your hat, on your head: drawn from the top of your frame, straight after you, so
        // anything standing in front of you covers it the way it covers you.
        cat.hat(home.look)?.let { h ->
            cat.fx(h.id)?.let { f -> standing += Triple(me.b, 3, Draw(h.id, me.x, me.b - cat.me.h + f.h - bob)) }
        }
        standing.sortedWith(compareBy({ it.first }, { it.second })).forEach { out += it.third }

        val (bx, lx) = sides(cat, me)
        if (letter) {
            val l = cat.fx(LETTER)
            val bang = cat.fx(BANG)
            if (l != null) {
                val lb = me.b - cat.me.letterDy - bob
                out += Draw(LETTER, lx, lb)
                if (bang != null) out += Draw(BANG, lx + (l.w - bang.w) / 2, lb - l.h - cat.me.bangGap)
            }
        }
        out += Draw(buddy(heart), bx, me.b - cat.me.buddyDy - (1 - bob))

        if (room == 0) {
            cat.sparkles.forEachIndexed { i, p ->
                if ((i + frame) % 2 == 0) out += Draw(SPARKLE, p.x, p.y)
            }
        }
        if (body == 0) out += Draw(SPARKLE, me.x + 2, me.b - cat.me.h + 6 - bob)
        if (burst) {
            BURST.forEachIndexed { i, (x, y) ->
                if ((i + frame) % 3 != 0) out += Draw(SPARKLE, x, y)
            }
        }
        return out
    }

    /** Width and height of a drawn sprite, item or effect, or null for an unknown id. */
    private fun size(cat: ItemCatalogue, id: String): Pair<Int, Int>? =
        cat.item(id)?.let { it.w to it.h } ?: cat.fx(id)?.let { it.w to it.h }

    private fun under(cat: ItemCatalogue, d: Draw, x: Int, y: Int): Boolean {
        val (w, h) = size(cat, d.id) ?: return false
        return x >= d.x && x < d.x + w && y >= d.b - h && y < d.b
    }

    /**
     * What is under a point, for dragging: the topmost item or you, or null for empty floor.
     * Mess, your buddy and the letter are not grabbable — mess goes by being cleaned, not by
     * being moved, and the other two go where you go.
     */
    fun hit(
        cat: ItemCatalogue,
        stage: Stage,
        home: HomeState,
        dirt: Map<Domain, Int>,
        x: Int,
        y: Int
    ): String? {
        val meIds = (0..Care.MAX_DIRT).map { meFrame(it) }.toSet()
        draws(cat, stage, home, dirt).asReversed().forEach { d ->
            val isMe = d.id in meIds
            if (!isMe && cat.item(d.id) == null) return@forEach
            if (under(cat, d, x, y)) return if (isMe) ME else d.id
        }
        return null
    }

    /**
     * Which task a tap in the room is asking for, or null for anywhere else.
     *
     * This is what makes the room the way in rather than a picture above a list: tap
     * yourself to look after your body, the mess to tidy, your heart to tend it, the letter
     * to go out. Checked topmost first, so the letter in front of you wins over you.
     */
    fun tap(
        cat: ItemCatalogue,
        stage: Stage,
        home: HomeState,
        dirt: Map<Domain, Int>,
        letter: Boolean,
        x: Int,
        y: Int
    ): RoomTap? {
        draws(cat, stage, home, dirt, letter = letter).asReversed().forEach { d ->
            val kind = kindOf(cat, d.id)
            if (kind != null && under(cat, d, x, y)) return kind
        }
        return null
    }

    /**
     * Where each task is in the room, for the tour to point at: you, your heart buddy, the
     * letter, and one piece of the mess. Only what is drawn is there — a clean floor has no
     * mess to point at, and a finished mission no letter.
     *
     * Chosen so that a tap in the middle of each one opens that task. The tour says "tap the
     * mess", so the piece of mess it lights up must not be one you are standing in front of.
     */
    fun spots(
        cat: ItemCatalogue,
        stage: Stage,
        home: HomeState,
        dirt: Map<Domain, Int>,
        letter: Boolean
    ): Map<RoomTap, Area> {
        val drawn = draws(cat, stage, home, dirt, letter = letter)
        return RoomTap.entries.mapNotNull { kind ->
            val areas = drawn
                .filter { kindOf(cat, it.id) == kind }
                .mapNotNull { d -> size(cat, d.id)?.let { (w, h) -> Area(d.x, d.b - h, w, h) } }
                .sortedByDescending { it.w * it.h }
            val pick = areas.firstOrNull { tap(cat, stage, home, dirt, letter, it.cx, it.cy) == kind }
                ?: areas.firstOrNull()
            pick?.let { kind to it }
        }.toMap()
    }

    /** Which task a drawn sprite stands for, or null for furniture and sparkles. */
    private fun kindOf(cat: ItemCatalogue, id: String): RoomTap? = when (id) {
        in BUDDIES -> RoomTap.HEART
        LETTER, BANG -> RoomTap.OUTSIDE
        in MES -> RoomTap.BODY
        else -> if (cat.mess.any { it.fx == id }) RoomTap.ROOM else null
    }

    private val MES = (0..Care.MAX_DIRT).map { meFrame(it) }.toSet()
    private val BUDDIES = (0..Care.MAX_DIRT).map { buddy(it) }.toSet()
}
