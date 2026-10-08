package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.engine.BudgetTier
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Forecasts
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Progress
import com.debubble.app.engine.Stage
import com.debubble.app.ui.components.Avatar
import com.debubble.app.ui.components.CheckBox
import com.debubble.app.ui.components.Divider
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Figure
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.HabitatRow
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PillarBar
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SelectRow
import com.debubble.app.ui.components.StatTile
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import com.debubble.app.ui.theme.roomGround

/**
 * You.
 *
 * The avatar sits at the top because it is the summary — level, clothes and armour say more
 * at a glance than any of the numbers underneath. Friction gets its own card, styled as a
 * credit, because that is the number this app wants people to be proud of.
 */
@Composable
fun ProfileScreen(
    state: AppState,
    dayIndex: Long,
    track: GoalTrack?,
    onRecalibrate: () -> Unit,
    onChangeGoal: () -> Unit,
    onOpenAvatar: () -> Unit,
    onOpenAudit: () -> Unit,
    onSetBudget: (BudgetTier) -> Unit,
    onToggleSound: () -> Unit,
    onToggleAmbient: () -> Unit,
    /** Whether a widget is already on a home screen, which changes the wording only. */
    widgetPlaced: Boolean,
    onAddWidget: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(roomGround())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
    ) {
        VSpace(18)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .panel(shape = RoundedCornerShape(Space.radiusLarge))
                .clickable(role = Role.Button, onClick = onOpenAvatar)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Avatar(avatar = state.avatar, friction = state.friction, size = 104)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text(
                    text = "Level ${state.level}",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.headlineMedium
                )
                ProgressTrack(state.levelProgress, Ink.Gold, height = 8)
                Label(
                    "${Progress.xpIntoLevel(state.xp)} / ${Progress.XP_PER_LEVEL} XP " +
                        "to level ${state.level + 1}"
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Label("Change how you look", color = Ink.Access, strong = true)
                    Glyph(Glyphs.ArrowRight, colour = Ink.Access, size = 15)
                }
            }
        }

        VSpace(14)
        FrictionCard(friction = state.friction)

        VSpace(14)
        Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            StatTile("Day", "$dayIndex", Modifier.weight(1f))
            StatTile(
                "Streak",
                "${state.streak}",
                Modifier.weight(1f),
                accent = if (state.streak > 0) Ink.Activity else Ink.Primary
            )
            StatTile("Done", "${state.tiersCleared}", Modifier.weight(1f))
        }

        VSpace(24)
        SectionHeader("How far out you are")
        VSpace(12)
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Pillar.order.forEach { p ->
                PillarBar(pillar = p, tier = state.state(p).tier)
            }
        }

        if (track != null && state.goalEnum != null) {
            VSpace(24)
            CampaignBlock(state.goalEnum!!, track, state)
        }

        VSpace(24)
        ForecastBlock(state)

        VSpace(24)
        EvidenceBlock(state)

        VSpace(24)
        SectionHeader("History", trailing = "${state.log.size} entries")
        VSpace(10)
        if (state.log.isEmpty()) {
            Text(
                text = "Nothing here yet. Finish something today and it shows up.",
                color = Ink.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(shape = RoundedCornerShape(Space.radiusLarge))
                    .padding(horizontal = 14.dp, vertical = 4.dp)
            ) {
                state.log.take(30).forEachIndexed { i, entry ->
                    if (i > 0) Divider()
                    HistoryRow(entry, dayIndex, state)
                }
            }
        }

        VSpace(24)
        SectionHeader("Your audit", trailing = "${state.debuffs.size} marked")
        VSpace(10)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Space.tap)
                .panel()
                .clickable(role = Role.Button, onClick = onOpenAudit)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Access, size = 20)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.debuffs.isEmpty()) "Run your systems audit"
                    else "${state.debuffs.size} target${if (state.debuffs.size == 1) "" else "s"} marked",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = if (state.debuffs.isEmpty())
                        "Mark specific habits and get challenges and reading for each."
                    else "Each one gets its own challenges, on rotation.",
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Glyph(Glyphs.ArrowRight, colour = Ink.Muted, size = 18)
        }

        VSpace(24)
        SectionHeader("Budget", trailing = state.budget.display)
        VSpace(6)
        Text(
            text = "A hard limit. Nothing above it is ever shown to you.",
            color = Ink.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
        VSpace(10)
        Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
            BudgetTier.all.forEach { t ->
                SelectRow(
                    label = t.display,
                    detail = t.detail,
                    selected = state.budget == t,
                    single = true,
                    accent = Ink.Activity,
                    onToggle = { onSetBudget(t) }
                )
            }
        }

        VSpace(24)
        SectionHeader("Sound")
        VSpace(10)
        Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
            SettingRow(
                label = "Sound effects",
                detail = "Short sounds when you commit or log something",
                on = state.soundOn,
                onToggle = onToggleSound
            )
            SettingRow(
                label = "Background hum",
                detail = "A low drone on the action screen. Never plays over music.",
                on = state.ambientOn,
                onToggle = onToggleAmbient
            )
        }

        VSpace(24)
        SectionHeader("The room", trailing = "${state.clearedRungs} rungs cleared")
        VSpace(10)
        HabitatRow(stage = state.stage, cleared = state.clearedRungs)
        VSpace(10)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .panel()
                .padding(vertical = 4.dp)
        ) {
            // The whole ladder, including the stages still ahead. Showing what is coming is
            // the point: a locked row someone can read is a reason to clear another rung,
            // and unlike a streak there is no way to lose one of these by missing a week.
            Stage.all.forEach { st ->
                val reached = state.clearedRungs >= st.threshold
                val here = st == state.stage
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (reached) {
                        Glyph(Glyphs.Check, colour = if (here) Ink.Gold else Ink.Muted, size = 16)
                    } else {
                        Glyph(Glyphs.Lock, colour = Ink.Faint, size = 16)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = st.label,
                            color = when {
                                here -> Ink.Primary
                                reached -> Ink.Secondary
                                else -> Ink.Muted
                            },
                            style = MaterialTheme.typography.labelLarge
                        )
                        if (here) Label("You are here", color = Ink.Gold)
                    }
                    Label(if (st.threshold == 0) "Start" else "${st.threshold}")
                }
            }
        }

        VSpace(24)
        SectionHeader("Home screen")
        VSpace(10)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .panel()
                .padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (widgetPlaced) {
                    "The widget is on your home screen. It shows the moment you planned " +
                        "for, so it is in front of you when the moment arrives."
                } else {
                    "A widget showing the moment you planned for. The plan only works if " +
                        "you see it when the cue turns up, and an app has to be opened first."
                },
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            SecondaryButton(
                if (widgetPlaced) "Add another" else "Add the widget",
                onClick = onAddWidget
            )
        }

        VSpace(24)
        Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
            SecondaryButton(
                if (state.goalEnum == null) "Pick a goal" else "Change your goal"
            ) { onChangeGoal() }
            SecondaryButton("Redo your setup") { onRecalibrate() }
        }

        VSpace(20)
        Text(
            text = "Everything here stays on this phone. There is no account and nothing " +
                "is sent anywhere.",
            color = Ink.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
        VSpace(28)
    }
}

