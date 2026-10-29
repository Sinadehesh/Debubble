package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Look
import com.debubble.app.engine.RepType
import com.debubble.app.engine.Served
import com.debubble.app.engine.World
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.LetterCard
import com.debubble.app.ui.components.MeHead
import com.debubble.app.ui.components.PageTip
import com.debubble.app.ui.components.PaperLabel
import com.debubble.app.ui.components.PaperTitle
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RepTracker
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SignHeader
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * Your goal, as a journey: thirty stops on a trail to a place, in three phases of ten.
 *
 * It replaces a terrain view, a reading list and a rep counter that each lived on their own
 * screen in the old style. One place now: the trail with you standing on it, today's stop as
 * a letter, the tries you are counting, and the ideas that open as you walk. A goal with its
 * distance visible keeps effort up through the long middle, where most goals are dropped.
 */
@Composable
fun JourneyScreen(
    art: RoomArt,
    look: Look,
    goal: Goal,
    track: GoalTrack,
    state: GoalState,
    /** Today's stop, or null when the whole journey is walked. */
    stop: Served?,
    stopDoneToday: Boolean,
    reps: List<RepType>,
    repsToday: Map<String, Int>,
    repTarget: Int,
    repsLifetime: Int,
    showTip: Boolean,
    onTipSeen: () -> Unit,
    onOpenStop: () -> Unit,
    onLogRep: (RepType) -> Unit,
    onUndoRep: (RepType) -> Unit,
    onOpenPrinciple: (Int) -> Unit,
    onChangeGoal: () -> Unit,
    onBack: () -> Unit
) {
    val accent = goal.homePillar.accent
    val complete = Goals.isComplete(state)

    Column(modifier = Modifier.fillMaxSize().background(Ink.Void)) {
        TopBar(
            title = goal.display,
            subtitle = if (complete) "Arrived" else "Stop ${state.step} of ${Goals.CAMPAIGN_LENGTH} · ${track.phaseOf(state.step)}",
            accent = accent,
            onBack = onBack,
            action = {
                Box(
                    modifier = Modifier
                        .heightIn(min = Space.tap)
                        .panel(border = Ink.Border)
                        .clickable(role = Role.Button, onClick = onChangeGoal)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Label("Change", color = Ink.Primary, strong = true)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            if (showTip) {
                PageTip(
                    art = art,
                    title = "A journey is thirty small stops",
                    body = "One stop a day, when you want it. Each one is a little bigger than the " +
                        "last, and you can count your tries in between.",
                    onDismiss = onTipSeen
                )
            }

            Text(text = track.premise, color = Ink.Secondary, style = MaterialTheme.typography.bodyLarge)

            TrailMap(
                art = art,
                look = look,
                goal = goal,
                track = track,
                done = state.completed,
                current = if (complete) Goals.CAMPAIGN_LENGTH else state.step,
                accent = accent
            )

            if (stop != null && !complete) {
                LetterCard(kicker = "Stop ${stop.tier} · ${stop.phase ?: ""}".trimEnd(' ', '·'), accent = accent) {
                    PaperTitle(stop.directive)
                    if (stopDoneToday) PaperLabel("Done for today. The next stop opens tomorrow.", color = Ink.PaperText)
                }
                if (!stopDoneToday) PrimaryButton("Open today's stop", accent = accent, onClick = onOpenStop)
            } else {
                Text(
                    text = "All thirty stops. This ground stays yours, whether or not you come back to it.",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            if (reps.isNotEmpty()) {
                SignHeader(art, World.SPARK, "Tries today")
                RepTracker(
                    reps = reps,
                    today = repsToday,
                    target = repTarget,
                    lifetime = repsLifetime,
                    accent = accent,
                    onLog = onLogRep,
                    onUndo = onUndoRep
                )
            }

            if (track.principles.isNotEmpty()) {
                SignHeader(
                    art,
                    World.BOOK,
                    "Ideas for the road",
                    trailing = "${track.principles.count { it.unlocksAt <= state.step }} of ${track.principles.size} open"
                )
                track.principles.forEachIndexed { i, p ->
                    val open = p.unlocksAt <= state.step
                    val read = i in state.read
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Space.tap)
                            .panel(fill = if (open) Ink.Surface else Ink.Void, border = if (open) accent.copy(alpha = 0.5f) else Ink.Faint)
                            .clickable(role = Role.Button, enabled = open) { onOpenPrinciple(i) }
                            .semantics {
                                stateDescription = when {
                                    !open -> "Opens at stop ${p.unlocksAt}"
                                    read -> "Read"
                                    else -> "Not read yet"
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpriteIcon(art, World.BOOK, pixelScale = pixelScale(1.6f), modifier = Modifier)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (open) p.title else "Idea ${i + 1}",
                                color = if (open) Ink.Primary else Ink.Muted,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Label(
                                when {
                                    !open -> "Opens at stop ${p.unlocksAt}"
                                    read -> "Read"
                                    else -> "New"
                                },
                                color = if (open && !read) accent else Ink.Muted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * The trail: thirty stops, three rows of ten winding back and forth, one row per phase.
 * Walked stops are filled, you stand on today's, and the place it leads to waits at the end.
 */
@Composable
private fun TrailMap(
    art: RoomArt,
    look: Look,
    goal: Goal,
    track: GoalTrack,
    done: Int,
    current: Int,
    accent: Color
) {
    val stops = Goals.CAMPAIGN_LENGTH
    val perRow = 10
    val rows = (stops + perRow - 1) / perRow
    val rowHeight = 78.dp
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics { contentDescription = "Journey: $done of $stops stops walked. You are on stop $current." }
    ) {
        val width = maxWidth
        val density = LocalDensity.current
        fun at(stop: Int): Pair<Dp, Dp> {
            val i = stop - 1
            val row = i / perRow
            val col = if (row % 2 == 0) i % perRow else perRow - 1 - i % perRow
            val x = width * ((col + 0.5f) / perRow)
            val y = rowHeight * row + rowHeight * 0.62f
            return x to y
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(rowHeight * rows)
                .drawBehind {
                    val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))
                    for (s in 1 until stops) {
                        val (ax, ay) = at(s)
                        val (bx, by) = at(s + 1)
                        drawLine(
                            color = if (s < done + 1) accent else Ink.Faint,
                            start = Offset(ax.toPx(), ay.toPx()),
                            end = Offset(bx.toPx(), by.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            pathEffect = if (s < done + 1) null else dash
                        )
                    }
                    for (s in 1 until stops) {
                        val (x, y) = at(s)
                        val big = s % perRow == 0
                        val r = (if (big) 8.dp else 5.5.dp).toPx()
                        drawCircle(Ink.Void, radius = r + 2.dp.toPx(), center = Offset(x.toPx(), y.toPx()))
                        drawCircle(
                            color = if (s <= done) accent else if (s == current) Ink.Paper else Ink.Faint,
                            radius = r,
                            center = Offset(x.toPx(), y.toPx())
                        )
                    }
                }
        )
        // Phase names, one per row, at the row's start.
        for (row in 0 until rows) {
            val phase = track.phaseOf(row * perRow + 1)
            if (phase.isNotBlank()) {
                Label(
                    phase,
                    color = if (done >= row * perRow) accent else Ink.Muted,
                    modifier = Modifier.offset(
                        x = if (row % 2 == 0) 0.dp else width * 0.5f,
                        y = rowHeight * row
                    )
                )
            }
        }
        // The place at the end of it.
        val (lx, ly) = at(stops)
        val landmark = art.rect(World.landmark(goal))
        if (landmark != null) {
            val scale = pixelScale(2.2f)
            val w = with(density) { (landmark[2] * scale).toDp() }
            val h = with(density) { (landmark[3] * scale).toDp() }
            SpriteIcon(
                art,
                World.landmark(goal),
                pixelScale = scale,
                modifier = Modifier.offset { IntOffset((lx - w / 2).roundToPx(), (ly - h / 2 - 4.dp).roundToPx()) }
            )
        }
        // You, standing on today's stop.
        val (mx, my) = at(current.coerceIn(1, stops))
        val head = pixelScale(1.3f)
        val hw = with(density) { (20 * head).toDp() }
        val hh = with(density) { (16 * head).toDp() }
        if (current < stops) {
            MeHead(
                art = art,
                look = look,
                pixelScale = head,
                modifier = Modifier.offset { IntOffset((mx - hw / 2).roundToPx(), (my - hh - 8.dp).roundToPx()) }
            )
        }
    }
}
