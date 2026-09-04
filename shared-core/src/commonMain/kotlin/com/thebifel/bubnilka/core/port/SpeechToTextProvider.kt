package com.thebifel.bubnilka.core.port

import com.thebifel.bubnilka.core.model.KeyboardFailure

sealed interface SpeechEvent {
    val sessionId: Long

    data class Ready(override val sessionId: Long) : SpeechEvent
    data class ListeningStarted(override val sessionId: Long) : SpeechEvent
    data class Partial(override val sessionId: Long, val text: String) : SpeechEvent
    data class AudioLevel(override val sessionId: Long, val level: Float) : SpeechEvent
    data class Final(override val sessionId: Long, val text: String) : SpeechEvent
    data class EndOfSpeech(override val sessionId: Long) : SpeechEvent
    data class Failure(
        override val sessionId: Long,
        val category: KeyboardFailure,
    ) : SpeechEvent
    data class Cancelled(override val sessionId: Long) : SpeechEvent
}

interface SpeechToTextProvider {
    val isAvailable: Boolean

    fun start(sessionId: Long, languageTag: String, onEvent: (SpeechEvent) -> Unit)

    fun cancel(sessionId: Long)

    fun close()
}
