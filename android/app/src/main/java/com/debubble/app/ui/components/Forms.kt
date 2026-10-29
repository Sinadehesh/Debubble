package com.debubble.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Forecasts
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * A text box. On paper it is a lined white field with plum ink, on the dark ground it is a
 * dark field with cream ink — either way it looks like a place to write.
 */
@Composable
fun FieldBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    lines: Int = 1,
    onPaper: Boolean = false,
    accent: Color = Ink.Access,
    max: Int = 280
) {
    val text = if (onPaper) Ink.PaperText else Ink.Primary
    val ground = if (onPaper) Color.White.copy(alpha = 0.55f) else Ink.Surface
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= max) onChange(it) },
        modifier = modifier
            .fillMaxWidth()
            .then(if (lines > 1) Modifier.heightIn(min = (lines * 40).dp) else Modifier),
        placeholder = {
            Text(
                text = placeholder,
                color = if (onPaper) Ink.PaperMuted else Ink.Muted,
                style = MaterialTheme.typography.bodyMedium
            )
        },
        singleLine = lines == 1,
        shape = RoundedCornerShape(Space.radius),
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = text,
            unfocusedTextColor = text,
            focusedBorderColor = if (onPaper) Ink.PaperText else accent,
            unfocusedBorderColor = if (onPaper) Ink.PaperEdge else Ink.Border,
            cursorColor = if (onPaper) Ink.PaperText else accent,
            focusedContainerColor = ground,
            unfocusedContainerColor = ground
        )
    )
}

/**
 * How hard something is, 0 to 10, with the words for each band. Used before (a guess) and
 * after (what it was really), so the two numbers are on the same scale and can be compared.
 */
@Composable
fun NerveSlider(
    label: String,
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = Ink.Ember,
    onPaper: Boolean = false
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = label,
                color = if (onPaper) Ink.PaperText else Ink.Primary,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = "$value/${Forecasts.SCALE} · ${Forecasts.label(value)}",
                color = if (onPaper) Ink.PaperText else accent,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..Forecasts.SCALE.toFloat(),
            steps = Forecasts.SCALE - 1,
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = if (onPaper) Ink.PaperEdge else Ink.Faint,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}
