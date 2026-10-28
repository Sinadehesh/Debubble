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

/** A sprite that is not an item: you, the mess, the weather, a sparkle. */
@Serializable
data class FxDef(val id: String, val x: Int, val y: Int, val w: Int, val h: Int)

/** A piece of Room mess, shown at [level] and above. */
@Serializable
data class MessDef(val fx: String, val x: Int, val b: Int, val level: Int)

@Serializable
data class SparkleDef(val x: Int, val y: Int)

@Serializable
data class MeDef(val x: Int, val b: Int, val weatherDrop: Int)

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
    val maxDirt: Int
) {
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
    val unseen: Set<String> = emptySet()
)

object Home {

    /** Width and height of the character frame. Matches fx.py. */
    const val ME_W = 20
    const val ME_H = 19

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

/**
 * What the room looks like, as an ordered list of sprites. Pure, so the order is tested
 * rather than eyeballed — draw order is the part that goes subtly wrong.
 *
 *  1. wall items, then the cobweb in the corner over them
 *  2. flat things on the floor (a rug, a mat), under everything that stands
 *  3. everything standing — furniture, mess and you — sorted by baseline, so whatever is
 *     nearer the viewer is drawn over whatever is further back
 *  4. the Heart weather over your head
 *  5. sparkles, last, because they are light
 *
 * The room shell itself is drawn first by the caller; it is a separate bitmap per stage.
 * Mirrors android/tools/pixel/scene.py, which renders the reference images.
 */
object Scene {

    const val ME = "me"
    private const val SPARKLE = "sparkle"

    /** Points the celebration burst twinkles across, as (x, baseline) in room pixels. */
    private val BURST = listOf(
        14 to 18, 40 to 30, 66 to 12, 92 to 26, 118 to 16, 146 to 30,
        24 to 50, 58 to 46, 104 to 52, 136 to 48, 30 to 76, 80 to 70, 128 to 74, 150 to 88
    )

    fun meFrame(body: Int): String = "me${body.coerceIn(0, Care.MAX_DIRT)}"

    fun weather(heart: Int): String? = when (heart.coerceIn(0, Care.MAX_DIRT)) {
        0 -> "heart_sun"
        1 -> null
        2 -> "heart_cloud"
        else -> "heart_storm"
    }

    /**
     * @param frame an animation tick. Odd frames bob you and the weather by a pixel and
     *   swap which sparkles are lit. Pass 0 forever for reduced motion.
     * @param moving an item being dragged and where it currently is, drawn there instead of
     *   at its saved spot.
     * @param burst sparkles everywhere — the moment after something was cleaned.
     */
    fun draws(
        cat: ItemCatalogue,
        stage: Stage,
        home: HomeState,
        dirt: Map<Domain, Int>,
        frame: Int = 0,
        moving: Pair<String, Spot>? = null,
        burst: Boolean = false
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
        standing.sortedWith(compareBy({ it.first }, { it.second })).forEach { out += it.third }

        weather(heart)?.let { id ->
            val w = cat.fx(id)
            if (w != null) {
                val x = me.x + (Home.ME_W - w.w) / 2
                out += Draw(id, x, me.b - Home.ME_H + cat.me.weatherDrop - bob)
            }
        }

        if (room == 0) {
            cat.sparkles.forEachIndexed { i, p ->
                if ((i + frame) % 2 == 0) out += Draw(SPARKLE, p.x, p.y)
            }
        }
        if (body == 0) out += Draw(SPARKLE, me.x + Home.ME_W - 4, me.b - Home.ME_H + 6 - bob)
        if (burst) {
            BURST.forEachIndexed { i, (x, y) ->
                if ((i + frame) % 3 != 0) out += Draw(SPARKLE, x, y)
            }
        }
        return out
    }

    /**
     * What is under a point, for dragging: the topmost item or you, or null for empty floor.
     * Mess and weather are not grabbable — mess goes by being cleaned, not by being moved.
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
            val (w, h) = when {
                isMe -> Home.ME_W to Home.ME_H
                else -> cat.item(d.id)?.let { it.w to it.h } ?: return@forEach
            }
            if (x >= d.x && x < d.x + w && y >= d.b - h && y < d.b) {
                return if (isMe) ME else d.id
            }
        }
        return null
    }
}
