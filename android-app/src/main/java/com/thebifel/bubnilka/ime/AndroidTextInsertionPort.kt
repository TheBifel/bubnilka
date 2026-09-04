package com.thebifel.bubnilka.ime

import android.view.inputmethod.InputConnection
import android.view.inputmethod.ExtractedTextRequest
import com.thebifel.bubnilka.core.model.MAX_FIELD_CONTEXT_LENGTH
import com.thebifel.bubnilka.core.model.MAX_TRANSFORMED_LENGTH
import com.thebifel.bubnilka.core.port.TextInsertionPort

interface CursorConnection {
    fun textBeforeCursor(maxCharacters: Int): CharSequence?
    fun commit(text: String): Boolean
    fun deleteBeforeCursor(characters: Int): Boolean
    fun backspace(): Boolean
    fun allText(maxCharacters: Int): CharSequence?
    fun replaceAll(replacement: String): Boolean
}

class FrameworkCursorConnection(
    private val connection: InputConnection,
) : CursorConnection {
    override fun textBeforeCursor(maxCharacters: Int): CharSequence? =
        connection.getTextBeforeCursor(maxCharacters, 0)

    override fun commit(text: String): Boolean = connection.commitText(text, 1)

    override fun deleteBeforeCursor(characters: Int): Boolean =
        connection.deleteSurroundingText(characters, 0)

    override fun backspace(): Boolean {
        val selectedText = connection.getSelectedText(0)
        return if (selectedText.isNullOrEmpty()) {
            val beforeCursor = connection.getTextBeforeCursor(BACKSPACE_LOOKBACK_CODE_POINTS, 0)
                ?: return false
            val codePointCount = BackspaceDeletion.codePointCount(beforeCursor)
            codePointCount > 0 && connection.deleteSurroundingTextInCodePoints(codePointCount, 0)
        } else {
            connection.commitText("", 1)
        }
    }

    override fun allText(maxCharacters: Int): CharSequence? {
        val request = ExtractedTextRequest().apply { hintMaxChars = maxCharacters }
        val extracted = connection.getExtractedText(request, 0) ?: return null
        if (extracted.startOffset != 0 ||
            extracted.partialStartOffset >= 0 ||
            extracted.partialEndOffset >= 0
        ) {
            return null
        }
        val text = extracted.text ?: return null
        return text.takeIf { it.length <= maxCharacters }
    }

    override fun replaceAll(replacement: String): Boolean {
        val currentLength = allText(MAX_TRANSFORMED_LENGTH)?.length ?: return false
        connection.beginBatchEdit()
        return try {
            connection.setSelection(0, currentLength) && connection.commitText(replacement, 1)
        } finally {
            connection.endBatchEdit()
        }
    }

    private companion object {
        const val BACKSPACE_LOOKBACK_CODE_POINTS = 256
    }
}

class AndroidTextInsertionPort(
    private val connectionProvider: () -> CursorConnection?,
) : TextInsertionPort {
    fun backspace(): Boolean = connectionProvider()?.backspace() == true

    override fun insert(text: String): Boolean {
        val normalized = text.trim()
        if (normalized.isEmpty()) return false
        return connectionProvider()?.commit(normalized) == true
    }

    override fun matchesBeforeCursor(text: String): Boolean {
        if (text.isEmpty()) return false
        val beforeCursor = connectionProvider()?.textBeforeCursor(text.length)?.toString() ?: return false
        return beforeCursor == text
    }

    override fun replace(previous: String, replacement: String): Boolean {
        val normalizedReplacement = replacement.trim()
        if (previous.isEmpty() || normalizedReplacement.isEmpty()) return false
        val connection = connectionProvider() ?: return false
        if (connection.textBeforeCursor(previous.length)?.toString() != previous) return false
        if (!connection.deleteBeforeCursor(previous.length)) return false
        return connection.commit(normalizedReplacement)
    }

    override fun undo(previous: String): Boolean {
        if (previous.isEmpty()) return false
        val connection = connectionProvider() ?: return false
        if (connection.textBeforeCursor(previous.length)?.toString() != previous) return false
        return connection.deleteBeforeCursor(previous.length)
    }

    override fun readAllText(): String? =
        connectionProvider()?.allText(MAX_FIELD_CONTEXT_LENGTH)?.toString()

    override fun replaceAll(replacement: String): Boolean {
        if (replacement.length > MAX_TRANSFORMED_LENGTH) return false
        return connectionProvider()?.replaceAll(replacement) == true
    }
}
