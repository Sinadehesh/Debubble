package com.debubble.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import kotlin.math.floor

/**
 * The icon set, in pixels.
 *
 * These were stroked curves on a 24-unit grid, round-capped and anti-aliased, which is how a
 * system icon is drawn and not how anything in the room is. Now each is a few rows of whole
 * pixels, scaled by a whole number so the pixels stay square and crisp, in the same hand as
 * the bell, the letter and the heart. They take the colour they are given.
 *
 * Every glyph is decorative — the control around it carries the label — so they clear their
 * own semantics rather than being announced twice.
 */
enum class Glyphs(val rows: List<String>) {
    Check(
        listOf(
            ".......#",
            "......##",
            "#....##.",
            "##..##..",
            ".####...",
            "..##....",
            "........"
        )
    ),
    Plus(
        listOf(
            "...#...",
            "...#...",
            "...#...",
            "#######",
            "...#...",
            "...#...",
            "...#..."
        )
    ),
    Minus(
        listOf(
            ".......",
            ".......",
            ".......",
            "#######",
            ".......",
            ".......",
            "......."
        )
    ),
    ArrowLeft(
        listOf(
            "...#...",
            "..##...",
            ".######",
            "#######",
            ".######",
            "..##...",
            "...#..."
        )
    ),
    ArrowRight(
        listOf(
            "...#...",
            "...##..",
            "######.",
            "#######",
            "######.",
            "...##..",
            "...#..."
        )
    ),
    Undo(
        listOf(
            "..#.....",
            ".##.....",
            "#######.",
            ".##....#",
            "..#....#",
            ".......#",
            "..#####."
        )
    ),
    Lock(
        listOf(
            "..###..",
            ".#...#.",
            ".#...#.",
            "#######",
            "###.###",
            "###.###",
            "#######"
        )
    ),
    Spark(
        listOf(
            "...#...",
            "...#...",
            "..###..",
            "#######",
            "..###..",
            "...#...",
            "...#..."
        )
    )
}

@Composable
fun Glyph(
    glyph: Glyphs,
    modifier: Modifier = Modifier,
    colour: Color = Color.White,
    size: Int = 20,
    /** Kept so older call sites still compile; a pixel glyph has no stroke to weigh. */
    @Suppress("UNUSED_PARAMETER") weight: Float = 2.2f
) {
    Canvas(
        modifier = modifier
            .size(size.dp)
            .clearAndSetSemantics { }
    ) {
        val rows = glyph.rows
        val cols = rows.maxOf { it.length }
        // A whole number of device pixels per glyph pixel, so every square is the same size.
        val cell = floor(minOf(this.size.width / cols, this.size.height / rows.size)).coerceAtLeast(1f)
        val left = floor((this.size.width - cell * cols) / 2f)
        val top = floor((this.size.height - cell * rows.size) / 2f)
        rows.forEachIndexed { y, row ->
            row.forEachIndexed { x, ch ->
                if (ch == '#') {
                    drawRect(colour, topLeft = Offset(left + x * cell, top + y * cell), size = Size(cell, cell))
                }
            }
        }
    }
}
