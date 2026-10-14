package org.celimited.manager.core.common.pdf

/**
 * Advance widths for the base-14 Helvetica faces, in 1/1000 em, for ASCII 32..126.
 *
 * Laying a table out needs real metrics: a flat guess makes every column visibly misaligned, and
 * PDF gives no way to ask the viewer how wide a string came out.
 */
private val HELVETICA_WIDTHS = intArrayOf(
    278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
    556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
    1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
    667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
    333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
    556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584
)

private val HELVETICA_BOLD_WIDTHS = intArrayOf(
    278, 333, 474, 556, 556, 889, 722, 238, 333, 333, 389, 584, 278, 333, 278, 278,
    556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 333, 333, 584, 584, 584, 611,
    975, 722, 722, 722, 722, 667, 611, 778, 722, 278, 556, 722, 611, 833, 722, 778,
    667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 333, 278, 333, 584, 556,
    333, 556, 611, 556, 611, 556, 333, 611, 611, 278, 278, 556, 278, 889, 611, 611,
    611, 611, 389, 556, 333, 611, 556, 778, 556, 556, 500, 389, 280, 389, 584
)

private const val FIRST_CHAR = 32
private const val LAST_CHAR = 126

/** Fallback for anything outside the measured range — the most common width in both faces. */
private const val DEFAULT_WIDTH = 556

private fun glyphWidth(char: Char, bold: Boolean): Int {
    val code = char.code
    if (code < FIRST_CHAR || code > LAST_CHAR) return DEFAULT_WIDTH
    val widths = if (bold) HELVETICA_BOLD_WIDTHS else HELVETICA_WIDTHS
    return widths[code - FIRST_CHAR]
}

/** Width of [text] in points when set in Helvetica at [fontSize]. */
internal fun helveticaWidth(text: String, fontSize: Float, bold: Boolean = false): Float {
    var total = 0
    text.forEach { total += glyphWidth(it, bold) }
    return total * fontSize / 1000f
}

/**
 * [text] shortened to fit [maxWidth], with an ellipsis when it had to be cut.
 *
 * Three ASCII periods rather than the '…' character, which has no WinAnsi code point.
 */
internal fun truncateToWidth(
    text: String,
    maxWidth: Float,
    fontSize: Float,
    bold: Boolean = false
): String {
    if (maxWidth <= 0f) return ""
    if (helveticaWidth(text, fontSize, bold) <= maxWidth) return text

    val ellipsis = "..."
    val ellipsisWidth = helveticaWidth(ellipsis, fontSize, bold)
    if (ellipsisWidth > maxWidth) return ""

    val budget = maxWidth - ellipsisWidth
    var used = 0f
    var end = 0
    while (end < text.length) {
        val next = used + glyphWidth(text[end], bold) * fontSize / 1000f
        if (next > budget) break
        used = next
        end++
    }
    return text.substring(0, end) + ellipsis
}

/** [text] broken into lines that each fit [maxWidth], splitting on spaces where possible. */
internal fun wrapToWidth(
    text: String,
    maxWidth: Float,
    fontSize: Float,
    bold: Boolean = false
): List<String> {
    if (text.isBlank()) return emptyList()

    val lines = mutableListOf<String>()
    var current = StringBuilder()

    text.split(' ').forEach { word ->
        val candidate = if (current.isEmpty()) word else "$current $word"
        if (helveticaWidth(candidate, fontSize, bold) <= maxWidth) {
            current = StringBuilder(candidate)
        } else {
            if (current.isNotEmpty()) {
                lines += current.toString()
                current = StringBuilder()
            }
            // A single word too long for the line gets hard-cut rather than overflowing.
            if (helveticaWidth(word, fontSize, bold) > maxWidth) {
                lines += truncateToWidth(word, maxWidth, fontSize, bold)
            } else {
                current = StringBuilder(word)
            }
        }
    }

    if (current.isNotEmpty()) lines += current.toString()
    return lines
}
