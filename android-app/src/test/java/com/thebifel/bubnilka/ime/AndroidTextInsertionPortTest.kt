package com.thebifel.bubnilka.ime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidTextInsertionPortTest {
    @Test
    fun insertsNonBlankText() {
        val connection = FakeCursorConnection("")
        val port = AndroidTextInsertionPort { connection }

        assertTrue(port.insert("готовий текст"))
        assertEquals("готовий текст", connection.content)
    }

    @Test
    fun replacesOnlyExactSuffix() {
        val connection = FakeCursorConnection("префікс старий текст")
        val port = AndroidTextInsertionPort { connection }

        assertTrue(port.replace("старий текст", "новий текст"))
        assertEquals("префікс новий текст", connection.content)
    }

    @Test
    fun unsafeReplacementDoesNotMutateHostText() {
        val connection = FakeCursorConnection("користувач змінив поле")
        val port = AndroidTextInsertionPort { connection }

        assertFalse(port.replace("старий текст", "новий текст"))
        assertEquals("користувач змінив поле", connection.content)
        assertEquals(0, connection.deleteCalls)
    }

    @Test
    fun backspaceDeletesOneEditorUnit() {
        val connection = FakeCursorConnection("готовий текст")
        val port = AndroidTextInsertionPort { connection }

        assertTrue(port.backspace())
        assertEquals("готовий текс", connection.content)
        assertEquals(1, connection.backspaceCalls)
    }

    @Test
    fun readsAndAlwaysReplacesTheEntireCurrentField() {
        val connection = FakeCursorConnection("Поточний текст")
        val port = AndroidTextInsertionPort { connection }

        assertEquals("Поточний текст", port.readAllText())
        connection.content = "Користувач змінив поле під час запиту"
        assertTrue(port.replaceAll("Повний результат"))
        assertEquals("Повний результат", connection.content)
        assertTrue(port.replaceAll(""))
        assertEquals("", connection.content)
    }

    private class FakeCursorConnection(initialContent: String) : CursorConnection {
        var content: String = initialContent
        var deleteCalls: Int = 0
        var backspaceCalls: Int = 0

        override fun textBeforeCursor(maxCharacters: Int): CharSequence = content.takeLast(maxCharacters)

        override fun commit(text: String): Boolean {
            content += text
            return true
        }

        override fun deleteBeforeCursor(characters: Int): Boolean {
            deleteCalls += 1
            content = content.dropLast(characters)
            return true
        }

        override fun backspace(): Boolean {
            backspaceCalls += 1
            content = content.dropLast(1)
            return true
        }

        override fun allText(maxCharacters: Int): CharSequence? =
            content.takeIf { it.length <= maxCharacters }

        override fun replaceAll(replacement: String): Boolean {
            content = replacement
            return true
        }
    }
}
