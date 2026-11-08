package com.debubble.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.debubble.app.R
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Quest
import com.debubble.app.engine.Stage

/* ------------------------------------------------------------------ palette

   One world. The app used to be two: a sunny pixel room, and around it a dark dashboard in
   a palette of its own, with neon blue, green and pink for the three ladders. Every screen
   was a cosy picture pasted onto that dashboard, and that is what made it feel like two apps
   sewn together.

   Now everything is drawn in the room's own colours (android/tools/pixel/palette.py). The
   ground is the room's wall cream, every card is its paper, every outline is its plum, and
   each colour is one of its ramps: a road, a domain or a reward is the colour of its own
   sprite. A colour has two jobs and two values for them: the light one fills things, the way
   sprites are filled, with plum on top; the dark one is for text and lines, and every dark
   value clears 4.5:1 on the ground, on paper and in a well.

   Contrast against the ground (#FFF3DC) / paper (#FFFDF5) / well (#F3DFBB).               */

object Ink {
    /** The room's wall: every screen stands on it. */
    val Void = Color(0xFFFFF3DC)

    /** Paper: every card. */
    val Surface = Color(0xFFFFFDF5)

    /** A card on a card, an option in a sheet, the one in front. */
    val SurfaceHigh = Color(0xFFFFFFFF)

    /** Recessed: tracks, counters, wells. The wall's own shade. */
    val Well = Color(0xFFF3DFBB)

    /** The hard shadow under a card, as a sprite would cast it: no blur. */
    val Shadow = Color(0xFFD7BE95)

    /** The little diamonds on the wall. */
    val Wallpaper = Color(0xFFF6E4C2)

    /** Outlines. The sprites' plum, so a card is drawn in the same line as the room. */
    val Border = Color(0xFF33243A)

    /** A focused or chosen control. */
    val BorderStrong = Color(0xFF33243A)

    /** Structure only — inactive ticks, dividers, a passive edge. Never text. */
    val Faint = Color(0xFFD7BE95)

    /** 13.1 / 14.2 / 11.1. */
    val Primary = Color(0xFF33243A)

    /** 7.7 / 8.3 / 6.5. Body copy. */
    val Secondary = Color(0xFF5A4660)

    /** 5.8 / 6.2 / 4.9. The floor for text at any size. */
    val Muted = Color(0xFF6B5A6E)

    /** Somewhere new: sky, the colour of its sign. 5.7 / 6.2 / 4.8. */
    val Access = Color(0xFF28658F)

    /** Something new: leaf. 5.6 / 6.1 / 4.7. */
    val Activity = Color(0xFF2C6E37)

    /** Someone new: clay, warm and apart from the heart's blush. 5.8 / 6.3 / 4.9. */
    val Social = Color(0xFF9C4428)

    /** Sparks of courage: amber, and only ever credit. 5.5 / 5.9 / 4.6. */
    val Ember = Color(0xFF9A4E16)

    /** Levels, unlocks and the reminder bell: the lamp's glow. 5.3 / 5.8 / 4.5. */
    val Gold = Color(0xFF8C5A0E)

    /** Text on any filled colour. */
    val OnAccent = Color(0xFF33243A)

    /** Text on plum. */
    val Cream = Color(0xFFFFF3DC)

    /* The three care domains, each its sprite's colour: body teal, room honey, heart blush. */

    /** Body. 5.9 / 6.3 / 4.9. */
    val Body = Color(0xFF2A676C)

    /** Room. 5.7 / 6.1 / 4.8. */
    val Room = Color(0xFF8A5428)

    /** Heart. 5.8 / 6.3 / 4.9. */
    val Heart = Color(0xFF9E3E5A)

    /** The frame around the room: its own outline colour. */
    val RoomFrame = Color(0xFF33243A)

    /* Paper for things that are written: letters, postcards. The same paper as a card, with
       the envelope drawn in the wall's shades. */
    val Paper = Color(0xFFFFFDF5)
    val PaperShade = Color(0xFFF3DFBB)
    val PaperEdge = Color(0xFFD7BE95)
    val PaperText = Color(0xFF33243A)
    val PaperMuted = Color(0xFF6B5A6E)
}

/** Each dark ink's light partner: what a button, a bar or a chip in that colour is filled with. */
private val Fills = mapOf(
    Ink.Access to Color(0xFF86CCF2),
    Ink.Activity to Color(0xFFB4E86A),
    Ink.Social to Color(0xFFE08A5F),
    Ink.Ember to Color(0xFFFFBF4D),
    Ink.Gold to Color(0xFFFFE08A),
    Ink.Body to Color(0xFF9BD7D1),
    Ink.Room to Color(0xFFE8B476),
    Ink.Heart to Color(0xFFF3B8C8),
    Ink.Primary to Color(0xFF33243A)
)

/** And its palest tint: the ground behind a chosen option or an icon. */
private val Washes = mapOf(
    Ink.Access to Color(0xFFDCF1FC),
    Ink.Activity to Color(0xFFE9F6D2),
    Ink.Social to Color(0xFFF8DCCB),
    Ink.Ember to Color(0xFFFFE7B5),
    Ink.Gold to Color(0xFFFFF2C6),
    Ink.Body to Color(0xFFDDF1EE),
    Ink.Room to Color(0xFFF6E3C6),
    Ink.Heart to Color(0xFFFBE4EB),
    Ink.Primary to Color(0xFFEDE4DD)
)

/** What this colour fills things with. */
val Color.fill: Color get() = Fills[this] ?: lerp(this, Color.White, 0.55f)

/** Its palest tint. */
val Color.wash: Color get() = Washes[this] ?: lerp(this, Color.White, 0.86f)

