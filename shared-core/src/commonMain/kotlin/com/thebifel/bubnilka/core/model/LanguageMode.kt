package com.thebifel.bubnilka.core.model

enum class LanguageMode(
    val id: String,
    val sourceLanguageTag: String,
    val targetLanguageTag: String,
) {
    UKRAINIAN(
        id = "ukrainian",
        sourceLanguageTag = "uk-UA",
        targetLanguageTag = "uk-UA",
    ),
    ENGLISH(
        id = "english",
        sourceLanguageTag = "en-US",
        targetLanguageTag = "en-US",
    ),
    UKRAINIAN_TO_ENGLISH(
        id = "ukrainian_to_english",
        sourceLanguageTag = "uk-UA",
        targetLanguageTag = "en-US",
    ),
    ENGLISH_TO_UKRAINIAN(
        id = "english_to_ukrainian",
        sourceLanguageTag = "en-US",
        targetLanguageTag = "uk-UA",
    ),
    ;

    companion object {
        val default: LanguageMode = UKRAINIAN

        fun fromId(id: String?): LanguageMode? = entries.firstOrNull { it.id == id }

        fun supports(languageTag: String): Boolean = entries.any {
            it.sourceLanguageTag == languageTag || it.targetLanguageTag == languageTag
        }
    }
}
