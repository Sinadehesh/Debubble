package com.debubble.app.data

import com.debubble.app.engine.Care
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Quest
import com.debubble.app.engine.Served
import com.debubble.app.engine.Today

/*
 * Today's list, read out of the saved state. Shared by the dashboard and the home-screen
 * widget so the two cannot disagree about what today is — the widget runs in a broadcast
 * receiver with no view model, which is exactly where a second copy of this would drift.
 * Pass a state already rolled to [today].
 */

/** A ladder's challenge as served today, with today's swap applied. */
fun AppState.serve(p: Pillar, curriculum: Curriculum): Served = Engine.serve(
    pillar = p,
    state = state(p),
    curriculum = curriculum,
    baseline = baseline,
    forceAlternate = p.name in swappedToday
)

/** How clean each domain is today. What the room draws. */
fun AppState.dirt(today: Long): Map<Domain, Int> =
    Domain.order.associateWith { Care.dirt(it, care(it), today) }

/** The ladder today's one mission comes from. */
fun AppState.outsidePillar(today: Long): Pillar =
    Today.outsidePillar(dayIndex(today), Pillar.order.filter { isDoneToday(it) }.toSet())

/** Body, room, heart, outside. */
fun AppState.quests(today: Long, library: CareLibrary, curriculum: Curriculum): List<Quest> {
    val levels = dirt(today)
    val care = Domain.order.map { d -> Today.care(d, library, care(d), levels.getValue(d), today) }
    val p = outsidePillar(today)
    val outside = if (isDoneToday(p)) {
        val did = log.lastOrNull {
            it.kind == "PILLAR" && it.pillar == p.name && it.epochDay == today && !it.friction
        }
        Today.outsideDone(p, did?.title ?: "Done", did?.tier ?: (state(p).tier - 1).coerceAtLeast(1))
    } else {
        val served = serve(p, curriculum)
        Today.outside(served, intentionFor("${Today.TIER}${p.name}:${served.tier}"))
    }
    return care + outside
}
