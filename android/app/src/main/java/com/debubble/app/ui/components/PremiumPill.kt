package com.debubble.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.debubble.app.ui.theme.Ink

/** The gold tag on anything Premium adds, wherever it appears. */
@Composable
fun PremiumPill(modifier: Modifier = Modifier) = Pill("Premium", Ink.Gold, modifier, filled = true)