/** Text that reads on this colour's fill: plum on every light fill, cream on plum. */
val Color.onFill: Color get() = if (this == Ink.Primary) Ink.Cream else Ink.OnAccent

val Domain.accent: Color
    get() = when (this) {
        Domain.BODY -> Ink.Body
        Domain.ROOM -> Ink.Room
        Domain.HEART -> Ink.Heart
    }

val Pillar.accent: Color
    get() = when (this) {
        Pillar.ACCESS -> Ink.Access
        Pillar.ACTIVITY -> Ink.Activity
        Pillar.SOCIAL -> Ink.Social
    }

/** A task's colour: its domain's for care, its road's for a mission. */
val Quest.accent: Color
    get() = domain?.accent ?: pillar?.accent ?: Ink.Gold

/** The tint behind a chosen control in this road's colour. */
val Pillar.wash: Color get() = accent.wash

/* ------------------------------------------------------------------ type

   Two faces, the same two as the icon, the store page and the website. Jersey 10, a pixel
   face, for what you read at a glance: screen and section titles, figures, the big words on
   a button. Nunito, rounded and plain, for everything you actually read. Long sentences are
   never set in the pixel face, however big they are.                                       */

val PixelFamily = FontFamily(Font(R.font.jersey10, FontWeight.Normal))

private val Round = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

/** Figures: the pixel face, so a number looks like the room counted it. */
val NumberFamily = PixelFamily

val Type = Typography(
    displayLarge = TextStyle(fontFamily = PixelFamily, fontSize = 60.sp, lineHeight = 58.sp),
    displayMedium = TextStyle(fontFamily = PixelFamily, fontSize = 46.sp, lineHeight = 46.sp),
    displaySmall = TextStyle(fontFamily = PixelFamily, fontSize = 38.sp, lineHeight = 38.sp),
    headlineLarge = TextStyle(fontFamily = PixelFamily, fontSize = 40.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = PixelFamily, fontSize = 34.sp, lineHeight = 34.sp),
    /** Long titles: a mission, a question. Heavy and round, never pixel. */
    headlineSmall = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp, lineHeight = 27.sp
    ),
    titleLarge = TextStyle(fontFamily = PixelFamily, fontSize = 30.sp, lineHeight = 31.sp),
    titleMedium = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp, lineHeight = 23.sp
    ),
    titleSmall = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.Bold,
        fontSize = 15.sp, lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.Normal,
        fontSize = 13.sp, lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.Bold,
        fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.1.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Round, fontWeight = FontWeight.Bold,
        fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp
    )
)

/** The big word on a button: the pixel face, at a size it reads at. */
val ButtonText = TextStyle(fontFamily = PixelFamily, fontSize = 26.sp, lineHeight = 26.sp)

/* ------------------------------------------------------------------ spacing */

object Space {
    val gutter = 20.dp
    val gap = 12.dp
    val block = 18.dp

    /** Kept for the few things still measured by it; shapes come from [Cut]. */
    val radius = 12.dp
    val radiusLarge = 18.dp

    /** How far a card stands off the wall, and a button off the page. */
    val lift = 4.dp

    /** Minimum touch target. Nothing interactive is allowed below this. */
    val tap = 52.dp
}

/** Centre-aligned variant used on completion and onboarding surfaces. */
val TextStyle.centered: TextStyle get() = copy(textAlign = TextAlign.Center)

@Composable
fun DeBubbleTheme(content: @Composable () -> Unit) {
    // Material's own components (sheets, dialogs, fields, the time picker) read these, so
    // they come out in the room's colours too.
    val scheme = lightColorScheme(
        primary = Ink.Primary,
        onPrimary = Ink.Cream,
        primaryContainer = Ink.Gold.fill,
        onPrimaryContainer = Ink.OnAccent,
        secondary = Ink.Body,
        onSecondary = Ink.Cream,
        secondaryContainer = Ink.Body.fill,
        onSecondaryContainer = Ink.OnAccent,
        tertiary = Ink.Heart,
        onTertiary = Ink.Cream,
        tertiaryContainer = Ink.Heart.fill,
        onTertiaryContainer = Ink.OnAccent,
        background = Ink.Void,
        onBackground = Ink.Primary,
        surface = Ink.Surface,
        onSurface = Ink.Primary,
        surfaceVariant = Ink.Well,
        onSurfaceVariant = Ink.Secondary,
        surfaceTint = Color.Transparent,
        surfaceBright = Ink.SurfaceHigh,
        surfaceDim = Ink.Well,
        surfaceContainerLowest = Ink.SurfaceHigh,
        surfaceContainerLow = Ink.Surface,
        surfaceContainer = Ink.Surface,
        surfaceContainerHigh = Ink.Surface,
        surfaceContainerHighest = Ink.Well,
        inverseSurface = Ink.Primary,
        inverseOnSurface = Ink.Cream,
        outline = Ink.Border,
        outlineVariant = Ink.Faint,
        scrim = Ink.Primary,
        error = Ink.Social,
        onError = Ink.Cream
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val bars = WindowCompat.getInsetsController(window, view)
            bars.isAppearanceLightStatusBars = true
            bars.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(colorScheme = scheme, typography = Type, shapes = CozyShapes, content = content)
}

/**
 * Which stage of the habitat the app is currently drawn inside.
 *
 * A composition local rather than a parameter on every screen, because this is a theme
 * value: it belongs to the whole tree and nothing decides it locally. Defaults to the
 * sealed room, which is what a preview or an unprovided subtree should look like.
 */
val LocalStage = staticCompositionLocalOf { Stage.SEALED }

/**
 * The ground for a surface that belongs to the room — the tabbed screens. The wall, whatever
 * the stage: the room opening is drawn in the room itself, not in the colour around it.
 */
@Composable
fun roomGround(): Color = Ink.Void
