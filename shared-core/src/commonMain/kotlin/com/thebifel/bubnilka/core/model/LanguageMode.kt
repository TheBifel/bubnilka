package com.thebifel.bubnilka.core.model

const val AUTO_DETECT_LANGUAGE_TAG: String = "auto"

enum class LanguageMode(
    val id: String,
    val recognitionLanguageTag: String,
    val targetLanguageTag: String,
) {
    UKRAINIAN(
        id = "ukrainian",
        recognitionLanguageTag = "uk-UA",
        targetLanguageTag = "uk-UA",
    ),
    ENGLISH(
        id = "english",
        recognitionLanguageTag = "en-US",
        targetLanguageTag = "en-US",
    ),
    ;

    companion object {
        val default: LanguageMode = UKRAINIAN

        fun fromId(id: String?): LanguageMode? = when (id) {
            "ukrainian", "english_to_ukrainian" -> UKRAINIAN
            "english", "ukrainian_to_english" -> ENGLISH
            else -> null
        }

        fun supports(languageTag: String): Boolean = entries.any { it.targetLanguageTag == languageTag }
    }
}
