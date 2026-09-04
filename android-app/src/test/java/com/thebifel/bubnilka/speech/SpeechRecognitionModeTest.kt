package com.thebifel.bubnilka.speech

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpeechRecognitionModeTest {
    @Test
    fun prefersOnDeviceRecognitionWhenBothServicesExist() {
        assertEquals(
            RecognitionMode.ON_DEVICE,
            selectRecognitionMode(onDeviceAvailable = true, systemAvailable = true),
        )
    }

    @Test
    fun fallsBackToSystemRecognitionWhenOnDeviceServiceIsMissing() {
        assertEquals(
            RecognitionMode.SYSTEM,
            selectRecognitionMode(onDeviceAvailable = false, systemAvailable = true),
        )
    }

    @Test
    fun reportsUnavailableWhenNoRecognitionServiceExists() {
        assertNull(selectRecognitionMode(onDeviceAvailable = false, systemAvailable = false))
    }

    @Test
    fun requestsAutomaticLanguageDetectionFromAndroid14() {
        assertEquals(false, supportsAutomaticLanguageDetection(sdkInt = 33))
        assertEquals(true, supportsAutomaticLanguageDetection(sdkInt = 34))
        assertEquals(true, supportsAutomaticLanguageDetection(sdkInt = 36))
    }

    @Test
    fun normalizesRecognizerRmsIntoStableVisualRange() {
        assertEquals(0f, normalizeRmsDb(Float.NaN), 0.001f)
        assertEquals(0f, normalizeRmsDb(-2f), 0.001f)
        assertEquals(0.5f, normalizeRmsDb(4f), 0.001f)
        assertEquals(1f, normalizeRmsDb(10f), 0.001f)
        assertEquals(1f, normalizeRmsDb(30f), 0.001f)
    }
}
