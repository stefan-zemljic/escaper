package ch.bytecraft.escaper

import ch.bytecraft.escaper.Escaper.Mode.*

class Escaper(
    private val escape: Int,
    private val replacements: Map<Int, Int>,
    private val handleRanges: Boolean,
    private val unicodePattern: Regex? = Regex("""\\u\{([0-9a-fA-F]{1,8})}"""),
    private val unicodeEscaper: StringBuilder.(Int) -> Unit = { append("\\u{", it.toUInt().toString(16), '}') },
    private val escapeMode: Mode = EscapeUTF32,
) {
    enum class Mode {
        EscapeNone,
        EscapeUTF32,
        EscapeNonASCII,
    }

    private val unreplacements = replacements.entries.associate { (k, v) -> v to k }

    fun escape(source: String, startIndex: Int = 0): String {
        return buildString { escapeTo(this, source, startIndex) }
    }

    fun escapeTo(builder: StringBuilder, source: String, startIndex: Int = 0): StringBuilder {
        var index = startIndex
        while (index < source.length) {
            index = builder.escapeStep(source, index)
        }
        return builder
    }

    fun unescape(source: String, startIndex: Int = 0): String {
        return buildString { unescapeTo(this, source, startIndex) }
    }

    fun unescapeTo(builder: StringBuilder, source: String, startIndex: Int = 0): StringBuilder {
        var index = startIndex
        while (index < source.length) {
            index = builder.unescapeStep(source, index)
        }
        return builder
    }

    private fun StringBuilder.escapeStep(source: String, startIndex: Int): Int {
        var index = startIndex
        val first = source.codePointAt(index)
        index += Character.charCount(first)
        if (!handleRanges) {
            appendEscaped(first)
            return index
        }
        var last = first
        var length = 1
        while (index < source.length) {
            val cp = source.codePointAt(index)
            if (last + 1 != cp) break
            last = cp
            length++
            index += Character.charCount(cp)
        }
        when (length) {
            1 -> appendEscaped(first)
            2 -> appendEscaped(first).appendEscaped(last)
            else -> appendEscaped(first).append('-').appendEscaped(last)
        }
        return index
    }

    private fun StringBuilder.appendEscaped(cp: Int): StringBuilder {
        if (cp == escape) {
            appendCodePoint(escape)
            appendCodePoint(escape)
        } else {
            val repl = replacements[cp]
            if (repl == null) {
                when {
                    escapeMode == EscapeUTF32 && Character.isBmpCodePoint(cp) -> appendCodePoint(cp)
                    escapeMode == EscapeNonASCII && cp and 0x7F == cp -> appendCodePoint(cp)
                    escapeMode == EscapeNone -> appendCodePoint(cp)
                    else -> unicodeEscaper(cp)
                }
            } else {
                appendCodePoint(escape)
                appendCodePoint(repl)
            }
        }
        return this
    }

    private fun StringBuilder.unescapeStep(source: String, startIndex: Int): Int {
        var index = startIndex
        val first = unescapeCodePoint(source, index).let {
            index = it.first
            it.second
        }
        if (!handleRanges || index >= source.length - 1 || source[index] != '-') {
            appendCodePoint(first)
            return index
        }
        index++
        val last = unescapeCodePoint(source, index).let {
            index = it.first
            it.second
        }
        var current = first
        while (current <= last) {
            appendCodePoint(current)
            current++
        }
        return index
    }

    private fun unescapeCodePoint(source: String, startIndex: Int): Pair<Int, Int> {
        var index = startIndex
        val match = unicodePattern?.matchAt(source, index)
        if (match != null) {
            val hex = match.groupValues[1]
            val codePoint = hex.toInt(16)
            index = match.range.last + 1
            return index to codePoint
        }
        val cp = source.codePointAt(index)
        index += Character.charCount(cp)
        if (cp != escape || index == source.length) return index to cp
        val next = source.codePointAt(index)
        if (next == escape) {
            index += Character.charCount(next)
            return index to escape
        }
        val repl = unreplacements[next] ?: return index to cp
        index += Character.charCount(next)
        return index to repl
    }
}