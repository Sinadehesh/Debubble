package com.debubble.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.ButtonText
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.NumberFamily
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.fill
import com.debubble.app.ui.theme.onFill
import com.debubble.app.ui.theme.wash
import kotlin.math.roundToInt

/* ==================================================================== surfaces

   Everything is drawn the way the room is drawn: a fill, a plum outline, corners stepped in
   whole pixels, and a hard shadow with no blur. A card stands off the wall and casts the
   wall's own shade; a button stands off the page on a plum lip and sinks onto it when it is
   pressed. Something flat, on the ground colour, with a faint edge, is done or passive.     */

/**
 * A card. Paper, outlined, standing a few pixels off the wall.
 *
 * It lies flat when it is on the ground colour or has only a faint edge: that is a thing that
 * is done, or only there to be read, and a shadow under it would say it can be picked up.
 */
fun Modifier.panel(
    shape: Shape = Cut.card,
    fill: Color = Ink.Surface,
    border: Color = Ink.Border,
    borderWidth: Dp = 2.dp,
    lift: Dp = 3.dp,
    shadow: Color = Ink.Shadow
): Modifier {
    val flat = fill == Ink.Void || border == Ink.Faint || border.alpha == 0f
    return this
        .then(if (flat || lift == 0.dp) Modifier else Modifier.castShadow(shape, lift, shadow))
        .clip(shape)
        .background(fill)
        .border(BorderStroke(borderWidth, border), shape)
}

/** The shadow alone, below and behind: the same shape, moved down, in one solid colour. */
fun Modifier.castShadow(shape: Shape = Cut.card, lift: Dp = 3.dp, colour: Color = Ink.Shadow): Modifier =
    this.drawBehind {
        val outline = shape.createOutline(size, layoutDirection, this)
        translate(top = lift.toPx().roundToInt().toFloat()) { drawOutline(outline, colour) }
    }

/**
 * A choice that is impossible to miss when chosen: its colour's wash behind it, a heavier
 * outline, and (wherever it is used) a tick supplied by the caller.
 */
fun Modifier.selectablePanel(
    selected: Boolean,
    accent: Color,
    shape: Shape = Cut.card
): Modifier = this.panel(
    shape = shape,
    fill = if (selected) accent.wash else Ink.Surface,
    border = Ink.Border,
    borderWidth = if (selected) 3.dp else 2.dp
)

/** An inset well — tracks, counters, anything that should read as pressed into the page. */
fun Modifier.well(shape: Shape = Cut.small): Modifier = this
    .clip(shape)
    .background(Ink.Well)

/**
 * The room's wallpaper: the wall's cream with its little diamonds, two dp to a pixel. Every
 * screen stands on it, so the app is the inside of the room you are looking after.
 */
fun Modifier.wallpaper(): Modifier = this.drawWithCache {
    val px = (2.dp.toPx()).roundToInt().coerceAtLeast(1)
    // One tile: two rows of diamonds, the second offset by half, as room.py draws the wall.
    val tile = ImageBitmap(8 * px, 12 * px)
    val canvas = androidx.compose.ui.graphics.Canvas(tile)
    canvas.drawRect(0f, 0f, tile.width.toFloat(), tile.height.toFloat(), Paint().apply { color = Ink.Void })
    val dot = Paint().apply { color = Ink.Wallpaper }
    fun diamond(cx: Int, cy: Int) {
        listOf(cx to cy - 1, cx - 1 to cy, cx to cy, cx + 1 to cy, cx to cy + 1).forEach { (x, y) ->
            canvas.drawRect(
                (x * px).toFloat(), (y * px).toFloat(), ((x + 1) * px).toFloat(), ((y + 1) * px).toFloat(), dot
            )
        }
    }
    diamond(2, 2)
    diamond(6, 8)
    val brush = ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated))
    onDrawBehind { drawRect(brush) }
}

/* ==================================================================== text */

/** A small label. Sentence case, rounded and bold, never tracked capitals. */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink.Muted,
    strong: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = if (strong) MaterialTheme.typography.labelLarge
        else MaterialTheme.typography.labelMedium
    )
}

/** A section heading, in the pixel face, with an optional value on the right. */
@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null,
    accent: Color = Ink.Primary
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f, fill = false).semantics { heading() },
            color = accent,
            style = MaterialTheme.typography.titleLarge
        )
        if (trailing != null) Label(trailing)
    }
}

/** A number, in the pixel face, as though the room counted it. */
@Composable
fun Figure(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink.Primary,
    large: Boolean = false
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        style = (if (large) MaterialTheme.typography.headlineLarge
        else MaterialTheme.typography.headlineMedium).copy(fontFamily = NumberFamily)
    )
}

