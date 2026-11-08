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
