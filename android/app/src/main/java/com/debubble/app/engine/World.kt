package com.debubble.app.engine

/**
 * Where everything lives, in one picture.
 *
 * The app grew in two layers: the outside missions, friction, goals and predictions came
 * first, in their own dark, vector, dashboard style; the room came second. Side by side they
 * read as two apps sewn together. So every older idea now has a place in the same pixel
 * world as the room:
 *
 *  - The three ladders are **three roads** out of your bubble, each with its own sign.
 *  - A mission is a **letter** in your hand; what you write afterwards is a **postcard**.
 *  - Friction — saying it did not go to plan — is a **spark in your courage jar**. It used to
 *    be armour plates on a separate avatar; the idea is the same (evidence you went to the
 *    edge, which nobody who stayed inside earns), kept somewhere you can watch it fill.
 *  - A goal is a **journey**: thirty stops on a trail to a place at the end of it.
 *  - Levels are **hats** you can wear in your own room, where the old wardrobe was clothes
 *    on a different character.
 *
 * One metaphor, consistently applied, is easier to learn than several (Lakoff & Johnson's
 * point that we understand abstract things through concrete ones; in interface terms,
 * Neale & Carroll, 1997): once "outside" means the world past your walls, a road, a letter
 * and a postcard need no explaining.
 */
object World {

    const val BELL = "icon_bell"
    const val SPARK = "icon_spark"
    const val POSTCARD = "icon_postcard"
    const val BOOK = "icon_book"

    const val TAB_HOME = "tab_home"
    const val TAB_OUT = "tab_out"
    const val TAB_JOURNAL = "tab_journal"
    const val TAB_LIBRARY = "tab_library"

    /** Each ladder's sign. */
    fun road(p: Pillar): String = when (p) {
        Pillar.ACCESS -> "icon_access"
        Pillar.ACTIVITY -> "icon_activity"
        Pillar.SOCIAL -> "icon_social"
    }

    /** What each road is, said the way someone would. */
    fun roadName(p: Pillar): String = when (p) {
        Pillar.ACCESS -> "Somewhere new"
        Pillar.ACTIVITY -> "Something new"
        Pillar.SOCIAL -> "Someone new"
    }

    /** The place at the end of each journey. */
    fun landmark(g: Goal): String = when (g) {
        Goal.INTERESTING -> "goal_interesting"
        Goal.FRIENDS -> "goal_friends"
        Goal.PARTNER -> "goal_partner"
        Goal.INTIMACY -> "goal_intimacy"
        Goal.CRAFT -> "goal_craft"
    }

    /**
     * How full the courage jar is, 0..4. Quick to start filling — the first spark is the one
     * that most needs to be seen — and slower after, so a full jar means something.
     */
    fun jarLevel(friction: Int): Int = when {
        friction <= 0 -> 0
        friction <= 2 -> 1
        friction <= 5 -> 2
        friction <= 9 -> 3
        else -> 4
    }

    fun jar(friction: Int): String = "jar${jarLevel(friction)}"

    /** Sparks still to go before the jar looks fuller, or null once it is full. */
    fun toNextSpark(friction: Int): Int? = when {
        friction <= 0 -> 1
        friction <= 2 -> 3 - friction
        friction <= 5 -> 6 - friction
        friction <= 9 -> 10 - friction
        else -> null
    }

    /** Hats [level] has earned, in unlock order. */
    fun hatsAt(cat: ItemCatalogue, level: Int): List<HatDef> = cat.hats.filter { it.level <= level }

    /** The next hat still to earn, if there is one. */
    fun nextHat(cat: ItemCatalogue, level: Int): HatDef? = cat.hats.firstOrNull { it.level > level }

    /** Every sprite the app draws outside the room. Checked against the atlas on load. */
    val ICONS: List<String> =
        listOf(BELL, SPARK, POSTCARD, BOOK, TAB_HOME, TAB_OUT, TAB_JOURNAL, TAB_LIBRARY) +
            Pillar.order.map { road(it) } +
            Goal.all.map { landmark(it) } +
            (0..4).map { "jar$it" }
}
