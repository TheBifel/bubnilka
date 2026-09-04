package com.thebifel.bubnilka.ime

import android.annotation.SuppressLint
import android.inputmethodservice.InputMethodService
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import com.thebifel.bubnilka.R
import com.thebifel.bubnilka.cloud.CloudProviderFactory
import com.thebifel.bubnilka.core.model.KeyboardFailure
import com.thebifel.bubnilka.core.model.KeyboardState
import com.thebifel.bubnilka.core.model.LanguageMode
import com.thebifel.bubnilka.core.model.RecoveryAction
import com.thebifel.bubnilka.core.model.StylePreset
import com.thebifel.bubnilka.core.session.KeyboardSession
import com.thebifel.bubnilka.core.session.VoiceFlow
import com.thebifel.bubnilka.debug.UiInspector
import com.thebifel.bubnilka.runtime.BubnilkaLog
import com.thebifel.bubnilka.runtime.CoroutineOwner
import com.thebifel.bubnilka.speech.AndroidSpeechToTextProvider
import com.thebifel.bubnilka.storage.ConsentRepository

class BubnilkaInputMethodService : InputMethodService() {
    private val coroutineOwner = CoroutineOwner()
    private var session = KeyboardSession()
    private var voiceFlow: VoiceFlow? = null
    private var speechProvider: AndroidSpeechToTextProvider? = null
    private var insertionPort: AndroidTextInsertionPort? = null
    private var keyboardView: View? = null
    private val consentRepository: ConsentRepository by lazy { ConsentRepository.from(this) }
    private var cloudConsentAccepted: Boolean = false
    private var includeFieldContext: Boolean = true
    private var previewAutoFollow: Boolean = true
    private var previewUserInteracting: Boolean = false
    private var previewSessionId: Long? = null
    private var backspaceRepeatView: View? = null
    private var backspaceRepeatCount: Int = 0
    private val backspaceRepeatAction = object : Runnable {
        override fun run() {
            val view = backspaceRepeatView ?: return
            insertionPort?.backspace()
            backspaceRepeatCount += 1
            view.postDelayed(this, backspaceRepeatDelay(backspaceRepeatCount))
        }
    }

    @SuppressLint("InflateParams") // InputMethodService does not expose a parent container here.
    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        keyboardView = view
        UiInspector.attach(view)
        cloudConsentAccepted = consentRepository.isAccepted()
        val speech = AndroidSpeechToTextProvider(this)
        speechProvider = speech
        val insertion = AndroidTextInsertionPort {
            currentInputConnection?.let(::FrameworkCursorConnection)
        }
        insertionPort = insertion
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
        session.selectLanguageMode(loadLanguageMode())
        includeFieldContext = loadFieldContextPreference()
        configurePreviewScrolling(view.findViewById(R.id.transcript_scroll))

