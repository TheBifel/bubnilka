package com.thebifel.bubnilka.core.session

import com.thebifel.bubnilka.core.model.InsertionRecord
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.LanguageMode
import com.thebifel.bubnilka.core.model.MAX_TRANSCRIPT_LENGTH
import com.thebifel.bubnilka.core.model.RecoveryAction
import com.thebifel.bubnilka.core.model.StylePreset
import com.thebifel.bubnilka.core.model.TransformationRequest
import com.thebifel.bubnilka.core.model.TransformationResult

class KeyboardSession(
    initialStyle: StylePreset = StylePreset.default,
    initialLanguageMode: LanguageMode = LanguageMode.default,
) {
    var state: KeyboardState = KeyboardState.Idle(initialStyle)
        private set

    var selectedStyle: StylePreset = initialStyle
        private set

    var selectedLanguageMode: LanguageMode = initialLanguageMode
        private set

    private var activeLanguageMode: LanguageMode = initialLanguageMode
    private var activeExistingFieldText: String? = null

    val activeSpeechLanguageTag: String
        get() = activeLanguageMode.sourceLanguageTag

    var activeSessionId: Long = 0
        private set

    var lastInsertion: InsertionRecord? = null
        private set

    fun startListening(existingFieldText: String? = null): Long {
        activeSessionId += 1
        activeLanguageMode = selectedLanguageMode
        activeExistingFieldText = existingFieldText
        state = KeyboardState.Listening(activeSessionId, "", selectedStyle)
        return activeSessionId
    }

    fun onPartialTranscript(sessionId: Long, text: String) {
        val current = state as? KeyboardState.Listening ?: return
        if (sessionId != activeSessionId || sessionId != current.sessionId) return
        val normalized = text.trim().take(MAX_TRANSCRIPT_LENGTH)
        if (normalized.isEmpty()) return
        state = current.copy(partialTranscript = normalized)
    }

    fun onFinalTranscript(sessionId: Long, text: String): TransformationRequest? {
        val current = state as? KeyboardState.Listening ?: return null
        if (sessionId != activeSessionId || sessionId != current.sessionId) return null
        val normalized = text.trim()
        if (normalized.isEmpty() || normalized.length > MAX_TRANSCRIPT_LENGTH) return null

        val request = TransformationRequest(
            sessionId = sessionId,
            rawTranscript = normalized,
            styleId = selectedStyle.id,
            sourceLanguageTag = activeLanguageMode.sourceLanguageTag,
            targetLanguageTag = activeLanguageMode.targetLanguageTag,
            existingFieldText = activeExistingFieldText,
        )
        state = KeyboardState.Transforming(sessionId, normalized, selectedStyle)
        return request
    }

    fun onTransformationResult(result: TransformationResult): String? {
        val current = state as? KeyboardState.Transforming ?: return null
        if (result.sessionId != activeSessionId || result.sessionId != current.sessionId) return null
        if (result.styleId != current.selectedStyle.id) return null
        return result.normalizedTextOrNull()
    }

    fun onInserted(result: TransformationResult): Boolean {
        val current = state as? KeyboardState.Transforming ?: return false
        val normalized = onTransformationResult(result) ?: return false
        lastInsertion = InsertionRecord(
            sessionId = current.sessionId,
            rawTranscript = current.rawTranscript,
            insertedText = normalized,
            styleId = current.selectedStyle.id,
            existingFieldText = activeExistingFieldText,
        )
        state = KeyboardState.Inserted(
            sessionId = current.sessionId,
            rawTranscript = current.rawTranscript,
            transformedText = normalized,
            selectedStyle = current.selectedStyle,
        )
        return true
    }

    fun fail(
        sessionId: Long?,
        failure: KeyboardFailure,
        rawTranscript: String? = currentRawTranscript(),
    ) {
        if (sessionId != null && sessionId != activeSessionId) return
        state = KeyboardState.Failure(
            sessionId = sessionId,
            rawTranscript = rawTranscript,
            failure = failure,
            actions = recoveryActions(failure, rawTranscript),
            selectedStyle = selectedStyle,
        )
    }

    fun selectStyle(style: StylePreset) {
        selectedStyle = style
        state = when (val current = state) {
            is KeyboardState.Idle -> current.copy(selectedStyle = style)
            is KeyboardState.Listening -> current.copy(selectedStyle = style)
            is KeyboardState.Inserted -> current.copy(selectedStyle = style)
            is KeyboardState.Failure -> current.copy(selectedStyle = style)
            else -> current
        }
    }

    fun selectLanguageMode(languageMode: LanguageMode) {
        selectedLanguageMode = languageMode
    }

    fun beginRegeneration(): TransformationRequest? {
        val insertion = lastInsertion ?: return null
        activeSessionId += 1
        val request = TransformationRequest(
            sessionId = activeSessionId,
            rawTranscript = insertion.rawTranscript,
            styleId = selectedStyle.id,
            sourceLanguageTag = selectedLanguageMode.sourceLanguageTag,
            targetLanguageTag = selectedLanguageMode.targetLanguageTag,
            existingFieldText = insertion.existingFieldText,
        )
        activeLanguageMode = selectedLanguageMode
        activeExistingFieldText = insertion.existingFieldText
        state = KeyboardState.Transforming(
            sessionId = request.sessionId,
            rawTranscript = request.rawTranscript,
            selectedStyle = selectedStyle,
        )
        return request
    }

    fun beginRetryTransformation(): TransformationRequest? {
        val failure = state as? KeyboardState.Failure ?: return null
        val rawTranscript = failure.rawTranscript?.takeIf(String::isNotBlank) ?: return null
        activeSessionId += 1
        val request = TransformationRequest(
            sessionId = activeSessionId,
            rawTranscript = rawTranscript,
            styleId = selectedStyle.id,
            sourceLanguageTag = activeLanguageMode.sourceLanguageTag,
            targetLanguageTag = activeLanguageMode.targetLanguageTag,
            existingFieldText = activeExistingFieldText,
        )
        state = KeyboardState.Transforming(
            sessionId = request.sessionId,
            rawTranscript = request.rawTranscript,
            selectedStyle = selectedStyle,
        )
        return request
    }

    fun onRawInserted(rawTranscript: String): Boolean {
        val current = state as? KeyboardState.Failure ?: return false
        if (current.rawTranscript != rawTranscript || rawTranscript.isBlank()) return false
        lastInsertion = InsertionRecord(
            sessionId = activeSessionId,
            rawTranscript = rawTranscript,
            insertedText = rawTranscript,
            styleId = selectedStyle.id,
        )
        state = KeyboardState.Inserted(
            sessionId = activeSessionId,
            rawTranscript = rawTranscript,
            transformedText = rawTranscript,
            selectedStyle = selectedStyle,
        )
        activeExistingFieldText = null
        return true
    }

    fun onUndoSucceeded() {
        invalidateActiveSession()
        lastInsertion = null
        activeExistingFieldText = null
        state = KeyboardState.Idle(selectedStyle)
    }

    fun enterSensitiveField() {
        invalidateActiveSession()
        lastInsertion = null
        activeExistingFieldText = null
        state = KeyboardState.SensitiveField
    }

    fun leaveSensitiveField() {
        if (state is KeyboardState.SensitiveField) {
            state = KeyboardState.Idle(selectedStyle)
        }
    }

    fun reset() {
        invalidateActiveSession()
        lastInsertion = null
        activeExistingFieldText = null
        state = KeyboardState.Idle(selectedStyle)
    }

    private fun currentRawTranscript(): String? = when (val current = state) {
        is KeyboardState.Transforming -> current.rawTranscript
        is KeyboardState.Inserted -> current.rawTranscript
        is KeyboardState.Failure -> current.rawTranscript
        else -> null
    }

    private fun invalidateActiveSession() {
        activeSessionId += 1
    }

    private fun recoveryActions(
        failure: KeyboardFailure,
        rawTranscript: String?,
    ): Set<RecoveryAction> = buildSet {
        if (failure != KeyboardFailure.PERMISSION_DENIED &&
            failure != KeyboardFailure.SPEECH_RECOGNITION_UNAVAILABLE &&
            failure != KeyboardFailure.CLOUD_NOT_CONFIGURED &&
            failure != KeyboardFailure.UNSUPPORTED_MODEL
        ) {
            add(RecoveryAction.RETRY)
        }
        if (!rawTranscript.isNullOrBlank()) add(RecoveryAction.INSERT_RAW)
        add(RecoveryAction.DISMISS)
    }
}
