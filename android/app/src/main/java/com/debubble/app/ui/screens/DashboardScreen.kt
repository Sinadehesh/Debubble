package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.Goal
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Principle
import com.debubble.app.engine.Progress
import com.debubble.app.engine.RepType
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.Avatar
import com.debubble.app.ui.components.BubbleRing
import com.debubble.app.ui.components.ChallengeCard
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.RepTracker
import com.debubble.app.ui.components.RingState
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * Home.
 *
 * Top to bottom: who you are and how far along, the bubble itself, today's three challenges,
 * the campaign, the attempt counters, and the ideas behind all of it. Every section is a
 * labelled block with a visible boundary, so the screen can be scanned rather than decoded.
 */
@Composable
fun DashboardScreen(
    state: AppState,
    dayIndex: Long,
    served: Map<Pillar, Served>,
    ring: RingState,
    mission: Served?,
    reps: List<RepType>,
    principles: List<Principle>,
    pulse: Pillar?,
    onOpen: (Pillar) -> Unit,
    onOpenMission: () -> Unit,
    onLogRep: (RepType) -> Unit,
    onUndoRep: (RepType) -> Unit,
    onOpenPrinciple: (Int) -> Unit,
    onAllPrinciples: () -> Unit,
    onPickGoal: () -> Unit,
    onOpenCampaign: () -> Unit,
    onOpenAvatar: () -> Unit
) {
    val tiers = Pillar.order.associateWith { state.state(it).tier }
    val open = Pillar.order.count { !state.isDoneToday(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .verticalScroll(rememberScrollState())
    ) {
        HeroBar(state = state, dayIndex = dayIndex, onOpenAvatar = onOpenAvatar)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .padding(horizontal = 8.dp)
        ) {
            BubbleRing(
                state = ring,
                pulse = pulse,
                animate = !rememberReducedMotion(),
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Pillar.order.forEach { p ->
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .panel(fill = Ink.Surface)
                        .padding(horizontal = 10.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Dot(p.accent, size = 8)
                    Column {
                        Label(p.display)
                        Text(
                            text = "Lv ${tiers.getValue(p)}",
                            color = p.accent,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            VSpace(12)
            SectionHeader(
                title = "Today",
                trailing = if (open == 0) "All done" else "$open left"
            )

            Pillar.order.forEach { pillar ->
                served[pillar]?.let { s ->
                    ChallengeCard(
                        served = s,
                        done = state.isDoneToday(pillar),
                        onClick = { onOpen(pillar) }
                    )
                }
            }

            if (open == 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .panel(border = Ink.Activity.copy(alpha = 0.6f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Glyph(Glyphs.Check, colour = Ink.Activity, size = 22)
                    Text(
                        text = "All three done. Your bubble got bigger in every direction " +
                            "today — that is not a normal day.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            val goal = state.goalEnum
            VSpace(14)

            if (goal == null) {
                NoGoalCard(onPickGoal)
            } else {
                SectionHeader(
                    title = "Your goal",
                    trailing = if (mission == null) "Finished" else "Step ${mission.tier} of 30"
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Space.tap)
                        .panel(fill = Ink.Surface)
                        .clickable(role = Role.Button, onClick = onOpenCampaign)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = goal.display,
                            color = Ink.Primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Label("See the whole plan")
                    }
                    Glyph(Glyphs.ArrowRight, colour = Ink.Muted, size = 18)
                }

                when {
                    mission == null -> CampaignComplete(goal, onPickGoal)
                    else -> ChallengeCard(
                        served = mission,
                        done = state.missionDoneToday,
                        onClick = onOpenMission
                    )
                }

                VSpace(14)
                RepTracker(
                    reps = reps,
                    today = state.repsToday,
                    target = mission?.repTarget ?: 0,
                    lifetime = state.repsOnGoal(goal),
                    accent = goal.homePillar.accent,
                    onLog = onLogRep,
                    onUndo = onUndoRep
                )

                VSpace(18)
                MindsetSection(
                    principles = principles,
                    step = state.goalState(goal).step,
                    read = state.goalState(goal).read,
                    accent = goal.homePillar.accent,
                    onOpen = onOpenPrinciple,
                    onSeeAll = onAllPrinciples
                )
            }

            VSpace(24)
        }
    }
}

/**
 * Who you are, at the top of your own home screen.
 *
 * The avatar is here rather than buried in a profile tab because it is the thing that changes
 * when you do something, and a reward you have to navigate to is not a reward.
 */
@Composable
private fun HeroBar(state: AppState, dayIndex: Long, onOpenAvatar: () -> Unit) {
    val level = state.level
    val plates = state.plates

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter)
            .padding(top = 14.dp, bottom = 4.dp)
            .panel(fill = Ink.Surface, shape = RoundedCornerShape(Space.radiusLarge))
            .clickable(role = Role.Button, onClick = onOpenAvatar)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Avatar(
            avatar = state.avatar,
            friction = state.friction,
            size = 76
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Level $level",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Pill("Day $dayIndex", Ink.Muted)
                if (plates > 0) Pill("$plates armour", Ink.Ember)
            }
            ProgressTrack(
                fraction = state.levelProgress,
                accent = Ink.Gold,
                height = 8
            )
            Label(
                "${Progress.xpIntoLevel(state.xp)} / ${Progress.XP_PER_LEVEL} XP " +
                    "to level ${level + 1}"
            )
        }
        Glyph(Glyphs.ArrowRight, colour = Ink.Muted, size = 18)
    }
}

/**
 * The ideas behind the app, promoted from a single link into a real list.
 *
 * Unlocked ideas look like something you would want to read. Locked ones state exactly what
 * unlocks them and how close you are, rather than being invisible until they appear.
 */
@Composable
private fun MindsetSection(
    principles: List<Principle>,
    step: Int,
    read: Set<Int>,
    accent: androidx.compose.ui.graphics.Color,
    onOpen: (Int) -> Unit,
    onSeeAll: () -> Unit
) {
    if (principles.isEmpty()) return
    val unlocked = principles.count { it.unlocksAt <= step }

    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
        SectionHeader(
            title = "The ideas underneath",
            trailing = "$unlocked of ${principles.size} unlocked"
        )
        Text(
            text = "Short reads on why any of this works. One unlocks every few steps.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )

        // Two cards, chosen to always give the reader something: whatever is unlocked and
        // still unread, then the next thing to look forward to.
        val unreadUnlocked = principles.indices
            .filter { principles[it].unlocksAt <= step && it !in read }
        val nextLocked = principles.indexOfFirst { it.unlocksAt > step }
        val featured = buildList {
            addAll(unreadUnlocked.take(2))
            if (size < 2 && nextLocked >= 0) add(nextLocked)
            // Everything read and everything unlocked: show the last one rather than nothing.
            if (isEmpty()) {
                principles.indices.lastOrNull { principles[it].unlocksAt <= step }?.let { add(it) }
            }
        }

        featured.forEach { i ->
            PrincipleCard(
                principle = principles[i],
                index = i,
                step = step,
                isRead = i in read,
                accent = accent,
                onOpen = { onOpen(i) }
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Space.tap)
                .panel()
                .clickable(role = Role.Button, onClick = onSeeAll)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Label("See all ${principles.size} ideas", color = Ink.Primary, strong = true)
            Glyph(Glyphs.ArrowRight, colour = Ink.Muted, size = 18)
        }
    }
}

@Composable
private fun PrincipleCard(
    principle: Principle,
    index: Int,
    step: Int,
    isRead: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    onOpen: () -> Unit
) {
    val locked = principle.unlocksAt > step

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(
                fill = if (locked) Ink.Void else Ink.Surface,
                border = if (locked) Ink.Faint else accent.copy(alpha = 0.55f)
            )
            .then(
                if (locked) Modifier
                else Modifier.clickable(role = Role.Button, onClick = onOpen)
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            if (locked) {
                Glyph(Glyphs.Lock, colour = Ink.Muted, size = 17)
            } else {
                Glyph(Glyphs.Spark, colour = accent, size = 17)
            }
            Text(
                text = principle.title,
                modifier = Modifier.weight(1f),
                color = if (locked) Ink.Muted else Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            if (!locked && isRead) Pill("Read", Ink.Activity)
        }

        if (locked) {
            Label("Unlocks at step ${principle.unlocksAt}. You are on step $step.")
            ProgressTrack(
                fraction = step.toFloat() / principle.unlocksAt,
                accent = Ink.Faint,
                height = 6
            )
        } else {
            Text(
                text = principle.body.take(110).trimEnd().let {
                    if (principle.body.length > 110) "$it…" else it
                },
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Label(if (isRead) "Read it again" else "Read this", color = accent, strong = true)
                Glyph(Glyphs.ArrowRight, colour = accent, size = 15)
            }
        }
    }
}

@Composable
private fun NoGoalCard(onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(shape = RoundedCornerShape(Space.radiusLarge), border = Ink.Access)
            .clickable(role = Role.Button, onClick = onPick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Label("No goal picked yet")
        Text(
            text = "What do you actually want?",
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "More friends. Someone to date. A hobby you are good at. Pick one and " +
                "you get a 30-step plan, daily attempt targets and short reads of its own.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Label("Pick a goal", color = Ink.Access, strong = true)
            Glyph(Glyphs.ArrowRight, colour = Ink.Access, size = 16)
        }
    }
}

@Composable
private fun CampaignComplete(goal: Goal, onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(
                shape = RoundedCornerShape(Space.radiusLarge),
                border = goal.homePillar.accent
            )
            .clickable(role = Role.Button, onClick = onPick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Glyph(Glyphs.Check, colour = goal.homePillar.accent, size = 22)
            Text(
                text = "All 30 steps done",
                color = goal.homePillar.accent,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
        Text(
            text = "Keep logging your attempts — those never stop. When you are ready, " +
                "point the next 30 steps at something else.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Label("Pick the next goal", color = Ink.Primary, strong = true)
            Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
        }
    }
}

/** Bottom navigation. Two destinations, both labelled. */
@Composable
fun TabBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink.Surface)
            .padding(top = 8.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Tab.entries.forEach { tab ->
            Column(
                modifier = Modifier
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { selected = tab == current }
                    .heightIn(min = Space.tap)
                    .wrapContentSize(Alignment.Center)
                    .padding(horizontal = 22.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Dot(if (tab == current) Ink.Access else Ink.Faint, size = 7)
                Text(
                    text = tab.label,
                    color = if (tab == current) Ink.Primary else Ink.Muted,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

enum class Tab(val label: String) {
    TODAY("Today"),
    PROFILE("You")
}