/**
 * Friction, presented as the achievement it is.
 *
 * Amber, prominent, with the badge and the armour count. Nowhere in this app is friction
 * shown in a warning colour or with a downward arrow.
 */
@Composable
private fun FrictionCard(friction: Int) {
    val plates = Progress.plates(friction)
    val toNext = Progress.toNextPlate(friction)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(
                shape = RoundedCornerShape(Space.radiusLarge),
                fill = Ink.SurfaceHigh,
                border = Ink.Ember,
                borderWidth = 2.dp
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Glyph(Glyphs.Spark, colour = Ink.Ember, size = 20)
                Label("Friction", color = Ink.Ember, strong = true)
            }
            Pill(Engine.courageBadge(friction), Ink.Ember, filled = true)
        }
        Figure("$friction", color = Ink.Ember, large = true)
        Text(
            text = "Times you went past what was comfortable — a no, a bail, an awkward " +
                "moment. This is the number worth having.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        ProgressTrack(plates / Progress.MAX_PLATES.toFloat(), Ink.Ember, height = 8)
        Label(
            if (toNext == null) "$plates armour plates — all of them"
            else "$plates armour plates · $toNext more for the next"
        )
    }
}

@Composable
private fun CampaignBlock(goal: Goal, track: GoalTrack, state: AppState) {
    val gs = state.goalState(goal)
    val complete = Goals.isComplete(gs)
    val accent = goal.homePillar.accent

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(shape = RoundedCornerShape(Space.radiusLarge), border = accent.copy(alpha = 0.6f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Label("Your goal", color = accent, strong = true)
        Text(
            text = goal.display,
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineMedium
        )
        ProgressTrack(Goals.progress(gs), accent, height = 8)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Label(if (complete) "All 30 steps done" else "Step ${gs.step} of 30")
            Label("${gs.read.size} of ${track.principles.size} ideas read")
        }
        Divider()
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            StatTile("Attempts", "${state.repsOnGoal(goal)}", Modifier.weight(1f), accent)
            StatTile("Steps done", "${gs.completed}", Modifier.weight(1f), accent)
        }
    }
}

