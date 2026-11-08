package com.debubble.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.HabitKind
import com.debubble.app.engine.HabitStats
import com.debubble.app.engine.Habits
import com.debubble.app.engine.Insight
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.PremiumPill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.fill
import com.debubble.app.ui.theme.wash
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.floor

/** Each habit's colour: the domain it belongs to, or gold for steps outside. */
fun habitAccent(k: HabitKind): Color = when (k) {
    HabitKind.BODY, HabitKind.TRAINING -> Ink.Body
    HabitKind.ROOM -> Ink.Room
    HabitKind.HEART, HabitKind.READING -> Ink.Heart
    HabitKind.OUTSIDE -> Ink.Gold
}

private val DAY_LETTERS = listOf("M", "T", "W", "T", "F", "S", "S")

/**
 * Your habits: every one, with this week's days, and its own page behind it.
 *
 * Insights look across them, and are Premium. Free users see how many are waiting, so the
 * lock says what is behind it rather than being a mystery.
 */
@Composable
fun HabitListScreen(
    art: RoomArt,
    stats: Map<HabitKind, HabitStats>,
    /** The routine each care habit is, by name: "Wash up". */
    names: Map<HabitKind, String>,
    insights: List<Insight>,
    premium: Boolean,
    onOpen: (HabitKind) -> Unit,
    onPremium: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        TopBar(title = "Your habits", subtitle = "Every day you did each one", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Label("No streaks here. A day done stays done, and a missed one is just a missed one.")
            HabitKind.entries.forEach { k ->
                val s = stats[k] ?: return@forEach
                HabitRow(art, k, names[k], s, onClick = { onOpen(k) })
            }

            VSpace(6)
            SectionHeader("Insights", trailing = if (premium) null else "Premium")
            if (premium) {
                if (insights.isEmpty()) {
                    Text(
                        text = "Insights appear once there are a couple of weeks of days to look at. " +
                            "Keep going: they will turn up here.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    insights.forEach { InsightCard(art, it) }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().panel(fill = Ink.Gold.wash).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Glyph(Glyphs.Lock, colour = Ink.Primary, size = 18)
                        Text(
                            text = when (insights.size) {
                                0 -> "Insights look across all your habits"
                                1 -> "One insight is waiting"
                                else -> "${insights.size} insights are waiting"
                            },
                            modifier = Modifier.weight(1f),
                            color = Ink.Primary,
                            style = MaterialTheme.typography.titleMedium
                        )
                        PremiumPill()
                    }
                    Text(
                        text = "Your best day of the week, which habits go together, what helps your " +
                            "mood, and how much stronger you are getting.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    SecondaryButton("See them with Premium", onClick = onPremium)
                }
            }
        }
    }
}

@Composable
private fun HabitRow(art: RoomArt, k: HabitKind, name: String?, s: HabitStats, onClick: () -> Unit) {
    val accent = habitAccent(k)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .panel()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(Cut.small)
                .background(accent.wash)
                .border(BorderStroke(2.dp, Ink.Border), Cut.small),
            contentAlignment = Alignment.Center
        ) {
            SpriteIcon(art, k.icon, pixelScale = pixelScale(2f))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(k.title, color = accent, style = MaterialTheme.typography.labelLarge)
                Label(name ?: k.blurb, modifier = Modifier.weight(1f, fill = false))
            }
            WeekStrip(s.week, accent)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${s.total}", color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
            Label(if (s.total == 1) "day" else "days")
        }
    }
}

