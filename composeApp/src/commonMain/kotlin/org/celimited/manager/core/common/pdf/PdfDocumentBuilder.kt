package org.celimited.manager.core.common.pdf

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** A colour as PDF device-RGB components, each 0..1. */
data class PdfColor(val red: Float, val green: Float, val blue: Float) {
    companion object {
        /** From a 0xRRGGBB / 0xAARRGGBB literal — the form colours are written in across the app. */
        fun fromArgb(argb: Long): PdfColor = PdfColor(
            red = ((argb shr 16) and 0xFF) / 255f,
            green = ((argb shr 8) and 0xFF) / 255f,
            blue = (argb and 0xFF) / 255f
        )

        val Black = PdfColor(0f, 0f, 0f)
        val White = PdfColor(1f, 1f, 1f)
    }
}

/**
 * Minimal PDF 1.4 writer: enough to place text, rectangles, lines and curves on A4 pages.
 *
 * Hand-written rather than delegated to a platform renderer so that Android and iOS produce
 * byte-identical output from one implementation, with no dependency and no `expect`/`actual`.
 *
 * Text is limited to the base-14 Helvetica faces under WinAnsi encoding: characters above U+00FF
 * have no code point and are written as '?'.
 */
class PdfDocumentBuilder(
    val pageWidth: Float = A4_WIDTH,
    val pageHeight: Float = A4_HEIGHT
) {
    private val pages = mutableListOf<StringBuilder>()
    private var current = StringBuilder().also { pages += it }

    /** Starts a new page and directs subsequent drawing to it. */
    fun newPage() {
        current = StringBuilder().also { pages += it }
    }

    val pageCount: Int get() = pages.size

    /**
     * Runs [block] once per page, with that page active.
     *
     * For anything that needs the final page count while drawing on earlier pages — footers, most
     * obviously — since the count is only known once the content is laid out.
     */
    fun forEachPage(block: (pageIndex: Int, pageCount: Int) -> Unit) {
        val active = current
        pages.forEachIndexed { index, page ->
            current = page
            block(index, pages.size)
        }
        current = active
    }

    fun text(
        x: Float,
        y: Float,
        text: String,
        fontSize: Float,
        bold: Boolean = false,
        color: PdfColor = PdfColor.Black
    ) {
        if (text.isEmpty()) return
        val font = if (bold) "/F2" else "/F1"
        current.append("BT ")
            .append(font).append(' ').append(num(fontSize)).append(" Tf ")
            .append(rgb(color)).append(" rg ")
            .append(num(x)).append(' ').append(num(y)).append(" Td ")
            .append('(').append(escape(text)).append(") Tj ET\n")
    }

    /** Draws [text] so that it ends at [right]. */
    fun textRightAligned(
        right: Float,
        y: Float,
        text: String,
        fontSize: Float,
        bold: Boolean = false,
        color: PdfColor = PdfColor.Black
    ) = text(right - helveticaWidth(text, fontSize, bold), y, text, fontSize, bold, color)

    /** Draws [text] centred on [centerX]. */
    fun textCentered(
        centerX: Float,
        y: Float,
        text: String,
        fontSize: Float,
        bold: Boolean = false,
        color: PdfColor = PdfColor.Black
    ) = text(centerX - helveticaWidth(text, fontSize, bold) / 2f, y, text, fontSize, bold, color)

    fun fillRect(x: Float, y: Float, width: Float, height: Float, color: PdfColor) {
        current.append(rgb(color)).append(" rg ")
            .append(num(x)).append(' ').append(num(y)).append(' ')
            .append(num(width)).append(' ').append(num(height)).append(" re f\n")
    }

    fun line(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        width: Float = 0.5f,
        color: PdfColor = PdfColor.Black
    ) {
        current.append(rgb(color)).append(" RG ").append(num(width)).append(" w ")
            .append(num(x1)).append(' ').append(num(y1)).append(" m ")
            .append(num(x2)).append(' ').append(num(y2)).append(" l S\n")
    }

    /** Polyline through [points], stroked only. */
    fun polyline(points: List<Pair<Float, Float>>, width: Float, color: PdfColor) {
        if (points.size < 2) return
        current.append(rgb(color)).append(" RG ").append(num(width)).append(" w ")
        points.forEachIndexed { index, (x, y) ->
            current.append(num(x)).append(' ').append(num(y))
                .append(if (index == 0) " m " else " l ")
        }
        current.append("S\n")
    }

    /**
     * Filled circular sector — one pie slice, centred at ([cx], [cy]).
     *
     * Angles are degrees measured counter-clockwise from the positive x axis. The arc is split into
     * spans of at most 90 degrees because a single cubic Bezier cannot approximate more than a
     * quadrant to acceptable accuracy.
     */
    fun fillSector(
        cx: Float,
        cy: Float,
        radius: Float,
        startAngle: Float,
        sweepAngle: Float,
        color: PdfColor
    ) {
        if (abs(sweepAngle) < 0.01f) return

        current.append(rgb(color)).append(" rg ")
            .append(num(cx)).append(' ').append(num(cy)).append(" m ")

        val segments = (abs(sweepAngle) / 90f).toInt() + 1
        val step = sweepAngle / segments
        var angle = startAngle

        current.append(num(cx + radius * cosDeg(angle))).append(' ')
            .append(num(cy + radius * sinDeg(angle))).append(" l ")

        repeat(segments) {
            val end = angle + step
            // Control-point offset for a Bezier approximating an arc of `step` degrees.
            val half = toRadians(step) / 2.0
            val k = (4.0 / 3.0 * (1 - cos(half)) / sin(half)).toFloat()

            val x1 = cx + radius * cosDeg(angle)
            val y1 = cy + radius * sinDeg(angle)
            val x2 = cx + radius * cosDeg(end)
            val y2 = cy + radius * sinDeg(end)

            current.append(num(x1 - k * radius * sinDeg(angle))).append(' ')
                .append(num(y1 + k * radius * cosDeg(angle))).append(' ')
                .append(num(x2 + k * radius * sinDeg(end))).append(' ')
                .append(num(y2 - k * radius * cosDeg(end))).append(' ')
                .append(num(x2)).append(' ').append(num(y2)).append(" c ")

            angle = end
        }

        current.append("h f\n")
    }

    /** Serialises the document. */
    fun build(): ByteArray {
        val out = StringBuilder()
        val offsets = mutableListOf<Int>()

        // The second line's high bytes are the conventional marker telling tools this file is
        // binary, so they never transfer it in text mode and mangle the line endings.
        out.append("%PDF-1.4\n%âãÏÓ\n")

        fun startObject(number: Int) {
            while (offsets.size < number) offsets += 0
            offsets[number - 1] = out.length
            out.append(number).append(" 0 obj\n")
        }

        // Object numbering: 1 catalog, 2 pages, 3 + 4 fonts, then per page a page object and its
        // content stream, interleaved.
        val firstPageObject = 5
        val pageObjectNumbers = pages.indices.map { firstPageObject + it * 2 }
        val contentObjectNumbers = pages.indices.map { firstPageObject + it * 2 + 1 }

        startObject(1)
        out.append("<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")

        startObject(2)
        out.append("<< /Type /Pages /Count ").append(pages.size).append(" /Kids [")
        pageObjectNumbers.forEach { out.append(it).append(" 0 R ") }
        out.append("] >>\nendobj\n")

        startObject(3)
        out.append("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>\nendobj\n")

        startObject(4)
        out.append("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>\nendobj\n")

        pages.forEachIndexed { index, content ->
            startObject(pageObjectNumbers[index])
            out.append("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ")
                .append(num(pageWidth)).append(' ').append(num(pageHeight))
                .append("] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents ")
                .append(contentObjectNumbers[index]).append(" 0 R >>\nendobj\n")

            val stream = content.toString()
            startObject(contentObjectNumbers[index])
            out.append("<< /Length ").append(stream.length).append(" >>\nstream\n")
                .append(stream)
                .append("endstream\nendobj\n")
        }

        val xrefOffset = out.length
        val objectCount = offsets.size
        out.append("xref\n0 ").append(objectCount + 1).append('\n')
        out.append("0000000000 65535 f \n")
        offsets.forEach { out.append(padOffset(it)).append(" 00000 n \n") }

        out.append("trailer\n<< /Size ").append(objectCount + 1).append(" /Root 1 0 R >>\n")
            .append("startxref\n").append(xrefOffset).append("\n%%EOF")

        return out.toString().toLatin1Bytes()
    }

    companion object {
        const val A4_WIDTH = 595f
        const val A4_HEIGHT = 842f
    }
}

