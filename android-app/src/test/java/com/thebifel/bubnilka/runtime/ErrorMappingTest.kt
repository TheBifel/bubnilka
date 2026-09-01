package com.thebifel.bubnilka.runtime

import android.speech.SpeechRecognizer
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.speech.AndroidOnDeviceSpeechToTextProvider
import java.io.IOException
import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorMappingTest {
    @Test
    fun mapsAndroidSpeechErrorsToStableCategories() {
        assertEquals(
            KeyboardFailure.PERMISSION_DENIED,
            AndroidOnDeviceSpeechToTextProvider.mapRecognitionError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS),
        )
        assertEquals(
            KeyboardFailure.NO_SPEECH,
            AndroidOnDeviceSpeechToTextProvider.mapRecognitionError(SpeechRecognizer.ERROR_NO_MATCH),
        )
        assertEquals(
            KeyboardFailure.UNSUPPORTED_ON_DEVICE_RECOGNITION,
            AndroidOnDeviceSpeechToTextProvider.mapRecognitionError(SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE),
        )
    }

    @Test
    fun mapsCloudTransportQuotaAndModelErrors() {
        assertEquals(KeyboardFailure.NETWORK_UNAVAILABLE, AndroidFailureMapper.mapCloud(IOException()))
        assertEquals(KeyboardFailure.CANCELLED, AndroidFailureMapper.mapCloud(CancellationException()))
        assertEquals(KeyboardFailure.QUOTA_EXCEEDED, AndroidFailureMapper.mapHttpStatus(429))
        assertEquals(KeyboardFailure.UNSUPPORTED_MODEL, AndroidFailureMapper.mapHttpStatus(404))
        assertEquals(KeyboardFailure.CLOUD_NOT_CONFIGURED, AndroidFailureMapper.mapHttpStatus(403))
        assertEquals(KeyboardFailure.UNEXPECTED, AndroidFailureMapper.mapHttpStatus(500))
    }
}
