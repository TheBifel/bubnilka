package com.thebifel.bubnilka.speech

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.ModelDownloadListener
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.port.SpeechEvent
import com.thebifel.bubnilka.core.port.SpeechToTextProvider
import com.thebifel.bubnilka.runtime.BubnilkaLog

internal enum class RecognitionMode {
    ON_DEVICE,
    SYSTEM,
}

internal fun selectRecognitionMode(
    onDeviceAvailable: Boolean,
    systemAvailable: Boolean,
): RecognitionMode? = when {
    onDeviceAvailable -> RecognitionMode.ON_DEVICE
    systemAvailable -> RecognitionMode.SYSTEM
    else -> null
}

class AndroidSpeechToTextProvider(
    private val context: Context,
) : SpeechToTextProvider {
    private var recognizer: SpeechRecognizer? = null
    private var modelDownloadRecognizer: SpeechRecognizer? = null
    private val requestedModelDownloads = mutableSetOf<String>()
    private var activeSessionId: Long? = null
    private var activeAttemptId: Long = 0
    private var listener: ((SpeechEvent) -> Unit)? = null
    private var finalDelivered: Boolean = false

    override val isAvailable: Boolean
        get() = recognitionMode() != null

    override fun start(
        sessionId: Long,
        languageTag: String,
        onEvent: (SpeechEvent) -> Unit,
    ) {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onEvent(SpeechEvent.Failure(sessionId, KeyboardFailure.PERMISSION_DENIED))
            return
        }
        val mode = recognitionMode()
        if (mode == null) {
            onEvent(SpeechEvent.Failure(sessionId, KeyboardFailure.SPEECH_RECOGNITION_UNAVAILABLE))
            return
        }

        activeSessionId?.let(::cancel)
        activeSessionId = sessionId
        listener = onEvent
        finalDelivered = false
        startRecognizerWithFallback(sessionId, languageTag, mode)
    }

    override fun cancel(sessionId: Long) {
        if (activeSessionId != sessionId) return
        recognizer?.cancel()
        emitIfActive(sessionId, activeAttemptId, SpeechEvent.Cancelled(sessionId))
        clearSession(sessionId)
    }

    override fun close() {
        activeSessionId?.let { sessionId ->
            recognizer?.cancel()
            clearSession(sessionId)
        }
        recognizer?.destroy()
        recognizer = null
        modelDownloadRecognizer?.destroy()
        modelDownloadRecognizer = null
    }

    private fun recognitionMode(): RecognitionMode? = selectRecognitionMode(
        onDeviceAvailable = SpeechRecognizer.isOnDeviceRecognitionAvailable(context),
        systemAvailable = SpeechRecognizer.isRecognitionAvailable(context),
    )

    private fun startRecognizerWithFallback(
        sessionId: Long,
        languageTag: String,
        mode: RecognitionMode,
    ) {
        try {
            startRecognizer(sessionId, languageTag, mode)
        } catch (failure: RuntimeException) {
            if (mode == RecognitionMode.ON_DEVICE && SpeechRecognizer.isRecognitionAvailable(context)) {
                BubnilkaLog.state("speech_fallback_system_after_start_failure")
                startSystemRecognizerOrFail(sessionId, languageTag)
            } else {
                BubnilkaLog.failure("speech_recognizer_start", failure.javaClass.simpleName)
                emitIfActive(
                    sessionId,
                    activeAttemptId,
                    SpeechEvent.Failure(sessionId, KeyboardFailure.SPEECH_RECOGNITION_UNAVAILABLE),
                )
                clearSession(sessionId)
            }
        }
    }

    private fun startRecognizer(
        sessionId: Long,
        languageTag: String,
        mode: RecognitionMode,
    ) {
        recognizer?.destroy()
        activeAttemptId += 1
        val attemptId = activeAttemptId
        val speechRecognizer = when (mode) {
            RecognitionMode.ON_DEVICE -> SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
            RecognitionMode.SYSTEM -> SpeechRecognizer.createSpeechRecognizer(context)
        }
        recognizer = speechRecognizer
        speechRecognizer.setRecognitionListener(
            SessionRecognitionListener(sessionId, attemptId, languageTag, mode),
        )
        speechRecognizer.startListening(recognitionIntent(languageTag, mode == RecognitionMode.ON_DEVICE))
        BubnilkaLog.state(
            if (mode == RecognitionMode.ON_DEVICE) "speech_started_on_device" else "speech_started_system",
        )
    }

    private fun startSystemRecognizerOrFail(sessionId: Long, languageTag: String) {
        try {
            startRecognizer(sessionId, languageTag, RecognitionMode.SYSTEM)
        } catch (failure: RuntimeException) {
            BubnilkaLog.failure("system_speech_recognizer_start", failure.javaClass.simpleName)
            emitIfActive(
                sessionId,
                activeAttemptId,
                SpeechEvent.Failure(sessionId, KeyboardFailure.SPEECH_RECOGNITION_UNAVAILABLE),
            )
            clearSession(sessionId)
        }
    }

    private fun recognitionIntent(languageTag: String, preferOffline: Boolean): Intent =
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }

    private inner class SessionRecognitionListener(
        private val sessionId: Long,
        private val attemptId: Long,
        private val languageTag: String,
        private val mode: RecognitionMode,
    ) : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            emitIfActive(sessionId, attemptId, SpeechEvent.Ready(sessionId))
        }

        override fun onBeginningOfSpeech() {
            emitIfActive(sessionId, attemptId, SpeechEvent.ListeningStarted(sessionId))
        }

        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            emitIfActive(sessionId, attemptId, SpeechEvent.EndOfSpeech(sessionId))
        }

        override fun onError(error: Int) {
            if (!isActive(sessionId, attemptId)) return
            if (mode == RecognitionMode.ON_DEVICE && shouldFallbackToSystem(error) &&
                SpeechRecognizer.isRecognitionAvailable(context)
            ) {
                BubnilkaLog.state("speech_fallback_system_after_on_device_error")
                startSystemRecognizerOrFail(sessionId, languageTag)
                return
            }
            if (mode == RecognitionMode.SYSTEM) requestOfflineModel(languageTag)
            emitIfActive(sessionId, attemptId, SpeechEvent.Failure(sessionId, mapRecognitionError(error)))
            clearSession(sessionId)
        }

        override fun onResults(results: Bundle?) {
            if (finalDelivered || !isActive(sessionId, attemptId)) return
            val text = firstResult(results)
            if (text.isNullOrBlank()) {
                emitIfActive(sessionId, attemptId, SpeechEvent.Failure(sessionId, KeyboardFailure.NO_SPEECH))
            } else {
                finalDelivered = true
                emitIfActive(sessionId, attemptId, SpeechEvent.Final(sessionId, text))
            }
            clearSession(sessionId)
            if (mode == RecognitionMode.SYSTEM) requestOfflineModel(languageTag)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val text = firstResult(partialResults) ?: return
            if (text.isNotBlank()) emitIfActive(sessionId, attemptId, SpeechEvent.Partial(sessionId, text))
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun requestOfflineModel(languageTag: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            !requestedModelDownloads.add(languageTag) ||
            !SpeechRecognizer.isRecognitionAvailable(context)
        ) {
            return
        }

        try {
            modelDownloadRecognizer?.destroy()
            val downloader = SpeechRecognizer.createSpeechRecognizer(context)
            modelDownloadRecognizer = downloader
            val intent = recognitionIntent(languageTag, preferOffline = true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                triggerModelDownloadWithListener(downloader, intent)
            } else {
                downloader.triggerModelDownload(intent)
                BubnilkaLog.state("speech_model_download_requested")
            }
        } catch (failure: RuntimeException) {
            BubnilkaLog.failure("speech_model_download", failure.javaClass.simpleName)
            releaseModelDownloadRecognizer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun triggerModelDownloadWithListener(downloader: SpeechRecognizer, intent: Intent) {
        downloader.triggerModelDownload(
            intent,
            context.mainExecutor,
            object : ModelDownloadListener {
                override fun onProgress(completedPercent: Int) = Unit

                override fun onSuccess() {
                    BubnilkaLog.state("speech_model_download_ready")
                    releaseModelDownloadRecognizer(downloader)
                }

                override fun onScheduled() {
                    BubnilkaLog.state("speech_model_download_scheduled")
                    releaseModelDownloadRecognizer(downloader)
                }

                override fun onError(error: Int) {
                    BubnilkaLog.failure("speech_model_download_error_$error")
                    releaseModelDownloadRecognizer(downloader)
                }
            },
        )
    }

    private fun releaseModelDownloadRecognizer(expected: SpeechRecognizer? = null) {
        val downloader = modelDownloadRecognizer ?: return
        if (expected != null && downloader !== expected) return
        downloader.destroy()
        modelDownloadRecognizer = null
    }

    private fun firstResult(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()

    private fun isActive(sessionId: Long, attemptId: Long): Boolean =
        activeSessionId == sessionId && activeAttemptId == attemptId

    private fun emitIfActive(sessionId: Long, attemptId: Long, event: SpeechEvent) {
        if (isActive(sessionId, attemptId)) listener?.invoke(event)
    }

    private fun clearSession(sessionId: Long) {
        if (activeSessionId != sessionId) return
        activeSessionId = null
        listener = null
    }

    companion object {
        fun shouldFallbackToSystem(error: Int): Boolean = when (error) {
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            -> true
            else -> false
        }

        fun mapRecognitionError(error: Int): KeyboardFailure = when (error) {
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> KeyboardFailure.PERMISSION_DENIED
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
            -> KeyboardFailure.NO_SPEECH
            SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
            SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
            SpeechRecognizer.ERROR_SERVER_DISCONNECTED,
            -> KeyboardFailure.SPEECH_RECOGNITION_UNAVAILABLE
            else -> KeyboardFailure.RECOGNITION_ERROR
        }
    }
}
