package com.thebifel.bubnilka.core.port

interface TextInsertionPort {
    fun insert(text: String): Boolean

    fun matchesBeforeCursor(text: String): Boolean

    fun replace(previous: String, replacement: String): Boolean

    fun undo(previous: String): Boolean

    fun readAllText(): String? = null

    fun replaceAll(replacement: String): Boolean = false
}
