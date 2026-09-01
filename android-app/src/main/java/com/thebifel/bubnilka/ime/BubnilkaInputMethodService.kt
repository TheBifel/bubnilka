package com.thebifel.bubnilka.ime

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.TextView
import com.thebifel.bubnilka.R
import com.thebifel.bubnilka.cloud.CloudProviderFactory
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.RecoveryAction
import com.thebifel.bubnilka.core.model.StylePreset
import com.thebifel.bubnilka.core.session.KeyboardSession
import com.thebifel.bubnilka.core.session.VoiceFlow
import com.thebifel.bubnilka.runtime.BubnilkaLog
import com.thebifel.bubnilka.runtime.CoroutineOwner
import com.thebifel.bubnilka.speech.AndroidSpeechToTextProvider
import com.thebifel.bubnilka.storage.ConsentRepository

class BubnilkaInputMethodService : InputMethodService() {
    private val coroutineOwner = CoroutineOwner()
    private var session = KeyboardSession()
    private var voiceFlow: VoiceFlow? = null
    private var speechProvider: AndroidSpeechToTextProvider? = null
    private var keyboardView: View? = null
    private val consentRepository: ConsentRepository by lazy { ConsentRepository.from(this) }
    private var cloudConsentAccepted: Boolean = false

    @SuppressLint("InflateParams") // InputMethodService does not expose a parent container here.
    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        cloudConsentAccepted = consentRepository.isAccepted()
        val speech = AndroidSpeechToTextProvider(this)
        speechProvider = speech
        val insertion = AndroidTextInsertionPort {
            currentInputConnection?.let(::FrameworkCursorConnection)
        }
        voiceFlow = VoiceFlow(
            session = session,
            speech = speech,
            cloud = CloudProviderFactory.create(this),
            insertion = insertion,
            scope = coroutineOwner.scope,
            onStateChanged = { state -> view.post { render(state) } },
        )

        bindStyle(view, R.id.style_structured_button, StylePreset.STRUCTURED)
        bindStyle(view, R.id.style_concise_button, StylePreset.CONCISE)
        bindStyle(view, R.id.style_professional_button, StylePreset.PROFESSIONAL)
        bindStyle(view, R.id.style_friendly_button, StylePreset.FRIENDLY)
        bindStyle(view, R.id.style_viking_button, StylePreset.VIKING)

