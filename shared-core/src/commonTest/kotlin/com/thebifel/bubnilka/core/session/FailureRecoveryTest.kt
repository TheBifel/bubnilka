package com.thebifel.bubnilka.core.session

import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.RecoveryAction
import com.thebifel.bubnilka.core.model.TransformationRequest
import com.thebifel.bubnilka.core.model.TransformationResult
import com.thebifel.bubnilka.core.port.CloudTransformException
import com.thebifel.bubnilka.core.port.CloudTransformProvider
import com.thebifel.bubnilka.core.port.SpeechEvent
import com.thebifel.bubnilka.core.port.SpeechToTextProvider
import com.thebifel.bubnilka.core.port.TextInsertionPort
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FailureRecoveryTest {
    @Test
    fun quotaFailurePreservesRawTextWithoutAutomaticRetry() = runTest {
        val speech = EmittingSpeech()
        val cloud = QueueCloud().apply {
            outcomes += Result.failure(CloudTransformException(KeyboardFailure.QUOTA_EXCEEDED))
        }
        val insertion = MemoryInsertion()
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, cloud, insertion, this)

        val id = flow.start()
        speech.emit(SpeechEvent.Final(id, "важливий сирий текст"))
        advanceUntilIdle()

        val failure = assertIs<KeyboardState.Failure>(session.state)
        assertEquals("важливий сирий текст", failure.rawTranscript)
        assertEquals(KeyboardFailure.QUOTA_EXCEEDED, failure.failure)
        assertEquals(setOf(RecoveryAction.RETRY, RecoveryAction.INSERT_RAW, RecoveryAction.DISMISS), failure.actions)
        assertEquals(1, cloud.calls)
        assertEquals("", insertion.content)
    }

    @Test
    fun retryTransformsStoredRawTextWithoutRecordingAgain() = runTest {
        val speech = EmittingSpeech()
        val cloud = QueueCloud().apply {
            outcomes += Result.failure(CloudTransformException(KeyboardFailure.NETWORK_UNAVAILABLE))
            outcomes += Result.success("відновлений текст")
        }
        val insertion = MemoryInsertion()
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, cloud, insertion, this)
        val id = flow.start()
        speech.emit(SpeechEvent.Final(id, "сирий текст"))
        advanceUntilIdle()

        assertTrue(flow.retry())
        advanceUntilIdle()

        assertEquals(1, speech.startCalls)
        assertEquals(2, cloud.calls)
        assertEquals("відновлений текст", insertion.content)
        assertIs<KeyboardState.Inserted>(session.state)
    }

    @Test
    fun rawTextIsInsertedOnlyAfterExplicitAction() = runTest {
        val speech = EmittingSpeech()
        val cloud = QueueCloud().apply {
            outcomes += Result.failure(CloudTransformException(KeyboardFailure.NETWORK_UNAVAILABLE))
        }
        val insertion = MemoryInsertion()
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, cloud, insertion, this)
        val id = flow.start()
        speech.emit(SpeechEvent.Final(id, "встав мене як є"))
        advanceUntilIdle()
        assertEquals("", insertion.content)

        assertTrue(flow.insertRaw())

        assertEquals("встав мене як є", insertion.content)
        assertIs<KeyboardState.Inserted>(session.state)
    }

    @Test
    fun rawFallbackAddsASeparatorInCombinedMode() = runTest {
        val speech = EmittingSpeech()
        val cloud = QueueCloud().apply {
            outcomes += Result.failure(CloudTransformException(KeyboardFailure.NETWORK_UNAVAILABLE))
        }
        val insertion = MemoryInsertion().apply { content = "Вже є речення." }
        val flow = VoiceFlow(KeyboardSession(), speech, cloud, insertion, this)

        val id = flow.start()
        speech.emit(SpeechEvent.Final(id, "Нове речення."))
        advanceUntilIdle()
        assertTrue(flow.insertRaw())

        assertEquals("Вже є речення. Нове речення.", insertion.content)
    }

    private class EmittingSpeech : SpeechToTextProvider {
        private var listener: ((SpeechEvent) -> Unit)? = null
        var startCalls = 0
        override val isAvailable = true
        override fun start(sessionId: Long, languageTag: String, onEvent: (SpeechEvent) -> Unit) {
            startCalls += 1
            listener = onEvent
        }
        override fun cancel(sessionId: Long) = Unit
        override fun close() = Unit
        fun emit(event: SpeechEvent) = listener?.invoke(event)
    }

    private class QueueCloud : CloudTransformProvider {
        val outcomes = ArrayDeque<Result<String>>()
        var calls = 0
        override suspend fun transform(request: TransformationRequest): TransformationResult {
            calls += 1
            val text = outcomes.removeFirst().getOrThrow()
            return TransformationResult(request.sessionId, request.styleId, text)
        }
    }

    private class MemoryInsertion : TextInsertionPort {
        var content = ""
        override fun insert(text: String): Boolean { content += text; return true }
        override fun matchesBeforeCursor(text: String) = content.endsWith(text)
        override fun replace(previous: String, replacement: String): Boolean = false
        override fun undo(previous: String): Boolean = false
        override fun readAllText(): String = content
        override fun replaceAll(replacement: String): Boolean { content = replacement; return true }
    }
}
