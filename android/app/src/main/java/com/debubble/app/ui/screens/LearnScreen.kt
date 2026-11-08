package com.debubble.app.ui.screens

import androidx.compose.ui.semantics.heading
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import com.debubble.app.ui.components.PageHeader
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.AuditCatalogue
import com.debubble.app.engine.LearnCurriculum
import com.debubble.app.engine.Lesson
import com.debubble.app.engine.World
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Markdown
import com.debubble.app.ui.components.PageTip
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.roomGround
import com.debubble.app.ui.theme.wash
import com.debubble.app.ui.theme.Cut

/**
 * The Learn tab.
 *
 * Two sections, in this order for a reason. **For you** comes first and holds the articles
 * the Systems Audit switched on — those are the ones the reader has already told us are
 * relevant, and they should not be below thirty days of course material. **The course** comes
 * second, one lesson a day, with the next lock visible so there is always something coming.
 */
@Composable
fun LearnScreen(
    art: RoomArt,
    showTip: Boolean,
    onTipSeen: () -> Unit,
    curriculum: LearnCurriculum,
    catalogue: AuditCatalogue,
    dayIndex: Long,
    debuffs: Set<String>,
    read: Set<String>,
    onOpen: (String) -> Unit
) {
    val targeted = curriculum.targetedFor(debuffs)
    val unlockedCourse = curriculum.course.filter { it.day <= dayIndex }
    val nextLocked = curriculum.nextLocked(dayIndex)
    val readCount = curriculum.readCount(read)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        PageHeader(
            art = art,
            icon = World.TAB_LIBRARY,
            title = "Library",
            subtitle = "$readCount read",
            modifier = Modifier
                .padding(horizontal = Space.gutter)
                .padding(top = 18.dp, bottom = 12.dp)
        )

        Column(modifier = Modifier.padding(horizontal = Space.gutter)) {
            if (showTip) {
                PageTip(
                    art = art,
                    title = "Why any of this works",
                    body = "A new page opens each day for thirty days, and every habit you " +
                        "marked gets one of its own. Five minutes, standing up.",
                    onDismiss = onTipSeen
                )
                VSpace(10)
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Text(
                    text = "Day ${dayIndex.coerceAtMost(30)} of 30",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.titleLarge
                )
                ProgressTrack(
                    fraction = (unlockedCourse.size / 30f).coerceIn(0f, 1f),
                    accent = Ink.Gold,
                    height = 10
                )
                Label("${unlockedCourse.size} of 30 pages open")
            }

            if (targeted.isNotEmpty()) {
                VSpace(24)
                SectionHeader("For you", trailing = "${targeted.size} page${plural(targeted.size)}")
                VSpace(6)
                Text(
                    text = "For the habits you marked.",
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                VSpace(12)
                Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                    targeted.forEach { lesson ->
                        LessonCard(
                            lesson = lesson,
                            locked = false,
                            isRead = lesson.id in read,
                            accent = Ink.Gold,
                            badge = matchedLabel(lesson, debuffs, catalogue),
                            onOpen = { onOpen(lesson.id) }
                        )
                    }
                }
            } else if (debuffs.isEmpty()) {
                VSpace(20)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .panel(fill = Ink.Gold.wash)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Glyph(Glyphs.Spark, colour = Ink.Gold, size = 19)
                        Text("No habits marked yet", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = "Mark the habits you want to change, under You, and a page " +
                            "for each of them opens here.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            VSpace(26)
            SectionHeader("Thirty pages", trailing = "${unlockedCourse.size} of 30")
            VSpace(12)

            Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                unlockedCourse.asReversed().forEach { lesson ->
                    LessonCard(
                        lesson = lesson,
                        locked = false,
                        isRead = lesson.id in read,
                        accent = Ink.Heart,
                        badge = "Day ${lesson.day}",
                        onOpen = { onOpen(lesson.id) }
                    )
                }
                if (nextLocked != null) {
                    LessonCard(
                        lesson = nextLocked,
                        locked = true,
                        isRead = false,
                        accent = Ink.Faint,
                        badge = "Day ${nextLocked.day}",
                        onOpen = {}
                    )
                }
            }

            VSpace(28)
        }
    }
}

private fun plural(n: Int) = if (n == 1) "" else "s"

/** Which of the reader's own marks pulled this article in. */
private fun matchedLabel(lesson: Lesson, debuffs: Set<String>, catalogue: AuditCatalogue): String {
    val hits = lesson.debuffs.filter { it in debuffs }
    val first = hits.firstOrNull()?.let { catalogue.debuff(it) } ?: return "For you"
    return if (hits.size > 1) "${hits.size} of your marks" else first.label.take(34)
}

@Composable
private fun LessonCard(
    lesson: Lesson,
    locked: Boolean,
    isRead: Boolean,
    accent: androidx.compose.ui.graphics.Color,
    badge: String,
    onOpen: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(
                fill = if (locked) Ink.Void else Ink.Surface,
                border = if (locked) Ink.Faint else Ink.Border
            )
            .then(if (locked) Modifier else Modifier.clickable(role = Role.Button, onClick = onOpen))
            .semantics {
                stateDescription = when {
                    locked -> "Locked"
                    isRead -> "Read"
                    else -> "Unread"
                }
            }
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(Cut.small)
                    .background(if (locked) Ink.Well else accent.wash)
                    .border(BorderStroke(2.dp, if (locked) Ink.Faint else Ink.Border), Cut.small),
                contentAlignment = Alignment.Center
            ) {
                Glyph(
                    if (locked) Glyphs.Lock else Glyphs.Spark,
                    colour = if (locked) Ink.Muted else accent,
                    size = 15
                )
            }
            Text(
                text = if (locked) "Opens on day ${lesson.day}" else lesson.title,
                modifier = Modifier.weight(1f),
                color = if (locked) Ink.Muted else Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            if (!locked && isRead) Pill("Read", Ink.Gold, filled = true)
        }

        if (!locked) {
            Text(
                text = lesson.summary,
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Pill(badge, Ink.Muted)
                Pill("${lesson.minutes} min", Ink.Muted)
            }
        } else {
            Label("Keep going — this one opens on day ${lesson.day}.")
        }
    }
}

/** One lesson, full screen. */
@Composable
fun LessonScreen(
    lesson: Lesson,
    isRead: Boolean,
    onAddNote: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        TopBar(
            title = if (lesson.isTargeted) "For you" else "Day ${lesson.day}",
            subtitle = "${lesson.minutes} min read",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Text(
                text = lesson.title,
                modifier = Modifier.semantics { heading() },
                color = Ink.Primary,
                style = MaterialTheme.typography.displaySmall
            )
            VSpace(10)
            Text(
                text = lesson.summary,
                color = Ink.Muted,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )

            Markdown(
                text = lesson.body,
                accent = if (lesson.isTargeted) Ink.Gold else Ink.Heart
            )

            VSpace(24)
        }

        Column(
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            // Reading without writing anything down is how this becomes entertainment.
            SecondaryButton("Write a postcard about this", onClick = onAddNote)
            PrimaryButton(text = if (isRead) "Done" else "Mark as read") { onBack() }
        }
    }
}