fun tierCode(tier: Int): String = tier.toString().padStart(3, '0')

/* ==================================================================== buttons */

/**
 * The button, in every size and colour the app has: a stepped block on a plum lip, with its
 * word in the pixel face. Pressing sinks it onto the lip, the way a key goes down. No ripple:
 * the movement is the feedback, and haptics come from whoever handles the click.
 */
@Composable
fun BlockButton(
    text: String,
    modifier: Modifier = Modifier,
    fill: Color = Ink.Primary,
    ink: Color = fill.onFill,
    enabled: Boolean = true,
    small: Boolean = false,
    onClick: () -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val lift = if (small) 3.dp else Space.lift
    val sink = if (pressed && enabled) lift else 0.dp
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = lift)
            .heightIn(min = if (small) 44.dp else Space.tap)
            .offset(y = sink)
            .then(if (enabled && sink == 0.dp) Modifier.castShadow(Cut.card, lift, Ink.Border) else Modifier)
            .clip(Cut.card)
            .background(if (enabled) fill else Ink.Well)
            .border(BorderStroke(2.dp, if (enabled) Ink.Border else Ink.Faint), Cut.card)
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = if (small) 6.dp else 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (enabled) ink else Ink.Muted,
            style = if (small) MaterialTheme.typography.labelLarge else ButtonText,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The main action on a screen. Plum, always, whatever the screen is about: the colours are
 * for what things are (a road, a domain, a spark), never for which button to press, so the
 * one to press always looks the same. A colour is passed only where the action belongs to
 * one of those things, and then the button is filled with it.
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = Ink.Primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) = BlockButton(text, modifier, fill = accent.fill, enabled = enabled, onClick = onClick)

/** The other choice: paper on the same lip, the same size, a smaller commitment. */
@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    colour: Color = Ink.Primary,
    onClick: () -> Unit
) = BlockButton(text, modifier, fill = Ink.Surface, ink = colour, onClick = onClick)

/** The quiet exit. Still a full-height target — quiet is about weight, not about size. */
@Composable
fun TextAction(
    text: String,
    modifier: Modifier = Modifier,
    colour: Color = Ink.Muted,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .clip(Cut.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = colour, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
}

/**
 * A small square button with a pixel glyph on it: back, plus, minus. The same block as every
 * other button, at icon size.
 */
@Composable
fun IconBlock(
    glyph: Glyphs,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fill: Color = Ink.Surface,
    enabled: Boolean = true,
    size: Int = 44
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val sink = if (pressed && enabled) 3.dp else 0.dp
    Box(
        modifier = modifier
            .padding(bottom = 3.dp)
            .size(size.dp)
            .offset(y = sink)
            .then(if (enabled && sink == 0.dp) Modifier.castShadow(Cut.small, 3.dp, Ink.Border) else Modifier)
            .clip(Cut.small)
            .background(if (enabled) fill else Ink.Well)
            .border(BorderStroke(2.dp, if (enabled) Ink.Border else Ink.Faint), Cut.small)
            .clickable(interactionSource = source, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Glyph(
            glyph,
            colour = when {
                !enabled -> Ink.Muted
                fill == Ink.Primary -> Ink.Cream
                else -> Ink.Primary
            },
            size = (size * 0.46f).toInt()
        )
    }
}

/* ==================================================================== navigation */

/**
 * The top of every screen that is not a tab: a back block, the title in the pixel face, and
 * a line under it. The same on every one of them, so there is one way back out of anything.
 */
@Composable
fun TopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    accent: Color = Ink.Primary,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (onBack != null) {
            IconBlock(Glyphs.ArrowLeft, description = "Go back", onClick = onBack)
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                color = if (accent == Ink.Primary) Ink.Primary else accent,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2
            )
            if (subtitle != null) Label(subtitle)
        }
        action?.invoke()
    }
}

/* ==================================================================== controls */

/**
 * One option: a full-width card with a tick that is visibly ticked, a label, and a plain
 * sentence explaining what picking it means.
 */
@Composable
fun SelectRow(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    accent: Color = Ink.Gold,
    single: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .selectablePanel(selected, accent)
            .clickable(
                role = if (single) Role.RadioButton else Role.Checkbox,
                onClick = onToggle
            )
            .semantics { stateDescription = if (selected) "Selected" else "Not selected" }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CheckBox(selected = selected, accent = accent, round = single)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                color = Ink.Primary,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                )
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * The tick: a pixel box, filled in its colour and ticked when on, empty paper when off. A
 * single choice gets a pixel dot instead of a tick, so the two kinds still read differently.
 */
