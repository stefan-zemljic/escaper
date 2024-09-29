package ch.bytecraft.escaper

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.*

import org.junit.jupiter.api.Test

class EscaperTest {
    private val escapeChar = '\\'.code
    private val unicodePattern = Regex("""\\u\{([0-9a-fA-F]{1,8})}""")
    private val unicodeEscaper: StringBuilder.(Int) -> Unit = { cp ->
        append("\\u{", cp.toString(16).uppercase(), '}')
    }

    @Test
    fun testSimpleAsciiStringHandleRangesFalse() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            escapeMode = Escaper.Mode.EscapeNone
        )
        val input = "abc"
        val expected = "abc"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testStringContainingEscapeCharacter() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            escapeMode = Escaper.Mode.EscapeNone
        )
        val input = "a\\b"
        val expected = "a\\\\b"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testStringWithCharactersInReplacementsMap() {
        val replacements = mapOf('a'.code to '1'.code)
        val escaper = Escaper(
            escape = escapeChar,
            replacements = replacements,
            handleRanges = false,
            escapeMode = Escaper.Mode.EscapeNone
        )
        val input = "abc"
        val expected = "\\1bc"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testEscapeNonAsciiModeWithNonAsciiCharacters() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            escapeMode = Escaper.Mode.EscapeNonASCII
        )
        val input = "abc€"
        val expected = "abc\\u{20ac}"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testEscapeUtf32ModeWithNonBmpCharacters() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            escapeMode = Escaper.Mode.EscapeUTF32
        )
        val input = "a𝄞b"
        val expected = "a\\u{1d11e}b"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testEscapingWithHandleRangesTrueConsecutiveCodePoints() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = true,
            escapeMode = Escaper.Mode.EscapeNone
        )
        val input = "abcde"
        val expected = "a-e"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testEscapingWithHandleRangesTrueNonConsecutiveCodePoints() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = true,
            escapeMode = Escaper.Mode.EscapeNone
        )
        val input = "abd"
        val expected = "abd"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testUnescapeStringWithRanges() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = true,
            escapeMode = Escaper.Mode.EscapeNone
        )
        val input = "a-e"
        val expected = "abcde"
        val output = escaper.unescape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testUnescapeStringWithUnicodeEscapeSequences() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            unicodePattern = unicodePattern
        )
        val input = "abc\\u{1D11E}def"
        val expected = "abc𝄞def"
        val output = escaper.unescape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testEmptyString() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false
        )
        val input = ""
        val expected = ""
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testStartIndexBeyondStringLength() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false
        )
        val input = "abc"
        val startIndex = 5
        val expected = ""
        val output = escaper.escape(input, startIndex)
        assertEquals(expected, output)
    }

    @Test
    fun testEscapeAndUnescapeConsistency() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            unicodePattern = unicodePattern,
            unicodeEscaper = unicodeEscaper,
            escapeMode = Escaper.Mode.EscapeUTF32
        )
        val input = "abc€𝄞"
        val escaped = escaper.escape(input)
        val unescaped = escaper.unescape(escaped)
        assertEquals(input, unescaped)
    }

    @Test
    fun testUnescapeStringWithEscapeCharacter() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            unicodePattern = unicodePattern
        )
        val input = "a\\\\b"
        val expected = "a\\b"
        val output = escaper.unescape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testUnescapeStringWithReplacements() {
        val replacements = mapOf('1'.code to 'a'.code)
        val escaper = Escaper(
            escape = escapeChar,
            replacements = replacements,
            handleRanges = false
        )
        val input = "\\abc"
        val expected = "1bc"
        val output = escaper.unescape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testHandleSurrogatePairsProperly() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = emptyMap(),
            handleRanges = false,
            escapeMode = Escaper.Mode.EscapeUTF32
        )
        val input = "𝄞"
        val expected = "\\u{1d11e}"
        val output = escaper.escape(input)
        assertEquals(expected, output)
    }

    @Test
    fun testHandleWrongEscapesLeniently() {
        val escaper = Escaper(
            escape = escapeChar,
            replacements = mapOf(
                't'.code to '\t'.code,
                'r'.code to '\r'.code,
                'n'.code to '\n'.code,
                '\\'.code to '\\'.code,
                '\''.code to '\''.code,
            ),
            handleRanges = false,
            escapeMode = Escaper.Mode.EscapeUTF32
        )
        assertThat(escaper.unescape("\\a")).isEqualTo("\\a")
    }

    @Test
    fun testHandleWhitespaceCorrectly() {
        val escaper = Escaper(
            escape = '\\'.code,
            replacements = mapOf(
                '\t'.code to 't'.code,
                '\r'.code to 'r'.code,
                '\n'.code to 'n'.code,
                '\\'.code to '\\'.code,
                '^'.code to '^'.code,
                ']'.code to ']'.code,
                '-'.code to '-'.code,
            ),
            handleRanges = true,
        )
        val source = """\t\r\n\\a\^\]\-"""
        val source2 = """\t\r\n\a^]-"""
        val expected = "\t\r\n\\a^]-"
        val output = escaper.unescape(source)
        val output2 = escaper.unescape(source2)
        assertEquals(expected, output)
        assertEquals(expected, output2)
        val back = escaper.escape(output)
        assertEquals(source, back)
    }
}
