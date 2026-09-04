package com.thebifel.bubnilka.core.session

import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.LanguageMode
import com.thebifel.bubnilka.core.model.StylePreset
import com.thebifel.bubnilka.core.model.TransformationResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KeyboardSessionTest {
    @Test
    fun startsIdleWithStructuredStyle() {
        val session = KeyboardSession()

        val state = assertIs<KeyboardState.Idle>(session.state)
        assertEquals(StylePreset.STRUCTURED, state.selectedStyle)
    }

    @Test
    fun ignoresPartialResultFromSupersededSession() {
        val session = KeyboardSession()
        val oldId = session.startListening()
        val currentId = session.startListening()

        session.onPartialTranscript(oldId, "старий текст")
        session.onPartialTranscript(currentId, "поточний текст")

        val state = assertIs<KeyboardState.Listening>(session.state)
        assertEquals(currentId, state.sessionId)
        assertEquals("поточний текст", state.partialTranscript)
    }

    @Test
    fun finalTranscriptCreatesTrimmedCloudRequest() {
        val session = KeyboardSession()
        val sessionId = session.startListening()

        val request = session.onFinalTranscript(sessionId, "  привіт світе  ")

        assertEquals("привіт світе", request?.rawTranscript)
        assertEquals(StylePreset.STRUCTURED.id, request?.styleId)
        val state = assertIs<KeyboardState.Transforming>(session.state)
        assertEquals("привіт світе", state.rawTranscript)
    }

    @Test
    fun selectedLanguageControlsFallbackRecognitionAndOutputWhileInputIsAutoDetected() {
        val session = KeyboardSession()
        session.selectLanguageMode(LanguageMode.ENGLISH)
        val sessionId = session.startListening()

        val request = session.onFinalTranscript(sessionId, "Збережи всі деталі")

        assertEquals("en-US", session.activeSpeechLanguageTag)
        assertEquals("auto", request?.sourceLanguageTag)
        assertEquals("en-US", request?.targetLanguageTag)
    }

    @Test
    fun wholeFieldContextIsPreservedInTransformationRequest() {
        val session = KeyboardSession()
        val sessionId = session.startListening(existingFieldText = "Вже написаний текст")

        val request = session.onFinalTranscript(sessionId, "і нова думка")

        assertEquals("Вже написаний текст", request?.existingFieldText)
    }

    @Test
    fun ignoresCloudResultForWrongSession() {
        val session = KeyboardSession()
        val sessionId = session.startListening()
        session.onFinalTranscript(sessionId, "текст")

        val accepted = session.onTransformationResult(
            TransformationResult(
                sessionId = sessionId + 1,
                styleId = StylePreset.STRUCTURED.id,
                text = "результат",
            ),
        )

        assertNull(accepted)
        assertIs<KeyboardState.Transforming>(session.state)
    }

    @Test
    fun sensitiveFieldInvalidatesActiveCallbacks() {
        val session = KeyboardSession()
        val sessionId = session.startListening()

        session.enterSensitiveField()
        session.onPartialTranscript(sessionId, "не має з'явитися")

        assertIs<KeyboardState.SensitiveField>(session.state)
        assertTrue(session.activeSessionId > sessionId)
    }
}