        view.findViewById<View>(R.id.microphone_button).setOnClickListener {
            if (!cloudConsentAccepted) return@setOnClickListener
            when (session.state) {
                is KeyboardState.Listening -> voiceFlow?.cancel()
                is KeyboardState.SensitiveField -> Unit
                else -> voiceFlow?.start(includeFieldContext = includeFieldContext)
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
        view.findViewById<View>(R.id.switch_keyboard_button).setOnClickListener {
            if (shouldOfferSwitchingToNextInputMethod()) {
                switchToNextInputMethod(false)
            } else {
                getSystemService(InputMethodManager::class.java).showInputMethodPicker()
            }
        }
        configureBackspace(view.findViewById(R.id.backspace_button))
        view.findViewById<View>(R.id.language_settings_button).setOnClickListener(::showLanguageMenu)
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
        stopBackspaceRepeat()
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
        keyboardView?.post(::startListeningWhenReady)
    }

    override fun onDestroy() {
        stopBackspaceRepeat()
        voiceFlow?.close()
        speechProvider = null
        insertionPort = null
        UiInspector.detach(keyboardView)
        keyboardView = null
        coroutineOwner.close()
        super.onDestroy()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configureBackspace(button: View) {
        button.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    stopBackspaceRepeat()
                    backspaceRepeatView = view
                    backspaceRepeatCount = 0
                    view.isPressed = true
                    insertionPort?.backspace()
                    view.postDelayed(backspaceRepeatAction, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> {
                    val clicked = event.actionMasked == MotionEvent.ACTION_UP
                    stopBackspaceRepeat()
                    view.isPressed = false
                    if (clicked) view.performClick()
                }
            }
            true
        }
    }

    private fun stopBackspaceRepeat() {
        backspaceRepeatView?.removeCallbacks(backspaceRepeatAction)
        backspaceRepeatView = null
        backspaceRepeatCount = 0
    }

    private fun backspaceRepeatDelay(repeatCount: Int): Long = when {
        repeatCount < 4 -> 130L
        repeatCount < 10 -> 90L
        else -> 55L
    }

    private fun bindStyle(view: View, buttonId: Int, style: StylePreset) {
        view.findViewById<Button>(buttonId).setOnClickListener {
            session.selectStyle(style)
            render(session.state)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun configurePreviewScrolling(scrollView: ScrollView) {
        scrollView.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> previewUserInteracting = true
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_CANCEL,
                -> scrollView.post {
                    previewUserInteracting = false
                    previewAutoFollow = !scrollView.canScrollVertically(1)
                }
            }
            false
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
        val transcriptScroll = view.findViewById<ScrollView>(R.id.transcript_scroll)
        val microphone = view.findViewById<View>(R.id.microphone_button)
        val retry = view.findViewById<Button>(R.id.retry_button)
        val insertRaw = view.findViewById<Button>(R.id.insert_raw_button)
        val recoveryActions = view.findViewById<View>(R.id.recovery_actions)

        retry.visibility = View.GONE
        insertRaw.visibility = View.GONE
        recoveryActions.visibility = View.GONE

        if (state is KeyboardState.Listening && previewSessionId != state.sessionId) {
            previewSessionId = state.sessionId
            previewAutoFollow = true
        } else if (state is KeyboardState.Idle) {
            previewSessionId = null
            previewAutoFollow = true
        }

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
                recoveryActions.visibility =
                    if (retry.visibility == View.VISIBLE || insertRaw.visibility == View.VISIBLE) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
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

        transcriptScroll.post {
            if (previewAutoFollow && !previewUserInteracting) {
                transcriptScroll.fullScroll(View.FOCUS_DOWN)
            }
        }

        view.findViewById<View>(R.id.language_settings_button).contentDescription = getString(
            if (includeFieldContext) {
                R.string.language_settings_selected_with_field
            } else {
                R.string.language_settings_selected
            },
            getString(languageModeTitle(session.selectedLanguageMode)),
        )
        microphone.contentDescription = getString(
            if (state is KeyboardState.Listening) R.string.cancel_dictation else R.string.start_dictation,
        )

        val mayEditInsertion = session.lastInsertion != null &&
            state !is KeyboardState.Transforming &&
            state !is KeyboardState.Listening &&
            state !is KeyboardState.SensitiveField
        view.findViewById<View>(R.id.post_insert_actions).visibility =
            if (mayEditInsertion) View.VISIBLE else View.GONE
        view.findViewById<Button>(R.id.undo_button).isEnabled = mayEditInsertion
        view.findViewById<Button>(R.id.regenerate_button).isEnabled = mayEditInsertion && cloudConsentAccepted

        if (!cloudConsentAccepted && state !is KeyboardState.SensitiveField) {
            status.setText(R.string.status_consent_required)
            microphone.isEnabled = false
            retry.visibility = View.GONE
            recoveryActions.visibility = View.GONE
        }
    }

    private fun renderStyle(view: View, buttonId: Int, selected: Boolean) {
        renderSelection(view, buttonId, selected)
    }

    private fun renderSelection(view: View, buttonId: Int, selected: Boolean) {
        view.findViewById<Button>(buttonId).isSelected = selected
    }

    private fun startListeningWhenReady() {
        if (!cloudConsentAccepted || session.state is KeyboardState.SensitiveField) return
        if (session.state is KeyboardState.Listening || session.state is KeyboardState.Transforming) return
        voiceFlow?.start(includeFieldContext = includeFieldContext)
    }

    private fun showLanguageMenu(anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(LANGUAGE_GROUP, MENU_UKRAINIAN, 0, R.string.language_ukrainian)
            menu.add(LANGUAGE_GROUP, MENU_ENGLISH, 1, R.string.language_english)
            menu.add(LANGUAGE_GROUP, MENU_UK_TO_EN, 2, R.string.language_uk_to_en_full)
            menu.add(LANGUAGE_GROUP, MENU_EN_TO_UK, 3, R.string.language_en_to_uk_full)
            menu.setGroupCheckable(LANGUAGE_GROUP, true, true)
            menu.findItem(menuId(session.selectedLanguageMode)).isChecked = true
            menu.add(0, MENU_FIELD_CONTEXT, 4, R.string.field_context_menu).apply {
                isCheckable = true
                isChecked = includeFieldContext
            }
            setOnMenuItemClickListener { item ->
                if (item.itemId == MENU_FIELD_CONTEXT) {
                    updateFieldContext(!includeFieldContext)
                    true
                } else {
                    languageMode(item.itemId)?.let(::updateLanguageMode) != null
                }
            }
            show()
        }
    }

    private fun updateLanguageMode(languageMode: LanguageMode) {
        val shouldStart = session.state !is KeyboardState.Transforming &&
            session.state !is KeyboardState.SensitiveField
        if (session.state is KeyboardState.Listening) voiceFlow?.cancel()
        session.selectLanguageMode(languageMode)
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
            .putString(KEY_LANGUAGE_MODE, languageMode.id)
            .apply()
        render(session.state)
        if (shouldStart) startListeningWhenReady()
    }

    private fun updateFieldContext(enabled: Boolean) {
        val shouldStart = session.state !is KeyboardState.Transforming &&
            session.state !is KeyboardState.SensitiveField
        if (session.state is KeyboardState.Listening) voiceFlow?.cancel()
        includeFieldContext = enabled
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).edit()
            .putBoolean(KEY_INCLUDE_FIELD_CONTEXT, enabled)
            .apply()
        render(session.state)
        if (shouldStart) startListeningWhenReady()
    }

