package com.debubble.app.ui.screens

import androidx.compose.foundation.ScrollState
import kotlin.math.floor
import com.debubble.app.ui.theme.fill
import com.debubble.app.ui.components.Pill
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Goal
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Habitat
import com.debubble.app.engine.Look
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Progress
import com.debubble.app.engine.Quest
import com.debubble.app.engine.QuestKind
import com.debubble.app.engine.RoomTap
import com.debubble.app.engine.Routed
import com.debubble.app.engine.Served
import com.debubble.app.engine.Today
import com.debubble.app.engine.Tour
import com.debubble.app.engine.TourSpot
import com.debubble.app.engine.World
import com.debubble.app.ui.components.BlockButton
import com.debubble.app.engine.Target
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.HabitatAnnouncement
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.MeHead
import com.debubble.app.ui.components.PageHeader
import com.debubble.app.ui.components.PaperLabel
import com.debubble.app.ui.components.PaperText
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.QuestRow
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TodayHeader
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.paper
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.components.tourSpot
import com.debubble.app.ui.components.tourViewport
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.centered
import com.debubble.app.ui.theme.roomGround
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.debubble.app.ui.theme.wash
import com.debubble.app.ui.theme.Cut

/**
 * Home: your room, and today.
 *
 * Top to bottom: the day and you, your room, any mail (a guess waiting to be checked), today's
 * four, anything extra folded away, how far the room has opened, and your journey. One list
 * for the day, in one visual language: what used to be four extra sections in the old
 * dashboard style — focus, goal, reps, reading — is now one card for the journey and one
 * fold for extras, so the day has a clear end.
 *
 * The room and the list are one thing. Each of today's four lives somewhere in the room — you
 * are your body, the floor is your room, the heart at your shoulder is your heart, and the
 * letter in your hand is today's mission outside — and tapping it there opens it, the same as
 * tapping its row. Care comes first because the foundation is what makes the stretch
 * possible: behavioural activation restores routine activities before harder ones for the
 * same reason.
 */
@Composable
fun DashboardScreen(
    state: AppState,
    dayIndex: Long,
    served: Map<Pillar, Served>,
    quests: List<Quest>,
    /** "19:00" for a row with a reminder set. */
    reminderChip: (Quest) -> String?,
    mission: Served?,
    routed: Routed,
    routedDone: Set<String>,
    openForecasts: List<Forecast>,
    debuffLabel: (String) -> String,
    onOpenQuest: (Quest) -> Unit,
    onOpen: (Pillar) -> Unit,
    onReview: (Long) -> Unit,
    onOpenRoutedCampaign: () -> Unit,
    onOpenRoutedRemediation: () -> Unit,
    onPickGoal: () -> Unit,
    onOpenJourney: () -> Unit,
    onOpenWardrobe: () -> Unit,
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
    onCheckIn: () -> Unit,
    /** Hoisted so the tour can scroll to what it is pointing at. */
    scroll: ScrollState = rememberScrollState(),
    /** One set of something, sized to last time. See engine/Training.kt. */
    move: Target? = null,
    trainedToday: Boolean = false,
    onMoveDone: (Target) -> Unit = {},
    onOpenTraining: () -> Unit = {}
) {
    val stage = state.stage
    val next = Today.next(quests)
    val outside = quests.firstOrNull { it.kind == QuestKind.OUTSIDE }
    val letter = Today.letter(quests)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .tourViewport(scroll)
            .verticalScroll(scroll)
    ) {
        TopLine(art = art, state = state, dayIndex = dayIndex, onOpenWardrobe = onOpenWardrobe)

        if (welcomeBack != null) {
            Text(
                text = welcomeBack,
                modifier = Modifier
                    .padding(horizontal = Space.gutter)
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .panel(fill = Ink.Heart.wash)
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
            letter = letter,
            onTap = { tap -> if (tap == null) onOpenRoom() else onRoomTap(tap) },
            modifier = Modifier.padding(horizontal = 8.dp),
            description = "Your room. " + Domain.order.joinToString(". ") { d ->
                "${library.info(d).name}: ${library.levelName(d, dirt[d] ?: 0)}"
            } + ". Tap yourself, your heart, the mess or the letter to start that task."
        )
        Text(
            text = if (letter) "Tap you, your heart, the mess, or the letter in your hand."
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

            openForecasts.take(2).forEach { f ->
                VSpace(4)
                Mail(art = art, forecast = f, onOpen = { onReview(f.id) })
            }

            VSpace(12)
            TodayHeader(quests, modifier = Modifier.tourSpot(TourSpot.TODAY))
            quests.forEach { q ->
                QuestRow(
                    art = art,
                    quest = q,
                    next = q == next,
                    reminder = reminderChip(q),
                    onClick = { onOpenQuest(q) },
                    modifier = Modifier.tourSpot(Tour.spotOf(q))
                )
            }

            MoreForToday(
                art = art,
                state = state,
                served = served,
                outside = outside,
                routed = routed,
                routedDone = routedDone,
                debuffLabel = debuffLabel,
                onOpen = onOpen,
                onOpenRoutedCampaign = onOpenRoutedCampaign,
                onOpenRoutedRemediation = onOpenRoutedRemediation
            )

            if (move != null) {
                VSpace(6)
                MoveCard(art, move, trainedToday, onDone = { onMoveDone(move) }, onOpen = onOpenTraining)
            }

            VSpace(6)
            WorldStrip(
                art = art,
                state = state,
                owned = state.home.owned.size,
                total = art.catalogue.items.size,
                onOpenRoom = onOpenRoom,
                onCheckIn = onCheckIn,
                modifier = Modifier.tourSpot(TourSpot.WORLD)
            )

            // After the list it puts on the home screen, because that adjacency is the
            // argument. Offering it on an empty dashboard on day one would be asking for
            // home-screen space before the app has earned any.
            if (offerWidget) {
                VSpace(10)
                WidgetOffer(art = art, onAdd = onAddWidget, onDismiss = onDismissWidget)
            }

            VSpace(14)
            JourneyCard(
                art = art,
                state = state,
                mission = mission,
                onOpen = onOpenJourney,
                onPick = onPickGoal
            )

            VSpace(24)
        }
    }
}

