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
   Chiaroscuro. Absolute black rather than a dark grey, because tenebrism needs real shadow
   for the light to mean anything — at tier 1 the screen should feel like a small lit space
   inside a large dark one. Grounds are warm, the colour of aged varnish, and the accents are
   earth pigments rather than neon: lapis, verdigris, madder, candlelight.

   Names are semantic and unchanged from the previous identity, so every screen inherits this
   without being touched. Contrast was measured, not eyeballed; the figures are per token.   */

object Ink {
    /** Absolute. Not #0A0A0A — the shadow has to be genuinely empty. */
    val Void = Color(0xFF000000)

    /** Panel. Warm near-black, like varnish over a dark ground. */
    val Strata = Color(0xFF0B0907)
    val Ridge = Color(0xFF141009)
    val Raise = Color(0xFF1C160E)
    val Edge = Color(0xFF241D14)
    val EdgeSoft = Color(0xFF17120C)

    /** Warm parchment rather than white. 16.6:1 on the panel ground. */
    val Primary = Color(0xFFF2EAD9)

    /** 9.6:1 on the panel. */
    val Ash = Color(0xFFC3B295)

    /** The floor for anything that is text. 5.9:1 on the panel, 6.3:1 on the void. */
    val Dim = Color(0xFF9C8A6E)

    /** Structure only — tracks, rules, inactive marks. Never text: it does not clear 4.5. */
    val Faint = Color(0xFF544838)

    /** Lapis. 5.3:1 on the panel. */
    val Access = Color(0xFF5E82CC)

    /** Verdigris. 9.3:1. */
    val Activity = Color(0xFFA8BA5C)

    /** Madder. 4.6:1 — chosen over a deeper red specifically so the small pillar labels
     *  that use it still clear the small-text threshold. */
    val Social = Color(0xFFCC4E62)

    /** Candlelight, and reserved for Friction alone. It never marks a warning or an error
     *  anywhere in the app, so the colour permanently reads as credit rather than fault. */
    val Ember = Color(0xFFE0A94A)
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
   A serif carries the app now — the weight of a printed page rather than a dashboard.
   Directives are set large and quietly, at regular weight: an instruction in a book does
   not shout, and a heavy grotesque at 30sp was doing exactly that.

   Monospace survives in one place only, for figures that have to align in columns. If it is
   a measurement it is monospace; everything else is set.                                   */

private val Bookish = FontFamily.Serif
val InstrumentFamily = FontFamily.Monospace

val Type = Typography(
    // Directives — the thing to go and do.
    displayLarge = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Normal,
        fontSize = 42.sp, lineHeight = 48.sp, letterSpacing = (-0.6).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Normal,
        fontSize = 31.sp, lineHeight = 37.sp, letterSpacing = (-0.4).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Normal,
        fontSize = 27.sp, lineHeight = 33.sp, letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Medium,
        fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp
    ),
    // Body.
    bodyLarge = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Normal,
        fontSize = 14.sp, lineHeight = 21.sp
    ),
    // Labels: set in the serif too, uppercase and widely tracked, so they read as engraving
    // rather than as UI chrome. Compose has no reliable small-caps, and tracked capitals are
    // the honest substitute.
    labelLarge = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, letterSpacing = 2.6.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Normal,
        fontSize = 11.sp, letterSpacing = 2.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Bookish, fontWeight = FontWeight.Normal,
        fontSize = 10.sp, letterSpacing = 1.7.sp
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
    // No light scheme and no dynamic colour. Chiaroscuro is not a preference — the whole
    // design depends on the shadow being real, so the system theme is not consulted.
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
