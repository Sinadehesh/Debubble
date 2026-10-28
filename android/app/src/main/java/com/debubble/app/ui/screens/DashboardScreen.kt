package com.debubble.app.ui.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Goal
import com.debubble.app.engine.Habitat
import com.debubble.app.engine.Intention
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Principle
import com.debubble.app.engine.Progress
import com.debubble.app.engine.Quest
import com.debubble.app.engine.QuestKind
import com.debubble.app.engine.RepType
import com.debubble.app.engine.RoomTap
import com.debubble.app.engine.Routed
import com.debubble.app.engine.Served
import com.debubble.app.engine.Today
import com.debubble.app.ui.components.ChallengeCard
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.HabitatAnnouncement
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.QuestRow
import com.debubble.app.ui.components.RepTracker
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TodayHeader
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.centered
import com.debubble.app.ui.theme.roomGround
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Home.
 *
 * Top to bottom: the day, your room, today's four, how far the room has opened, then your
 * plans, the focus from your audit, and your goal. Every section is a labelled block with a
 * visible boundary, so the screen can be scanned rather than decoded.
 *
 * The room and the list are one thing. Each of today's four lives somewhere in the room — you
 * are your body, the floor is your room, the heart at your shoulder is your heart, and the
 * letter in your hand is today's mission outside — and tapping it there opens it, the same as
 * tapping its row. Care comes first in the list because the foundation is what makes the
 * stretch possible: behavioural activation restores routine activities before harder ones for
 * the same reason. Three are the same every day; the fourth is new every day.
 */
