package com.debubble.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Look
import com.debubble.app.engine.Looks
import com.debubble.app.engine.Quest
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Today
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent
import kotlin.math.roundToInt

/** Screen pixels per art pixel for roughly [dp] dp each, rounded to a whole number. */
@Composable
fun pixelScale(dp: Float): Int = (LocalDensity.current.density * dp).roundToInt().coerceAtLeast(1)

/**
 * "Today", how many are done, and four segments that fill in each task's own colour.
 *
 * Segments rather than a percentage, because people work harder as the end of a set comes
 * into view (the goal-gradient effect) and four boxes make the end visible in a way "75%"
 * does not.
 */
@Composable
fun TodayHeader(quests: List<Quest>, modifier: Modifier = Modifier) {
    val done = Today.doneCount(quests)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Today", color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "$done of ${quests.size}",
                color = if (done == quests.size) Ink.Gold else Ink.Secondary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "$done of ${quests.size} done today" },
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quests.forEach { q ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (q.done) q.accent else Ink.Well)
                        .border(BorderStroke(1.dp, if (q.done) q.accent else Ink.Faint), RoundedCornerShape(50))
                )
            }
        }
        Label(Today.headline(quests))
    }
}

/**
 * One of today's four, as a row: its pixel icon, what it is, when and the smallest version,
 * and a box that ticks itself.
 *
 * The icon is the same sprite the widget draws, and the room shows the same thing — you,
 * the mess, your heart, the letter — so a task, its place in the room and its line on the
 * home screen are visibly one thing. The next one to do gets its colour as a border; done
 * ones stay, dimmed and ticked, because a list that hides what you did tells you nothing
 * about your day.
 */
@Composable
fun QuestRow(
    art: RoomArt,
    quest: Quest,
    next: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = quest.accent
    val scale = pixelScale(2.6f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .panel(
                fill = if (quest.done) Ink.Void else if (next) Ink.SurfaceHigh else Ink.Surface,
                border = when {
                    quest.done -> Ink.Faint
                    next -> accent
                    else -> Ink.Border
                },
                borderWidth = if (next && !quest.done) 2.dp else 1.dp
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { stateDescription = if (quest.done) "Done today" else "To do" }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(Space.radius))
                .background(if (quest.done) Ink.Well else accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            SpriteIcon(art, quest.icon, pixelScale = scale)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = quest.name,
                    color = if (quest.done) Ink.Muted else accent,
                    style = MaterialTheme.typography.labelLarge
                )
                Label(quest.status, modifier = Modifier.weight(1f, fill = false))
            }
            Text(
                text = quest.title,
                color = if (quest.done) Ink.Secondary else Ink.Primary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (!quest.done) {
                Text(
                    text = quest.line,
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (next) Label("Up next", color = accent, strong = true)
                    if (quest.minutes > 0) Label("Smallest: ${quest.minutes} min")
                }
            } else {
                Label("Done today", color = accent, strong = true)
            }
        }
        SpriteIcon(art, if (quest.done) Today.ICON_DONE else Today.ICON_TODO, pixelScale = scale)
    }
}

/**
 * Skin, hair and top, with you drawn above in whatever is picked.
 *
 * People keep going longer with a character they made to look like them (Birk et al., 2016),
 * so this comes on the day you move in rather than in a settings page found later.
 */
@Composable
fun LookPicker(
    art: RoomArt,
    look: Look,
    onChange: (Look) -> Unit,
    modifier: Modifier = Modifier
) {
    val looks = art.catalogue.looks
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .panel(fill = Ink.Room.copy(alpha = 0.14f), border = Ink.Room.copy(alpha = 0.5f))
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            SpriteIcon(art, Scene.meFrame(0), pixelScale = pixelScale(4f), look = look)
        }
        Swatches("Skin", looks.skin, look.skin) { onChange(look.copy(skin = it)) }
        Swatches("Hair", looks.hair, look.hair) { onChange(look.copy(hair = it)) }
        Swatches("Top", looks.top, look.top) { onChange(look.copy(top = it)) }
    }
}

@Composable
private fun Swatches(name: String, ramps: List<List<String>>, picked: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Label(name)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ramps.forEachIndexed { i, ramp ->
                val on = i == picked
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .heightIn(min = 40.dp)
                        .clip(RoundedCornerShape(Space.radius))
                        .background(if (on) Ink.SurfaceHigh else Ink.Surface)
                        .border(
                            BorderStroke(if (on) 2.dp else 1.dp, if (on) Ink.Primary else Ink.Border),
                            RoundedCornerShape(Space.radius)
                        )
                        .clickable(role = Role.RadioButton) { onPick(i) }
                        .semantics {
                            selected = on
                            contentDescription = "$name ${i + 1} of ${ramps.size}"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(Looks.argb(ramp[1])))
                            .border(BorderStroke(2.dp, Color(Looks.argb(ramp[0]))), CircleShape)
                    )
                }
            }
            // Short rows keep the same swatch size as long ones.
            repeat((6 - ramps.size).coerceAtLeast(0)) { Box(modifier = Modifier.weight(1f)) }
        }
    }
}