/** Monday to Sunday as seven little blocks. */
@Composable
private fun WeekStrip(week: List<Boolean>, accent: Color, big: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = "${week.count { it }} of 7 days this week" },
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        week.forEachIndexed { i, on ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = if (big) 30.dp else 18.dp)
                    .clip(Cut.small)
                    .background(if (on) accent.fill else Ink.Well)
                    .border(BorderStroke(if (big) 2.dp else 1.5.dp, if (on) Ink.Border else Ink.Faint), Cut.small),
                contentAlignment = Alignment.Center
            ) {
                if (big) Text(DAY_LETTERS[i], color = Ink.Primary, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun InsightCard(art: RoomArt, i: Insight) {
    Row(
        modifier = Modifier.fillMaxWidth().panel(fill = Ink.Gold.wash).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        SpriteIcon(art, i.icon, pixelScale = pixelScale(1.8f), modifier = Modifier.padding(top = 2.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(i.title, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Text(i.body, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** One line of a habit's history: when, and what. */
data class HabitLine(val day: Long, val text: String)

/**
 * One habit, in full: how many days, this week, twelve weeks of squares, when it usually
 * happens, how you felt afterwards and how automatic it is getting, where those are known.
 */
@Composable
fun HabitPageScreen(
    art: RoomArt,
    kind: HabitKind,
    stats: HabitStats,
    /** The routine's name, for a care habit. */
    name: String?,
    /** "When I get home, I do it." */
    cue: String?,
    feelings: Habits.Feelings?,
    /** 0 to 1, for a care habit. */
    automaticity: Float?,
    daysWithRoutine: Int?,
    reminder: String?,
    recent: List<HabitLine>,
    today: Long,
    h24: Boolean,
    doLabel: String,
    onDo: () -> Unit,
    onBack: () -> Unit
) {
    val accent = habitAccent(kind)
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        TopBar(title = kind.title, subtitle = name ?: kind.blurb, onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().panel(fill = accent.wash).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteIcon(art, kind.icon, pixelScale = pixelScale(2.6f))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${stats.total} ${if (stats.total == 1) "day" else "days"}",
                        modifier = Modifier.semantics { heading() },
                        color = Ink.Primary,
                        style = MaterialTheme.typography.displaySmall
                    )
                    Label(
                        stats.first?.let { "Since ${date(it)}. Every one of them counts, for good." }
                            ?: "Nothing yet. The first day is the one that matters most."
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
                Fact("This week", "${stats.weekCount} of 7", Modifier.weight(1f))
                Fact("Last 30 days", "${stats.last30}", Modifier.weight(1f))
                Fact("Best week", "${stats.bestWeek} of 7", Modifier.weight(1f))
            }

            Column(
                modifier = Modifier.fillMaxWidth().panel().padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("This week", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                WeekStrip(stats.week, accent, big = true)
                VSpace(2)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Twelve weeks", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                    Label("oldest on the left")
                }
                WeeksGrid(stats, accent, today)
            }

            stats.usualMinute?.let { m ->
                Column(
                    modifier = Modifier.fillMaxWidth().panel().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("Usually in the ${Habits.partOfDay(m)}", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                    Label("Around ${Habits.clock(m, h24)}. The same moment each day is what makes it automatic.")
                }
            }

            if (cue != null || reminder != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().panel().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (cue != null) Text(cue, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                    if (reminder != null) Label("Reminder: $reminder", color = Ink.Gold, strong = true)
                }
            }

            if (automaticity != null) {
                Column(
                    modifier = Modifier.fillMaxWidth().panel().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("How automatic", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                        Label("about ${(automaticity * 100).toInt()}%")
                    }
                    ProgressTrack(automaticity, accent)
                    Label(
                        "${daysWithRoutine ?: 0} ${if (daysWithRoutine == 1) "day" else "days"} with this routine. " +
                            "Most people in Lally et al. (2010) were close to automatic by day 66."
                    )
                }
            }

            if (feelings != null && feelings.total > 0) {
                Column(
                    modifier = Modifier.fillMaxWidth().panel().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("How you felt afterwards", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                    FeelBar("Better", feelings.better, feelings.total, accent)
                    FeelBar("About the same", feelings.same, feelings.total, accent)
                    FeelBar("Worse", feelings.worse, feelings.total, accent)
                    Label("Your own answers, after doing it. That is evidence about you, not about people in general.")
                }
            }

            if (recent.isNotEmpty()) {
                SectionHeader("Recent")
                Column(modifier = Modifier.fillMaxWidth().panel().padding(horizontal = 14.dp, vertical = 6.dp)) {
                    recent.forEach { line ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Label(shortDate(line.day, today), modifier = Modifier.padding(top = 1.dp), color = Ink.Primary, strong = true)
                            Text(line.text, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        Column(modifier = Modifier.padding(horizontal = Space.gutter, vertical = 12.dp)) {
            PrimaryButton(doLabel, onClick = onDo)
        }
    }
}

@Composable
private fun Fact(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.panel().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Label(label)
        Text(value, color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
    }
}

@Composable
private fun FeelBar(label: String, n: Int, total: Int, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Label(label, color = Ink.Primary, strong = true)
            Label("$n")
        }
        ProgressTrack(if (total == 0) 0f else n / total.toFloat(), accent, height = 10)
    }
}

/**
 * Twelve weeks as a grid of pixel squares: one column a week, Monday at the top. Done days
 * are filled in the habit's colour; days still to come are left out.
 */
@Composable
private fun WeeksGrid(stats: HabitStats, accent: Color, today: Long) {
    val weeks = Habits.GRID_WEEKS
    val fillColour = accent.fill
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(weeks / 7f)
            .clearAndSetSemantics {
                contentDescription = "${stats.grid.count { it }} days done in the last twelve weeks"
            }
            .drawBehind {
                val cell = floor(minOf(size.width / weeks, size.height / 7f))
                val gap = floor(cell * 0.18f).coerceAtLeast(1f)
                val line = floor(1.5f * density).coerceAtLeast(1f)
                val side = cell - gap
                for (w in 0 until weeks) for (d in 0 until 7) {
                    val index = w * 7 + d
                    val day = stats.gridStart + index
                    if (day > today) continue
                    val x = floor(w * cell)
                    val y = floor(d * cell)
                    val on = stats.grid.getOrElse(index) { false }
                    drawRect(if (on) Ink.Border else Ink.Faint, topLeft = Offset(x, y), size = Size(side, side))
                    drawRect(
                        if (on) fillColour else Ink.Well,
                        topLeft = Offset(x + line, y + line),
                        size = Size(side - line * 2, side - line * 2)
                    )
                }
            }
    )
}

private fun date(day: Long): String =
    LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("d MMMM"))

private fun shortDate(day: Long, today: Long): String = when (today - day) {
    0L -> "Today"
    1L -> "Yesterday"
    else -> LocalDate.ofEpochDay(day).format(DateTimeFormatter.ofPattern("EEE d MMM"))
}
