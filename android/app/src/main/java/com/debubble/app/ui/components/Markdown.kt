package com.debubble.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * A small Markdown renderer.
 *
 * Deliberately small. The lessons and the journal need headings, bold, italic, lists, block
 * quotes and inline code, and that is the entire requirement — pulling in a full CommonMark
 * parser and an HTML view to render seven constructs would add a dependency, a WebView and a
 * theming problem in exchange for footnotes nobody will write.
 *
 * Unsupported syntax degrades to plain text rather than showing its own markup, which is the
 * right failure for a journal: someone typing a table gets their words, not asterisks.
 */
@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = Ink.Access,
    body: Color = Ink.Secondary
) {
    val blocks = remember(text) { parse(text) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is Block.Heading -> {
                    VSpace(if (block.level <= 2) 22 else 16)
                    Text(
                        text = block.text,
                        color = if (block.level <= 2) Ink.Primary else accent,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.headlineLarge
                            2 -> MaterialTheme.typography.headlineMedium
                            else -> MaterialTheme.typography.titleMedium
                        }
                    )
                    VSpace(8)
                }

                is Block.Paragraph -> {
                    Text(
                        text = inline(block.text, accent),
                        color = body,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    VSpace(12)
                }

                is Block.Bullet -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = block.marker,
                            modifier = Modifier.width(20.dp),
                            color = accent,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = inline(block.text, accent),
                            color = body,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }

                is Block.Quote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(Space.radius))
                            .background(Ink.SurfaceHigh)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .background(accent)
                        )
                        Text(
                            text = inline(block.text, accent),
                            color = Ink.Primary,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontStyle = FontStyle.Italic
                            )
                        )
                    }
                    VSpace(8)
                }

                is Block.Rule -> {
                    VSpace(8)
                    Divider()
                    VSpace(12)
                }
            }
        }
    }
}

private sealed interface Block {
    data class Heading(val level: Int, val text: String) : Block
    data class Paragraph(val text: String) : Block
    data class Bullet(val marker: String, val text: String) : Block
    data class Quote(val text: String) : Block
    data object Rule : Block
}

/**
 * Block-level parse. Consecutive non-empty lines join into one paragraph, which is what makes
 * hard-wrapped source (all of the lesson content) render as flowing text rather than as one
 * paragraph per line.
 */
private fun parse(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val paragraph = StringBuilder()

    fun flush() {
        if (paragraph.isNotBlank()) out += Block.Paragraph(paragraph.toString().trim())
        paragraph.clear()
    }

    src.lines().forEach { raw ->
        val line = raw.trimEnd()
        val trimmed = line.trim()
        when {
            trimmed.isEmpty() -> flush()

            trimmed.startsWith("#") -> {
                flush()
                val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
                out += Block.Heading(level, trimmed.dropWhile { it == '#' }.trim())
            }

            trimmed == "---" || trimmed == "***" -> {
                flush()
                out += Block.Rule
            }

            trimmed.startsWith("> ") -> {
                flush()
                out += Block.Quote(trimmed.removePrefix("> ").trim())
            }

            trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                flush()
                out += Block.Bullet("•", trimmed.drop(2).trim())
            }

            NUMBERED.matches(trimmed) -> {
                flush()
                val marker = trimmed.takeWhile { it != ' ' }
                out += Block.Bullet(marker, trimmed.substringAfter(' ').trim())
            }

            else -> {
                if (paragraph.isNotEmpty()) paragraph.append(' ')
                paragraph.append(trimmed)
            }
        }
    }
    flush()
    return out
}

private val NUMBERED = Regex("""^\d+\.\s+.*""")

/**
 * Inline spans: **bold**, *italic*, `code`.
 *
 * A single left-to-right pass. Unmatched markers are emitted as the literal characters, so a
 * journal entry containing a stray asterisk shows the asterisk instead of swallowing the rest
 * of the paragraph.
 */
private fun inline(text: String, accent: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val rest = text.substring(i)
        val bold = if (rest.startsWith("**")) closing(rest, "**", 2) else -1
        val code = if (rest.startsWith("`")) closing(rest, "`", 1) else -1
        val italic = if (rest.startsWith("*") && !rest.startsWith("**")) closing(rest, "*", 1) else -1

        when {
            bold > 0 -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Ink.Primary)) {
                    append(rest.substring(2, bold))
                }
                i += bold + 2
            }

            code > 0 -> {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, color = accent)) {
                    append(rest.substring(1, code))
                }
                i += code + 1
            }

            italic > 0 -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = Ink.Primary)) {
                    append(rest.substring(1, italic))
                }
                i += italic + 1
            }

            else -> {
                append(text[i])
                i += 1
            }
        }
    }
}

/** Index of the closing marker, or -1 when there is not one. */
private fun closing(rest: String, marker: String, from: Int): Int {
    val at = rest.indexOf(marker, startIndex = from)
    // An empty span (`****`) is not emphasis, it is two literal pairs.
    return if (at > from) at else -1
}
