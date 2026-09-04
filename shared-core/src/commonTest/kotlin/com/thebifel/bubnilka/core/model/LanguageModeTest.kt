package com.thebifel.bubnilka.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageModeTest {
    @Test
    fun exposesOnlyTwoPersistentOutputLanguages() {
        assertEquals(2, LanguageMode.entries.size)
        assertEquals(setOf("uk-UA", "en-US"), LanguageMode.entries.map { it.targetLanguageTag }.toSet())
        assertEquals(LanguageMode.entries.size, LanguageMode.entries.map { it.id }.toSet().size)
    }

    @Test
    fun migratesLegacyTranslationModesToTheirPreviousTargetLanguage() {
        assertEquals(LanguageMode.ENGLISH, LanguageMode.fromId("ukrainian_to_english"))
        assertEquals(LanguageMode.UKRAINIAN, LanguageMode.fromId("english_to_ukrainian"))
    }
}
