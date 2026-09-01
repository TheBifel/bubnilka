package com.thebifel.bubnilka.core.model

const val MAX_TRANSCRIPT_LENGTH: Int = 2_000
const val MAX_TRANSFORMED_LENGTH: Int = 4_000

data class TransformationRequest(
    val sessionId: Long,
    val rawTranscript: String,
    val styleId: String,
    val languageTag: String = "uk-UA",
) {
    init {
        require(sessionId > 0)
        require(rawTranscript.isNotBlank())
        require(rawTranscript.length <= MAX_TRANSCRIPT_LENGTH)
        require(rawTranscript == rawTranscript.trim())
        require(StylePreset.fromId(styleId) != null)
        require(languageTag.isNotBlank())
    }
}

data class TransformationResult(
    val sessionId: Long,
    val styleId: String,
    val text: String,
) {
    fun normalizedTextOrNull(): String? {
        val normalized = text.trim()
        if (sessionId <= 0 || StylePreset.fromId(styleId) == null) return null
        if (normalized.isEmpty() || normalized.length > MAX_TRANSFORMED_LENGTH) return null
        if (ASSISTANT_PREAMBLES.any { normalized.startsWith(it, ignoreCase = true) }) return null
        return normalized
    }

    private companion object {
        val ASSISTANT_PREAMBLES = listOf(
            "ось ваш текст",
            "ось перероблений текст",
            "звичайно",
            "certainly",
            "here is",
        )
    }
}
