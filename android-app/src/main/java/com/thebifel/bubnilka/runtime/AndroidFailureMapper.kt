package com.thebifel.bubnilka.runtime

import com.google.firebase.ai.type.QuotaExceededException
import com.thebifel.bubnilka.core.model.KeyboardFailure
import java.io.IOException
import java.util.concurrent.CancellationException

object AndroidFailureMapper {
    fun mapCloud(throwable: Throwable): KeyboardFailure = when (throwable) {
        is CancellationException -> KeyboardFailure.CANCELLED
        is QuotaExceededException -> KeyboardFailure.QUOTA_EXCEEDED
        is IOException -> KeyboardFailure.NETWORK_UNAVAILABLE
        else -> mapMessage(throwable.message)
    }

    fun mapHttpStatus(status: Int): KeyboardFailure = when (status) {
        403 -> KeyboardFailure.CLOUD_NOT_CONFIGURED
        404 -> KeyboardFailure.UNSUPPORTED_MODEL
        408 -> KeyboardFailure.NETWORK_UNAVAILABLE
        429 -> KeyboardFailure.QUOTA_EXCEEDED
        else -> KeyboardFailure.UNEXPECTED
    }

    private fun mapMessage(message: String?): KeyboardFailure {
        val normalized = message.orEmpty().lowercase()
        return when {
            "429" in normalized || "quota" in normalized || "resource exhausted" in normalized ->
                KeyboardFailure.QUOTA_EXCEEDED
            "404" in normalized || "model not found" in normalized || "not supported" in normalized ->
                KeyboardFailure.UNSUPPORTED_MODEL
            "403" in normalized || "permission denied" in normalized || "app check" in normalized ->
                KeyboardFailure.CLOUD_NOT_CONFIGURED
            "timeout" in normalized || "network" in normalized || "offline" in normalized ->
                KeyboardFailure.NETWORK_UNAVAILABLE
            else -> KeyboardFailure.UNEXPECTED
        }
    }
}
