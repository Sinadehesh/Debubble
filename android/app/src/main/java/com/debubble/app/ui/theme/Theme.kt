package com.debubble.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.debubble.app.engine.Pillar

/* ------------------------------------------------------------------ palette
   Single-theme by intent. The Bubble Map is additive light, and additive light only
   reads as energy on a black ground, so there is no light variant to design.
   Neutrals are biased cool, toward the Access axis, rather than being pure grey.     */

object Ink {
    val Void = Color(0xFF06070A)
    val Strata = Color(0xFF0E1116)
    val Ridge = Color(0xFF141821)
    val Raise = Color(0xFF1A1F2A)
    val Edge = Color(0xFF232A38)
    val EdgeSoft = Color(0xFF181D27)

    val Primary = Color(0xFFE9EEF6)
    val Ash = Color(0xFF8792A6)
    val Dim = Color(0xFF525C6E)
    val Faint = Color(0xFF333B49)

    val Access = Color(0xFF2F6BFF)
    val Activity = Color(0xFFB4FF25)
    val Social = Color(0xFFFF2D55)

    /** Reserved for Friction alone. It never marks a warning or an error anywhere in the
     *  app, so the colour permanently reads as credit rather than fault. */
    val Ember = Color(0xFFFF8A1F)
}

val Pillar.accent: Color
    get() = when (this) {
        Pillar.ACCESS -> Ink.Access
        Pillar.ACTIVITY -> Ink.Activity
        Pillar.SOCIAL -> Ink.Social
    }

val Pillar.tint: Color
    get() = accent.copy(alpha = 0.13f)

val Pillar.line: Color
    get() = accent.copy(alpha = 0.32f)

/* ------------------------------------------------------------------ type
   Two roles. A heavy grotesque with tight tracking for anything the user must *do*,
   and a wide-tracked monospace for everything the system *reports*. If it is a
   measurement, it is monospace.                                                     */

private val Grotesque = FontFamily.SansSerif
val InstrumentFamily = FontFamily.Monospace

val Type = Typography(
    // Directives — the thing to go and do.
    displayLarge = TextStyle(
        fontFamily = Grotesque, fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp, lineHeight = 42.sp, letterSpacing = (-1.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Grotesque, fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp, lineHeight = 33.sp, letterSpacing = (-1.2).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Grotesque, fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp, lineHeight = 29.sp, letterSpacing = (-1.0).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Grotesque, fontWeight = FontWeight.Bold,
        fontSize = 22.sp, lineHeight = 26.sp, letterSpacing = (-0.7).sp
    ),
    // Body.
    bodyLarge = TextStyle(
        fontFamily = Grotesque, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 21.sp, letterSpacing = (-0.1).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Grotesque, fontWeight = FontWeight.Normal,
        fontSize = 13.5f.sp, lineHeight = 19.sp
    ),
    // Instrument readouts.
    labelLarge = TextStyle(
        fontFamily = InstrumentFamily, fontWeight = FontWeight.Bold,
        fontSize = 11.sp, letterSpacing = 2.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InstrumentFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 10.sp, letterSpacing = 1.8.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InstrumentFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 9.sp, letterSpacing = 1.4.sp
    )
)

/* ------------------------------------------------------------------ spacing */

object Space {
    val gutter = 22.dp
    val gap = 9.dp
    val block = 20.dp
    val radius = 12.dp
    val radiusLarge = 14.dp
}

/** Centre-aligned variant used on the completion screen. */
val TextStyle.centered: TextStyle get() = copy(textAlign = TextAlign.Center)

@Composable
fun DeBubbleTheme(content: @Composable () -> Unit) {
    // No light scheme and no dynamic colour: the app commits to one instrument-panel look,
    // so the system theme setting is intentionally not consulted.
    val scheme = darkColorScheme(
        primary = Ink.Primary,
        onPrimary = Ink.Void,
        secondary = Ink.Access,
        background = Ink.Void,
        onBackground = Ink.Primary,
        surface = Ink.Strata,
        onSurface = Ink.Primary,
        surfaceVariant = Ink.Ridge,
        onSurfaceVariant = Ink.Ash,
        outline = Ink.Edge,
        outlineVariant = Ink.EdgeSoft,
        error = Ink.Social,
        onError = Ink.Primary
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(colorScheme = scheme, typography = Type, content = content)
}