/**
 * The day, and you. Your own head in the corner, wearing whatever you have earned, with your
 * level under it — tapping it opens the wardrobe, which is where a level means something.
 */
@Composable
private fun TopLine(art: RoomArt, state: AppState, dayIndex: Long, onOpenWardrobe: () -> Unit) {
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM")) }
    PageHeader(
        art = art,
        icon = World.TAB_HOME,
        title = "Day $dayIndex",
        subtitle = today,
        modifier = Modifier
            .padding(horizontal = Space.gutter)
            .padding(top = 16.dp),
        trailing = {
            Row(
                modifier = Modifier
                    .heightIn(min = Space.tap)
                    .panel(fill = Ink.Gold.wash, shape = Cut.card)
                    .clickable(role = Role.Button, onClickLabel = "Open the wardrobe", onClick = onOpenWardrobe)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MeHead(art = art, look = state.home.look, pixelScale = pixelScale(1.6f))
                Column(modifier = Modifier.width(80.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Level ${state.level}", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                    ProgressTrack(fraction = state.levelProgress, accent = Ink.Gold, height = 10)
                    Label("${Progress.xpIntoLevel(state.xp)} of ${Progress.XP_PER_LEVEL} XP")
                }
            }
        }
    )
}

/**
 * A guess written before a mission and never checked: mail. It is the one thing the app
 * nags about, because the comparison is where the learning is.
 */
@Composable
private fun Mail(art: RoomArt, forecast: Forecast, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Gold.wash, borderWidth = 3.dp, shadow = Ink.Border)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, World.POSTCARD, pixelScale = pixelScale(2.4f))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("You have mail", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "How did \"${forecast.challengeTitle.take(60)}\" really go?",
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
    }
}

/**
 * Everything optional, folded into one place: the other two roads, and the two picked from
 * your audit and your journey. Optional is the important word — the day is the four above,
 * and a list that grows extra sections every week is a list nobody finishes.
 */
@Composable
private fun MoreForToday(
    art: RoomArt,
    state: AppState,
    served: Map<Pillar, Served>,
    outside: Quest?,
    routed: Routed,
    routedDone: Set<String>,
    debuffLabel: (String) -> String,
    onOpen: (Pillar) -> Unit,
    onOpenRoutedCampaign: () -> Unit,
    onOpenRoutedRemediation: () -> Unit
) {
    val roads = Pillar.order.filter { it != outside?.pillar && !state.isDoneToday(it) }
    val focus = buildList {
        routed.campaign?.let { c -> add(Triple(c.toServed(), c.id in routedDone, onOpenRoutedCampaign)) }
        routed.remediation?.let { r -> add(Triple(r.toServed(debuffLabel(r.debuff)), r.id in routedDone, onOpenRoutedRemediation)) }
    }
    val count = roads.size + focus.count { !it.second }
    if (count == 0 && focus.isEmpty()) return
    var open by remember { mutableStateOf(false) }

    if (!open) {
        TextAction(
            text = if (outside?.done == true) "Want more? $count more for today" else "$count more for today, if you want them",
            colour = Ink.Secondary,
            onClick = { open = true }
        )
        return
    }
    Label("Extras. Optional — today's four are what count.")
    roads.forEach { p ->
        served[p]?.let { s ->
            LetterRow(
                art = art,
                icon = World.road(p),
                accent = p.accent,
                kicker = "${World.roadName(p)} · step ${s.tier}",
                title = s.directive,
                done = false,
                onClick = { onOpen(p) }
            )
        }
    }
    if (focus.isNotEmpty()) {
        Label(routed.rationale.ifBlank { "Picked for you" })
        focus.forEach { (s, done, open1) ->
            LetterRow(
                art = art,
                icon = World.road(s.pillar),
                accent = s.pillar.accent,
                kicker = s.phase ?: World.roadName(s.pillar),
                title = s.directive,
                done = done,
                onClick = open1
            )
        }
    }
}

