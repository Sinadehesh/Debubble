package com.debubble.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.NumberFamily
import com.debubble.app.ui.theme.Space

/* ==================================================================== surfaces

   Everything you can touch looks like it. A card has a fill and a visible border; a selected
   card has an accent-tinted fill, a two-pixel accent border and a checkmark. There is no
   state in this app you have to infer from a four-percent change in brightness.            */

/** A plain panel. Filled and outlined, so it is unambiguously an object on the page. */
fun Modifier.panel(
    shape: Shape = RoundedCornerShape(Space.radius),
    fill: Color = Ink.Surface,
    border: Color = Ink.Border,
    borderWidth: androidx.compose.ui.unit.Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(fill)
    .border(BorderStroke(borderWidth, border), shape)

/**
 * A panel whose selected state is impossible to miss: accent fill, accent border at double
 * width, and (wherever it is used) a checkmark supplied by the caller.
 */
fun Modifier.selectablePanel(
    selected: Boolean,
    accent: Color,
    shape: Shape = RoundedCornerShape(Space.radius)
): Modifier = this.panel(
    shape = shape,
    fill = if (selected) accent.copy(alpha = 0.16f) else Ink.Surface,
    border = if (selected) accent else Ink.Border,
    borderWidth = if (selected) 2.dp else 1.dp
)

/** An inset well — progress tracks, counters, anything that should read as recessed. */
fun Modifier.well(shape: Shape = RoundedCornerShape(Space.radius)): Modifier = this
    .clip(shape)
    .background(Ink.Well)

/* ==================================================================== text */

/**
 * A small label. Sentence case at 13sp, not tracked capitals at 10 — the old labels were the
 * single least readable thing in the app and every screen was built out of them.
 */
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

/** A section heading with an optional value on the right. */
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
            color = accent,
            style = MaterialTheme.typography.titleMedium
        )
        if (trailing != null) Label(trailing)
    }
}

/** A number set in the monospace face, for figures that sit in columns. */
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
 * The main action on a screen. Filled with the accent colour at full strength — a button
 * that needs a border to be visible is not a primary button.
 */
@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = Ink.Access,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap),
        shape = RoundedCornerShape(Space.radius),
        colors = ButtonDefaults.buttonColors(
            containerColor = accent,
            contentColor = Ink.OnAccent,
            disabledContainerColor = Ink.SurfaceHigh,
            disabledContentColor = Ink.Muted
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

/** Secondary action: outlined, same size and weight of presence, lower commitment. */
@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    colour: Color = Ink.Primary,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .panel(border = Ink.Border)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colour,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

/** The quiet exit. Still a full-height target — quiet is about weight, not about size. */
@Composable
fun TextAction(
    text: String,
    modifier: Modifier = Modifier,
    colour: Color = Ink.Muted,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = Space.tap),
        shape = RoundedCornerShape(Space.radius)
    ) {
        Text(text = text, color = colour, style = MaterialTheme.typography.labelMedium)
    }
}

/* ==================================================================== navigation */

/**
 * The standard top bar. Every screen that is not the dashboard gets one, with a real back
 * arrow at a real touch size — the old design expected people to find an unlabelled word at
 * the bottom of a scroll.
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(Space.tap)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = onBack)
                    .semantics { contentDescription = "Go back" },
                contentAlignment = Alignment.Center
            ) {
                Glyph(Glyphs.ArrowLeft, colour = Ink.Primary, size = 22)
            }
        } else {
            Spacer(modifier = Modifier.width(4.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = accent,
                style = MaterialTheme.typography.titleMedium
            )
            if (subtitle != null) Label(subtitle)
        }
        action?.invoke()
    }
}

/* ==================================================================== controls */

/**
 * One multi-select option: a full-width row with a checkbox that is visibly ticked, a label,
 * and a plain sentence explaining what picking it means.
 */
@Composable
fun SelectRow(
    label: String,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    detail: String? = null,
    accent: Color = Ink.Access,
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
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                )
            )
            if (detail != null) {
                Text(
                    text = detail,
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** The tick itself. Filled and checked when on, hollow and outlined when off. */
@Composable
fun CheckBox(
    selected: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
    round: Boolean = false,
    size: Int = 24
) {
    val shape = if (round) CircleShape else RoundedCornerShape(7.dp)
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(shape)
            .background(if (selected) accent else Color.Transparent)
            .border(
                BorderStroke(2.dp, if (selected) accent else Ink.Border),
                shape
            )
            // The row already announces its own selected state; a second announcement here
            // just makes TalkBack say "selected" twice.
            .clearAndSetSemantics { },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            if (round) {
                Box(
                    modifier = Modifier
                        .size((size * 0.42f).dp)
                        .clip(CircleShape)
                        .background(Ink.OnAccent)
                )
            } else {
                Glyph(Glyphs.Check, colour = Ink.OnAccent, size = (size * 0.62f).toInt())
            }
        }
    }
}

/**
 * A round icon button — the +/- on a counter, and anything else that needs to be tappable
 * without carrying a word.
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
) {
    val live = enabled
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(
                when {
                    !live -> Color.Transparent
                    filled -> accent
                    else -> accent.copy(alpha = 0.14f)
                }
            )
            .border(
                BorderStroke(if (filled) 0.dp else 1.5.dp, if (live) accent else Ink.Faint),
                CircleShape
            )
            .then(
                if (live) Modifier.clickable(role = Role.Button, onClick = onClick)
                else Modifier
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Glyph(
            glyph,
            colour = when {
                !live -> Ink.Faint
                filled -> Ink.OnAccent
                else -> accent
            },
            size = (size * 0.42f).toInt()
        )
    }
}

/* ==================================================================== progress */

/**
 * A progress bar with a visible track. The track is a well rather than a faint tint, so an
 * empty bar still shows how far there is to go.
 */
@Composable
fun ProgressTrack(
    fraction: Float,
    accent: Color,
    modifier: Modifier = Modifier,
    height: Int = 10
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .well(RoundedCornerShape(50))
            .border(BorderStroke(1.dp, Ink.Faint), RoundedCornerShape(50))
    ) {
        if (fraction > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(height.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
        }
    }
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
        ProgressTrack(fraction = fraction, accent = accent, height = 8)
    }
}

/** A labelled statistic. */
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
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

@Composable
fun Dot(colour: Color, size: Int = 8) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(colour)
    )
}

/** A small filled pill carrying one word of status. */
@Composable
fun Pill(
    text: String,
    colour: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) colour else colour.copy(alpha = 0.16f))
            .border(BorderStroke(1.dp, colour.copy(alpha = if (filled) 0f else 0.5f)), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = if (filled) Ink.OnAccent else colour,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}
