package com.thebifel.bubnilka.ime

internal object BackspaceDeletion {
    fun codePointCount(textBeforeCursor: CharSequence): Int {
        val codePoints = textBeforeCursor.toString().toCodePoints()
        if (codePoints.isEmpty()) return 0

        var cursor = codePoints.lastIndex
        while (cursor >= 0 && Character.isWhitespace(codePoints[cursor])) cursor -= 1
        if (cursor < 0) return codePoints.size

        if (isWordPart(codePoints[cursor])) {
            while (cursor >= 0 && isWordPart(codePoints[cursor])) cursor -= 1
        } else {
            cursor -= 1
        }
        return codePoints.size - cursor - 1
    }

    private fun isWordPart(codePoint: Int): Boolean =
        Character.isLetterOrDigit(codePoint) ||
            codePoint == '\''.code ||
            codePoint == '’'.code ||
            codePoint == '-'.code ||
            when (Character.getType(codePoint)) {
                Character.NON_SPACING_MARK.toInt(),
                Character.COMBINING_SPACING_MARK.toInt(),
                Character.ENCLOSING_MARK.toInt(),
                -> true
                else -> false
            }

    private fun String.toCodePoints(): List<Int> = buildList {
        var offset = 0
        while (offset < length) {
            val codePoint = codePointAt(offset)
            add(codePoint)
            offset += Character.charCount(codePoint)
        }
    }
}