/** An extra letter: its road's sign, what it is, and whether it is done. */
@Composable
private fun LetterRow(
    art: RoomArt,
    icon: String,
    accent: Color,
    kicker: String,
    title: String,
    done: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .panel(fill = if (done) Ink.Void else Ink.Surface, border = if (done) Ink.Faint else Ink.Border)
            .clickable(role = Role.Button, enabled = !done, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(Cut.small)
                .background(if (done) Ink.Well else accent.wash)
                .border(BorderStroke(2.dp, if (done) Ink.Faint else Ink.Border), Cut.small),
            contentAlignment = Alignment.Center
        ) {
            SpriteIcon(art, icon, pixelScale = pixelScale(1.8f))
        }
        Column(modifier = Modifier.weight(1f)) {
            Label(kicker, color = if (done) Ink.Muted else accent, strong = true)
            Text(
                text = title,
                color = if (done) Ink.Secondary else Ink.Primary,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2
            )
        }
        SpriteIcon(art, if (done) Today.ICON_DONE else Today.ICON_TODO, pixelScale = pixelScale(2f))
    }
}

/**
 * A move for right now: one set of one exercise, sized to what you did last time. An exercise
 * snack, which is how fitness gets built by people who would never do a full session. The
 * card opens Training; the button logs the set as suggested.
 */
@Composable
private fun MoveCard(art: RoomArt, move: Target, trainedToday: Boolean, onDone: () -> Unit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Body.wash)
            .clickable(role = Role.Button, onClickLabel = "Open training", onClick = onOpen)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(Cut.small)
                .background(Ink.Surface)
                .border(BorderStroke(2.dp, Ink.Border), Cut.small),
            contentAlignment = Alignment.Center
        ) {
            SpriteIcon(art, move.exercise.icon, pixelScale = pixelScale(1.8f))
        }
        Column(modifier = Modifier.weight(1f)) {
            Label(if (trainedToday) "Another move?" else "A move for right now", color = Ink.Body, strong = true)
            Text(
                text = move.label.replaceFirstChar { it.uppercase() },
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
        BlockButton("Did it", modifier = Modifier.width(92.dp), small = true, onClick = onDone)
    }
}

/**
 * How far the room has opened, and what is in it. Missions outside are what open the walls,
 * so this sits right under the list that has today's mission in it.
 */
