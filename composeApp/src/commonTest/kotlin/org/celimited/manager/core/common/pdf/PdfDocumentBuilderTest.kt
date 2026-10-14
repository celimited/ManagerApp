package org.celimited.manager.core.common.pdf

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun ByteArray.asLatin1(): String =
    buildString(size) { this@asLatin1.forEach { append((it.toInt() and 0xFF).toChar()) } }

class PdfDocumentBuilderTest {

    private fun singlePagePdf(text: String = "Hello"): ByteArray =
        PdfDocumentBuilder().apply { text(40f, 800f, text, 12f) }.build()

    @Test
    fun `starts with the PDF header and ends with the EOF marker`() {
        val pdf = singlePagePdf().asLatin1()

        assertTrue(pdf.startsWith("%PDF-1.4"), "missing header, got: ${pdf.take(20)}")
        assertTrue(pdf.endsWith("%%EOF"), "missing EOF marker")
    }

    @Test
    fun `startxref points at the xref table`() {
        val pdf = singlePagePdf().asLatin1()

        val marker = "startxref\n"
        val offset = pdf.substring(pdf.lastIndexOf(marker) + marker.length)
            .substringBefore('\n')
            .trim()
            .toInt()

        assertTrue(offset in pdf.indices, "startxref offset $offset outside the document")
        assertTrue(
            pdf.startsWith("xref", offset),
            "startxref points at '${pdf.substring(offset, minOf(offset + 12, pdf.length))}', not 'xref'"
        )
    }

    @Test
    fun `xref declares one entry per object plus the free entry`() {
        val pdf = singlePagePdf().asLatin1()

        val objectCount = Regex("""^\d+ 0 obj""", RegexOption.MULTILINE).findAll(pdf).count()
        val declared = Regex("""xref\n0 (\d+)""").find(pdf)!!.groupValues[1].toInt()
        val entries = Regex("""^\d{10} \d{5} [nf] $""", RegexOption.MULTILINE).findAll(pdf).count()

        // Object 0 is the mandatory free entry, so there is always one more entry than object.
        assertEquals(objectCount + 1, declared, "xref count does not match object count")
        assertEquals(declared, entries, "xref entry rows do not match the declared count")
    }

    @Test
    fun `parentheses and backslashes in text are escaped`() {
        val pdf = PdfDocumentBuilder()
            .apply { text(0f, 0f, """a(b)c\d""", 10f) }
            .build()
            .asLatin1()

        assertTrue(pdf.contains("""a\(b\)c\\d"""), "unescaped text would terminate the string early")
    }

    @Test
    fun `characters outside Latin-1 become question marks`() {
        // Bengali, and an em dash: neither has a WinAnsi code point.
        val pdf = PdfDocumentBuilder()
            .apply { text(0f, 0f, "abব—c", 10f) }
            .build()
            .asLatin1()

        assertTrue(pdf.contains("(ab??c)"), "expected '?' substitutions, got: $pdf")
    }

    @Test
    fun `every byte survives as a single byte`() {
        // A Latin-1 character must not be UTF-8 encoded, or /Length and the xref offsets desync.
        val bytes = PdfDocumentBuilder().apply { text(0f, 0f, "café", 10f) }.build()

        assertTrue(bytes.none { (it.toInt() and 0xFF) == 0xC3 }, "text looks UTF-8 encoded")
        assertTrue(bytes.any { (it.toInt() and 0xFF) == 0xE9 }, "missing the Latin-1 'e-acute' byte")
    }

    @Test
    fun `stream length matches the actual stream bytes`() {
        val pdf = singlePagePdf().asLatin1()

        val declared = Regex("""<< /Length (\d+) >>\nstream\n""").find(pdf)!!
        val length = declared.groupValues[1].toInt()
        val streamStart = declared.range.last + 1

        assertEquals(
            "endstream",
            pdf.substring(streamStart + length, streamStart + length + 9),
            "declared /Length does not land exactly on 'endstream'"
        )
    }

    @Test
    fun `page count matches the number of page objects`() {
        val pdf = PdfDocumentBuilder().apply {
            text(0f, 0f, "one", 10f)
            newPage()
            text(0f, 0f, "two", 10f)
            newPage()
            text(0f, 0f, "three", 10f)
        }.build().asLatin1()

        val pageObjects = Regex("""/Type /Page[^s]""").findAll(pdf).count()
        val declared = Regex("""/Type /Pages /Count (\d+)""").find(pdf)!!.groupValues[1].toInt()

        assertEquals(3, pageObjects)
        assertEquals(3, declared)
    }

    @Test
    fun `forEachPage draws on each page and restores the active one`() {
        val builder = PdfDocumentBuilder()
        builder.text(0f, 0f, "first", 10f)
        builder.newPage()
        builder.forEachPage { index, _ -> builder.text(0f, 0f, "footer$index", 8f) }
        builder.text(0f, 0f, "back-on-last", 10f)

        val pdf = builder.build().asLatin1()

        assertTrue(pdf.contains("(footer0)"))
        assertTrue(pdf.contains("(footer1)"))
        assertTrue(pdf.contains("(back-on-last)"))
        assertEquals(2, builder.pageCount)
    }
}

class HelveticaMetricsTest {

    @Test
    fun `width grows with the string and with the font size`() {
        assertTrue(helveticaWidth("ii", 10f) < helveticaWidth("iii", 10f))
        assertTrue(helveticaWidth("abc", 10f) < helveticaWidth("abc", 20f))
        // 'i' is the narrowest lowercase glyph, 'm' among the widest.
        assertTrue(helveticaWidth("i", 10f) < helveticaWidth("m", 10f))
    }

    @Test
    fun `bold is at least as wide as regular`() {
        val text = "Total Sales 1,234"
        assertTrue(helveticaWidth(text, 10f, bold = true) >= helveticaWidth(text, 10f))
    }

    @Test
    fun `truncate never exceeds the limit and keeps short text intact`() {
        val text = "A J Enterprise Limited, Dhaka"
        val limit = 60f

        assertEquals(text, truncateToWidth(text, 500f, 9f))

        val truncated = truncateToWidth(text, limit, 9f)
        assertTrue(truncated.endsWith("..."), "expected an ellipsis, got '$truncated'")
        assertTrue(
            helveticaWidth(truncated, 9f) <= limit,
            "'$truncated' is ${helveticaWidth(truncated, 9f)}pt, over the ${limit}pt limit"
        )
    }

    @Test
    fun `truncate degrades gracefully when nothing fits`() {
        assertEquals("", truncateToWidth("anything", 0f, 9f))
        assertEquals("", truncateToWidth("anything", 1f, 9f))
    }

    @Test
    fun `wrap breaks on spaces and respects the width`() {
        val lines = wrapToWidth("Compare monthly sales trend in 2025 across every outlet", 80f, 9f)

        assertTrue(lines.size > 1, "expected the text to wrap")
        lines.forEach {
            assertTrue(helveticaWidth(it, 9f) <= 80f, "line '$it' overflows")
        }
        assertEquals(
            "Compare monthly sales trend in 2025 across every outlet",
            lines.joinToString(" ")
        )
    }

    @Test
    fun `wrap hard-cuts a single word too long for the line`() {
        val lines = wrapToWidth("Supercalifragilisticexpialidocious", 30f, 9f)

        assertEquals(1, lines.size)
        assertTrue(helveticaWidth(lines.first(), 9f) <= 30f)
    }
}
