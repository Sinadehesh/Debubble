package com.debubble.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Forecasts
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.fill
import kotlin.math.roundToInt
import kotlin.math.floor
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.drawBehind

/**
 * A text box: a white page with a plum outline and plum ink, the same on a card and on a
 * letter — either way it looks like a place to write.
 */
@Composable
fun FieldBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    lines: Int = 1,
    /** Kept for call sites that say so; paper and cards are the same paper now. */
    @Suppress("UNUSED_PARAMETER") onPaper: Boolean = false,
    accent: Color = Ink.Primary,
    max: Int = 280
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= max) onChange(it) },
        modifier = modifier
            .fillMaxWidth()
            .then(if (lines > 1) Modifier.heightIn(min = (lines * 40).dp) else Modifier),
        placeholder = {
            Text(
                text = placeholder,
                color = Ink.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        singleLine = lines == 1,
        shape = Cut.card,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink.Primary,
            unfocusedTextColor = Ink.Primary,
            focusedBorderColor = if (accent == Ink.Primary) Ink.Border else accent,
            unfocusedBorderColor = Ink.Border,
            cursorColor = Ink.Primary,
            focusedContainerColor = Ink.SurfaceHigh,
            unfocusedContainerColor = Ink.SurfaceHigh
        )
    )
}

/**
 * How hard something is, 0 to 10, as a gauge of eleven blocks: tap one, or drag across.
 * Used before (a guess) and after (what it was really), so the two are on the same scale
 * and can be compared. Easier to hit than a thin slider, and it still is one to TalkBack.
 */
@Composable
fun NerveSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Ink.Ember,
    @Suppress("UNUSED_PARAMETER") onPaper: Boolean = false
) {
    val scale = Forecasts.SCALE
    val set by rememberUpdatedState(onChange)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(text = label, color = Ink.Primary, style = MaterialTheme.typography.labelLarge)
            Text(
                text = "$value/$scale · ${Forecasts.label(value)}",
                color = accent,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(38.dp)
                .semantics {
                    contentDescription = label
                    stateDescription = "$value of $scale, ${Forecasts.label(value)}"
                    progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..scale.toFloat(), steps = scale - 1)
                    setProgress { target ->
                        set(target.roundToInt().coerceIn(0, scale))
                        true
                    }
                }
                .pointerInput(scale) {
                    detectTapGestures { p -> set(((p.x / size.width) * (scale + 1)).toInt().coerceIn(0, scale)) }
                }
                .pointerInput(scale) {
                    detectHorizontalDragGestures { change, _ ->
                        set(((change.position.x / size.width) * (scale + 1)).toInt().coerceIn(0, scale))
                    }
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0..scale) {
                val on = i <= value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(if (i == value) 38.dp else 30.dp)
                        .align(Alignment.Bottom)
                        .clip(Cut.small)
                        .background(if (on) accent.fill else Ink.Well)
                        .border(BorderStroke(2.dp, if (on || i == value) Ink.Border else Ink.Faint), Cut.small)
                )
            }
        }
    }
}

/**
 * A number on a slider, the way the room would draw one: a sunken track with a plum edge,
 * filled in its colour up to a block you drag, with − and + blocks either side for the last
 * step. Tap anywhere on the track to jump there. TalkBack sees an ordinary slider.
 */
@Composable
fun PixelSlider(
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    step: Int = 1,
    accent: Color = Ink.Gold,
    description: String = "",
    /** What TalkBack says for the value: "12 km from home". */
    valueText: String = value.toString()
) {
    val set by rememberUpdatedState(onChange)
    val span = (range.last - range.first).coerceAtLeast(1)
    val stride = step.coerceAtLeast(1)
    fun snapValue(v: Float): Int =
        (range.first + ((v - range.first) / stride).roundToInt() * stride).coerceIn(range.first, range.last)
    fun snapFraction(f: Float): Int = snapValue(range.first + f.coerceIn(0f, 1f) * span)
    val fraction = ((value - range.first) / span.toFloat()).coerceIn(0f, 1f)
    val fillColour = accent.fill
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBlock(
            glyph = Glyphs.Minus,
            description = "Less",
            onClick = { set((value - stride).coerceAtLeast(range.first)) },
            enabled = value > range.first,
            size = 40
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .semantics {
                    contentDescription = description
                    stateDescription = valueText
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        value.toFloat(),
                        range.first.toFloat()..range.last.toFloat(),
                        steps = (span / stride - 1).coerceAtLeast(0)
                    )
                    setProgress { target ->
                        set(snapValue(target))
                        true
                    }
                }
                .pointerInput(range, stride) {
                    detectTapGestures { p -> set(snapFraction(p.x / size.width)) }
                }
                .pointerInput(range, stride) {
                    detectHorizontalDragGestures { change, _ -> set(snapFraction(change.position.x / size.width)) }
                }
                .drawBehind {
                    val line = floor(2.dp.toPx()).coerceAtLeast(1f)
                    val thumbW = floor(20.dp.toPx())
                    val track = floor(14.dp.toPx())
                    val top = floor((size.height - track) / 2f)
                    val x = floor((size.width - thumbW) * fraction)
                    // The track, sunk into the page, filled to the middle of the block.
                    drawRect(Ink.Border, topLeft = Offset(0f, top), size = Size(size.width, track))
                    drawRect(Ink.Well, topLeft = Offset(line, top + line), size = Size(size.width - line * 2, track - line * 2))
                    drawRect(fillColour, topLeft = Offset(line, top + line), size = Size(x + thumbW / 2f - line, track - line * 2))
                    // The block: plum lip below, paper face, two grip lines.
                    drawRect(Ink.Border, topLeft = Offset(x, 0f), size = Size(thumbW, size.height))
                    drawRect(Ink.Surface, topLeft = Offset(x + line, line), size = Size(thumbW - line * 2, size.height - line * 4))
                    val grip = floor(thumbW / 2f - line * 1.5f)
                    drawRect(Ink.Border, topLeft = Offset(x + grip, size.height * 0.3f), size = Size(line, size.height * 0.3f))
                    drawRect(Ink.Border, topLeft = Offset(x + grip + line * 2, size.height * 0.3f), size = Size(line, size.height * 0.3f))
                }
        )
        IconBlock(
            glyph = Glyphs.Plus,
            description = "More",
            onClick = { set((value + stride).coerceAtMost(range.last)) },
            enabled = value < range.last,
            size = 40
        )
    }
}
