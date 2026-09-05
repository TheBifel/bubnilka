package com.thebifel.bubnilka.presets

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.thebifel.bubnilka.R
import com.thebifel.bubnilka.core.model.StylePreset
import com.thebifel.bubnilka.storage.StylePresetRepository
import java.util.UUID

class PresetEditActivity : Activity() {
    private val repository by lazy { StylePresetRepository.from(this) }
    private var existing: StylePreset? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preset_edit)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)

        existing = intent.getStringExtra(EXTRA_PRESET_ID)?.let(repository::find)
        findViewById<TextView>(R.id.preset_edit_title).setText(
            if (existing == null) R.string.preset_create_title else R.string.preset_edit_title,
        )
        findViewById<EditText>(R.id.preset_name).setText(existing?.title.orEmpty())
        findViewById<EditText>(R.id.preset_prompt).setText(existing?.instruction.orEmpty())
        findViewById<ImageButton>(R.id.preset_edit_back).setOnClickListener { finish() }
        findViewById<ImageButton>(R.id.preset_save).setOnClickListener { save() }
    }

    private fun save() {
        val nameField = findViewById<EditText>(R.id.preset_name)
        val promptField = findViewById<EditText>(R.id.preset_prompt)
        val name = nameField.text.toString().trim()
        val prompt = promptField.text.toString().trim()
        if (name.isBlank()) {
            nameField.error = getString(R.string.preset_name_required)
            nameField.requestFocus()
            return
        }
        if (prompt.isBlank()) {
            promptField.error = getString(R.string.preset_prompt_required)
            promptField.requestFocus()
            return
        }

        val preset = existing?.copy(title = name, instruction = prompt) ?: StylePreset(
            id = "custom-${UUID.randomUUID()}",
            title = name,
            instruction = prompt,
        )
        repository.save(preset)
        finish()
    }

    companion object {
        const val EXTRA_PRESET_ID = "preset_id"
    }
}