@Composable
fun DashboardScreen(
    state: AppState,
    dayIndex: Long,
    served: Map<Pillar, Served>,
    quests: List<Quest>,
    mission: Served?,
    reps: List<RepType>,
    principles: List<Principle>,
    routed: Routed,
    routedDone: Set<String>,
    openForecasts: List<Forecast>,
    livePlans: List<Intention>,
    debuffLabel: (String) -> String,
    onOpenQuest: (Quest) -> Unit,
    onOpen: (Pillar) -> Unit,
    onOpenMission: () -> Unit,
    onLogRep: (RepType) -> Unit,
    onUndoRep: (RepType) -> Unit,
    onReview: (Long) -> Unit,
    onOpenRoutedCampaign: () -> Unit,
    onOpenRoutedRemediation: () -> Unit,
    onOpenPrinciple: (Int) -> Unit,
    onAllPrinciples: () -> Unit,
    onPickGoal: () -> Unit,
    onOpenCampaign: () -> Unit,
    onOpenAvatar: () -> Unit,
    /** Whether to offer the home-screen widget. See [WidgetOffer]. */
    offerWidget: Boolean,
    onAddWidget: () -> Unit,
    onDismissWidget: () -> Unit,
    onAcknowledgeHabitat: () -> Unit,
    /* ---- the room ---- */
    art: RoomArt,
    library: CareLibrary,
    dirt: Map<Domain, Int>,
    welcomeBack: String?,
    onRoomTap: (RoomTap) -> Unit,
    onOpenRoom: () -> Unit,
    onCheckIn: () -> Unit
) {
    val stage = state.stage
    val next = Today.next(quests)
    val outside = quests.firstOrNull { it.kind == QuestKind.OUTSIDE }
    // The other two ladders, for anyone who wants more than one mission today. Optional,
    // folded away, and never counted against the day.
    val extras = Pillar.order.filter { it != outside?.pillar && !state.isDoneToday(it) }
    var showExtras by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(roomGround())
            .verticalScroll(rememberScrollState())
    ) {
        TopLine(state = state, dayIndex = dayIndex, onOpenAvatar = onOpenAvatar)

        if (welcomeBack != null) {
            Text(
                text = welcomeBack,
                modifier = Modifier
                    .padding(horizontal = Space.gutter)
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .panel(fill = Ink.SurfaceHigh, border = Ink.Heart.copy(alpha = 0.7f))
                    .padding(14.dp),
                color = Ink.Primary,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // The room: the largest thing on the screen, and the first. A mirror of the real
        // ones, and the way into today's tasks — tap whatever needs you. Anywhere else opens
        // it full screen to rearrange.
        VSpace(10)
        HouseScene(
            art = art,
            stage = stage,
            home = state.home,
            dirt = dirt,
            letter = Today.letter(quests),
            onTap = { tap -> if (tap == null) onOpenRoom() else onRoomTap(tap) },
            modifier = Modifier.padding(horizontal = 8.dp),
            description = "Your room. " + Domain.order.joinToString(". ") { d ->
                "${library.info(d).name}: ${library.levelName(d, dirt[d] ?: 0)}"
            } + ". Tap yourself, your heart, the mess or the letter to start that task."
        )
        Text(
            text = if (Today.letter(quests)) "Tap you, your heart, the mess, or the letter in your hand."
            else "Tap you, your heart or the mess. Anywhere else to rearrange.",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
                .padding(top = 6.dp),
            color = Ink.Muted,
            style = MaterialTheme.typography.labelMedium.centered
        )

        Column(
            modifier = Modifier.padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            if (state.habitatChanged) {
                VSpace(10)
                HabitatAnnouncement(stage = stage, onDismiss = onAcknowledgeHabitat)
            }

            if (openForecasts.isNotEmpty()) {
                VSpace(10)
                SectionHeader("Waiting on you", trailing = "${openForecasts.size}")
                openForecasts.take(2).forEach { f -> ForecastPrompt(f) { onReview(f.id) } }
            }

            VSpace(12)
            TodayHeader(quests)
            quests.forEach { q ->
                QuestRow(
                    art = art,
                    quest = q,
                    next = q == next,
                    onClick = { onOpenQuest(q) }
                )
            }

            if (extras.isNotEmpty()) {
                if (showExtras) {
                    Label("Extra missions. Optional — today's one is the one that counts.")
                    extras.forEach { p ->
                        served[p]?.let { s -> ChallengeCard(served = s, done = false, onClick = { onOpen(p) }) }
                    }
                } else {
                    TextAction(
                        text = if (outside?.done == true) "Want more? ${extras.size} more missions outside"
                        else "${extras.size} more missions outside, if you want them",
                        colour = Ink.Secondary,
                        onClick = { showExtras = true }
                    )
                }
            }

            VSpace(6)
            WorldStrip(
                state = state,
                owned = state.home.owned.size,
                total = art.catalogue.items.size,
                onOpenRoom = onOpenRoom,
                onCheckIn = onCheckIn
            )

            if (livePlans.isNotEmpty()) {
                VSpace(10)
                SectionHeader("Your plans")
                livePlans.take(3).forEach { PlanRow(it) }
            }

            // After the list it puts on the home screen, because that adjacency is the
            // argument. Offering it on an empty dashboard on day one would be asking for
            // home-screen space before the app has earned any.
            if (offerWidget) {
                VSpace(10)
                WidgetOffer(onAdd = onAddWidget, onDismiss = onDismissWidget)
            }

            if (!routed.isEmpty) {
                VSpace(14)
                SectionHeader("Focus", trailing = routed.rationale.ifBlank { null })
                routed.campaign?.let { c ->
                    ChallengeCard(
                        served = c.toServed(),
                        done = c.id in routedDone,
                        onClick = onOpenRoutedCampaign
                    )
                }
                routed.remediation?.let { r ->
                    ChallengeCard(
                        served = r.toServed(debuffLabel(r.debuff)),
                        done = r.id in routedDone,
                        onClick = onOpenRoutedRemediation
                    )
                }
            }

            val goal = state.primaryGoal
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpriteIcon(art, Today.ICON_GOAL, pixelScale = pixelScale(2.6f))
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
 * The day, and your level, in one line. Slim on purpose: the room is the hero of this
 * screen, and a header that competed with it would be saying the number matters more.
 */
@Composable
private fun TopLine(state: AppState, dayIndex: Long, onOpenAvatar: () -> Unit) {
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM")) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter)
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Day $dayIndex", color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
            Label(today)
        }
        Column(
            modifier = Modifier
                .width(132.dp)
                .heightIn(min = Space.tap)
                .panel(fill = Ink.Surface, shape = RoundedCornerShape(Space.radius))
                .clickable(role = Role.Button, onClick = onOpenAvatar)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Level ${state.level}", color = Ink.Primary, style = MaterialTheme.typography.labelLarge)
                if (state.plates > 0) Label("${state.plates}", color = Ink.Ember, strong = true)
            }
            ProgressTrack(fraction = state.levelProgress, accent = Ink.Gold, height = 6)
            Label("${Progress.xpIntoLevel(state.xp)} / ${Progress.XP_PER_LEVEL} XP")
        }
    }
}

/**
 * How far the room has opened, and what is in it. Missions outside are what open the walls,
 * so this sits right under the list that has today's mission in it.
 */
@Composable
private fun WorldStrip(
    state: AppState,
    owned: Int,
    total: Int,
    onOpenRoom: () -> Unit,
    onCheckIn: () -> Unit
) {
    val stage = state.stage
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stage.label, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Label("$owned of $total things")
        }
        ProgressTrack(Habitat.progress(state.clearedRungs), Ink.Gold, height = 6)
        Label(Habitat.caption(state.clearedRungs))
        Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            Box(modifier = Modifier.weight(1f)) {
                TextAction("Honest check-in", colour = Ink.Secondary, onClick = onCheckIn)
            }
            Box(modifier = Modifier.weight(1f)) {
                TextAction("Arrange your room", colour = Ink.Secondary, onClick = onOpenRoom)
            }
        }
    }
}