private fun cosDeg(degrees: Float): Float = cos(toRadians(degrees)).toFloat()

private fun sinDeg(degrees: Float): Float = sin(toRadians(degrees)).toFloat()

private fun toRadians(degrees: Float): Double = degrees * PI / 180.0

private fun rgb(color: PdfColor): String =
    "${num(color.red)} ${num(color.green)} ${num(color.blue)}"

/** Two decimal places, without depending on a platform number formatter. */
private fun num(value: Float): String {
    val scaled = (value * 100f).roundToInt()
    val whole = scaled / 100
    val fraction = abs(scaled % 100)
    return if (fraction == 0) {
        whole.toString()
    } else {
        val sign = if (scaled < 0 && whole == 0) "-" else ""
        "$sign$whole.${fraction.toString().padStart(2, '0')}"
    }
}

private fun padOffset(offset: Int): String = offset.toString().padStart(10, '0')

private fun escape(text: String): String {
    val builder = StringBuilder(text.length)
    text.forEach { char ->
        when {
            char == '\\' || char == '(' || char == ')' -> builder.append('\\').append(char)
            // Above Latin-1 there is no WinAnsi code point; control characters would break the
            // stream, so both collapse to '?'.
            char.code in 32..255 -> builder.append(char)
            else -> builder.append('?')
        }
    }
    return builder.toString()
}

/**
 * One byte per character.
 *
 * Deliberately not `encodeToByteArray()`: that emits UTF-8, which would turn every character above
 * U+007F into two bytes and desynchronise the `/Length` counts and xref offsets.
 */
private fun String.toLatin1Bytes(): ByteArray =
    ByteArray(length) { index -> (this[index].code and 0xFF).toByte() }