@Composable
private fun WorldStrip(
    art: RoomArt,
    state: AppState,
    owned: Int,
    total: Int,
    onOpenRoom: () -> Unit,
    onCheckIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stage = state.stage
    Column(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Ink.Surface)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteIcon(art, World.TAB_HOME, pixelScale = pixelScale(1.8f))
            Text(stage.label, color = Ink.Primary, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Label("$owned of $total things")
        }
        ProgressTrack(Habitat.progress(state.clearedRungs), Ink.Gold, height = 10)
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
 * Your goal, as one card: where the journey goes, how far along you are, and today's stop.
 * Everything else about it — the whole trail, your tries, the reading — is one tap away on
 * the journey itself, rather than four sections stacked under the day.
 */
@Composable
internal fun JourneyCard(
    art: RoomArt,
    state: AppState,
    mission: Served?,
    onOpen: () -> Unit,
    onPick: () -> Unit
) {
    val goal: Goal? = state.primaryGoal
    if (goal == null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .panel(fill = Ink.Gold.wash)
                .clickable(role = Role.Button, onClick = onPick)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteIcon(art, World.landmark(Goal.INTERESTING), pixelScale = pixelScale(2.4f))
            Column(modifier = Modifier.weight(1f)) {
                Text("Pick a journey", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                Label("Thirty small steps towards something you actually want.")
            }
            Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
        }
        return
    }
    val gs = state.goalState(goal)
    val accent = goal.homePillar.accent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Surface)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteIcon(art, World.landmark(goal), pixelScale = pixelScale(2.4f))
            Column(modifier = Modifier.weight(1f)) {
                Label("Your journey", color = accent, strong = true)
                Text(goal.display, color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
            }
            Pill(if (mission == null) "Arrived" else "Stop ${gs.step} of ${Goals.CAMPAIGN_LENGTH}", accent)
        }
        Trail(stops = Goals.CAMPAIGN_LENGTH, done = gs.completed, accent = accent)
        if (mission != null) {
            Text(
                text = if (state.missionDoneToday) "Today's stop is done." else mission.directive,
                color = if (state.missionDoneToday) Ink.Secondary else Ink.Primary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 3
            )
        } else {
            Text("All thirty stops. This ground stays yours.", color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Thirty stops as a row of pixel squares, filled in the road's ink as far as you have walked
 * it. Every tenth stop is a bigger square: a landmark to walk towards.
 */
@Composable
fun Trail(stops: Int, done: Int, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .semantics { contentDescription = "$done of $stops stops walked" }
            .drawBehind {
                val gap = size.width / stops
                val small = floor(minOf(gap * 0.55f, size.height * 0.55f)).coerceAtLeast(2f)
                val big = floor(minOf(gap * 0.85f, size.height)).coerceAtLeast(small)
                for (i in 0 until stops) {
                    val side = if (i % 10 == 9) big else small
                    val x = floor(gap * i + (gap - side) / 2f)
                    val y = floor((size.height - side) / 2f)
                    drawRect(
                        color = if (i < done) accent else Ink.Faint,
                        topLeft = Offset(x, y),
                        size = Size(side, side)
                    )
                }
            }
    )
}

/**
 * The offer to put your room and today on the home screen.
 *
 * This is the one piece of promotion in the app, and it is here because the mechanism needs
 * it rather than because growth does. A plan works by being remembered at the moment it
 * names; a plan filed inside an app that has to be opened is remembered by someone who
 * already remembered. On the home screen it is there when the moment is.
 *
 * Shown once. The launcher never reports back whether the user accepted, so a card that
 * reappeared until it detected a widget would reappear forever for everyone who said no.
 */
@Composable
private fun WidgetOffer(art: RoomArt, onAdd: () -> Unit, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Gold.wash)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteIcon(art, World.TAB_HOME, pixelScale = pixelScale(1.8f))
            Text("Put your room on your home screen", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = "Your room and today's four, where you will see them, with the times you " +
                "picked. On your home screen it is there when the moment is.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            Box(modifier = Modifier.weight(1f)) {
                PrimaryButton("Add the widget", onClick = onAdd)
            }
            Box(modifier = Modifier.weight(1f)) {
                SecondaryButton("Not now", onClick = onDismiss)
            }
        }
    }
}

/* ==================================================================== the tab bar */

/** Five places: your room, the world outside it, your journal, the library, and you. */
enum class Tab(val label: String) {
    HOME("Home"),
    OUTSIDE("Outside"),
    JOURNAL("Journal"),
    LIBRARY("Library"),
    YOU("You")
}

/**
 * The tab bar, in pixels: a house, a signpost, a notebook, a stack of books, and your own
 * face. The same art as the room, so moving between tabs never feels like leaving it.
 */
@Composable
fun TabBar(
    art: RoomArt,
    look: Look,
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink.Surface)
            .drawBehind { drawRect(Ink.Border, size = size.copy(height = 2.dp.toPx())) }
            .padding(top = 8.dp, bottom = 8.dp, start = 6.dp, end = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Tab.entries.forEach { tab ->
            val on = tab == current
            Column(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Space.tap)
                    .clip(Cut.card)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { selected = on }
                    .padding(vertical = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 54.dp, height = 32.dp)
                        .then(
                            if (on) Modifier
                                .clip(Cut.small)
                                .background(Ink.Gold.fill)
                                .border(BorderStroke(2.dp, Ink.Border), Cut.small)
                            else Modifier
                        )
                        .alpha(if (on) 1f else 0.7f),
                    contentAlignment = Alignment.Center
                ) {
                    when (tab) {
                        Tab.YOU -> MeHead(art = art, look = look, pixelScale = pixelScale(1.25f))
                        else -> SpriteIcon(art, iconOf(tab), pixelScale = pixelScale(1.6f))
                    }
                }
                Text(
                    text = tab.label,
                    color = if (on) Ink.Primary else Ink.Muted,
                    style = if (on) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

private fun iconOf(tab: Tab): String = when (tab) {
    Tab.HOME -> World.TAB_HOME
    Tab.OUTSIDE -> World.TAB_OUT
    Tab.JOURNAL -> World.TAB_JOURNAL
    Tab.LIBRARY -> World.TAB_LIBRARY
    Tab.YOU -> World.TAB_HOME
}
