package com.thebifel.bubnilka.cloud

import android.os.SystemClock
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.model.TransformationRequest
import com.thebifel.bubnilka.core.model.TransformationResult
import com.thebifel.bubnilka.core.port.CloudTransformException
import com.thebifel.bubnilka.core.port.CloudTransformProvider
import com.thebifel.bubnilka.core.prompt.PromptBuilder
import com.thebifel.bubnilka.runtime.AndroidFailureMapper
import com.thebifel.bubnilka.runtime.BubnilkaLog
import kotlinx.coroutines.CancellationException

class FirebaseGeminiTransformProvider(
    private val model: GenerativeModel,
) : CloudTransformProvider {
    override suspend fun transform(request: TransformationRequest): TransformationResult {
        val startedAt = SystemClock.elapsedRealtime()
        BubnilkaLog.state("gemini_generate_started")
        try {
            val response = model.generateContent(PromptBuilder.build(request))
            val text = response.text ?: throw CloudTransformException(KeyboardFailure.INVALID_CLOUD_RESPONSE)
            return TransformationResult(
                sessionId = request.sessionId,
                styleId = request.styleId,
                text = text,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (known: CloudTransformException) {
            throw known
        } catch (failure: Throwable) {
            throw CloudTransformException(AndroidFailureMapper.mapCloud(failure), failure)
        } finally {
            BubnilkaLog.timing(
                category = "gemini_generate",
                durationMillis = SystemClock.elapsedRealtime() - startedAt,
            )
        }
    }

    companion object {
        fun create(app: FirebaseApp, modelName: String): FirebaseGeminiTransformProvider {
            require(modelName in FirebaseConfiguration.ALLOWED_FREE_MODELS)
            val model = Firebase.ai(
                app = app,
                backend = GenerativeBackend.googleAI(),
            ).generativeModel(modelName)
            return FirebaseGeminiTransformProvider(model)
        }
    }
}

class UnconfiguredCloudTransformProvider : CloudTransformProvider {
    override suspend fun transform(request: TransformationRequest): TransformationResult {
        throw CloudTransformException(KeyboardFailure.CLOUD_NOT_CONFIGURED)
    }
}
