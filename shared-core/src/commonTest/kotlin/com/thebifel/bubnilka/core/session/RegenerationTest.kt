package com.thebifel.bubnilka.core.session

import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.StylePreset
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RegenerationTest {
    @Test
    fun regeneratesStoredRawTextAndSafelyReplacesLastInsertion() = runTest {
        val insertion = RecordingInsertionPort()
        val cloud = StyleEchoCloud()
        val session = insertedSession(insertion)
        val flow = VoiceFlow(session, ImmediateSpeech(), cloud, insertion, this)

        session.selectStyle(StylePreset.VIKING)
        assertTrue(flow.regenerate())
        advanceUntilIdle()

        assertEquals("viking: сирий текст", insertion.content)
        val state = assertIs<KeyboardState.Inserted>(session.state)
        assertEquals(StylePreset.VIKING, state.selectedStyle)
        assertEquals("сирий текст", state.rawTranscript)
    }

    @Test
    fun regenerationReplacesTheWholeFieldEvenWhenItChanged() = runTest {
        val insertion = RecordingInsertionPort()
        val session = insertedSession(insertion)
        val flow = VoiceFlow(session, ImmediateSpeech(), StyleEchoCloud(), insertion, this)
        insertion.content = "поле вже змінив користувач"

        assertTrue(flow.regenerate())
        advanceUntilIdle()

        assertEquals("structured: сирий текст", insertion.content)
        assertIs<KeyboardState.Inserted>(session.state)
    }

    @Test
    fun undoRestoresTheOriginalWholeFieldAndClearsRecord() {
        val insertion = RecordingInsertionPort()
        val session = insertedSession(insertion)
        val flow = VoiceFlow(session, ImmediateSpeech(), StyleEchoCloud(), insertion, kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined))

        assertTrue(flow.undo())

        assertEquals("", insertion.content)
        assertNull(session.lastInsertion)
        assertIs<KeyboardState.Idle>(session.state)
        assertFalse(flow.undo())
    }

    private fun insertedSession(insertion: RecordingInsertionPort): KeyboardSession {
        val session = KeyboardSession()
        val id = session.startListening(existingFieldText = "")
        session.onFinalTranscript(id, "сирий текст")
        val result = TransformationResult(id, StylePreset.STRUCTURED.id, "structured: сирий текст")
        insertion.replaceAll(result.text)
        session.onInserted(result)
        return session
    }

    private class ImmediateSpeech : SpeechToTextProvider {
        override val isAvailable = true
        override fun start(sessionId: Long, languageTag: String, onEvent: (SpeechEvent) -> Unit) = Unit
        override fun cancel(sessionId: Long) = Unit
        override fun close() = Unit
    }

    private class StyleEchoCloud : CloudTransformProvider {
        override suspend fun transform(request: TransformationRequest) = TransformationResult(
            request.sessionId,
            request.styleId,
            "${request.styleId}: ${request.rawTranscript}",
        )
    }

    private class RecordingInsertionPort : TextInsertionPort {
        var content: String = ""

        override fun insert(text: String): Boolean {
            content += text
            return true
        }

        override fun matchesBeforeCursor(text: String): Boolean = content.endsWith(text)

        override fun replace(previous: String, replacement: String): Boolean {
            if (!matchesBeforeCursor(previous)) return false
            content = content.dropLast(previous.length) + replacement
            return true
        }

        override fun undo(previous: String): Boolean {
            if (!matchesBeforeCursor(previous)) return false
            content = content.dropLast(previous.length)
            return true
        }

        override fun readAllText(): String = content

        override fun replaceAll(replacement: String): Boolean {
            content = replacement
            return true
        }
    }

}