    private fun loadLanguageMode(): LanguageMode = LanguageMode.fromId(
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getString(KEY_LANGUAGE_MODE, null),
    ) ?: LanguageMode.default

    private fun loadFieldContextPreference(): Boolean =
        getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE).getBoolean(KEY_INCLUDE_FIELD_CONTEXT, true)

    private fun menuId(languageMode: LanguageMode): Int = when (languageMode) {
        LanguageMode.UKRAINIAN -> MENU_UKRAINIAN
        LanguageMode.ENGLISH -> MENU_ENGLISH
        LanguageMode.UKRAINIAN_TO_ENGLISH -> MENU_UK_TO_EN
        LanguageMode.ENGLISH_TO_UKRAINIAN -> MENU_EN_TO_UK
    }

    private fun languageMode(menuId: Int): LanguageMode? = when (menuId) {
        MENU_UKRAINIAN -> LanguageMode.UKRAINIAN
        MENU_ENGLISH -> LanguageMode.ENGLISH
        MENU_UK_TO_EN -> LanguageMode.UKRAINIAN_TO_ENGLISH
        MENU_EN_TO_UK -> LanguageMode.ENGLISH_TO_UKRAINIAN
        else -> null
    }

    private fun languageModeTitle(languageMode: LanguageMode): Int = when (languageMode) {
        LanguageMode.UKRAINIAN -> R.string.language_ukrainian
        LanguageMode.ENGLISH -> R.string.language_english
        LanguageMode.UKRAINIAN_TO_ENGLISH -> R.string.language_uk_to_en_full
        LanguageMode.ENGLISH_TO_UKRAINIAN -> R.string.language_en_to_uk_full
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

    private companion object {
        const val PREFERENCES_NAME = "bubnilka_keyboard"
        const val KEY_LANGUAGE_MODE = "language_mode"
        const val KEY_INCLUDE_FIELD_CONTEXT = "include_field_context"
        const val LANGUAGE_GROUP = 1
        const val MENU_UKRAINIAN = 101
        const val MENU_ENGLISH = 102
        const val MENU_UK_TO_EN = 103
        const val MENU_EN_TO_UK = 104
        const val MENU_FIELD_CONTEXT = 201
    }
}
