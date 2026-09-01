package com.thebifel.bubnilka.core.session

import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.TransformationRequest
import com.thebifel.bubnilka.core.model.TransformationResult
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
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceFlowTest {
    @Test
    fun finalSpeechTransformsAndInsertsExactlyOnce() = runTest {
        val speech = FakeSpeechProvider()
        val cloud = FakeCloudProvider()
        val insertion = FakeInsertionPort()
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, cloud, insertion, this)

        val sessionId = flow.start()
        speech.emit(SpeechEvent.Partial(sessionId, "чернетка"))
        assertNull(insertion.inserted)

        speech.emit(SpeechEvent.Final(sessionId, "  фінальний текст  "))
        advanceUntilIdle()

        assertEquals("Оформлено: фінальний текст", insertion.inserted)
        assertEquals(1, insertion.insertCalls)
        assertIs<KeyboardState.Inserted>(session.state)
    }

    @Test
    fun unavailableSpeechFailsWithoutCallingCloud() = runTest {
        val speech = FakeSpeechProvider(available = false)
        val cloud = FakeCloudProvider()
        val flow = VoiceFlow(
            session = KeyboardSession(),
            speech = speech,
            cloud = cloud,
            insertion = FakeInsertionPort(),
            scope = this,
        )

        flow.start()
        advanceUntilIdle()

        assertEquals(0, cloud.calls)
        assertEquals(0, speech.startCalls)
    }

    private class FakeSpeechProvider(
        private val available: Boolean = true,
    ) : SpeechToTextProvider {
        private var listener: ((SpeechEvent) -> Unit)? = null
        var startCalls: Int = 0

        override val isAvailable: Boolean get() = available

        override fun start(sessionId: Long, languageTag: String, onEvent: (SpeechEvent) -> Unit) {
            startCalls += 1
            listener = onEvent
        }

        override fun cancel(sessionId: Long) = Unit
        override fun close() = Unit

        fun emit(event: SpeechEvent) {
            listener?.invoke(event)
        }
    }

    private class FakeCloudProvider : CloudTransformProvider {
        var calls: Int = 0

        override suspend fun transform(request: TransformationRequest): TransformationResult {
            calls += 1
            return TransformationResult(
                sessionId = request.sessionId,
                styleId = request.styleId,
                text = "Оформлено: ${request.rawTranscript}",
            )
        }
    }

    private class FakeInsertionPort : TextInsertionPort {
        var inserted: String? = null
        var insertCalls: Int = 0

        override fun insert(text: String): Boolean {
            inserted = text
            insertCalls += 1
            return true
        }

        override fun matchesBeforeCursor(text: String): Boolean = false
        override fun replace(previous: String, replacement: String): Boolean = false
        override fun undo(previous: String): Boolean = false
    }
}
