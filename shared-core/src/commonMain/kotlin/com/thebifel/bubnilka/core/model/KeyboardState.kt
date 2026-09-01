package com.thebifel.bubnilka.core.model

sealed interface KeyboardState {
    data class Idle(val selectedStyle: StylePreset) : KeyboardState

    data class Listening(
        val sessionId: Long,
        val partialTranscript: String,
        val selectedStyle: StylePreset,
    ) : KeyboardState

    data class Transforming(
        val sessionId: Long,
        val rawTranscript: String,
        val selectedStyle: StylePreset,
    ) : KeyboardState

    data class Inserted(
        val sessionId: Long,
        val rawTranscript: String,
        val transformedText: String,
        val selectedStyle: StylePreset,
    ) : KeyboardState

    data class Failure(
        val sessionId: Long?,
        val rawTranscript: String?,
        val failure: KeyboardFailure,
        val actions: Set<RecoveryAction>,
        val selectedStyle: StylePreset,
    ) : KeyboardState

    data object SensitiveField : KeyboardState
}
