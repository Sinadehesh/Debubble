package com.debubble.app.engine

import kotlinx.serialization.Serializable

/**
 * The avatar: how the user is represented back to themselves.
 *
 * Everything here is an index into a fixed table rather than a colour or a path, so the whole
 * thing serialises to five integers and the drawing code stays the single source of truth for
 * what any of them look like.
 */
@Serializable
data class AvatarState(
    val body: Int = 0,
    val shape: Int = 0,
    val hat: Int = 0,
    val shirt: Int = 0,
    val backdrop: Int = 0
)

/** One unlockable item. [level] of 1 means it is available from the start. */
data class Cosmetic(
    val id: Int,
    val name: String,
    val level: Int
)

/**
 * XP, levels and armour.
 *
 * Two currencies, deliberately. **XP** comes from doing things and unlocks casual wear —
 * hats, shirts, backdrops. **Armour** comes only from friction: being turned down, bailing
 * out, admitting a challenge was too big. It cannot be bought with completions, so the plates
 * around someone's avatar are a record of the times they went past what was comfortable.
 *
 * That split is the whole point. A profile covered in cosmetics says "this person shows up".
 * A profile in heavy armour says "this person has been rejected a lot and came back", which
 * is the harder and more useful thing to be able to see about yourself.
 */
object Progress {

    const val XP_CHALLENGE = 20
    const val XP_MISSION = 30
    const val XP_REP = 5

    /** Friction pays XP too — it is output, not failure, and the app never scores it lower. */
    const val XP_FRICTION = 15

    /** XP needed for each level after the first. */
    const val XP_PER_LEVEL = 120

    /** Friction events per armour plate. */
    const val FRICTION_PER_PLATE = 4

    /** Plates the avatar can carry. Beyond this the aura keeps intensifying instead. */
    const val MAX_PLATES = 6

    fun level(xp: Int): Int = 1 + (xp / XP_PER_LEVEL)

    /** Progress through the current level, 0..1. */
    fun levelProgress(xp: Int): Float = (xp % XP_PER_LEVEL) / XP_PER_LEVEL.toFloat()

    fun xpIntoLevel(xp: Int): Int = xp % XP_PER_LEVEL

    fun plates(friction: Int): Int =
        (friction / FRICTION_PER_PLATE).coerceAtMost(MAX_PLATES)

    /** Friction events still needed for the next plate, or null once every plate is earned. */
    fun toNextPlate(friction: Int): Int? {
        if (plates(friction) >= MAX_PLATES) return null
        return FRICTION_PER_PLATE - (friction % FRICTION_PER_PLATE)
    }

    /** How bright the aura burns once the plates run out, 0..1. */
    fun auraStrength(friction: Int): Float {
        val past = friction - MAX_PLATES * FRICTION_PER_PLATE
        if (past <= 0) return plates(friction) / MAX_PLATES.toFloat() * 0.7f
        return (0.7f + past / 60f).coerceAtMost(1f)
    }

    /* ---- the catalogues. Index 0 is always "none" for wearables. ---- */

    val bodies = listOf(
        Cosmetic(0, "Slate", 1),
        Cosmetic(1, "Ocean", 1),
        Cosmetic(2, "Moss", 1),
        Cosmetic(3, "Clay", 1),
        Cosmetic(4, "Plum", 3),
        Cosmetic(5, "Ember", 5)
    )

    val shapes = listOf(
        Cosmetic(0, "Round", 1),
        Cosmetic(1, "Square", 1),
        Cosmetic(2, "Diamond", 2)
    )

    val hats = listOf(
        Cosmetic(0, "No hat", 1),
        Cosmetic(1, "Beanie", 2),
        Cosmetic(2, "Cap", 4),
        Cosmetic(3, "Bucket hat", 6),
        Cosmetic(4, "Headphones", 8)
    )

    val shirts = listOf(
        Cosmetic(0, "Plain", 1),
        Cosmetic(1, "Stripes", 3),
        Cosmetic(2, "Collar", 5),
        Cosmetic(3, "Hoodie", 7),
        Cosmetic(4, "Jacket", 10)
    )

    val backdrops = listOf(
        Cosmetic(0, "No background", 1),
        Cosmetic(1, "Sunrise", 2),
        Cosmetic(2, "Night", 5),
        Cosmetic(3, "Rings", 8),
        Cosmetic(4, "Grid", 12)
    )

    fun unlocked(item: Cosmetic, level: Int): Boolean = level >= item.level

    /** Everything that became available on the step from [from] to [to]. */
    fun newlyUnlocked(from: Int, to: Int): List<Cosmetic> {
        if (to <= from) return emptyList()
        val range = (from + 1)..to
        return (bodies + shapes + hats + shirts + backdrops).filter { it.level in range }
    }
}