/**
 * A prediction that was made and never checked.
 *
 * Deliberately the first thing on the screen. An unresolved forecast is the one piece of state
 * in this app with a deadline on its usefulness — the memory of what you expected decays fast,
 * and once it has gone the challenge is remembered as "it was fine" with the catastrophic
 * prediction quietly edited out. Which is exactly the edit this whole mechanic exists to stop.
 */
@Composable
private fun ForecastPrompt(forecast: Forecast, onReview: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh, border = Ink.Ember)
            .clickable(role = Role.Button, onClick = onReview)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Ember, size = 18)
            Text(
                text = "How did it actually go?",
                modifier = Modifier.weight(1f),
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            Pill("${forecast.predictedDistress}/10 expected", Ink.Ember)
        }
        Text(
            text = forecast.challengeTitle,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Label("Check your prediction", color = Ink.Ember, strong = true)
            Glyph(Glyphs.ArrowRight, colour = Ink.Ember, size = 15)
        }
    }
}

/** A live if-then plan, shown as the sentence it is. */
@Composable
private fun PlanRow(plan: Intention) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .panel(border = Ink.Activity.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Glyph(Glyphs.Check, colour = Ink.Activity, size = 17)
        Text(
            text = plan.sentence,
            color = Ink.Primary,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
        )
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
                    .weight(1f)
                    .heightIn(min = Space.tap)
                    .wrapContentSize(Alignment.Center)
                    .padding(vertical = 4.dp),
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
    LEARN("Learn"),
    NOTES("Notes"),
    PROFILE("You")
}

/**
 * The offer to put today's cue on the home screen.
 *
 * This is the one piece of promotion in the app, and it is here because the mechanism needs
 * it rather than because growth does. An if-then plan works by being retrieved at the moment
 * it names; a plan filed inside an app that has to be opened is retrieved by someone who
 * already remembered, which is the group that did not need the plan. On the home screen it is
 * in front of them at the moment the cue arrives.
 *
 * Shown once. The launcher never reports back whether the user accepted, so a card that
 * reappeared until it detected a widget would reappear forever for everyone who said no.
 */
@Composable
private fun WidgetOffer(onAdd: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh, border = Ink.Gold.copy(alpha = 0.7f))
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Gold, size = 18)
            Label("Put your room on your home screen", color = Ink.Gold, strong = true)
        }
        Text(
            text = "Your room and today's four, where you will see them. A plan you have to " +
                "open an app to read gets read by people who already remembered; on your " +
                "home screen it is there when the moment is.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            Box(modifier = Modifier.weight(1f)) {
                PrimaryButton("Add the widget", accent = Ink.Gold, onClick = onAdd)
            }
            Box(modifier = Modifier.weight(1f)) {
                SecondaryButton("Not now", onClick = onDismiss)
            }
        }
    }
}
