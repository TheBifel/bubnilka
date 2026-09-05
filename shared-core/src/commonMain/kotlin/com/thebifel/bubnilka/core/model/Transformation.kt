package com.thebifel.bubnilka.core.model

const val MAX_TRANSCRIPT_LENGTH: Int = 2_000
const val MAX_FIELD_CONTEXT_LENGTH: Int = 6_000
const val MAX_TRANSFORMED_LENGTH: Int = 8_000

data class TransformationRequest(
    val sessionId: Long,
    val rawTranscript: String,
    val styleId: String,
    val styleInstruction: String = StylePreset.fromId(styleId)?.instruction.orEmpty(),
    val sourceLanguageTag: String = AUTO_DETECT_LANGUAGE_TAG,
    val targetLanguageTag: String = LanguageMode.default.targetLanguageTag,
    val existingFieldText: String? = null,
) {
    init {
        require(sessionId > 0)
        require(rawTranscript.isNotBlank())
        require(rawTranscript.length <= MAX_TRANSCRIPT_LENGTH)
        require(rawTranscript == rawTranscript.trim())
        require(styleId.isNotBlank() && styleId.length <= MAX_STYLE_ID_LENGTH)
        require(styleInstruction.isNotBlank() && styleInstruction.length <= MAX_STYLE_INSTRUCTION_LENGTH)
        require(sourceLanguageTag == AUTO_DETECT_LANGUAGE_TAG || LanguageMode.supports(sourceLanguageTag))
        require(LanguageMode.supports(targetLanguageTag))
        require(existingFieldText == null || existingFieldText.length <= MAX_FIELD_CONTEXT_LENGTH)
    }
}

data class TransformationResult(
    val sessionId: Long,
    val styleId: String,
    val text: String,
) {
    fun normalizedTextOrNull(): String? {
        val normalized = text.trim()
        if (sessionId <= 0 || styleId.isBlank() || styleId.length > MAX_STYLE_ID_LENGTH) return null
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
