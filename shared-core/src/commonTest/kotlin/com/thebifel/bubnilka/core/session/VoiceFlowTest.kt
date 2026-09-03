package com.thebifel.bubnilka.core.session

import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.model.LanguageMode
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
import kotlin.test.assertTrue

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

    @Test
    fun englishToUkrainianUsesEnglishRecognition() = runTest {
        val speech = FakeSpeechProvider()
        val session = KeyboardSession()
        session.selectLanguageMode(LanguageMode.ENGLISH_TO_UKRAINIAN)
        val flow = VoiceFlow(session, speech, FakeCloudProvider(), FakeInsertionPort(), this)

        flow.start()

        assertEquals("en-US", speech.lastLanguageTag)
    }

    @Test
    fun wholeFieldModeSendsContextAndSafelyReplacesEntireEditor() = runTest {
        val speech = FakeSpeechProvider()
        val cloud = FakeCloudProvider()
        val insertion = FakeInsertionPort().apply { content = "Перший абзац." }
        val flow = VoiceFlow(KeyboardSession(), speech, cloud, insertion, this)

        val sessionId = flow.start(includeFieldContext = true)
        speech.emit(SpeechEvent.Final(sessionId, "Додай другий абзац."))
        advanceUntilIdle()

        assertEquals("Перший абзац.", cloud.lastRequest?.existingFieldText)
        assertEquals(1, insertion.replaceAllCalls)
        assertEquals(0, insertion.insertCalls)
        assertEquals("Оформлено: Додай другий абзац.", insertion.content)
        assertTrue(flow.undo())
        assertEquals("Перший абзац.", insertion.content)
    }

    @Test
    fun wholeFieldModeRefusesToOverwriteConcurrentUserEdit() = runTest {
        val speech = FakeSpeechProvider()
        val insertion = FakeInsertionPort().apply { content = "Початковий текст" }
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, FakeCloudProvider(), insertion, this)

        val sessionId = flow.start(includeFieldContext = true)
        speech.emit(SpeechEvent.Final(sessionId, "Доповнення"))
        insertion.content = "Користувач уже змінив поле"
        advanceUntilIdle()

        assertEquals("Користувач уже змінив поле", insertion.content)
        assertEquals(KeyboardFailure.UNSAFE_REPLACEMENT, assertIs<KeyboardState.Failure>(session.state).failure)
    }

    private class FakeSpeechProvider(
        private val available: Boolean = true,
    ) : SpeechToTextProvider {
        private var listener: ((SpeechEvent) -> Unit)? = null
        var startCalls: Int = 0
        var lastLanguageTag: String? = null

        override val isAvailable: Boolean get() = available

        override fun start(sessionId: Long, languageTag: String, onEvent: (SpeechEvent) -> Unit) {
            startCalls += 1
            lastLanguageTag = languageTag
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
        var lastRequest: TransformationRequest? = null

        override suspend fun transform(request: TransformationRequest): TransformationResult {
            calls += 1
            lastRequest = request
            return TransformationResult(
                sessionId = request.sessionId,
                styleId = request.styleId,
                text = "Оформлено: ${request.rawTranscript}",
            )
        }
    }

    private class FakeInsertionPort : TextInsertionPort {
        var inserted: String? = null
        var content: String = ""
        var insertCalls: Int = 0
        var replaceAllCalls: Int = 0

        override fun insert(text: String): Boolean {
            inserted = text
            content += text
            insertCalls += 1
            return true
        }

        override fun matchesBeforeCursor(text: String): Boolean = false
        override fun replace(previous: String, replacement: String): Boolean = false
        override fun undo(previous: String): Boolean = false

        override fun readAllText(): String = content

        override fun replaceAll(expected: String, replacement: String): Boolean {
            if (content != expected) return false
            content = replacement
            replaceAllCalls += 1
            return true
        }
    }
}
