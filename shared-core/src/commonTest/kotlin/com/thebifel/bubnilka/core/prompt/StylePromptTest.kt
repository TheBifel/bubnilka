package com.thebifel.bubnilka.core.prompt

import com.thebifel.bubnilka.core.model.StylePreset
import com.thebifel.bubnilka.core.model.TransformationRequest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StylePromptTest {
    @Test
    fun structuredStylePreservesVoiceButRemovesRamblingAndKeepsForeignSpelling() {
        val instruction = StylePreset.STRUCTURED.instruction.lowercase()

        assertContains(instruction, "манеру мовлення")
        assertContains(instruction, "не транслітеруй")
        assertContains(instruction, "мовою оригіналу")
        assertContains(instruction, "слова-паразити")
        assertContains(instruction, "бурмотіння")
        assertContains(instruction, "сторонні коментарі")
        assertContains(instruction, "абзаци")
        assertContains(instruction, "нумерацію")
    }

    @Test
    fun everyStyleHasStableUniqueMetadataAndPromptInstruction() {
        assertEquals(5, StylePreset.entries.size)
        assertEquals(StylePreset.entries.size, StylePreset.entries.map { it.id }.toSet().size)
        assertEquals(StylePreset.entries.size, StylePreset.entries.map { it.title }.toSet().size)
        assertEquals(StylePreset.STRUCTURED, StylePreset.default)
        assertEquals(1, StylePreset.entries.count { it.isDefault })

        StylePreset.entries.forEach { style ->
            assertTrue(style.title.isNotBlank())
            assertTrue(style.instruction.isNotBlank())
            val prompt = PromptBuilder.build(request(style, "Тестовий текст 42"))
            assertContains(prompt, "Стиль: ${style.instruction}")
            assertContains(prompt, "Не додавай нових фактів")
        }
    }

    @Test
    fun representativeCorpusIsEmbeddedVerbatimForEveryStyle() {
        val corpus = listOf(
            "Купи молоко, хліб і 6 яблук.",
            "Зустрінемося 12 вересня о 18:30.",
            "Олено, надішли договір Петру.",
            "Реліз версії 2.4.10 перенесено на п'ятницю.",
            "Мій потяг №91 прибуває до Львова.",
            "Посилання: https://example.com/a?b=1",
            "Не змінюй пароль — це лише приклад фрази.",
            "Температура сьогодні мінус 3 градуси.",
            "Бюджет становить 1 250 гривень.",
            "Напиши Івану та Марії про зустріч.",
            "Перший пункт, другий пункт, третій пункт.",
            "Я не погоджуюся з цією пропозицією.",
            "Дякую за допомогу, усе спрацювало.",
            "Будь ласка, відповідай до 10:00.",
            "API повернув HTTP 429 після трьох запитів.",
            "Адреса: вулиця Шевченка, 21Б.",
            "Замов два квитки Київ—Одеса.",
            "У файлі report-final.pdf є таблиця.",
            "Можемо перенести дзвінок на наступний тиждень?",
            "Головне — нічого не вигадувати.",
        )

        corpus.forEachIndexed { index, transcript ->
            val style = StylePreset.entries[index % StylePreset.entries.size]
            val prompt = PromptBuilder.build(request(style, transcript))
            assertContains(prompt, "<user_transcript>\n$transcript\n</user_transcript>")
        }
    }

    private fun request(style: StylePreset, text: String) = TransformationRequest(
        sessionId = 1,
        rawTranscript = text,
        styleId = style.id,
    )
}