        view.findViewById<Button>(R.id.microphone_button).setOnClickListener {
            if (!cloudConsentAccepted) return@setOnClickListener
            when (session.state) {
                is KeyboardState.Listening -> voiceFlow?.cancel()
                is KeyboardState.SensitiveField -> Unit
                else -> voiceFlow?.start()
            }
        }
        view.findViewById<Button>(R.id.undo_button).setOnClickListener {
            voiceFlow?.undo()
        }
        view.findViewById<Button>(R.id.regenerate_button).setOnClickListener {
            if (cloudConsentAccepted) voiceFlow?.regenerate()
        }
        view.findViewById<Button>(R.id.retry_button).setOnClickListener {
            voiceFlow?.retry()
        }
        view.findViewById<Button>(R.id.insert_raw_button).setOnClickListener {
            voiceFlow?.insertRaw()
        }
        view.findViewById<Button>(R.id.switch_keyboard_button).setOnClickListener {
            if (shouldOfferSwitchingToNextInputMethod()) {
                switchToNextInputMethod(false)
            } else {
                getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            }
        }
        applyEditorPolicy(currentInputEditorInfo)
        render(session.state)
        BubnilkaLog.state("keyboard_view_created")
        return view
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        if (!restarting) session.reset()
        applyEditorPolicy(attribute)
        render(session.state)
    }

    override fun onFinishInput() {
        speechProvider?.close()
        session.reset()
        render(session.state)
        super.onFinishInput()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        cloudConsentAccepted = consentRepository.isAccepted()
        applyEditorPolicy(info)
        render(session.state)
    }

    override fun onDestroy() {
        voiceFlow?.close()
        speechProvider = null
        keyboardView = null
        coroutineOwner.close()
        super.onDestroy()
    }

    private fun bindStyle(view: View, buttonId: Int, style: StylePreset) {
        view.findViewById<Button>(buttonId).setOnClickListener {
            session.selectStyle(style)
            render(session.state)
        }
    }

    private fun applyEditorPolicy(editorInfo: EditorInfo?) {
        if (editorInfo != null && SensitiveFieldPolicy.isSensitive(editorInfo.inputType)) {
            speechProvider?.close()
            session.enterSensitiveField()
        } else {
            session.leaveSensitiveField()
        }
    }

    private fun render(state: KeyboardState) {
        val view = keyboardView ?: return
        val status = view.findViewById<TextView>(R.id.keyboard_status)
        val transcript = view.findViewById<TextView>(R.id.transcript_text)
        val microphone = view.findViewById<Button>(R.id.microphone_button)
        val retry = view.findViewById<Button>(R.id.retry_button)
        val insertRaw = view.findViewById<Button>(R.id.insert_raw_button)

        retry.visibility = View.GONE
        insertRaw.visibility = View.GONE

        when (state) {
            is KeyboardState.Idle -> {
                status.setText(R.string.status_ready)
                transcript.setText(R.string.transcript_placeholder)
                microphone.isEnabled = true
            }
            is KeyboardState.Listening -> {
                status.setText(R.string.status_listening)
                transcript.text = state.partialTranscript.ifBlank { getString(R.string.transcript_listening) }
                microphone.isEnabled = true
            }
            is KeyboardState.Transforming -> {
                status.setText(R.string.status_transforming)
                transcript.text = state.rawTranscript
                microphone.isEnabled = false
            }
            is KeyboardState.Inserted -> {
                status.setText(R.string.status_inserted)
                transcript.text = state.transformedText
                microphone.isEnabled = true
            }
            is KeyboardState.Failure -> {
                status.setText(failureMessage(state.failure))
                transcript.text = state.rawTranscript ?: getString(R.string.transcript_placeholder)
                microphone.isEnabled = true
                retry.visibility = if (RecoveryAction.RETRY in state.actions) View.VISIBLE else View.GONE
                insertRaw.visibility = if (RecoveryAction.INSERT_RAW in state.actions) View.VISIBLE else View.GONE
            }
            KeyboardState.SensitiveField -> {
                status.setText(R.string.status_sensitive_field)
                transcript.setText(R.string.transcript_sensitive_field)
                microphone.isEnabled = false
            }
        }

        val selectedStyle = when (state) {
            is KeyboardState.Idle -> state.selectedStyle
            is KeyboardState.Listening -> state.selectedStyle
            is KeyboardState.Transforming -> state.selectedStyle
            is KeyboardState.Inserted -> state.selectedStyle
            is KeyboardState.Failure -> state.selectedStyle
            KeyboardState.SensitiveField -> session.selectedStyle
        }
        renderStyle(view, R.id.style_structured_button, selectedStyle == StylePreset.STRUCTURED)
        renderStyle(view, R.id.style_concise_button, selectedStyle == StylePreset.CONCISE)
        renderStyle(view, R.id.style_professional_button, selectedStyle == StylePreset.PROFESSIONAL)
        renderStyle(view, R.id.style_friendly_button, selectedStyle == StylePreset.FRIENDLY)
        renderStyle(view, R.id.style_viking_button, selectedStyle == StylePreset.VIKING)

        val mayEditInsertion = session.lastInsertion != null &&
            state !is KeyboardState.Transforming &&
            state !is KeyboardState.Listening &&
            state !is KeyboardState.SensitiveField
        view.findViewById<Button>(R.id.undo_button).isEnabled = mayEditInsertion
        view.findViewById<Button>(R.id.regenerate_button).isEnabled = mayEditInsertion && cloudConsentAccepted

        if (!cloudConsentAccepted && state !is KeyboardState.SensitiveField) {
            status.setText(R.string.status_consent_required)
            microphone.isEnabled = false
            retry.visibility = View.GONE
        }
    }

    private fun renderStyle(view: View, buttonId: Int, selected: Boolean) {
        view.findViewById<Button>(buttonId).apply {
            isSelected = selected
            alpha = if (selected) 1f else 0.55f
        }
    }

    private fun failureMessage(failure: KeyboardFailure): Int = when (failure) {
        KeyboardFailure.SPEECH_RECOGNITION_UNAVAILABLE -> R.string.error_speech_unavailable
        KeyboardFailure.PERMISSION_DENIED -> R.string.error_microphone_permission
        KeyboardFailure.NO_SPEECH -> R.string.error_no_speech
        KeyboardFailure.CLOUD_NOT_CONFIGURED -> R.string.error_cloud_not_configured
        KeyboardFailure.NETWORK_UNAVAILABLE -> R.string.error_network
        KeyboardFailure.QUOTA_EXCEEDED -> R.string.error_quota
        KeyboardFailure.UNSUPPORTED_MODEL -> R.string.error_unsupported_model
        KeyboardFailure.INVALID_CLOUD_RESPONSE -> R.string.error_invalid_response
        KeyboardFailure.UNSAFE_REPLACEMENT -> R.string.error_unsafe_replacement
        KeyboardFailure.CANCELLED -> R.string.status_cancelled
        KeyboardFailure.RECOGNITION_ERROR,
        KeyboardFailure.UNEXPECTED,
        -> R.string.error_unexpected
    }
}
