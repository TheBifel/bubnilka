package com.thebifel.bubnilka.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.port.SpeechEvent
import com.thebifel.bubnilka.core.port.SpeechToTextProvider

class AndroidOnDeviceSpeechToTextProvider(
    private val context: Context,
) : SpeechToTextProvider {
    private var recognizer: SpeechRecognizer? = null
    private var activeSessionId: Long? = null
    private var listener: ((SpeechEvent) -> Unit)? = null
    private var finalDelivered: Boolean = false

    override val isAvailable: Boolean
        get() = SpeechRecognizer.isOnDeviceRecognitionAvailable(context)

    override fun start(
        sessionId: Long,
        languageTag: String,
        onEvent: (SpeechEvent) -> Unit,
    ) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onEvent(SpeechEvent.Failure(sessionId, KeyboardFailure.PERMISSION_DENIED))
            return
        }
        if (!isAvailable) {
            onEvent(SpeechEvent.Failure(sessionId, KeyboardFailure.UNSUPPORTED_ON_DEVICE_RECOGNITION))
            return
        }

        activeSessionId?.let(::cancel)
        activeSessionId = sessionId
        listener = onEvent
        finalDelivered = false

        val speechRecognizer = recognizer ?: SpeechRecognizer
            .createOnDeviceSpeechRecognizer(context)
            .also { recognizer = it }
        speechRecognizer.setRecognitionListener(SessionRecognitionListener(sessionId))
        speechRecognizer.startListening(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            },
        )
    }

    override fun cancel(sessionId: Long) {
        if (activeSessionId != sessionId) return
        recognizer?.cancel()
        emitIfActive(sessionId, SpeechEvent.Cancelled(sessionId))
        clearSession(sessionId)
    }

    override fun close() {
        activeSessionId?.let { sessionId ->
            recognizer?.cancel()
            clearSession(sessionId)
        }
        recognizer?.destroy()
        recognizer = null
    }

    private inner class SessionRecognitionListener(
        private val sessionId: Long,
    ) : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            emitIfActive(sessionId, SpeechEvent.Ready(sessionId))
        }

        override fun onBeginningOfSpeech() {
            emitIfActive(sessionId, SpeechEvent.ListeningStarted(sessionId))
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            emitIfActive(sessionId, SpeechEvent.EndOfSpeech(sessionId))
        }

        override fun onError(error: Int) {
            emitIfActive(sessionId, SpeechEvent.Failure(sessionId, mapRecognitionError(error)))
            clearSession(sessionId)
        }

        override fun onResults(results: Bundle?) {
            if (finalDelivered || activeSessionId != sessionId) return
            val text = firstResult(results)
            if (text.isNullOrBlank()) {
                emitIfActive(sessionId, SpeechEvent.Failure(sessionId, KeyboardFailure.NO_SPEECH))
            } else {
                finalDelivered = true
                emitIfActive(sessionId, SpeechEvent.Final(sessionId, text))
            }
            clearSession(sessionId)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults) ?: return
            if (text.isNotBlank()) emitIfActive(sessionId, SpeechEvent.Partial(sessionId, text))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun emitIfActive(sessionId: Long, event: SpeechEvent) {
        if (activeSessionId == sessionId) listener?.invoke(event)
    }

    private fun clearSession(sessionId: Long) {
        if (activeSessionId != sessionId) return
        activeSessionId = null
        listener = null
    }

    companion object {
        fun mapRecognitionError(error: Int): KeyboardFailure = when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> KeyboardFailure.PERMISSION_DENIED
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            -> KeyboardFailure.NO_SPEECH
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            -> KeyboardFailure.UNSUPPORTED_ON_DEVICE_RECOGNITION
            else -> KeyboardFailure.RECOGNITION_ERROR
        }
    }
}
