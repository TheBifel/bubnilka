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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceFlowTest {
    @Test
    fun startsOneWarmUpImmediatelyAndSendsDictationOnlyAfterItCompletes() = runTest {
        val speech = FakeSpeechProvider()
        val cloud = BlockingWarmUpCloud()
        val flow = VoiceFlow(KeyboardSession(), speech, cloud, FakeInsertionPort(), this)

        val firstSession = flow.start()
        advanceUntilIdle()
        assertEquals(1, cloud.warmUpCalls)
        assertEquals(0, cloud.transformCalls)

        speech.emit(SpeechEvent.Final(firstSession, "фінальний текст"))
        runCurrent()
        assertEquals(0, cloud.transformCalls)

        cloud.warmUpCompleted.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, cloud.transformCalls)

        flow.start()
        advanceUntilIdle()
        assertEquals(1, cloud.warmUpCalls)
    }

    @Test
    fun warmUpFailureDoesNotBlockTheDictatedTransformation() = runTest {
        val speech = FakeSpeechProvider()
        val cloud = FailingWarmUpCloud()
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, cloud, FakeInsertionPort(), this)

        val sessionId = flow.start()
        speech.emit(SpeechEvent.Final(sessionId, "фінальний текст"))
        advanceUntilIdle()

        assertEquals(1, cloud.warmUpCalls)
        assertEquals(1, cloud.transformCalls)
        assertIs<KeyboardState.Inserted>(session.state)
    }

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

        assertEquals("Оформлено: фінальний текст", insertion.content)
        assertEquals("", cloud.lastRequest?.existingFieldText)
        assertEquals(0, insertion.insertCalls)
        assertEquals(1, insertion.replaceAllCalls)
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
    fun selectedOutputLanguageIsAlsoRecognitionFallback() = runTest {
        val speech = FakeSpeechProvider()
        val session = KeyboardSession()
        session.selectLanguageMode(LanguageMode.ENGLISH)
        val flow = VoiceFlow(session, speech, FakeCloudProvider(), FakeInsertionPort(), this)

        flow.start()

        assertEquals("en-US", speech.lastLanguageTag)
    }

    @Test
    fun routesActiveAudioLevelToUiAndIgnoresStaleSessions() = runTest {
        val speech = FakeSpeechProvider()
        var observedLevel = 0f
        val flow = VoiceFlow(
            session = KeyboardSession(),
            speech = speech,
            cloud = FakeCloudProvider(),
            insertion = FakeInsertionPort(),
            scope = this,
            onAudioLevelChanged = { observedLevel = it },
        )

        val sessionId = flow.start()
        speech.emit(SpeechEvent.AudioLevel(sessionId, 0.75f))
        assertEquals(0.75f, observedLevel)

        speech.emit(SpeechEvent.AudioLevel(sessionId + 1, 0.25f))
        assertEquals(0.75f, observedLevel)
    }

    @Test
    fun disabledFieldCombinationTransformsOnlyDictationAndInsertsAtCursor() = runTest {
        val speech = FakeSpeechProvider()
        val cloud = FakeCloudProvider()
        val insertion = FakeInsertionPort().apply { content = "Наявний текст " }
        val flow = VoiceFlow(KeyboardSession(), speech, cloud, insertion, this)

        val sessionId = flow.start(includeFieldContext = false)
        speech.emit(SpeechEvent.Final(sessionId, "нове речення"))
        advanceUntilIdle()

        assertNull(cloud.lastRequest?.existingFieldText)
        assertEquals(1, insertion.insertCalls)
        assertEquals(0, insertion.replaceAllCalls)
        assertEquals("Наявний текст Оформлено: нове речення", insertion.content)
    }

    @Test
    fun wholeFieldModeSendsContextAndSafelyReplacesEntireEditor() = runTest {
        val speech = FakeSpeechProvider()
        val cloud = FakeCloudProvider()
        val insertion = FakeInsertionPort().apply { content = "Перший абзац." }
        val flow = VoiceFlow(KeyboardSession(), speech, cloud, insertion, this)

        val sessionId = flow.start()
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
    fun wholeFieldModeReplacesConcurrentEditorChangesWithUnifiedResult() = runTest {
        val speech = FakeSpeechProvider()
        val insertion = FakeInsertionPort().apply { content = "Початковий текст" }
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, FakeCloudProvider(), insertion, this)

        val sessionId = flow.start()
        speech.emit(SpeechEvent.Final(sessionId, "Доповнення"))
        insertion.content = "Користувач уже змінив поле"
        advanceUntilIdle()

        assertEquals("Оформлено: Доповнення", insertion.content)
        assertIs<KeyboardState.Inserted>(session.state)
    }

    @Test
    fun unavailableWholeFieldPreventsRecognitionAndCloudWork() = runTest {
        val speech = FakeSpeechProvider()
        val cloud = FakeCloudProvider()
        val insertion = FakeInsertionPort().apply { canReadAllText = false }
        val session = KeyboardSession()
        val flow = VoiceFlow(session, speech, cloud, insertion, this)

        flow.start()
        advanceUntilIdle()

        assertEquals(0, speech.startCalls)
        assertEquals(0, cloud.calls)
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

    private class BlockingWarmUpCloud : CloudTransformProvider {
        val warmUpCompleted = CompletableDeferred<Unit>()
        var warmUpCalls = 0
        var transformCalls = 0

        override suspend fun warmUp() {
            warmUpCalls += 1
            warmUpCompleted.await()
        }

        override suspend fun transform(request: TransformationRequest): TransformationResult {
            transformCalls += 1
            return TransformationResult(request.sessionId, request.styleId, request.rawTranscript)
        }
    }

    private class FailingWarmUpCloud : CloudTransformProvider {
        var warmUpCalls = 0
        var transformCalls = 0

        override suspend fun warmUp() {
            warmUpCalls += 1
            error("warm-up failed")
        }

        override suspend fun transform(request: TransformationRequest): TransformationResult {
            transformCalls += 1
            return TransformationResult(request.sessionId, request.styleId, request.rawTranscript)
        }
    }

    private class FakeInsertionPort : TextInsertionPort {
        var inserted: String? = null
        var content: String = ""
        var insertCalls: Int = 0
        var replaceAllCalls: Int = 0
        var canReadAllText: Boolean = true

        override fun insert(text: String): Boolean {
            inserted = text
            content += text
            insertCalls += 1
            return true
        }

        override fun matchesBeforeCursor(text: String): Boolean = false
        override fun replace(previous: String, replacement: String): Boolean = false
        override fun undo(previous: String): Boolean = false

        override fun readAllText(): String? = content.takeIf { canReadAllText }

        override fun replaceAll(replacement: String): Boolean {
            content = replacement
            replaceAllCalls += 1
            return true
        }
    }
}
