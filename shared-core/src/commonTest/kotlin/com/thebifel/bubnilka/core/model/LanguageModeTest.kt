package com.thebifel.bubnilka.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

class LanguageModeTest {
    @Test
    fun exposesTwoMonolingualAndTwoTranslationModes() {
        assertEquals(4, LanguageMode.entries.size)
        assertEquals(2, LanguageMode.entries.count { it.sourceLanguageTag == it.targetLanguageTag })
        assertEquals(2, LanguageMode.entries.count { it.sourceLanguageTag != it.targetLanguageTag })
        assertEquals(LanguageMode.entries.size, LanguageMode.entries.map { it.id }.toSet().size)
    }
}
