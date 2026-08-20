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

   High-affordance dark. The previous identity was dark-on-dark by design — surfaces were
   separated by a few points of luminance and defined by where light fell on them. It looked
   like a painting and read like nothing: you could not tell what was tappable.

   This palette does the opposite. Every layer is a clearly visible step up from the one
   below, borders exist again, and selection states are loud. The rule is simple: if you
   cannot tell at a glance whether something is a button, the design has failed.

   Contrast ratios below are measured against Surface (#181D26) unless stated.              */

object Ink {
    /** App ground. Deep slate rather than pure black, so a raised surface can read as raised. */
    val Void = Color(0xFF0E1116)

    /** Default card. A clear, obvious step up from the ground. */
    val Surface = Color(0xFF181D26)

    /** Raised card — pressed states, nested panels, the thing in front. */
    val SurfaceHigh = Color(0xFF232B38)

    /** Inset wells: progress tracks, counters, anything that should look recessed. */
    val Well = Color(0xFF11151C)

    /** Visible resting border. Not decoration — this is what makes a control look like one.
     *  3.7:1 on Surface, which clears the WCAG 3:1 floor for non-text UI boundaries. */
    val Border = Color(0xFF6B7789)

    /** Border on a focused or selected control. 5.4:1. */
    val BorderStrong = Color(0xFF8593A6)

    /** 15.8:1 on Surface. */
    val Primary = Color(0xFFF5F7FA)

    /** 9.0:1. Body copy. */
    val Secondary = Color(0xFFB4BECD)

    /** 5.4:1, and 4.6:1 on SurfaceHigh. The floor for anything that is text at any size. */
    val Muted = Color(0xFF8593A6)

    /** Structure only — inactive ticks, dividers. Never text. */
    val Faint = Color(0xFF4A5462)

    /** Access. 6.4:1. */
    val Access = Color(0xFF4DA3FF)

    /** Activity. 9.6:1. */
    val Activity = Color(0xFF3DDC97)

    /** Social. 6.2:1. */
    val Social = Color(0xFFFF6B81)

    /** Friction. Amber, and reserved for it — it never marks an error anywhere in the app,
     *  so the colour permanently reads as credit rather than fault. 9.2:1. */
    val Ember = Color(0xFFFFB020)

    /** XP and unlocks. 11.0:1. */
    val Gold = Color(0xFFFFC94D)

    /** Text/icon colour to place on top of a filled accent block. 7.1:1 on the worst
     *  accent (Social), so filled buttons are readable in every pillar colour. */
    val OnAccent = Color(0xFF0B0E13)
}

val Pillar.accent: Color
    get() = when (this) {
        Pillar.ACCESS -> Ink.Access
        Pillar.ACTIVITY -> Ink.Activity
        Pillar.SOCIAL -> Ink.Social
    }

/** The wash behind a selected control in this pillar's colour. */
val Pillar.wash: Color get() = accent.copy(alpha = 0.16f)

/* ------------------------------------------------------------------ type

   Plain sans throughout. The serif was doing the same job as the literary copy — making
   simple instructions feel like literature — and both are gone. Sizes are up across the
   board; the old 10-12sp tracked capitals were unreadable in ordinary light.

   Monospace survives for figures only, where digits have to line up in columns.            */

private val Plain = FontFamily.SansSerif
val NumberFamily = FontFamily.Monospace

val Type = Typography(
    displayLarge = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.Bold,
        fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.8).sp
    ),
    displayMedium = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.Bold,
        fontSize = 30.sp, lineHeight = 37.sp, letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.Bold,
        fontSize = 25.sp, lineHeight = 32.sp, letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 27.sp, letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp, lineHeight = 23.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    // Labels are sentence case at a readable size. Tracked micro-capitals were the single
    // biggest legibility problem in the old design.
    labelLarge = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp, letterSpacing = 0.2.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.Medium,
        fontSize = 13.sp, letterSpacing = 0.2.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Plain, fontWeight = FontWeight.Medium,
        fontSize = 12.sp, letterSpacing = 0.3.sp
    )
)

/* ------------------------------------------------------------------ spacing */

object Space {
    val gutter = 20.dp
    val gap = 10.dp
    val block = 18.dp

    /** Rounded and tactile. Things you can press should look like things you can press. */
    val radius = 14.dp
    val radiusLarge = 20.dp

    /** Minimum touch target. Nothing interactive is allowed below this. */
    val tap = 52.dp
}

/** Centre-aligned variant used on completion and onboarding surfaces. */
val TextStyle.centered: TextStyle get() = copy(textAlign = TextAlign.Center)

@Composable
fun DeBubbleTheme(content: @Composable () -> Unit) {
    val scheme = darkColorScheme(
        primary = Ink.Access,
        onPrimary = Ink.OnAccent,
        secondary = Ink.Activity,
        background = Ink.Void,
        onBackground = Ink.Primary,
        surface = Ink.Surface,
        onSurface = Ink.Primary,
        surfaceVariant = Ink.SurfaceHigh,
        onSurfaceVariant = Ink.Secondary,
        outline = Ink.Border,
        outlineVariant = Ink.Faint,
        error = Ink.Social,
        onError = Ink.OnAccent
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