@Composable
fun CheckBox(
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    round: Boolean = false,
    size: Int = 26
) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(Cut.small)
            .background(if (selected) accent.fill else Ink.Surface)
            .border(BorderStroke(2.dp, Ink.Border), Cut.small)
            // The row already announces its own selected state; a second announcement here
            // just makes TalkBack say "selected" twice.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            if (round) {
                Box(modifier = Modifier.size((size * 0.38f).dp).background(Ink.Primary, RectangleShape))
            } else {
                Glyph(Glyphs.Check, colour = Ink.Primary, size = (size * 0.7f).toInt())
            }
        }
    }
}

/** An on/off: a pixel track with a block that slides across, filled in its colour when on. */
@Composable
fun PixelSwitch(checked: Boolean, modifier: Modifier = Modifier, accent: Color = Ink.Gold) {
    Box(
        modifier = modifier
            .size(width = 54.dp, height = 30.dp)
            .clip(Cut.small)
            .background(if (checked) accent.fill else Ink.Well)
            .border(BorderStroke(2.dp, Ink.Border), Cut.small)
            .clearAndSetSemantics { }
            .padding(4.dp),
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(Cut.small)
                .background(Ink.Surface)
                .border(BorderStroke(2.dp, Ink.Border), Cut.small)
        )
    }
}

/**
 * A square button with a glyph: the +/- on a counter, and anything else that needs to be
 * tappable without carrying a word. The same block as [IconBlock], filled in its colour
 * when it is the one to press.
 */
@Composable
fun RoundButton(
    glyph: Glyphs,
    description: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    size: Int = 44
) = IconBlock(
    glyph = glyph,
    description = description,
    onClick = onClick,
    modifier = modifier,
    fill = if (filled) accent.fill else Ink.Surface,
    enabled = enabled,
    size = size
)

/* ==================================================================== progress */

/**
 * A progress bar the way a game draws one: a recessed track with a plum outline, filled in
 * its colour, with a lighter line along the top of the fill so it reads as a solid thing.
 */
@Composable
fun ProgressTrack(
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Int = 12
) {
    val h = height.coerceAtLeast(10)
    val f = fraction.coerceIn(0f, 1f)
    val fillColour = accent.fill
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(h.dp)
            .clip(Cut.small)
            .background(Ink.Well)
            .drawBehind {
                if (f > 0f) {
                    val inset = 2.dp.toPx()
                    val w = (size.width - inset * 2) * f
                    drawRect(fillColour, topLeft = Offset(inset, inset), size = Size(w, size.height - inset * 2))
                    drawRect(
                        Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(inset, inset),
                        size = Size(w, ((size.height - inset * 2) / 3f).coerceAtLeast(1f))
                    )
                }
            }
            .border(BorderStroke(2.dp, Ink.Border), Cut.small)
    )
}

/** A labelled bar with its own caption and figure. */
@Composable
fun LabelledProgress(
    label: String,
    value: String,
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Label(label, color = accent, strong = true)
            Label(value)
        }
        ProgressTrack(fraction = fraction, accent = accent)
    }
}

/** A labelled number on a card. */
@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = Ink.Primary
) {
    Column(
        modifier = modifier
            .panel()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Label(label)
        Figure(value, color = accent)
    }
}

/* ==================================================================== atoms */

@Composable
fun VSpace(height: Int) = Spacer(modifier = Modifier.height(height.dp))

@Composable
fun HSpace(width: Int) = Spacer(modifier = Modifier.width(width.dp))

/** A pixel dot: square, as everything this small is in the room. */
@Composable
fun Dot(colour: Color, size: Int = 8) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .background(colour, RectangleShape)
    )
}

/** A small tag carrying one or two words: its colour's wash, its ink, a thin outline. */
@Composable
fun Pill(
    text: String,
    colour: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(Cut.small)
            .background(if (filled) colour.fill else colour.wash)
            .border(BorderStroke(1.5.dp, Ink.Border), Cut.small)
            .padding(horizontal = 9.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            color = if (filled) colour.onFill else colour,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold)
        )
    }
}

/** A row of ticks: done ones filled, the rest recessed. A day's four, a week, a set. */
@Composable
fun Ticks(total: Int, done: Int, accent: Color, modifier: Modifier = Modifier, height: Int = 12) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(total) { i ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(height.dp)
                    .clip(Cut.small)
                    .background(if (i < done) accent.fill else Ink.Well)
                    .border(BorderStroke(2.dp, if (i < done) Ink.Border else Ink.Faint), Cut.small)
            )
        }
    }
}

/** A tall thin edge in a colour, for marking which road a row belongs to. */
@Composable
fun EdgeMark(colour: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier.width(6.dp).fillMaxHeight().background(colour.fill))
}
