package com.debubble.app.ui.screens

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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.AvatarState
import com.debubble.app.engine.Cosmetic
import com.debubble.app.engine.Progress
import com.debubble.app.ui.components.Avatar
import com.debubble.app.ui.components.CheckBox
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.avatarColour
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * Where you dress the avatar.
 *
 * Two currencies are shown side by side on purpose. Casual wear comes from XP, which comes
 * from showing up. Armour comes only from Friction, which comes from being turned down or
 * turning back — it cannot be bought with completions at any level. Someone who never risks
 * anything can reach level 12 in a nice jacket with no plate on them at all, and the screen
 * should make that legible.
 */
@Composable
fun AvatarScreen(
    avatar: AvatarState,
    xp: Int,
    friction: Int,
    onChange: (AvatarState) -> Unit,
    onBack: () -> Unit
) {
    val level = Progress.level(xp)
    val plates = Progress.plates(friction)
    val toNext = Progress.toNextPlate(friction)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = "Your avatar",
            subtitle = "Level $level",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(shape = RoundedCornerShape(Space.radiusLarge))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Avatar(avatar = avatar, friction = friction, size = 200)
            }

            VSpace(14)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Space.gap)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .panel(fill = Ink.SurfaceHigh)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Glyph(Glyphs.Spark, colour = Ink.Gold, size = 17)
                        Label("Clothes", color = Ink.Gold, strong = true)
                    }
                    Text(
                        text = "Level $level",
                        color = Ink.Primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    ProgressTrack(Progress.levelProgress(xp), Ink.Gold, height = 6)
                    Label("${Progress.xpIntoLevel(xp)} / ${Progress.XP_PER_LEVEL} XP")
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .panel(fill = Ink.SurfaceHigh, border = Ink.Ember.copy(alpha = 0.6f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Glyph(Glyphs.Spark, colour = Ink.Ember, size = 17)
                        Label("Armour", color = Ink.Ember, strong = true)
                    }
                    Text(
                        text = "$plates of ${Progress.MAX_PLATES}",
                        color = Ink.Primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    ProgressTrack(
                        fraction = plates / Progress.MAX_PLATES.toFloat(),
                        accent = Ink.Ember,
                        height = 6
                    )
                    Label(
                        if (toNext == null) "Every plate earned"
                        else "$toNext more friction for the next"
                    )
                }
            }

            VSpace(12)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(fill = Ink.SurfaceHigh)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Glyph(Glyphs.Lock, colour = Ink.Muted, size = 18)
                Text(
                    text = "Clothes come from finishing challenges. Armour only comes from " +
                        "Friction — getting turned down, or turning back. You cannot buy a " +
                        "plate by being careful.",
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            VSpace(24)

            ColourPicker(
                selected = avatar.body,
                level = level,
                onPick = { onChange(avatar.copy(body = it)) }
            )

            VSpace(22)
            Picker("Shape", Progress.shapes, avatar.shape, level) {
                onChange(avatar.copy(shape = it))
            }

            VSpace(22)
            Picker("Hat", Progress.hats, avatar.hat, level) {
                onChange(avatar.copy(hat = it))
            }

            VSpace(22)
            Picker("Top", Progress.shirts, avatar.shirt, level) {
                onChange(avatar.copy(shirt = it))
            }

            VSpace(22)
            Picker("Background", Progress.backdrops, avatar.backdrop, level) {
                onChange(avatar.copy(backdrop = it))
            }

            VSpace(28)
        }
    }
}

@Composable
private fun ColourPicker(selected: Int, level: Int, onPick: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader("Colour")
        // Six full-size targets do not fit across a 360dp screen, and shrinking them below
        // 48dp is not an option, so the row scrolls instead.
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Progress.bodies.forEach { item ->
                val unlocked = Progress.unlocked(item, level)
                val on = selected == item.id
                Box(
                    modifier = Modifier
                        .size(Space.tap)
                        .clip(CircleShape)
                        .background(
                            if (unlocked) avatarColour(item.id) else Ink.SurfaceHigh
                        )
                        .then(
                            if (unlocked) Modifier.clickable(
                                role = Role.RadioButton,
                                onClick = { onPick(item.id) }
                            ) else Modifier
                        )
                        .semantics {
                            contentDescription =
                                if (unlocked) item.name
                                else "${item.name}, locked until level ${item.level}"
                            stateDescription = if (on) "Selected" else "Not selected"
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !unlocked -> Glyph(Glyphs.Lock, colour = Ink.Muted, size = 18)
                        on -> Glyph(Glyphs.Check, colour = Ink.OnAccent, size = 22, weight = 3f)
                    }
                }
            }
        }
    }
}

/**
 * A list of one cosmetic category. Locked rows stay visible and say what unlocks them, which
 * is the point — an empty wardrobe with things you can see coming beats a short one.
 */
@Composable
private fun Picker(
    title: String,
    items: List<Cosmetic>,
    selected: Int,
    level: Int,
    onPick: (Int) -> Unit
) {
    val have = items.count { Progress.unlocked(it, level) }
    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
        SectionHeader(title, trailing = "$have of ${items.size}")
        items.forEach { item ->
            val unlocked = Progress.unlocked(item, level)
            val on = selected == item.id
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(
                        fill = if (unlocked) Ink.Surface else Ink.Void,
                        border = when {
                            on -> Ink.Gold
                            unlocked -> Ink.Border
                            else -> Ink.Faint
                        },
                        borderWidth = if (on) 2.dp else 1.dp
                    )
                    .then(
                        if (unlocked) Modifier.clickable(
                            role = Role.RadioButton,
                            onClick = { onPick(item.id) }
                        ) else Modifier
                    )
                    .semantics {
                        stateDescription = when {
                            !unlocked -> "Locked until level ${item.level}"
                            on -> "Selected"
                            else -> "Not selected"
                        }
                    }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (unlocked) {
                    CheckBox(selected = on, accent = Ink.Gold, round = true)
                } else {
                    Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                        Glyph(Glyphs.Lock, colour = Ink.Muted, size = 18)
                    }
                }
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    color = if (unlocked) Ink.Primary else Ink.Muted,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
                    )
                )
                if (!unlocked) {
                    LevelTag(item.level)
                }
            }
        }
    }
}

@Composable
private fun LevelTag(level: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Ink.SurfaceHigh)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = "Level $level",
            color = Ink.Muted,
            style = MaterialTheme.typography.labelSmall
        )
    }
}
