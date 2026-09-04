package com.thebifel.bubnilka.ime

import org.junit.Assert.assertEquals
import org.junit.Test

class BackspaceDeletionTest {
    @Test
    fun deletesAWholeWord() {
        assertEquals(5, BackspaceDeletion.codePointCount("hello"))
        assertEquals(6, BackspaceDeletion.codePointCount("hello "))
        assertEquals(5, BackspaceDeletion.codePointCount("Привіт, слово"))
    }

    @Test
    fun deletesOnePunctuationMarkWithTrailingWhitespace() {
        assertEquals(1, BackspaceDeletion.codePointCount("Привіт!"))
        assertEquals(2, BackspaceDeletion.codePointCount("Привіт! "))
        assertEquals(1, BackspaceDeletion.codePointCount("Що?!"))
    }

    @Test
    fun treatsApostrophesAndHyphensAsPartOfWords() {
        assertEquals(5, BackspaceDeletion.codePointCount("п'ять"))
        assertEquals(9, BackspaceDeletion.codePointCount("будь-який"))
    }

    @Test
    fun emptyInputDeletesNothingAndWhitespaceIsOneBlock() {
        assertEquals(0, BackspaceDeletion.codePointCount(""))
        assertEquals(3, BackspaceDeletion.codePointCount("   "))
    }
}
