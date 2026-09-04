package com.thebifel.bubnilka.core.session

import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.InsertionRecord
import com.thebifel.bubnilka.core.model.RecoveryAction
import com.thebifel.bubnilka.core.model.TransformationRequest
import com.thebifel.bubnilka.core.port.CloudTransformProvider
import com.thebifel.bubnilka.core.port.CloudTransformException
import com.thebifel.bubnilka.core.port.SpeechEvent
import com.thebifel.bubnilka.core.port.SpeechToTextProvider
import com.thebifel.bubnilka.core.port.TextInsertionPort
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class VoiceFlow(
    private val session: KeyboardSession,
    private val speech: SpeechToTextProvider,
    private val cloud: CloudTransformProvider,
    private val insertion: TextInsertionPort,
    private val scope: CoroutineScope,
    private val onStateChanged: (KeyboardState) -> Unit = {},
    private val onAudioLevelChanged: (Float) -> Unit = {},
) {
    fun start(includeFieldContext: Boolean = true): Long {
        val existingFieldText = if (includeFieldContext) insertion.readAllText() else null
        if (includeFieldContext && existingFieldText == null) {
            val sessionId = session.startListening("")
            session.fail(sessionId, KeyboardFailure.UNSAFE_REPLACEMENT)
            notifyState()
            return sessionId
        }
        val sessionId = session.startListening(existingFieldText)
        if (!speech.isAvailable) {
            session.fail(sessionId, KeyboardFailure.SPEECH_RECOGNITION_UNAVAILABLE)
            notifyState()
            return sessionId
        }
        speech.start(sessionId, session.activeSpeechLanguageTag, ::handleSpeechEvent)
        notifyState()
        return sessionId
    }

    fun cancel() {
        val sessionId = session.activeSessionId
        speech.cancel(sessionId)
        session.fail(sessionId, KeyboardFailure.CANCELLED)
        notifyState()
    }

    fun close() {
        speech.close()
        session.reset()
        notifyState()
    }

    fun regenerate(): Boolean {
        val previous = session.lastInsertion ?: return false
        val request = session.beginRegeneration() ?: return false
        notifyState()
        scope.launch {
            try {
                val result = cloud.transform(request)
                val text = session.onTransformationResult(result)
                if (text == null) {
                    session.fail(request.sessionId, KeyboardFailure.INVALID_CLOUD_RESPONSE, request.rawTranscript)
                } else if (replacePrevious(previous, text)) {
                    session.onInserted(result)
                } else {
                    session.fail(request.sessionId, KeyboardFailure.UNSAFE_REPLACEMENT, request.rawTranscript)
                }
            } catch (failure: CloudTransformException) {
                session.fail(request.sessionId, failure.category, request.rawTranscript)
            } catch (cancelled: CancellationException) {
                session.fail(request.sessionId, KeyboardFailure.CANCELLED, request.rawTranscript)
                throw cancelled
            } catch (_: Throwable) {
                session.fail(request.sessionId, KeyboardFailure.UNEXPECTED, request.rawTranscript)
            } finally {
                notifyState()
            }
        }
        return true
    }

    fun undo(): Boolean {
        val previous = session.lastInsertion ?: return false
        val succeeded = if (previous.existingFieldText == null) {
            insertion.undo(previous.insertedText)
        } else {
            insertion.replaceAll(previous.existingFieldText)
        }
        if (succeeded) {
            session.onUndoSucceeded()
        } else {
            session.fail(session.activeSessionId, KeyboardFailure.UNSAFE_REPLACEMENT, previous.rawTranscript)
        }
        notifyState()
        return succeeded
    }

    fun retry(): Boolean {
        val failure = session.state as? KeyboardState.Failure ?: return false
        if (RecoveryAction.RETRY !in failure.actions) return false
        val request = session.beginRetryTransformation()
        if (request != null) {
            notifyState()
            launchInitialTransformation(request)
        } else {
            start()
        }
        return true
    }

    fun insertRaw(): Boolean {
        val failure = session.state as? KeyboardState.Failure ?: return false
        val rawTranscript = failure.rawTranscript ?: return false
        if (RecoveryAction.INSERT_RAW !in failure.actions) return false
        val existingFieldText = session.activeExistingFieldText
        val completeText = if (existingFieldText == null) {
            rawTranscript
        } else {
            concatenate(existingFieldText, rawTranscript)
        }
        val succeeded = if (existingFieldText == null) {
            insertion.insert(completeText)
        } else {
            insertion.replaceAll(completeText)
        }
        if (succeeded) {
            session.onRawInserted(rawTranscript, completeText)
        } else {
            session.fail(session.activeSessionId, KeyboardFailure.UNEXPECTED, rawTranscript)
        }
        notifyState()
        return succeeded
    }

    private fun handleSpeechEvent(event: SpeechEvent) {
        if (event is SpeechEvent.AudioLevel) {
            if (event.sessionId == session.activeSessionId && session.state is KeyboardState.Listening) {
                onAudioLevelChanged(event.level)
            }
            return
        }
        when (event) {
            is SpeechEvent.Partial -> session.onPartialTranscript(event.sessionId, event.text)
            is SpeechEvent.Final -> transformFinal(event)
            is SpeechEvent.Failure -> session.fail(event.sessionId, event.category)
            is SpeechEvent.Cancelled -> session.fail(event.sessionId, KeyboardFailure.CANCELLED)
            is SpeechEvent.Ready,
            is SpeechEvent.ListeningStarted,
            is SpeechEvent.EndOfSpeech,
            -> Unit
            is SpeechEvent.AudioLevel -> Unit
        }
        notifyState()
    }

    private fun transformFinal(event: SpeechEvent.Final) {
        val request = session.onFinalTranscript(event.sessionId, event.text) ?: return
        launchInitialTransformation(request)
    }

    private fun launchInitialTransformation(request: TransformationRequest) {
        scope.launch {
            try {
                val result = cloud.transform(request)
                val text = session.onTransformationResult(result)
                if (text == null) {
                    session.fail(request.sessionId, KeyboardFailure.INVALID_CLOUD_RESPONSE, request.rawTranscript)
                } else if (insertTransformation(request, text)) {
                    session.onInserted(result)
                } else {
                    session.fail(
                        request.sessionId,
                        if (request.existingFieldText == null) {
                            KeyboardFailure.UNEXPECTED
                        } else {
                            KeyboardFailure.UNSAFE_REPLACEMENT
                        },
                        request.rawTranscript,
                    )
                }
            } catch (failure: CloudTransformException) {
                session.fail(request.sessionId, failure.category, request.rawTranscript)
            } catch (cancelled: CancellationException) {
                session.fail(request.sessionId, KeyboardFailure.CANCELLED, request.rawTranscript)
                throw cancelled
            } catch (_: Throwable) {
                session.fail(request.sessionId, KeyboardFailure.UNEXPECTED, request.rawTranscript)
            } finally {
                notifyState()
            }
        }
    }

    private fun notifyState() {
        onStateChanged(session.state)
    }

    private fun insertTransformation(request: TransformationRequest, text: String): Boolean =
        if (request.existingFieldText == null) {
            insertion.insert(text)
        } else {
            insertion.replaceAll(text)
        }

    private fun replacePrevious(
        previous: InsertionRecord,
        replacement: String,
    ): Boolean = if (previous.existingFieldText == null) {
        insertion.replace(previous.insertedText, replacement)
    } else {
        insertion.replaceAll(replacement)
    }

    private fun concatenate(existing: String, additional: String): String = when {
        existing.isBlank() -> additional
        existing.last().isWhitespace() -> existing + additional
        else -> "$existing $additional"
    }
}
