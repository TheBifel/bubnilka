package com.thebifel.bubnilka.ime

import android.view.inputmethod.InputConnection
import com.thebifel.bubnilka.core.port.TextInsertionPort

interface CursorConnection {
    fun textBeforeCursor(maxCharacters: Int): CharSequence?
    fun commit(text: String): Boolean
    fun deleteBeforeCursor(characters: Int): Boolean
}

class FrameworkCursorConnection(
    private val connection: InputConnection,
) : CursorConnection {
    override fun textBeforeCursor(maxCharacters: Int): CharSequence? =
        connection.getTextBeforeCursor(maxCharacters, 0)

    override fun commit(text: String): Boolean = connection.commitText(text, 1)

    override fun deleteBeforeCursor(characters: Int): Boolean =
        connection.deleteSurroundingText(characters, 0)
}

class AndroidTextInsertionPort(
    private val connectionProvider: () -> CursorConnection?,
) : TextInsertionPort {
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
}