/**
 * The record of predictions against outcomes.
 *
 * This is the only number in the app that measures its actual claim. Levels measure
 * attendance and Friction measures nerve, but neither shows whether the thing that kept
 * someone inside — a forecast that is reliably catastrophic — has moved. This does, and it
 * does it with their own sentences rather than a score the app made up.
 */
@Composable
private fun ForecastBlock(state: AppState) {
    val done = Forecasts.resolved(state.forecasts)
    val mean = Forecasts.meanViolation(state.forecasts)
    val shift = Forecasts.calibrationShift(state.forecasts)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(
                shape = RoundedCornerShape(Space.radiusLarge),
                border = if (done.isEmpty()) Ink.Border else Ink.Access.copy(alpha = 0.6f)
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Glyph(Glyphs.Spark, colour = Ink.Access, size = 19)
            Label("Expected against actual", color = Ink.Access, strong = true)
        }

        if (done.isEmpty()) {
            Text(
                text = Forecasts.summary(state.forecasts),
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
                StatTile(
                    "Checked", "${done.size}", Modifier.weight(1f)
                )
                StatTile(
                    "Easier than feared",
                    "${(Forecasts.overestimateRate(state.forecasts) * 100).toInt()}%",
                    Modifier.weight(1f),
                    accent = Ink.Activity
                )
                StatTile(
                    "Average gap",
                    if (mean >= 0) "−%.1f".format(mean) else "+%.1f".format(-mean),
                    Modifier.weight(1f),
                    accent = if (mean >= 0) Ink.Activity else Ink.Ember
                )
            }
            Text(
                text = Forecasts.summary(state.forecasts),
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (shift != null && shift > 0.5f) {
                Divider()
                Text(
                    text = "Your predictions have got more accurate over time. That is the " +
                        "thing actually changing — not that it stopped being frightening, " +
                        "but that you stopped being wrong about it.",
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EvidenceBlock(state: AppState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(shape = RoundedCornerShape(Space.radiusLarge))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Label("What you have actually done", color = Ink.Primary, strong = true)
        Pillar.order.forEach { p ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Dot(p.accent)
                Text(
                    text = "${state.evidence(p)} ${p.evidenceNoun}",
                    modifier = Modifier.weight(1f),
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Divider()
        Text(
            text = "${state.minutesInvested} minutes spent outside the routine.",
            color = Ink.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun HistoryRow(entry: LogEntry, dayIndex: Long, state: AppState) {
    val colour: Color = if (entry.friction) Ink.Ember else entry.pillarEnum.accent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(modifier = Modifier.padding(top = 6.dp)) { Dot(colour, size = 8) }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = entry.title,
                color = Ink.Primary,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                )
            )
            if (entry.note.isNotBlank()) {
                Text(
                    text = entry.note,
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (entry.friction) Pill("Friction", Ink.Ember)
                Label(
                    when (entry.kind) {
                        "MISSION" -> "Goal step ${entry.tier}"
                        "REP" -> "Attempt"
                        else -> "Level ${entry.tier}"
                    }
                )
                Label("Day ${state.dayIndexOfEntry(entry, dayIndex)}")
            }
        }
    }
}

/** Which day of the user's run an entry happened on. */
private fun AppState.dayIndexOfEntry(entry: LogEntry, todayIndex: Long): Long {
    if (startedDay == 0L) return todayIndex
    return (entry.epochDay - startedDay + 1).coerceAtLeast(1L)
}

@Composable
private fun SettingRow(
    label: String,
    detail: String,
    on: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .panel(border = if (on) Ink.Activity else Ink.Border)
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(selected = on, accent = Ink.Activity)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = Ink.Primary,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = detail,
                color = Ink.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Text(
            text = if (on) "On" else "Off",
            color = if (on) Ink.Activity else Ink.Muted,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
